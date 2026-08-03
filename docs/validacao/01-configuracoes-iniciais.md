# 01 — Configurações iniciais

Tudo o que precisa estar cadastrado/configurado **antes** de tentar gerar a primeira entrega. Cada seção segue o mesmo template: caminho, objetivo, campos, exemplo, validação, possíveis erros e critérios de aceite.

> Ordem importa. Cada cadastro depende do anterior. Não pule etapas.

---

## Mapa rápido das dependências

```text
infra/env (1)
   └─► Produto (2)
          ├─► Aba GitHub (3)         ← integração de download
          ├─► Aba Jenkins (4)        ← integração de CI (opcional)
          ├─► Aba Módulos (5)        ← obrigatório
          └─► Catálogo funcional (6) ← Domínios + Funcionalidades por produto
   └─► Cliente (7)
          ├─► Aba Contatos (8)
          ├─► Aba Produtos contratados (9)
          │     └─► Módulos contratados + versão atual
          ├─► Aba Funcionalidades (10)
          └─► Aba Config. entrega (11) ← PASTA/FTP/SFTP/BUCKET
   └─► Release (12)                  ← PUBLICADA antes da entrega
          └─► Artefatos (13)         ← upload ou Sync GitHub
```

---

## 1. Configurações de infraestrutura (backend)

Não é tela — é configuração do `application-dev.yml` ou variáveis de ambiente do backend. Precisa estar correto antes de subir o backend.

### Caminho
- Arquivo: `nexus-portal-api/application/src/main/resources/application-dev.yml`
- Ou exportar como env var antes de `./mvnw spring-boot:run`.

### Variáveis relevantes

| Variável | Default dev | Para que serve |
|---|---|---|
| `ARTEFATOS_DIR` | `./storage/artefatos` | Cache de artefatos baixados do GitHub e uploads manuais |
| `ENTREGAS_DIR` | `./storage/entregas` | Onde o ZIP final de cada entrega é gravado |
| `PUBLICACOES_DIR` | `./storage/publicacoes` | Pacotes que serão publicados em destinos remotos |
| `RELEASE_ORCHESTRATOR_ENCRYPTION_KEY` | (chave DEV) | AES-GCM 32 bytes em base64 para cifrar senhas de FTP/SFTP/BUCKET |
| `RELEASE_ORCHESTRATOR_WEBHOOKS_JENKINS_SECRET` | (vazio) | Shared secret do webhook Jenkins. Sem isso, webhook devolve 403 |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | bate com `infra/docker/.env` | Conexão Postgres |

### Como validar
```bash
# Pastas existem e são graváveis
ls -ld nexus-portal-api/storage/{artefatos,entregas,publicacoes}

# Backend está vivo
curl -fs http://localhost:8080/actuator/health | jq

# Health específicos do release-orchestrator
curl -s http://localhost:8080/actuator/health/storage | jq '.status'   # → "UP"
curl -s http://localhost:8080/actuator/health/github | jq '.status'    # UP só depois de produto com PAT
curl -s http://localhost:8080/actuator/health/jenkins | jq '.status'   # UP só depois de produto com Jenkins
```

### Critérios de aceite
- [ ] `/actuator/health` → status global `UP`.
- [ ] `/actuator/health/storage` → `UP`.
- [ ] As 3 pastas de storage existem e o backend grava nelas sem erro de permissão.
- [ ] `RELEASE_ORCHESTRATOR_ENCRYPTION_KEY` exportada se for usar destino remoto.

---

## 2. Cadastro de Produto

Produto é a unidade comercializável vendida aos clientes (ex.: Suite Nexus). Cada produto agrupa módulos, integrações de CI e o catálogo funcional.

### Caminho
Sidebar → **Release Orchestrator → Produtos** → botão **Novo produto** (canto sup. direito).

### Objetivo
Registrar um produto cujas releases serão entregues a clientes. A sigla do produto vira parte do nome dos pacotes ZIP.

### Quando usar
Toda vez que um novo produto for incorporado ao Release Orchestrator.

### Campos (aba Geral)

| Campo | Obrigatório | Exemplo | Observação |
|---|---|---|---|
| Sigla | Sim | `Nexus` | CAIXA-ALTA, sem espaços, ≤ 20 caracteres. Vira parte do nome do pacote |
| Nome | Sim | `Suite Nexus` | Nome comercial |
| Cor | Não | `#2563eb` | Badge na UI |
| Ativo | Sim | ✅ | Produtos inativos não podem ter novas releases |

### O que acontece ao salvar
- `POST /api/v1/release-orchestrator/produtos`.
- Linha em `tb_produto_rh` com `gen_random_uuid()`.
- UI redireciona para o detalhe do produto, abrindo as abas Geral, GitHub, Jenkins e Módulos.

### Como validar
1. Lista de produtos exibe o novo card.
2. Banco:
   ```sql
   SELECT id, sigla, nome, ativo FROM tb_produto_rh WHERE sigla = 'Nexus';
   ```
3. Sigla é mostrada como prefixo nos nomes de pacotes esperados (verificar na seção 11.5 do fluxo).

### Possíveis erros

| Mensagem | Causa | Como corrigir |
|---|---|---|
| `Sigla já cadastrada` | Já existe `tb_produto_rh` com a mesma sigla (UNIQUE) | Use outra sigla ou edite o produto existente |
| `Sigla obrigatória` | Campo vazio | Preencher |
| `Sigla deve conter apenas letras, números e hífen` | Caracteres inválidos | Remover espaços/acentos |

### Critérios de aceite
- [ ] Produto aparece na lista após salvar.
- [ ] `tb_produto_rh.sigla = 'Nexus'` e `ativo = TRUE`.
- [ ] Botão **Salvar** fica desabilitado enquanto faltam campos obrigatórios.

---

## 3. Aba GitHub do produto

Configura de onde o portal vai baixar os artefatos (WAR/JAR/ZIP) e calcular o delta BANCO/KETTLE.

### Caminho
Detalhe do produto → aba **GitHub**.

### Quando usar
Sempre que o produto for buildado por CI e publicar assets em GitHub Releases. Sem isso, o portal aceita só **upload manual** de artefatos.

### Campos

| Campo | Obrigatório | Exemplo | Observação |
|---|---|---|---|
| Repositório | Sim (se ativar GitHub) | `nexus/nexus-suite` | `owner/repo`; aceita URL completa (será normalizada) |
| Branch padrão | Sim | `main` | Base do compare API |
| Regex de tag | Sim | `^v\d+\.\d+\.\d+$` | Tags que entram no delta |
| Token | Sim | `ghp_…` | PAT com escopo `repo`. Armazenado em texto plano no MVP |

### O que acontece ao salvar
- `PATCH /produtos/{id}` atualiza `repositorio_github`, `branch_padrao`, `padrao_tag`, `github_token` em `tb_produto_rh`.
- Botão **Testar conexão** chama `GET /produtos/{id}/testar-github` → valida o PAT e devolve as últimas 5 releases (ou mensagem amigável se zero).

### Como validar
- Mensagem **verde** "Conexão OK" e prévia das releases recentes.
- Se ainda não houver release no repo: mensagem "Nenhuma release encontrada" (sucesso parcial, esperado).
- Banco:
  ```sql
  SELECT repositorio_github, branch_padrao, padrao_tag
  FROM tb_produto_rh WHERE sigla = 'Nexus';
  ```

### Possíveis erros

| Mensagem | Causa | Como corrigir |
|---|---|---|
| `401 Unauthorized` | PAT inválido ou expirado | Gerar novo PAT em github.com → Settings → Tokens |
| `404 Not Found` | Repositório errado ou PAT sem acesso | Conferir owner/repo + escopo `repo` |
| `Regex inválido` | Padrão de tag mal-formado | Usar `^v\d+\.\d+\.\d+$` ou variação válida |

### Critérios de aceite
- [ ] **Testar conexão** retorna verde.
- [ ] PAT cifrado/persistido (UI não devolve o valor em GET subsequente).
- [ ] `/actuator/health/github` → `UP` após salvar.

---

## 4. Aba Jenkins do produto (opcional)

Permite o portal: (1) ler o status do último build via webhook, (2) exibir badge no detalhe da release com link para o build no Jenkins. Disparar build a partir do portal **não está implementado** no MVP.

### Caminho
Detalhe do produto → aba **Jenkins**.

### Campos

| Campo | Obrigatório (se ativar Jenkins) | Exemplo | Observação |
|---|---|---|---|
| URL base | Sim | `http://localhost:8090` | Sem `/` final |
| Job | Sim | `nexus-suite-build` | Nome exato do job |
| Usuário | Sim | `admin` | Usuário Jenkins |
| API token | Sim | (gerar em Jenkins → user → Configure → API Token) | Texto plano no MVP |
| Modo de trigger | Sim | `BUILD_ON_TAG` | Ou `MANUAL` |

### O que acontece ao salvar
- `PATCH /produtos/{id}` atualiza colunas `jenkins_*` em `tb_produto_rh`.
- **Testar conexão** → `GET /produtos/{id}/testar-jenkins` → consulta `JobApi.exists()` + último build.

### Como validar
- Mensagem verde "Job encontrado" + número do último build (ou `nenhum`).
- Banco:
  ```sql
  SELECT jenkins_url, jenkins_job, jenkins_trigger_mode
  FROM tb_produto_rh WHERE sigla = 'Nexus';
  ```

### Possíveis erros

| Mensagem | Causa | Como corrigir |
|---|---|---|
| `401` | API token inválido | Regerar e colar |
| `404` | Job não existe | Conferir grafia (case-sensitive) |
| `Connection refused` | URL errada ou Jenkins fora do ar | Conferir URL e `docker compose ps jenkins` |

### Critérios de aceite
- [ ] **Testar conexão** verde.
- [ ] `/actuator/health/jenkins` → `UP`.

---

## 5. Aba Módulos do produto

Define os componentes que serão entregues. Toda release e toda entrega referenciam módulos cadastrados aqui.

### Caminho
Detalhe do produto → aba **Módulos** → **Novo módulo**.

### Campos

| Campo | Obrigatório | Exemplo | Observação |
|---|---|---|---|
| Código | Sim | `nexus-ld` | UNIQUE por produto. Vira nome de pasta dentro do ZIP |
| Nome | Sim | `Nexus Linha Digital` | Exibição na UI |
| Tipo | Sim | `WEB` | `WEB`, `BATCH`, `BANCO`, `KETTLE`, `FUNCIONALIDADES`, `REGRAS` |
| Gera delta | Sim | `true` para BANCO/KETTLE, `false` para WEB/BATCH | Decide se entra no cálculo de delta Git |
| Obrigatório | Sim | `true` se o módulo sempre entra na entrega | Apenas hint na UI |
| Ordem | Não | `10` | Ordenação visual |
| Ativo | Sim | ✅ | Módulos inativos não aparecem no wizard |
| Config específica | Não | JSON | Ver abaixo |

#### Config específica (apenas BANCO e KETTLE)

Para módulos `BANCO`:
```json
{
  "caminhoRepo": "db/nexus",
  "prefixoDDL": "DDL_",
  "prefixoDML": "DML_",
  "dialetos": [
    { "nome": "oracle",    "caminhoRepo": "db/nexus/oracle"    },
    { "nome": "sqlserver", "caminhoRepo": "db/nexus/sqlserver" }
  ]
}
```

Para módulos `KETTLE`:
```json
{
  "caminhoRepo": "kettle/nexus",
  "incluirDependencias": true
}
```

### O que acontece ao salvar
- `POST /produtos/{id}/modulos` cria linha em `tb_modulo_produto`.
- Lista atualiza in-place.

### Como validar
```sql
SELECT codigo, nome, tipo, gera_delta, ativo
FROM tb_modulo_produto
WHERE produto_id = (SELECT id FROM tb_produto_rh WHERE sigla = 'Nexus')
ORDER BY ordem;
```

### Possíveis erros

| Mensagem | Causa | Como corrigir |
|---|---|---|
| `Código já existe neste produto` | UNIQUE (produto_id, codigo) | Usar outro código ou editar existente |
| `Tipo inválido` | Tipo fora do enum | Usar um dos 6 tipos válidos |
| `Config específica deve ser JSON válido` | JSON malformado | Validar com `jq` antes de colar |

### Critérios de aceite
- [ ] Cada módulo planejado aparece na lista.
- [ ] Banco confirma `gera_delta`, `tipo` e `ativo` conforme planejado.
- [ ] Módulos BANCO/KETTLE têm `config_especifica` populado.

---

## 6. Catálogo funcional do produto (Domínios + Funcionalidades)

Diferente dos módulos, isso descreve as **funcionalidades de negócio** do produto. É a base da matriz "cliente × funcionalidade" da seção 10.

### Caminho
Detalhe do produto → aba **Catálogo funcional** (ou nome equivalente: "Domínios").

### Domínio
Agrupa funcionalidades por área (ex.: "Crédito", "Cobrança", "Relatórios").

| Campo | Obrigatório | Exemplo |
|---|---|---|
| Código | Sim | `CREDITO` |
| Nome | Sim | `Crédito` |
| Código legado | Não | `CRD` |
| Descrição | Não | "Operações de crédito direto" |
| Ordem | Sim | `10` |
| Ativo | Sim | ✅ |

### Funcionalidade (dentro de um domínio)

| Campo | Obrigatório | Exemplo |
|---|---|---|
| Código | Sim | `SIMULACAO_CREDITO` |
| Nome | Sim | `Simulação de crédito` |
| Código legado | Não | `SCR` |
| Código de operação | Não | `OP-001` |
| Crítica | Sim | ✅ se exige aceite formal por release |
| Ordem | Sim | `10` |
| Ativo | Sim | ✅ |

### Como validar
```sql
SELECT d.codigo AS dominio, f.codigo, f.nome, f.critica
FROM tb_dominio_produto d
JOIN tb_funcionalidade_produto f ON f.dominio_produto_id = d.id
WHERE d.produto_id = (SELECT id FROM tb_produto_rh WHERE sigla = 'Nexus')
ORDER BY d.ordem, f.ordem;
```

### Possíveis erros

| Mensagem | Causa | Como corrigir |
|---|---|---|
| `Código já existe neste domínio` | UNIQUE (dominio_produto_id, codigo) | Usar outro código |

### Critérios de aceite
- [ ] Pelo menos 1 domínio com funcionalidades cadastrado se for testar matriz cliente × funcionalidade.
- [ ] Funcionalidades críticas marcadas corretamente.

---

## 7. Cadastro de Cliente

### Caminho
Sidebar → **Release Orchestrator → Clientes** → **Novo cliente**.

### Objetivo
Cadastrar o cliente que vai receber pacotes de entrega.

### Campos (aba Geral)

| Campo | Obrigatório | Exemplo | Observação |
|---|---|---|---|
| Sigla | Sim | `XPTO` | CAIXA-ALTA, ≤ 20 chars. Vira nome de pasta no destino |
| Nome | Sim | `Banco XPTO` | Nome comercial |
| Razão social | Não | `Banco XPTO S.A.` | |
| CNPJ | Não | `00000000000100` | UNIQUE se preenchido. Sem máscara |
| Ambiente padrão | Sim | `HOM` | Enum: `PROD`/`HOM`/`DEV`/`TEST` |
| Tipo de banco | Não | `ORACLE` | `ORACLE`/`SQLSERVER`/`POSTGRES` |
| Codificação | Não | `UTF-8` | |
| Fuso horário | Não | `America/Sao_Paulo` | |
| Ativo | Sim | ✅ | Clientes inativos não aparecem no wizard |
| Observações | Não | texto livre | |

### O que acontece ao salvar
- `POST /api/v1/release-orchestrator/clientes` → linha em `tb_cliente_orchestrator`.
- UI redireciona para detalhe do cliente com 5 abas: Geral, Contatos, Produtos contratados, Funcionalidades, Config. entrega, Próximas entregas.

### Como validar
1. Cliente aparece na lista filtrável.
2. Banco:
   ```sql
   SELECT sigla, nome, ambiente_padrao, ativo
   FROM tb_cliente_orchestrator WHERE sigla = 'XPTO';
   ```

### Possíveis erros

| Mensagem | Causa | Como corrigir |
|---|---|---|
| `Sigla já cadastrada` | UNIQUE | Outra sigla |
| `CNPJ já cadastrado` | UNIQUE | Conferir se cliente já existe |
| `Ambiente inválido` | Não está no enum | Escolher um dos 4 |

### Critérios de aceite
- [ ] Cliente persistido e visível na lista.
- [ ] Sigla é usada nos paths de destino (validar na seção 11 do fluxo).

---

## 8. Aba Contatos do cliente

### Caminho
Detalhe do cliente → aba **Contatos** → **Novo contato**.

### Campos

| Campo | Obrigatório | Exemplo |
|---|---|---|
| Nome | Sim | `Maria Silva` |
| Papel | Sim | `TECNICO` (ou `COMERCIAL`/`OPERACIONAL`/`FINANCEIRO`/`OUTRO`) |
| E-mail | Sim | `maria@xpto.com` |
| Telefone | Não | `(11) 99999-9999` |

### O que acontece ao salvar
- `POST /clientes/{id}/contatos` → linha em `tb_contato_orchestrator`.
- E-mails são usados nas notificações de entrega (quando habilitadas no `application.yml`).

### Critérios de aceite
- [ ] Pelo menos 1 contato `TECNICO` cadastrado (recebe notificação de geração).
- [ ] `tb_contato_orchestrator` tem o e-mail e papel salvos.

---

## 9. Aba Produtos contratados + módulos contratados

Define quais produtos o cliente comprou, em qual ambiente, e qual versão de cada módulo ele tem instalada hoje.

### Caminho
Detalhe do cliente → aba **Produtos** → **Contratar produto** → seleciona `Nexus`.

### Campos do contrato

| Campo | Obrigatório | Exemplo |
|---|---|---|
| Produto | Sim | `Nexus` |
| Ambiente | Sim | `HOM` (vem do contrato, pode diferir do ambiente padrão do cliente) |
| Ativo | Sim | ✅ |

### Módulos contratados
Após adicionar o produto, aparecem **todos os módulos do produto** com checkbox **Ativo** e coluna **Versão atual**. Para cada um:

| Coluna | Obrigatório | Exemplo |
|---|---|---|
| Ativo | Sim | ✅ módulos que o cliente realmente usa |
| Versão atual | Sim | `1.0.0` — versão hoje instalada no cliente. É o **FROM padrão do delta** |

### O que acontece ao salvar
- `POST /clientes/{id}/produtos` cria `tb_cliente_produto`.
- Para cada módulo marcado, cria `tb_cliente_produto_modulo` com `versao_atual`.

### Como validar
```sql
SELECT cp.ambiente, mp.codigo AS modulo, cpm.versao_atual, cpm.ativo
FROM tb_cliente_produto cp
JOIN tb_cliente_produto_modulo cpm ON cpm.cliente_produto_id = cp.id
JOIN tb_modulo_produto mp ON mp.id = cpm.modulo_produto_id
WHERE cp.cliente_id = (SELECT id FROM tb_cliente_orchestrator WHERE sigla = 'XPTO')
ORDER BY mp.ordem;
```

### Possíveis erros

| Mensagem | Causa | Como corrigir |
|---|---|---|
| `Cliente já contratou este produto` | UNIQUE (cliente_id, produto_id) | Editar o contrato existente |
| `Versão obrigatória` | Módulo ativo sem `versao_atual` | Preencher (use `0.0.0` se for primeira instalação) |

### Critérios de aceite
- [ ] Todos os módulos do contrato têm `versao_atual` definida.
- [ ] Banco mostra 1 linha em `tb_cliente_produto` + N em `tb_cliente_produto_modulo`.

---

## 10. Aba Funcionalidades do cliente

Matriz domínio × funcionalidade onde se marca o que cada cliente tem licença de usar. Alimenta o release-notes filtrado por cliente.

### Caminho
Detalhe do cliente → aba **Funcionalidades**.

### Comportamento
- Cada linha é uma funcionalidade do produto contratado.
- Checkbox **Habilitada**.
- Campo **Origem** (`MANUAL`/`TEMPLATE`/`HERDADA`) — sempre `MANUAL` quando o operador marca direto.

### O que acontece ao salvar
- `PUT /clientes/{id}/funcionalidades` substitui o conjunto. Cria/atualiza linhas em `tb_cliente_funcionalidade_produto`.

### Como validar
```sql
SELECT f.codigo, cfp.habilitada, cfp.origem
FROM tb_cliente_funcionalidade_produto cfp
JOIN tb_funcionalidade_produto f ON f.id = cfp.funcionalidade_produto_id
WHERE cfp.cliente_id = (SELECT id FROM tb_cliente_orchestrator WHERE sigla = 'XPTO')
  AND cfp.habilitada = TRUE
ORDER BY f.codigo;
```

### Critérios de aceite
- [ ] Pelo menos as funcionalidades críticas do contrato estão marcadas.
- [ ] PDF de release-notes filtrado por cliente menciona apenas as habilitadas (validar na seção 7 do fluxo).

---

## 11. Aba Config. entrega

Destino do pacote ZIP gerado. Existem 4 tipos.

### Caminho
Detalhe do cliente → aba **Config. entrega** → **Editar**.

### Tipo `PASTA` (local — mais simples para validar)

| Campo | Obrigatório | Exemplo |
|---|---|---|
| Tipo | Sim | `PASTA` |
| Caminho base | Sim | `/tmp/nexus-entregas/xpto` |
| Exigir aprovação | Não | ✅ se entrega só sai com aprovação manual |
| E-mails de notificação | Não | `maria@xpto.com,operacao@xpto.com` |

> Crie a pasta antes: `mkdir -p /tmp/nexus-entregas/xpto`.

### Tipo `SFTP`

| Campo | Obrigatório | Exemplo |
|---|---|---|
| Host | Sim | `sftp.xpto.com` (ou `localhost` em dev) |
| Porta | Sim | `22` (ou `2222` no SFTP do compose) |
| Usuário | Sim | `foo` |
| Senha | Sim | `senha` (cifrada em AES-GCM no banco) |
| Caminho base | Sim | `/upload` |
| Validar fingerprint | Sim | ✅ produção, ❌ dev local |

### Tipo `FTP`

| Campo | Obrigatório | Exemplo |
|---|---|---|
| Host / Porta / Usuário / Senha / Caminho base | Sim | igual SFTP |
| Modo passivo | Sim | ✅ (default) |

### Tipo `BUCKET` (S3/MinIO)

| Campo | Obrigatório | Exemplo |
|---|---|---|
| Bucket | Sim | `xpto-releases` |
| Endpoint | Não | `http://localhost:9000` (MinIO); vazio = AWS |
| Região | Não | `us-east-1` |
| Access key | Sim | `minioadmin` (campo "Usuário") |
| Secret key | Sim | `minioadmin` (campo "Senha", cifrada) |
| Path-style access | Sim | ✅ MinIO, ❌ AWS |

### Botão **Testar conexão**
- `POST /clientes/{id}/config-entrega/testar` faz uma conexão real e retorna OK/erro com mensagem.
- **Sempre rodar antes de salvar**.

### Como validar
```sql
SELECT tipo_destino, host, porta, caminho_base, bucket, endpoint, modo_passivo,
       (senha_cifrada IS NOT NULL) AS senha_definida
FROM tb_config_entrega_orchestrator
WHERE cliente_id = (SELECT id FROM tb_cliente_orchestrator WHERE sigla = 'XPTO');
```

### Possíveis erros

| Mensagem | Causa | Como corrigir |
|---|---|---|
| `Conexão recusada` | Host/porta errados ou serviço offline | Conferir `docker compose ps` |
| `Permissão negada` | Usuário/senha errados | Validar credenciais isoladamente (`sftp foo@localhost`) |
| `Caminho base não existe ou não é gravável` | Path inválido | Criar pasta / ajustar permissão |
| `Senha obrigatória` | Tipo remoto sem senha | Preencher (em edit, deixar em branco preserva atual) |
| `Encryption key não configurada` | Backend sem `RELEASE_ORCHESTRATOR_ENCRYPTION_KEY` | Exportar antes de subir |

### Critérios de aceite
- [ ] **Testar conexão** verde.
- [ ] Linha em `tb_config_entrega_orchestrator` com `tipo_destino` correto.
- [ ] Em remoto: `senha_cifrada` populada (mas a UI nunca devolve o valor em texto).

---

## 12. Release publicada (pré-requisito da entrega)

Entrega só consegue ser gerada a partir de releases `PUBLICADA`. Cobre-se em detalhe em [`docs/release-orchestrator/14-release-orchestrator-detalhe.md`](../release-orchestrator/14-release-orchestrator-detalhe.md).

### Caminho
Sidebar → **Releases → Nova release**.

### Campos

| Campo | Obrigatório | Exemplo |
|---|---|---|
| Produto | Sim | `Nexus` |
| Versão | Sim | `1.1.0` (UNIQUE por produto) |
| Tipo | Sim | `MAJOR`/`MINOR`/`PATCH` |
| Título | Sim | `Sprint 24 — novos relatórios` |
| Resumo | Não | Texto livre (vai no PDF) |
| Data prevista | Não | `2026-07-01` |

### Itens da release (aba Itens)
Cada item vira uma linha do release-notes.

| Campo | Obrigatório | Exemplo |
|---|---|---|
| Categoria | Sim | `NOVIDADE`/`MELHORIA`/`CORRECAO`/`BREAKING_CHANGE` |
| Título | Sim | `Novo relatório de inadimplência` |
| Descrição | Não | Markdown |
| Visibilidade | Sim | `TODOS` (default) ou restringir |
| Ticket / Commit / PR | Não | links |

### Vínculo versão por módulo (aba Artefatos → coluna **Versão**)
Para cada módulo do produto, defina a versão da release **naquele módulo** (geralmente igual à versão da release, mas pode diferir).

```text
NEXUS-LD       → 1.1.0
Nexus-CR       → 1.1.0
POWERMATCH    → 1.1.0
Nexus-BANCO    → 1.1.0
```

### Transição de status
- `RASCUNHO` → **Enviar para revisão** → `EM_REVISAO` → **Aprovar** → `APROVADA` → **Publicar** → `PUBLICADA`.
- Cada transição registra `tb_release_historico`.

### Como validar
```sql
SELECT r.versao, r.status, r.titulo, COUNT(ri.id) AS qtd_itens
FROM tb_release r
LEFT JOIN tb_release_item ri ON ri.release_id = r.id
WHERE r.produto_id = (SELECT id FROM tb_produto_rh WHERE sigla = 'Nexus')
GROUP BY r.id;

-- Versões por módulo
SELECT mp.codigo, rmv.versao
FROM tb_release_modulo_versao rmv
JOIN tb_modulo_produto mp ON mp.id = rmv.modulo_produto_id
WHERE rmv.release_id = (SELECT id FROM tb_release WHERE versao = '1.1.0');
```

### Critérios de aceite
- [ ] Release está `PUBLICADA`.
- [ ] Histórico tem 3+ transições (RASCUNHO → ... → PUBLICADA).
- [ ] Cada módulo tem versão definida em `tb_release_modulo_versao`.

---

## 13. Artefatos da release (aba Artefatos)

Onde ficam os arquivos físicos que serão entregues. Há dois caminhos: **upload manual** ou **sincronizar do GitHub**.

### Caminho
Detalhe da release → aba **Artefatos**.

### Caminho A — Sincronizar do GitHub
Pré-requisito: aba GitHub do produto configurada (seção 3) **e** GitHub Release publicado com asset nomeado `{SIGLA}-{MODULO}-{VERSAO}.{EXT}`.

1. Clicar **Sincronizar do GitHub** no header da aba.
2. Sistema lê a release `v1.1.0` no GitHub, baixa cada asset, cacheia em `storage/artefatos/github-cache/Nexus/1.1.0/{modulo}/` e cria linhas em `tb_artefato_release_modulo` com `sha256` calculado.

### Caminho B — Upload manual
Para cada módulo, clicar **Adicionar artefato** e fazer upload de um arquivo (WAR/JAR/ZIP).

### Como validar
```sql
SELECT mp.codigo, arm.nome_arquivo, arm.tamanho_bytes, arm.sha256
FROM tb_artefato_release_modulo arm
JOIN tb_modulo_produto mp ON mp.id = arm.modulo_produto_id
WHERE arm.release_id = (SELECT id FROM tb_release WHERE versao = '1.1.0')
ORDER BY mp.ordem;

-- Disco
ls -lh nexus-portal-api/storage/artefatos/
```

### Critérios de aceite
- [ ] Cada módulo WEB/BATCH tem pelo menos 1 artefato (`tb_artefato_release_modulo`).
- [ ] SHA-256 calculado e persistido.
- [ ] Arquivo existe no disco no path `caminho_armazenado`.

---

## Resumo — dados mínimos para seguir para o fluxo de entrega

Checklist do estado mínimo após estas configurações:

- [ ] Backend e frontend up; `/actuator/health` `UP`.
- [ ] Produto `Nexus` cadastrado, ativo.
- [ ] Aba GitHub do produto preenchida e **testar conexão verde** (se vai sincronizar do CI).
- [ ] (Opcional) aba Jenkins preenchida.
- [ ] Módulos `NEXUS-LD`, `Nexus-CR`, `POWERMATCH`, `Nexus-BANCO` cadastrados.
- [ ] Pelo menos 1 domínio com funcionalidades cadastrado.
- [ ] Cliente `XPTO` ativo com ambiente `HOM`.
- [ ] 1+ contato `TECNICO` no cliente.
- [ ] Cliente tem produto `Nexus` contratado e módulos com `versao_atual = 1.0.0`.
- [ ] Funcionalidades do cliente marcadas conforme licença.
- [ ] Config. entrega configurada (PASTA local mais rápido para smoke) e **testar conexão verde**.
- [ ] Release `1.1.0` do produto `Nexus` está `PUBLICADA` com itens e artefatos.

A partir daqui, siga [`03-fluxo-completo-entrega.md`](03-fluxo-completo-entrega.md) para gerar a entrega.
