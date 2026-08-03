# 09 — Passo a passo local, tela a tela

Guia 100% prático para subir backend + frontend na sua máquina e fazer o
primeiro fluxo completo, clicando tela por tela.

Diferente do [runbook 08](08-runbook-primeira-publicacao.md) (que cobre
produção em VM), este aqui é **localhost-only** — perfil `dev`, Postgres
local, defaults relaxados.

> **Tempo estimado:** 45 min do clone do repo até ver "Entrega CONCLUIDA"
> na tela.

---

## §0 — Pré-requisitos da sua máquina

| Ferramenta | Versão | Como instalar |
|---|---|---|
| Java | 21 | `sdk install java 21.0.11-tem` (SDKMAN) |
| Maven wrapper | — | já vem nos repos (`./mvnw`) |
| Node | 20+ | `nvm install 20 && nvm use 20` |
| PostgreSQL | 15+ | `apt install postgresql-15` ou Docker (§1.1) |
| Git | qualquer | já tem |

Confira:

```bash
java -version    # openjdk 21
node -v          # v20.x
psql --version   # 15.x
```

---

## §1 — Subir o PostgreSQL local

A forma mais simples é usar a stack Docker centralizada em
[`infra/docker/`](../../infra/docker/), que já tem tudo configurado:

```bash
cd nexus-portal-api/infra/docker
cp .env.example .env       # defaults batem com application-dev.yml
docker compose up -d       # sobe só o Postgres (sem profiles)
docker compose ps          # confere que está UP (healthy)
```

> Defaults: db `nexus_platform`, user `nexus_platform`, senha `nexus!@#`,
> porta `5432`. Para mudar, edite o `.env` e (atenção!) exporte
> `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` antes de subir o backend.

Quer só validar a conexão antes de seguir?

```bash
docker compose exec postgres pg_isready -U nexus_platform
# /var/run/postgresql:5432 - accepting connections
```

> Detalhes do que mais o compose oferece (Jenkins, MinIO, SFTP via
> profiles) em [`infra/docker/README.md`](../../infra/docker/README.md).
> Pra este guia tela-a-tela com upload manual, só Postgres basta.

---

## §2 — Subir o backend

```bash
cd nexus-portal-api

# Pastas onde os artefatos/entregas vão parar (dev usa caminhos relativos)
mkdir -p storage/artefatos storage/entregas storage/publicacoes

# Sobe com perfil dev (pega application-dev.yml)
./mvnw -pl application spring-boot:run \
  -Dspring-boot.run.profiles=dev
```

Aguarde a linha `Started NexusPortalApplication in X.Xs`. O Flyway aplica
todas as migrations (V1..V14) automaticamente. Cheque:

```bash
curl -s http://localhost:8080/actuator/health | jq
# {"status":"UP", ...}
```

> Se aparecer erro de conexão com Postgres, volte ao §1 — provavelmente o
> container não está de pé ou a senha diverge.

---

## §3 — Subir o frontend

Em **outro terminal**:

```bash
cd nexus-portal-web/frontend
npm ci          # primeira vez; depois usa npm i
npm start       # ng serve com proxy.conf.json
```

Aguarde a linha `Application bundle generation complete`. Acesse:

```
http://localhost:4200
```

Você deve ver a **tela de login** do Nexus Portal.

---

## §4 — Login

### Tela: Login

| Campo | Valor padrão dev |
|---|---|
| E-mail | `admin@nexus.local` |
| Senha | `admin` |

> A senha vem de `ADMIN_PASSWORD` no `application-dev.yml`. O usuário-seed
> é criado pela migration de segurança. Se preferir, exporte
> `ADMIN_PASSWORD=outra` antes de subir o backend.

Clique **Entrar**.

Você cai na **Tela inicial** com cards por módulo (DocFlow, Release
Orchestrator, Segurança). Sidebar à esquerda com a navegação.

---

## §5 — Criar usuários extras (opcional)

Se você for trabalhar sozinho, **pule**. Senão:

### Tela: Segurança → Usuários

Sidebar → **Segurança** → cabeçalho **Usuários** → botão **Novo usuário**.

| Campo | Exemplo |
|---|---|
| Nome | João Operador |
| E-mail | `joao@nexus.local` |
| Senha temporária | `troqueisso` |
| Grupos | `RBAC_RELEASE_ORCHESTRATOR_OP` (ou `LEITOR` se for só consultar) |

Salvar → o card do usuário aparece na lista.

---

## §6 — Cadastrar o primeiro produto

### Tela: Release Orchestrator → Produtos

Sidebar → **Release Orchestrator** → submenu **Produtos**.

Botão azul **Novo produto** no canto direito.

#### Modal "Novo produto" — aba **Geral**

| Campo | Exemplo | Obs |
|---|---|---|
| Sigla | `NEXUSLD` | CAIXA-ALTA; vira parte do nome dos pacotes |
| Nome | `Nexus Lite` | |
| Cor | clique no quadradinho → `#2563eb` | badge na UI |
| Ativo | ✅ marcado | |

Clique **Salvar** → o produto entra na lista.

#### Aba **GitHub** (cadastrar depois)

Volte na lista, clique no card do produto, vá na seção **Integração GitHub**:

| Campo | Exemplo |
|---|---|
| Repositório | `nexus/nexus-lite` |
| Branch padrão | `main` |
| Regex de tag | `^v\d+\.\d+\.\d+$` |
| Token | cole um PAT com escopo `repo` |

> **Teste rápido sem GitHub real:** você pode pular essa aba se vai apenas
> simular geração com artefatos manuais. Sem GitHub, a tela de Wizard de
> entrega ignora o passo de sincronização de assets.

Botão **Testar conexão** → mensagem verde + lista das últimas 5 releases.

#### Aba **Jenkins** (opcional para local)

Pode pular se não tem Jenkins local. Sem essa config, o webhook de
status de build (S11 P2) não funciona — mas o resto do fluxo segue.

#### Aba **Módulos** do produto

Na tela do produto → seção **Módulos** → **Novo módulo**.

Adicione pelo menos:

| Código | Nome | Tipo | Observação |
|---|---|---|---|
| `nexus-web` | Web | `WEB` | Aceita upload de WAR |
| `nexus-batch` | Batch | `BATCH` | Aceita upload de JAR |
| `nexus-banco` | Banco | `BANCO` | Delta via GitHub se configurado |
| `nexus-kettle` | Kettle | `KETTLE` | Delta via GitHub |

Para BANCO, expanda **Configuração de banco** e preencha:
- `caminhoRepo`: ex `db/nexus`
- `prefixoDDL`: ex `DDL_`
- `prefixoDML`: ex `DML_`
- Dialetos: `[{ nome: "oracle", caminhoRepo: "db/nexus/oracle" }]` (se monodialeto, deixe vazio)

---

## §7 — Cadastrar o primeiro cliente

### Tela: Release Orchestrator → Clientes

Sidebar → **Release Orchestrator** → **Clientes** → botão **Novo cliente**.

#### Form do cliente

| Campo | Exemplo |
|---|---|
| Sigla | `ACME` |
| Nome | `ACME Indústria Ltda` |
| Razão social | `ACME Indústria e Comércio Ltda` |
| CNPJ | `00.000.000/0001-00` |
| Ambiente padrão | `PROD` |
| Tipo banco | `ORACLE` |
| Codificação | `UTF-8` |
| Fuso | `America/Sao_Paulo` |
| Ativo | ✅ |

**Salvar** → cai automaticamente no **Detalhe do cliente** com 6 abas:
**Visão geral · Contatos · Produtos · Funcionalidades · Config. entrega · Próximas entregas**.

#### Aba Contatos

Botão **Novo contato**:

| Nome | Papel | Email |
|---|---|---|
| Maria Silva | TECNICO | `maria@acme.com` |
| Pedro Santos | OPERACIONAL | `pedro@acme.com` |

Receberão notificações de entrega.

#### Aba Produtos

Botão **Contratar produto** → seleciona `NEXUSLD` no dropdown → **Adicionar**.

Aparece a linha com os módulos do produto. Para cada módulo, edite a
coluna **Versão atual** com a versão que o cliente está rodando hoje.
Exemplo: `1.4.0` em todos os módulos. **Isso é o FROM padrão do delta.**

Marque ✅ na coluna **Ativo** dos módulos que o cliente realmente usa.

#### Aba Funcionalidades

Matriz domínio × funcionalidade. Marque as funcionalidades que o cliente
tem licença de usar. (Se você não cadastrou funcionalidades no produto,
essa aba fica vazia — pode ignorar pra primeiro teste.)

#### Aba **Config. entrega**

A mais importante. Botão **Editar** (canto direito).

##### Opção 1 — PASTA (mais simples pra local)

| Campo | Valor |
|---|---|
| Tipo de destino | `Pasta local` |
| Caminho base | `/tmp/nexus-entregas/acme` (caminho absoluto) |
| Exigir aprovação | desmarcado |
| E-mails | `maria@acme.com` |

Antes de salvar, crie a pasta: `mkdir -p /tmp/nexus-entregas/acme`.

##### Opção 2 — SFTP local (se quiser testar publicação remota)

Suba o serviço SFTP via profile do compose centralizado:

```bash
cd infra/docker
docker compose --profile sftp up -d
```

E configure:

| Campo | Valor |
|---|---|
| Tipo de destino | `SFTP` |
| Host | `localhost` |
| Porta | `2222` |
| Usuário | `foo` |
| Senha | `senha` |
| Caminho base | `/upload` |
| Validar fingerprint | **desmarcado** (testes locais) |

##### Opção 3 — BUCKET (S3/MinIO local)

Suba MinIO via profile do compose centralizado:

```bash
cd infra/docker
docker compose --profile bucket up -d
```

Acesse `http://localhost:9001`, login `minioadmin/minioadmin`, crie um bucket
`acme-releases`.

Na config de entrega:

| Campo | Valor |
|---|---|
| Tipo de destino | `Bucket (S3/MinIO)` |
| Bucket | `acme-releases` |
| Endpoint | `http://localhost:9000` |
| Região | `us-east-1` |
| Access Key | `minioadmin` |
| Secret Key | `minioadmin` |
| Path-style access | ✅ |

Em qualquer caso, **clique "Testar conexão"** antes de salvar. Mensagem
verde = ok. Vermelho = ajuste antes.

**Salvar**.

---

## §8 — Cadastrar a primeira release

### Tela: Release Orchestrator → Releases

Sidebar → **Release Orchestrator** → **Releases** → **Nova release**.

| Campo | Exemplo |
|---|---|
| Produto | `NEXUSLD` |
| Versão | `1.5.0` |
| Tipo | `MINOR` |
| Título | `Sprint 23 — relatórios novos` |
| Resumo | parágrafo curto que vai no PDF |

Salvar → cai no **Detalhe da release** com 3 abas: **Itens · Artefatos · Histórico**.

#### Aba Itens

Botão **Adicionar item**:

| Categoria | Título | Visibilidade |
|---|---|---|
| `NOVIDADE` | Novo relatório de produtividade | `TODOS` |
| `MELHORIA` | Performance da tela de pedidos +40% | `TODOS` |
| `CORRECAO` | Erro ao exportar XLS com >10k linhas | `TODOS` |

Adicione 3-5 itens. Eles formam o release-notes em PDF.

#### Aba Artefatos

**Se você cadastrou GitHub no produto:**
Botão **Sincronizar do GitHub** → o sistema baixa os assets das releases
publicadas no GitHub e cria registros em `artefato_release_modulo`.

**Se não cadastrou GitHub** (caminho mais fácil pra local):
Botão **Adicionar artefato** (upload manual) por módulo:
- Selecione o módulo (`nexus-web`)
- Faça upload de qualquer ZIP de teste (até um arquivo `.txt` renomeado pra
  `.war` serve pra esse fluxo)
- Sistema calcula SHA-256 + tamanho automaticamente

#### Transicionar status

No header do detalhe da release, botões à direita:

`RASCUNHO` → **Enviar para revisão** → `EM_REVISAO` → **Aprovar** → `APROVADA` → **Publicar** → `PUBLICADA`.

Só releases `PUBLICADA` podem ser usadas em entregas.

---

## §9 — Criar a primeira entrega (Wizard 5 passos)

### Tela: Release Orchestrator → Entregas → Nova entrega

Sidebar → **Release Orchestrator** → **Entregas** → **Nova entrega**.
Abre o **Wizard** com barra de progresso 1/5.

#### Passo 1 — Cliente e produto

| Campo | Valor |
|---|---|
| Cliente | `ACME — ACME Indústria Ltda` |
| Produto | `NEXUSLD — Nexus Lite` |
| Ambiente | `PROD` (vem do contrato) |

**Próximo**.

#### Passo 2 — Release e responsável

| Campo | Valor |
|---|---|
| Release | `1.5.0` (lista apenas as PUBLICADAS do produto) |
| Responsável | seu usuário |
| Observações | `Primeira entrega via portal` |

**Próximo**.

#### Passo 3 — Módulos a entregar

Tabela com os módulos do produto. Para cada um:

- ✅ **Incluir**: marcar
- **FROM**: pré-preenchido com a versão atual no contrato (`1.4.0`)
- **TO**: pré-preenchido com a versão da release (`1.5.0`)

Você pode editar o FROM se quiser pular versões (ex: `1.2.0`). O sistema
concatena os deltas das versões intermediárias.

**Próximo**.

#### Passo 4 — Calcular delta

Botão **Calcular delta**. O backend:
- WEB/BATCH: lista artefatos da release `1.5.0`
- BANCO: se GitHub configurado, gera `DDL.sql` + `DML.sql` por dialeto
- KETTLE: se GitHub configurado, lista `.ktr/.kjb` + dependências

Resultado: tabela com **Módulo · Contagem · Itens**. Se aparecer 0 itens em
algum módulo, normalmente é versão FROM = TO (nada mudou).

Coluna **Detalhes** → mostra qual artefato/SQL/transformation vai no pacote.

**Próximo**.

#### Passo 5 — Documento e geração

Editor markdown com release-notes pré-preenchido a partir dos itens.
Pode ajustar o texto que vai pro PDF entregue ao cliente.

Botão **Gerar entrega** (azul, canto direito).

O sistema:
1. Cria registro `Entrega` com status `RASCUNHO`.
2. Marca `EM_GERACAO` e dispara worker assíncrono.
3. Redireciona para o **Detalhe da entrega**.

---

## §10 — Acompanhar a geração

### Tela: Detalhe da entrega

URL: `/release-orchestrator/entregas/:id`.

#### Header

- Badge **Status entrega** vai de `EM_GERACAO` → `CONCLUIDA` (ou `FALHA`)
- A página faz **polling de 5s** automaticamente; não precisa F5.

#### KPIs no topo

- Módulos selecionados
- Itens no delta
- Tamanho do pacote (preenche quando concluir)
- SHA-256 (preenche quando concluir)

#### Quando vira CONCLUIDA

Card extra aparece:

- **Pacote** com nome, SHA-256, caminho local. Botão **Baixar pacote** (ZIP).
- Se a config de entrega for remota (FTP/SFTP/BUCKET), aparece também
  card **Publicação remota**:
  - Badge **PENDENTE** (primeira tentativa marcada para "agora")
  - Em até 2 min, o `PublicacaoRetryJob` dispara
  - Vira `OK` (verde) com destino final (ex: `sftp://localhost:2222/upload/nexusld-...zip`)
  - Se algo falhar, badge `PENDENTE` com mensagem da falha + próxima tentativa
  - Botão **Tentar agora** força nova tentativa sem esperar o backoff

#### Aba Módulos

Lista cada módulo entregue, com versão FROM/TO, total de itens, tipo.

#### Aba Delta

Tabela detalhada de cada item do delta — útil pra suporte rastrear "o que
foi entregue".

---

## §11 — Conferir o resultado em disco

Abra outro terminal:

```bash
# Pacote ZIP gerado
ls -lh nexus-portal-api/storage/entregas/acme/nexusld/1.5.0/

# Conteúdo (releases-notes em PDF + artefatos por módulo)
unzip -l nexus-portal-api/storage/entregas/acme/nexusld/1.5.0/*.zip
```

Estrutura esperada dentro do ZIP:

```
documento-acme-nexusld-1.5.0.pdf
modulos/
  nexus-web/
    nexus-web-1.5.0.war
  nexus-banco/
    oracle/
      DDL.sql
      DML.sql
```

Se você configurou SFTP/BUCKET local (§7), confira lá também:

```bash
# SFTP test container
docker exec sftp-test ls -la /home/foo/upload

# MinIO
docker exec minio mc ls local/acme-releases 2>/dev/null \
  || echo "use o console em http://localhost:9001"
```

---

## §12 — PDF de release-notes (bônus)

Volte na **Tela: Detalhe da release** (`/release-orchestrator/releases/:id`).

Botões no header:
- **PDF Cliente** — versão com itens marcados `TODOS` ou `CLIENTE`
- **Suporte** — adiciona itens `SUPORTE`
- **Interno** — tudo, incluindo notas internas

PDF é baixado direto pelo browser.

---

## §13 — O que está rodando agora

| Componente | URL local | Como ver |
|---|---|---|
| Backend API | http://localhost:8080 | `curl /actuator/health` |
| Frontend Angular | http://localhost:4200 | navegador |
| Métricas Prometheus | http://localhost:8080/actuator/prometheus | grep `entrega_geracao` |
| H2 console (não, é Postgres) | — | `psql -h localhost -U nexus_platform nexus_platform` |
| Logs do backend | terminal do `mvnw spring-boot:run` | tail no terminal |
| Logs do frontend | terminal do `npm start` | reload automático nas mudanças |

---

## §14 — Resetar tudo se algo deu errado

```bash
cd nexus-portal-api/infra/docker
docker compose --profile all down -v    # tudo + volumes

# Pacotes gerados pelo backend
rm -rf ../../storage/{artefatos,entregas,publicacoes}/*
```

> No primeiro startup o Flyway aplica V1..V14 do zero e o seed cria o
> admin com senha `admin` (ou `$ADMIN_PASSWORD`).

---

## §14b — Nexus AI (assistente de páginas) — ~15 min

Runbook completo: [`../ai/RUNBOOK-LOCAL.md`](../ai/RUNBOOK-LOCAL.md).

```bash
export NEXUS_AI_ENABLED=true
# opcional: export OPENROUTER_API_KEY=sk-or-...
```

1. Confirme `GET /api/v1/ai/status` com `enabled: true`.
2. No front: DocFlow → Páginas → **Criar com IA**.
3. Cole briefing → gerar → **Aplicar no editor** → salvar rascunho.

Sem `OPENROUTER_API_KEY` o backend usa `FakeLlmProvider` (HTML determinístico).

---

## §15 — Próximos passos depois desse fluxo funcionar

1. **Conectar GitHub real** (PAT em produto) → testar sincronização de assets.
2. **Conectar Jenkins** → fazer `git tag v1.5.0` e ver o badge de build aparecer.
3. **Configurar destino remoto real** (SFTP do cliente) e validar o retry job.
4. **Cadastrar funcionalidades** no produto (matriz `dominio × funcionalidade`)
   e ligar à licença do cliente.
5. **Ler o [runbook 08](08-runbook-primeira-publicacao.md)** quando for
   provisionar a VM de produção.
6. **Roteiro de demo** em [05-demo-roteiro-acme.md](05-demo-roteiro-acme.md)
   pra treinar operadores.

---

## §16 — Atalhos de diagnóstico (local)

| Sintoma | O que fazer |
|---|---|
| Backend não sobe — erro de connection refused | Postgres não está rodando (§1) |
| Backend sobe, Flyway falha | Banco já tinha dados de outra versão; resete (§14) |
| Login devolve 401 | Senha errada — `ADMIN_PASSWORD=admin` é o default |
| `/api/...` devolve 404 no frontend | `npm start` não pegou `proxy.conf.json`; mate e suba de novo |
| Gerar entrega marca FALHA na hora | Olhe o terminal do backend; provavelmente é caminho de storage ausente — crie `storage/entregas` |
| Publicação remota presa em PENDENTE por horas | Job roda a cada 2 min; `tail` no log e procure por `Publicação retry job` |
| `application-dev.yml` não carrega | Você esqueceu `-Dspring-boot.run.profiles=dev` |
| Nada aparece em "Releases" | Release ainda em RASCUNHO; só PUBLICADA fica disponível pro wizard |
| "Nenhuma release disponível" no wizard | Confirme que a release está PUBLICADA e tem pelo menos 1 artefato (manual ou via GitHub) |
