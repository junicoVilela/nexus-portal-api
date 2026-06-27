# 03 — Fluxo completo de entrega (XPTO / DTEC v1.0.0 → v1.1.0)

Cenário de validação executando o sistema ponta a ponta. Cada passo tem **ação**, **resultado esperado**, **onde confirmar**, **critério de aceite**.

> Pré-requisito: terminou [`01-configuracoes-iniciais.md`](01-configuracoes-iniciais.md). Cliente `XPTO`, produto `DTEC`, módulos, contrato e release `1.1.0` PUBLICADA já existem.

---

## Cenário

| Item | Valor |
|---|---|
| Cliente | `XPTO` — Banco XPTO S.A. |
| Produto | `DTEC` — Suite DTEC |
| Módulos contratados | `DTEC-LD` (WEB), `DTEC-CR` (WEB), `POWERMATCH` (BATCH), `DTEC-BANCO` (BANCO) |
| Ambiente do contrato | `HOM` |
| Versão atual instalada | `1.0.0` (todos os módulos) |
| Versão alvo | `1.1.0` |
| Destino | `PASTA` local → `/tmp/softon-entregas/xpto` |
| Pacote esperado | `XPTO_DTEC_1.0.0_1.1.0.zip` (ou nome equivalente conforme política do projeto) |

> Nome canônico do projeto: `{SIGLA_CLIENTE}_{SIGLA_PRODUTO}_{VERSAO_FROM}_{VERSAO_TO}.zip`. Pode haver variação (timestamp, ambiente) — confira o real na seção 11 abaixo e ajuste o critério.

---

## Visão macro do fluxo

```text
1. Login admin
2. Conferir que release 1.1.0 está PUBLICADA com artefatos sincronizados
3. Abrir wizard de Nova entrega
   ├── Passo 1: Cliente XPTO + Produto DTEC
   ├── Passo 2: Release 1.1.0 + responsável
   ├── Passo 3: 4 módulos marcados, FROM 1.0.0, TO 1.1.0
   ├── Passo 4: Calcular delta
   └── Passo 5: Documento + Gerar entrega
4. Acompanhar geração (polling)
5. Validar pacote em disco
6. Validar conteúdo do ZIP
7. Validar documento PDF
8. Validar scripts de banco (delta SQL)
9. Validar publicação remota (se aplicável)
10. Conferir histórico
11. Reentrega (smoke test)
```

---

## Passo 0 — Confirmação do estado inicial

### Ação
Abrir uma sessão psql ou DevTools para validar pré-requisitos.

```sql
-- Cliente XPTO existe e está ativo?
SELECT sigla, ambiente_padrao, ativo FROM tb_cliente_orchestrator WHERE sigla = 'XPTO';
-- esperado: 1 linha, ativo = TRUE

-- Cliente tem produto DTEC contratado e módulos com versao_atual?
SELECT mp.codigo, cpm.versao_atual, cpm.ativo
FROM tb_cliente_produto cp
JOIN tb_cliente_produto_modulo cpm ON cpm.cliente_produto_id = cp.id
JOIN tb_modulo_produto mp ON mp.id = cpm.modulo_produto_id
JOIN tb_cliente_orchestrator c ON c.id = cp.cliente_id
WHERE c.sigla = 'XPTO';
-- esperado: 4 linhas (DTEC-LD, DTEC-CR, POWERMATCH, DTEC-BANCO), todas com versao_atual=1.0.0

-- Release 1.1.0 está PUBLICADA?
SELECT r.versao, r.status, COUNT(arm.id) AS qtd_artefatos
FROM tb_release r
LEFT JOIN tb_artefato_release_modulo arm ON arm.release_id = r.id
WHERE r.versao = '1.1.0'
  AND r.produto_id = (SELECT id FROM tb_produto_rh WHERE sigla = 'DTEC')
GROUP BY r.id;
-- esperado: status=PUBLICADA, qtd_artefatos ≥ 3 (1 por módulo WEB/BATCH)

-- Config. entrega XPTO está como PASTA?
SELECT tipo_destino, caminho_base
FROM tb_config_entrega_orchestrator
WHERE cliente_id = (SELECT id FROM tb_cliente_orchestrator WHERE sigla = 'XPTO');
-- esperado: PASTA, /tmp/softon-entregas/xpto

-- Pasta existe?
\! ls -ld /tmp/softon-entregas/xpto
```

### Critério de aceite
- [ ] Todas as 4 queries retornam o esperado.
- [ ] Pasta `/tmp/softon-entregas/xpto` existe e é gravável.

---

## Passo 1 — Login

### Ação
`http://localhost:4200/login` → `admin / admin` → **Entrar**.

### Resultado esperado
- Redireciona para `/` (cards dos módulos).
- Card "Release Orchestrator" visível.

### Critério de aceite
- [ ] Token em `localStorage` (DevTools → Application → Local Storage → `auth.token`).
- [ ] `GET /api/v1/auth/me` retornou grupos `ADMIN` (DevTools → Network).

---

## Passo 2 — Abrir o wizard

### Ação
Sidebar → **Release Orchestrator → Entregas → Nova entrega** (ou direto `/release-orchestrator/entregas/nova`).

### Resultado esperado
- Wizard abre no passo 1/5 com barra de progresso.
- Botão **Próximo** desabilitado.

### Critério de aceite
- [ ] Wizard responsivo, sem erros no console.
- [ ] URL muda para `/release-orchestrator/entregas/nova`.

---

## Passo 3 — Wizard passo 1: Cliente + Produto

### Ação
| Campo | Valor |
|---|---|
| Cliente | `XPTO — Banco XPTO S.A.` |
| Produto | `DTEC — Suite DTEC` |
| Ambiente | `HOM` (default vem do contrato) |

Clicar **Próximo**.

### Resultado esperado
- Dropdown de produto lista apenas produtos contratados pelo cliente (não todos os ativos).
- Após selecionar produto, ambiente preenche automaticamente com o ambiente do contrato.
- Botão **Próximo** habilita.

### Critério de aceite
- [ ] Sem `XPTO` cadastrado, dropdown cliente vazio (não bug, é estado).
- [ ] Trocar cliente reseta o dropdown de produto.

---

## Passo 4 — Wizard passo 2: Release + responsável

### Ação
| Campo | Valor |
|---|---|
| Release | `1.1.0` |
| Responsável | (seu usuário; default = admin) |
| Observações | `Primeira entrega de validação` |

Clicar **Próximo**.

### Resultado esperado
- Dropdown de release lista apenas **PUBLICADAS** do produto.
- Não aparecem RASCUNHO/EM_REVISAO/APROVADA.

### Critério de aceite
- [ ] Se não há release publicada → dropdown vazio + mensagem orientativa.
- [ ] Responsável padrão = usuário logado.

---

## Passo 5 — Wizard passo 3: Módulos

### Ação
Marcar os 4 módulos como **Ativos**. Para cada um:
- FROM: `1.0.0` (pré-preenchido pelo contrato)
- TO: `1.1.0` (pré-preenchido pela release)

Clicar **Próximo**.

### Resultado esperado
- 4 linhas: `DTEC-LD`, `DTEC-CR`, `POWERMATCH`, `DTEC-BANCO`.
- Toggle "Ativo" default = `true` para módulos contratados.
- Coluna **Fora do contrato** aparece se algum módulo do produto não está no `tb_cliente_produto_modulo` do cliente.

### Critério de aceite
- [ ] Editar FROM altera o valor exibido no preview do passo 4.
- [ ] Pelo menos 1 módulo precisa estar marcado para habilitar **Próximo**.

---

## Passo 6 — Wizard passo 4: Delta

### Ação
Clicar **Calcular delta**.

### Resultado esperado
- Loading spinner por alguns segundos.
- Tabela aparece com 4 linhas (1 por módulo marcado), cada uma com **contagem `+N`** e **tamanho**:

| Módulo | Tipo | Contagem | O que está dentro |
|---|---|---|---|
| `DTEC-LD` | WEB | `+1` | `dtec-ld-1.1.0.war` |
| `DTEC-CR` | WEB | `+1` | `dtec-cr-1.1.0.war` |
| `POWERMATCH` | BATCH | `+1` | `powermatch-1.1.0.jar` |
| `DTEC-BANCO` | BANCO | `+N` (depende do diff entre tags v1.0.0 e v1.1.0 no GitHub) | `DDL.sql` + `DML.sql` (por dialeto) |

- Header mostra **total agregado** (soma de N e bytes).
- Botão **Detalhes** por linha expande a lista de arquivos.

### Validação detalhada

#### WEB / BATCH
- Origem: artefatos da release (`tb_artefato_release_modulo`).
- Se `DTEC-LD` tem 0 artefatos na release → linha aparece com `+0` e badge amarelo "Sem artefato".

#### BANCO
- Origem: `GitHubReleasesAdapter.compare()` entre tag `v1.0.0` e `v1.1.0`.
- Sistema filtra `.sql` por `caminhoRepo` (de `config_especifica`), classifica via `prefixoDDL`/`prefixoDML`.
- Multi-dialeto: aparece em subpastas (`oracle/DDL.sql`, `sqlserver/DDL.sql`).
- Se config_especifica não está populada → linha com erro "Configuração BANCO ausente — defina caminhoRepo".

#### KETTLE (se houver)
- Lista `.ktr`/`.kjb` alterados.
- Se `incluirDependencias: true` → parser regex resolve `<filename>` literais e inclui dependências.

### Critério de aceite
- [ ] `POST /api/v1/release-orchestrator/entregas/{rascunho_id}/delta/calcular` retorna 200.
- [ ] Tabela mostra os 4 módulos com contagem coerente.
- [ ] DevTools mostra payload com `arquivos[]` por módulo.
- [ ] Se quiser ajustar FROM, usar tela [`/entregas/{id}/delta`](02-validacao-tela-por-tela.md#17-tela-de-delta-editável) (smoke separado).

---

## Passo 7 — Wizard passo 5: Documento + geração

### Ação
- Editor Markdown abre pré-preenchido com release-notes (vem dos itens da release 1.1.0).
- Confira que **apenas itens visíveis ao cliente XPTO** aparecem (ou seja, itens marcados com visibilidade `TODOS` ou explicitamente `XPTO`, e referenciando funcionalidades habilitadas).
- (Opcional) Editar o texto.
- Clicar **Gerar entrega** (azul, canto direito).

### Resultado esperado
1. Sistema cria `Entrega` em `RASCUNHO`.
2. Marca status `EM_GERACAO` e dispara `GeracaoEntregaService.gerar()` (`@Async`).
3. Redireciona para `/release-orchestrator/entregas/{id}` em < 1s.

### Critério de aceite
- [ ] `POST /api/v1/release-orchestrator/entregas/{rascunho_id}/geracao/iniciar` retorna 202.
- [ ] Banco:
  ```sql
  SELECT status, status_publicacao FROM tb_entrega
  WHERE id = '{entrega_id}';
  -- esperado: EM_GERACAO, NAO_APLICAVEL (vai virar PENDENTE se destino remoto)
  ```
- [ ] Log do backend mostra entrada com `entregaId` no MDC.

---

## Passo 8 — Acompanhar geração

### Ação
Página `/entregas/{id}` faz polling automático a cada 5s.

### Resultado esperado
Sequência observável:
1. Badge `EM_GERACAO` (amarelo) por alguns segundos.
2. Eventos no Histórico vão aparecendo (módulos processados, ZIP criado, SHA-256 calculado).
3. Badge muda para `CONCLUIDA` (verde) **OU** `FALHA` (vermelho com mensagem).

Tempo esperado em dev local: 5–30 segundos com 4 módulos.

### Validação durante o processo
- KPIs atualizam: "Módulos selecionados" = 4, "Itens no delta" = soma do passo 6.
- Storage: `softon-portal-api/storage/entregas/xpto/dtec/1.1.0/` começa a ter arquivos temporários, depois consolida no ZIP final.

### Critério de aceite
- [ ] Polling para automaticamente quando status sai de `EM_GERACAO`.
- [ ] `data_inicio_geracao` e `data_conclusao` preenchidos:
  ```sql
  SELECT status, data_inicio_geracao, data_conclusao,
         tamanho_bytes, arquivo_pacote_sha256
  FROM tb_entrega WHERE id = '{entrega_id}';
  ```
- [ ] Métrica Micrometer foi incrementada:
  ```bash
  curl -s http://localhost:8080/actuator/metrics/entrega.geracao.duration | jq '.measurements'
  ```

---

## Passo 9 — Validação do pacote ZIP em disco

### Ação
```bash
# Local do pacote
ls -lh softon-portal-api/storage/entregas/xpto/dtec/1.1.0/

# Conferir SHA-256
sha256sum softon-portal-api/storage/entregas/xpto/dtec/1.1.0/*.zip
# deve bater com tb_entrega.arquivo_pacote_sha256 do passo 8

# Listar conteúdo
unzip -l softon-portal-api/storage/entregas/xpto/dtec/1.1.0/*.zip
```

### Conteúdo esperado do ZIP

```text
XPTO_DTEC_1.0.0_1.1.0/
├── documento-xpto-dtec-1.1.0.pdf
├── manifest.json
├── SHA256SUMS.txt
├── modulos/
│   ├── dtec-ld/
│   │   └── dtec-ld-1.1.0.war
│   ├── dtec-cr/
│   │   └── dtec-cr-1.1.0.war
│   ├── powermatch/
│   │   └── powermatch-1.1.0.jar
│   └── dtec-banco/
│       ├── oracle/
│       │   ├── DDL.sql
│       │   └── DML.sql
│       └── sqlserver/
│           ├── DDL.sql
│           └── DML.sql
└── (eventualmente) kettle/... se houver módulo KETTLE
```

### Validação detalhada

#### `manifest.json`
```bash
unzip -p softon-portal-api/storage/entregas/xpto/dtec/1.1.0/*.zip manifest.json | jq
```
Deve conter:
- `cliente`, `produto`, `ambiente`, `versaoFrom`, `versaoTo`
- `geradoEm`, `sha256Pacote`
- `modulos[]` com tipo, versão, artefatos e SHA-256 de cada

#### `SHA256SUMS.txt`
```bash
unzip -p softon-portal-api/storage/entregas/xpto/dtec/1.1.0/*.zip SHA256SUMS.txt
```
Uma linha por arquivo do pacote no formato padrão `sha256  caminho`.

Para validar integridade:
```bash
cd /tmp && unzip softon-portal-api/storage/entregas/xpto/dtec/1.1.0/*.zip
cd XPTO_DTEC_1.0.0_1.1.0 && sha256sum -c SHA256SUMS.txt
# todas as linhas devem dar OK
```

### Critério de aceite
- [ ] ZIP existe em `storage/entregas/xpto/dtec/1.1.0/` (caminho conforme `caminho_base` da config).
- [ ] SHA-256 do arquivo bate com `tb_entrega.arquivo_pacote_sha256`.
- [ ] `unzip -l` mostra a estrutura esperada.
- [ ] `manifest.json` é JSON válido com cliente=XPTO, produto=DTEC, versões.
- [ ] `sha256sum -c SHA256SUMS.txt` → todos OK.

---

## Passo 10 — Validação do documento PDF

### Ação
- Opção 1: clicar **Baixar PDF** no card "Documento" da tela de detalhe.
- Opção 2: extrair do ZIP.

```bash
unzip -j softon-portal-api/storage/entregas/xpto/dtec/1.1.0/*.zip "documento-*.pdf" -d /tmp/
xdg-open /tmp/documento-xpto-dtec-1.1.0.pdf  # ou abrir manual
```

### Conteúdo esperado
- Capa: logo (se configurado), nome do cliente, produto, versão, data.
- Sumário automático.
- Seções por categoria: NOVIDADE / MELHORIA / CORRECAO / BREAKING_CHANGE.
- Cada item mostra título + descrição + (opcionalmente) ticket/PR.
- Footer com SHA-256 do pacote.

### Validação por tipo (gerado a partir do mesmo conjunto de itens, com filtros diferentes)

| Tipo | Visibilidade | Diferença |
|---|---|---|
| `CLIENTE` | Apenas itens com `visibilidade IN ('TODOS', 'CLIENTE_XPTO')` | Versão "limpa" para o cliente |
| `SUPORTE` | Todos os itens | Acrescenta seção técnica (commit hashes, tickets) |
| `INTERNO` | Todos os itens + observações internas | Tudo de SUPORTE + observações da release |

Pode gerar os 3 tipos pelo botão dropdown no header da release (`/releases/{id}` → **Gerar PDF**).

### Critério de aceite
- [ ] PDF abre sem erros.
- [ ] Cliente, produto, versão TO estão corretos na capa.
- [ ] Apenas itens visíveis ao cliente XPTO aparecem no tipo CLIENTE.
- [ ] Itens da categoria `BREAKING_CHANGE` (se houver) aparecem destacados.

---

## Passo 11 — Validação dos scripts de banco (delta SQL)

### Ação
```bash
# Extrair só a parte de banco
unzip -p softon-portal-api/storage/entregas/xpto/dtec/1.1.0/*.zip 'modulos/dtec-banco/oracle/DDL.sql' > /tmp/DDL.sql
unzip -p softon-portal-api/storage/entregas/xpto/dtec/1.1.0/*.zip 'modulos/dtec-banco/oracle/DML.sql' > /tmp/DML.sql

# Inspecionar
head -50 /tmp/DDL.sql
head -50 /tmp/DML.sql
```

### Conteúdo esperado
Cada bloco no arquivo deve ter um header indicando a procedência:

```sql
-- ============================================================
-- Origem: db/dtec/oracle/DDL_002__alter_tabela_xpto.sql
-- Tag:    v1.0.1
-- Commit: a1b2c3d
-- Autor:  Fulano <fulano@softon.com>
-- ============================================================
ALTER TABLE tb_foo ADD COLUMN bar VARCHAR(100);
...
```

### Validação
1. Cada bloco tem o header → procedência rastreável.
2. Ordem dos blocos respeita ordem cronológica das tags v1.0.1, v1.0.2, ..., v1.1.0.
3. Multi-dialeto: `oracle/` e `sqlserver/` têm conteúdo diferente (script-fonte diferente por dialeto).
4. Apenas arquivos que **mudaram** entre tags FROM..TO aparecem (não traz todo o banco).

### Critério de aceite
- [ ] Header de procedência em todos os blocos SQL.
- [ ] DDL.sql contém apenas scripts com prefixo configurado (`DDL_`).
- [ ] DML.sql contém apenas scripts com prefixo configurado (`DML_`).
- [ ] Sintaxe SQL válida (rodar `psql --dry-run` ou `sqlfluff parse`).

> Se a release entre 1.0.0 e 1.1.0 não teve nenhum script SQL alterado → arquivos vêm vazios (só com comentário "Sem alterações"). É válido — não é bug.

---

## Passo 12 — Validação da publicação remota

Aplica **apenas se** a config. entrega do cliente é `FTP`, `SFTP` ou `BUCKET`. Se for `PASTA`, pule.

### Ação
No detalhe da entrega, observe o card **Publicação remota**:
- Badge muda de `PENDENTE` para `OK` em até 2 min (cron do `PublicacaoRetryJob` é `0 */2 * * * *`).
- Campo "Destino final" mostra a URL/path completo (ex.: `sftp://localhost:2222/upload/XPTO_DTEC_1.0.0_1.1.0.zip`).

### Validar destino
Dependendo do tipo:

```bash
# SFTP
docker compose -f softon-portal-api/infra/docker/docker-compose.yml exec sftp \
  ls -lh /home/foo/upload/

# FTP (com client local)
ftp localhost  # navegar até o path

# MinIO
docker compose -f softon-portal-api/infra/docker/docker-compose.yml exec minio \
  mc ls local/xpto-releases/  # se mc estiver no container; senão usar console http://localhost:9001
```

### Validação SHA-256 no destino
Baixar o arquivo de volta e comparar com `tb_entrega.arquivo_pacote_sha256`:
```bash
# SFTP
sftp -P 2222 foo@localhost <<< "get /upload/XPTO_DTEC_1.0.0_1.1.0.zip /tmp/baixado.zip"
sha256sum /tmp/baixado.zip
# bate com tb_entrega.arquivo_pacote_sha256
```

### Forçar reagendamento (smoke test do retry)
1. Clicar **Tentar agora** no card.
2. `POST /entregas/{id}/publicacao/reagendar` zera `tentativas_publicacao` e marca PENDENTE.
3. Próximo ciclo do job (≤ 2 min) re-publica.

### Critério de aceite
- [ ] Badge `OK` em ≤ 2 min após `CONCLUIDA`.
- [ ] Arquivo existe no destino com SHA-256 idêntico.
- [ ] `tb_entrega.destino_publicacao` populado com URL/path final.
- [ ] `data_publicacao` preenchido.
- [ ] **Tentar agora** dispara nova tentativa (observável no histórico).

---

## Passo 13 — Validação do histórico de entregas

### Ação
Sidebar → **Entregas** (`/release-orchestrator/entregas`).

### Resultado esperado
- Lista mostra a entrega que acabou de gerar no topo.
- Filtro por cliente `XPTO` → só essa entrega aparece (ou mais, se houver de testes anteriores).
- Click → volta para o detalhe.

### Verificação adicional (aba Próximas entregas, se origem foi agendada)
- Se a entrega veio de uma `tb_proxima_entrega`, o status dela vai para `CONVERTIDA` e mostra ID da entrega gerada.

### Banco
```sql
SELECT e.created_at, e.status, e.status_publicacao,
       e.tamanho_bytes, e.arquivo_pacote_sha256,
       e.proxima_entrega_id
FROM tb_entrega e
WHERE e.cliente_id = (SELECT id FROM tb_cliente_orchestrator WHERE sigla = 'XPTO')
ORDER BY e.created_at DESC;
```

### Critério de aceite
- [ ] Entrega aparece na lista com status correto.
- [ ] Filtro por status `CONCLUIDA` mostra apenas entregas concluídas.
- [ ] Filtros persistem na URL.

---

## Passo 14 — Reentrega (smoke test)

### Ação
1. No detalhe da entrega CONCLUIDA, clicar **Reentregar**.
2. Confirmar no modal.

### Resultado esperado
- Sistema cria nova entrega em `RASCUNHO`, copiando configuração da original (cliente, produto, release, módulos, FROM/TO).
- `entrega_original_id` aponta para a entrega original.
- Wizard abre no passo 5 (todas as decisões prévias mantidas).

### Validação
```sql
SELECT e.id, e.status, e.entrega_original_id
FROM tb_entrega e
WHERE e.entrega_original_id = '{entrega_original_id}'
ORDER BY e.created_at DESC;
```

### Critério de aceite
- [ ] Nova entrega tem `entrega_original_id` preenchido.
- [ ] Gerar a reentrega produz ZIP novo com SHA-256 **diferente** (se artefatos mudaram) ou **igual** (se nada mudou — deterministic).

---

## Passo 15 — Atualização do contrato pós-entrega

### Comportamento esperado
Quando a entrega vai para `CONCLUIDA`, `GeracaoEntregaService` atualiza `tb_cliente_produto_modulo.versao_atual` para a versão TO de cada módulo entregue.

### Validação
```sql
SELECT mp.codigo, cpm.versao_atual
FROM tb_cliente_produto cp
JOIN tb_cliente_produto_modulo cpm ON cpm.cliente_produto_id = cp.id
JOIN tb_modulo_produto mp ON mp.id = cpm.modulo_produto_id
WHERE cp.cliente_id = (SELECT id FROM tb_cliente_orchestrator WHERE sigla = 'XPTO');
-- esperado: todos os módulos entregues agora com versao_atual = '1.1.0'
```

### Critério de aceite
- [ ] `versao_atual` dos módulos entregues atualizou para `1.1.0`.
- [ ] Próxima entrega para XPTO terá FROM padrão = `1.1.0` (não mais 1.0.0).

---

## Passo 16 — Logs e observabilidade

### Ação
Conferir os logs do backend (stdout) durante a geração.

### O que deve aparecer
- Logs JSON estruturados (profile `prod`) ou padrão legível (`dev`).
- Cada linha relacionada à entrega tem `correlationId`, `entregaId`, `clienteId` no MDC.
- Logs `INFO` para início, etapas (módulos processados), conclusão.
- Logs `WARN`/`ERROR` se houver falha.

### Métricas Micrometer
```bash
# Duração das gerações (Timer com percentis)
curl -s http://localhost:8080/actuator/metrics/entrega.geracao.duration | jq

# Contador por resultado
curl -s http://localhost:8080/actuator/metrics/entrega.geracao.resultado?tag=status:sucesso | jq
curl -s http://localhost:8080/actuator/metrics/entrega.geracao.resultado?tag=status:falha | jq
```

### Critério de aceite
- [ ] Cada entrega gerada incrementa `entrega.geracao.duration.count`.
- [ ] Em caso de sucesso, contador `status:sucesso` += 1.
- [ ] Logs contêm `correlationId` único por requisição.

---

## Resumo do que foi validado

Ao final deste fluxo, você confirmou:

| Capacidade | Onde |
|---|---|
| Login + autorização | Passo 1 |
| Wizard de entrega ponta a ponta | Passos 3–7 |
| Cálculo de delta multi-tipo (WEB/BATCH/BANCO) | Passo 6 |
| Geração assíncrona com polling | Passo 8 |
| Pacote ZIP com estrutura correta | Passo 9 |
| PDF release-notes filtrado por cliente | Passo 10 |
| Scripts SQL com procedência | Passo 11 |
| Publicação remota com retry | Passo 12 |
| Histórico auditável | Passo 13 |
| Reentrega com lineage | Passo 14 |
| Atualização do contrato pós-entrega | Passo 15 |
| Observabilidade (logs JSON + métricas) | Passo 16 |

Próximo passo: [`04-checklist-final.md`](04-checklist-final.md) consolida a lista marcável que cobre tudo isso + o `01` + o `02`.
