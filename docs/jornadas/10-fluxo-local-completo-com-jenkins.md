# 10 — Fluxo local completo, com Jenkins (sem upload manual)

Cobre **todos os serviços rodando na sua máquina** — Postgres + Portal +
Jenkins + (opcional) MinIO — e o pipeline real: `git tag` → Jenkins
builda → GitHub Release ganha asset → portal sincroniza → wizard gera
entrega → publica no destino.

Diferente do [guia 09](09-passo-a-passo-local-tela-a-tela.md) (que faz
upload manual de artefatos), aqui o portal **nunca recebe arquivo via UI**:
o Jenkins publica no GitHub e o portal só consome.

> **Tempo:** 90-120 min na primeira vez. Depois disso cada produto novo
> leva ~20 min.

---

## §0 — Plano de portas (importante)

Como tudo roda em `localhost`, planeje as portas antes:

| Serviço | Porta | Conflito? |
|---|---|---|
| PostgreSQL | 5432 | — |
| **Portal API** | **8080** | ⚠ conflita com Jenkins default |
| **Jenkins** | **8090** | ← vamos mudar a default 8080 pra 8090 |
| Portal Frontend | 4200 | — |
| MinIO API | 9000 | — |
| MinIO Console | 9001 | — |
| SFTP (atmoz) | 2222 | — |

> Vamos rodar **Jenkins em 8090** pra liberar 8080 pro portal. Todos os
> exemplos abaixo já estão ajustados.

---

## §1 — Pré-requisitos

| Ferramenta | Versão | Verificar |
|---|---|---|
| Docker | qualquer recente | `docker --version` |
| Java 21 | local | `java -version` |
| Node 20 | local | `node -v` |
| Git + GitHub CLI | `gh` opcional | `gh --version` |
| Conta no GitHub | com repo de teste | — |

Crie um **repo no GitHub** para servir de produto-cobaia. Pode ser um repo
Java/Maven mínimo. Exemplo: `seuuser/dtec-lite-mock` com um `pom.xml` simples
que gera um WAR. Vou referenciar como `seuuser/dtec-lite-mock` daqui pra
frente.

---

## §2 — Subir Postgres, Portal API e Frontend

Esse pedaço é o mesmo do guia 09 §1-§4. Resumo rápido:

```bash
# Postgres
docker run -d --name softon-postgres \
  -e POSTGRES_DB=softon_intranet \
  -e POSTGRES_USER=softon_intranet \
  -e POSTGRES_PASSWORD='softon!@#' \
  -p 5432:5432 -v softon-pgdata:/var/lib/postgresql/data \
  postgres:15

# Portal API — exporte o webhook secret antes de subir
cd softon-portal-api
mkdir -p storage/{artefatos,entregas,publicacoes}
export RELEASE_ORCHESTRATOR_WEBHOOKS_JENKINS_SECRET=$(openssl rand -hex 32)
echo "ANOTE ISSO: $RELEASE_ORCHESTRATOR_WEBHOOKS_JENKINS_SECRET"
./mvnw -pl application spring-boot:run -Dspring-boot.run.profiles=dev
```

> Anote o `WEBHOOK_SECRET` em um arquivo temporário — vamos colar dentro
> do Jenkins no §4. Se rebuildar o portal sem exportar de novo, o secret
> some e o webhook volta a dar 403.

Em outro terminal:

```bash
cd softon-portal-web/frontend
npm ci && npm start
```

Aguarde:
- Portal API em `http://localhost:8080/actuator/health` → `UP`
- Frontend em `http://localhost:4200` → tela de login

Faça login (`admin@softon.com.br` / `admin`).

---

## §3 — Subir Jenkins local

### 3.1 Container

```bash
docker volume create jenkins_home

docker run -d \
  --name jenkins \
  --restart unless-stopped \
  -p 8090:8080 -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  jenkins/jenkins:lts-jdk21
```

> A primeira porta `8090` é o host; `8080` interna do container. Acesse
> Jenkins em `http://localhost:8090`.

### 3.2 Senha inicial

```bash
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

Cole a senha em `http://localhost:8090`.

### 3.3 Setup wizard

1. **Install suggested plugins** (Pipeline, Git, GitHub, Credentials, etc).
2. Espere a instalação terminar (~5 min).
3. Crie o usuário admin local:
   - User: `admin`
   - Password: `admin` (local dev — não use em prod)
   - Nome completo, e-mail
4. **Jenkins URL:** confirme `http://localhost:8090/` (default).
5. **Start using Jenkins**.

### 3.4 Plugins adicionais

Em **Manage Jenkins → Plugins → Available plugins**, instale:

| Plugin | Pra quê |
|---|---|
| `GitHub Branch Source` | Trigger por tag |
| `Pipeline: Stage View` | UI bonita do pipeline |
| `Timestamper` | Timestamps no log (Jenkinsfile template usa) |

Marque **Restart Jenkins when installation is complete and no jobs are
running** → aguarde reiniciar.

### 3.5 Tools (JDK + Maven)

**Manage Jenkins → Tools**:

- **JDK installations** → Add JDK
  - Name: `jdk-21`
  - ✅ Install automatically → Adoptium → `jdk-21.0.x+x`
- **Maven installations** → Add Maven
  - Name: `maven-3.9`
  - ✅ Install automatically → `3.9.9`

**Save** no fim.

> Os nomes `jdk-21` e `maven-3.9` são referenciados literalmente no
> template Jenkinsfile (`tools { maven 'maven-3.9'; jdk 'jdk-21' }`).

### 3.6 Credencial GitHub (PAT)

No GitHub:
1. **Settings → Developer settings → Personal access tokens (classic)**
2. **Generate new token (classic)**, escopo `repo` + `workflow`.
3. Copie o token (`ghp_...`).

No Jenkins, **Manage Jenkins → Credentials → System → Global credentials → Add Credentials**:

| Campo | Valor |
|---|---|
| Kind | `Secret text` |
| ID | `github-pat-softon` (**exato**, hardcoded no Jenkinsfile) |
| Secret | cole o PAT |
| Description | `GitHub PAT - upload de releases` |

### 3.7 Credencial do webhook do portal

Mesmo lugar (Global credentials → Add Credentials):

| Campo | Valor |
|---|---|
| Kind | `Secret text` |
| ID | `softon-portal-webhook-secret` (**exato**) |
| Secret | cole aqui o `WEBHOOK_SECRET` que você exportou no §2 |
| Description | `Webhook secret do Softon Portal` |

### 3.8 Smoke test do Jenkins

**New Item → Pipeline**, nome `smoke-test`. Pipeline script:

```groovy
pipeline {
  agent any
  tools { maven 'maven-3.9'; jdk 'jdk-21' }
  stages {
    stage('Sanity') {
      steps {
        sh 'java -version'
        sh 'mvn -v'
      }
    }
  }
}
```

**Save → Build Now**. Console deve mostrar Java 21 + Maven 3.9. Se OK,
**Delete** esse job (foi só pra confirmar).

---

## §4 — Repositório do produto (`dtec-lite-mock`)

Vamos preparar o repo de teste no GitHub.

### 4.1 Repo mínimo

Estrutura:

```
dtec-lite-mock/
├── Jenkinsfile
├── pom.xml
└── src/main/webapp/index.html
```

`pom.xml` minimalista (Maven build → WAR):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.softon</groupId>
  <artifactId>dtec-web</artifactId>
  <version>1.0.0</version>
  <packaging>war</packaging>
  <build>
    <finalName>dtec-web</finalName>
  </build>
</project>
```

`src/main/webapp/index.html`:

```html
<!doctype html>
<html><body><h1>DTEC Lite mock 1.0.0</h1></body></html>
```

### 4.2 Jenkinsfile

Copie o template e ajuste:

```bash
cd dtec-lite-mock
cp ~/softon-portal-api/docs/release-orchestrator/templates/Jenkinsfile .
```

Edite os campos no topo do Jenkinsfile:

```groovy
environment {
  PRODUTO_SIGLA    = 'DTECLD'
  MODULO_CODIGO    = 'dtec-web'
  ARTIFACT_EXT     = 'war'
  BUILD_CMD        = 'mvn -B -ntp clean package -DskipTests'
  BUILD_OUTPUT_DIR = 'target'

  // 👇 IMPORTANTE: usa host.docker.internal pra acessar o portal local
  PORTAL_WEBHOOK_URL = 'http://host.docker.internal:8080/api/v1/release-orchestrator/webhooks/jenkins'

  GH_TOKEN     = credentials('github-pat-softon')
  PORTAL_SECRET = credentials('softon-portal-webhook-secret')
}
```

> `host.docker.internal` é o "localhost da máquina host" visto de dentro
> do container Jenkins. No Linux puro, garanta que está rodando o Docker
> com `--add-host=host.docker.internal:host-gateway` ou use o IP da bridge.

Commit e push:

```bash
git init && git add . && git commit -m "chore: setup"
git branch -M main
git remote add origin git@github.com:seuuser/dtec-lite-mock.git
git push -u origin main
```

---

## §5 — Job no Jenkins apontando pro repo

### 5.1 Criar o job

Jenkins → **New Item → Pipeline**, nome: `dtec-ld-build`.

#### General
- ✅ GitHub project
  - Project url: `https://github.com/seuuser/dtec-lite-mock/`

#### Build Triggers
- ✅ **GitHub hook trigger for GITScm polling**

#### Pipeline
- Definition: **Pipeline script from SCM**
- SCM: **Git**
- Repository URL: `https://github.com/seuuser/dtec-lite-mock.git`
- Credentials: **Add** → Username + Password
  - Username: seu user do GitHub
  - Password: o mesmo PAT (`ghp_...`)
  - ID: `github-user-pat`
- Branch Specifier (blank for 'any'): `refs/tags/v*.*.*`
- Script Path: `Jenkinsfile`

**Save**.

### 5.2 Webhook GitHub → Jenkins

Como Jenkins está em `localhost:8090`, o GitHub público **não consegue
disparar webhook**. Soluções:

#### Opção A — `ngrok` (rápida pra dev)

```bash
ngrok http 8090
# pega a URL https://abc123.ngrok-free.app
```

No GitHub: repo → **Settings → Webhooks → Add webhook**:
- Payload URL: `https://abc123.ngrok-free.app/github-webhook/`
- Content type: `application/json`
- Events: **Just the push event**

#### Opção B — Build manual (sem webhook)

Na tela do job, clique **Build with Parameters** e passe a tag manualmente.
Mais simples pra rodar tudo offline, mas você perde o gatilho automático.

> Pra esse fluxo local, **opção B basta**. Se quiser sentir o pipeline
> real ponta-a-ponta, use ngrok.

---

## §6 — Cadastros no portal

Agora a parte que conecta o portal ao GitHub + Jenkins.

### 6.1 Produto (sidebar Release Orchestrator → Produtos)

**Novo produto** → preencha:

| Campo | Valor |
|---|---|
| Sigla | `DTECLD` |
| Nome | `DTec Lite` |
| Cor | qualquer |
| Ativo | ✅ |

Save. Entra no detalhe do produto.

### 6.2 Aba GitHub

| Campo | Valor |
|---|---|
| Repositório | `seuuser/dtec-lite-mock` |
| Branch padrão | `main` |
| Regex de tag | `^v\d+\.\d+\.\d+$` |
| Token | cole o mesmo PAT do §3.6 |

**Testar conexão** → mensagem verde + "Nenhuma release encontrada" (ainda
não tem releases — é esperado).

### 6.3 Aba Jenkins

| Campo | Valor |
|---|---|
| URL base | `http://localhost:8090` |
| Nome do job | `dtec-ld-build` |
| Usuário | `admin` |
| API token | gere em Jenkins → top-right user → Configure → API Token → Add new |

**Testar conexão** → mensagem verde + "Job encontrado, último build: nenhum".

### 6.4 Aba Módulos

Cadastre só o módulo que vamos buildar:

| Código | Nome | Tipo |
|---|---|---|
| `dtec-web` | Web | `WEB` |

Pra esse fluxo de teste, **um módulo só basta**. Depois você adiciona BANCO/KETTLE.

### 6.5 Cliente ACME

Mesmo do guia 09 §7 — versão resumida:

1. Sidebar → **Clientes → Novo cliente**, sigla `ACME`.
2. Aba **Produtos → Contratar produto → DTECLD**. Versão atual do `dtec-web`: `0.9.0`.
3. Aba **Config. entrega → Editar**:
   - Tipo: `Pasta local`
   - Caminho base: `/tmp/softon-entregas/acme` (crie a pasta antes: `mkdir -p /tmp/softon-entregas/acme`)
4. **Salvar**.

---

## §7 — Primeira release publicada AUTOMATICAMENTE

Agora a parte legal: não vamos fazer upload manual.

### 7.1 Criar a release no portal

Sidebar → **Release Orchestrator → Releases → Nova release**:

| Campo | Valor |
|---|---|
| Produto | `DTECLD` |
| Versão | `1.0.0` (vai casar com a primeira tag) |
| Tipo | `MAJOR` |
| Título | `Primeiro release automatizado` |
| Resumo | qualquer texto |

Adicione 1-2 itens na aba **Itens**.

Status: `RASCUNHO → EM_REVISAO → APROVADA → PUBLICADA`.

### 7.2 Disparar o build no Jenkins

#### Caminho A — webhook automático (se você usou ngrok)

No repo do produto:

```bash
cd dtec-lite-mock
git tag v1.0.0
git push origin v1.0.0
```

O webhook GitHub bate no Jenkins → job dispara automaticamente.

#### Caminho B — build manual

Jenkins → job `dtec-ld-build` → **Build with Parameters**.

Se o job não tem parâmetro, mude temporariamente pra ter `TAG_NAME`
ou crie a tag e clique **Scan repository now / Build Now** (Pipeline
script puxa a tag mais recente).

Forma mais simples sem webhook: edite o job **General → Pipeline → Branch
Specifier** pra `main` (em vez de `refs/tags/v*.*.*`), force um build,
veja a esteira funcionar, e depois volte pra tag spec.

### 7.3 Acompanhar o build

Jenkins job → **Build History → #1 → Console Output**.

Stages esperadas:
1. **Checkout tag** — clona do GitHub
2. **Build** — `mvn package` → gera `target/dtec-web.war`
3. **Locate artifact** — encontra o WAR + monta `dtec-web-1.0.0.war`
4. **Publish to GitHub Release** — cria release no GitHub e sobe o asset
5. **post → success → notificarPortal('SUCCESS')** → POST no webhook do portal

### 7.4 Conferir no portal

Volte na **Tela: Detalhe da release** (`/release-orchestrator/releases/:id`).

Em até 1 segundo após o `success` do Jenkins:
- Aparece **badge verde** no topo: `Build: Sucesso · #1`
- Clicar no badge abre o build no Jenkins em nova aba

Na aba **Artefatos** da release, clique **Sincronizar do GitHub**:
- Portal lê a release `v1.0.0` no GitHub
- Baixa o `dtec-web-1.0.0.war`
- Cacheia em `storage/artefatos/github-cache/DTECLD/1.0.0/dtec-web/`
- Cria registro `ArtefatoReleaseModulo` com SHA-256 calculado

**Pronto:** a release tem artefato sem você ter feito upload.

---

## §8 — Wizard de entrega com artefato vindo do Jenkins

### 8.1 Nova entrega

Sidebar → **Entregas → Nova entrega**.

| Passo | Valor |
|---|---|
| 1 | Cliente `ACME` + Produto `DTECLD` |
| 2 | Release `1.0.0` + responsável (você) |
| 3 | Módulo `dtec-web` marcado, FROM `0.9.0`, TO `1.0.0` |
| 4 | **Calcular delta** → mostra 1 artefato (`dtec-web-1.0.0.war`) |
| 5 | Documento gerado → **Gerar entrega** |

### 8.2 Acompanhar

Tela: **Detalhe da entrega**.

- Badge: `EM_GERACAO` → `CONCLUIDA` (polling de 5s)
- KPI Tamanho do pacote = bytes do WAR + PDF
- Botão **Baixar pacote** baixa o ZIP local

```bash
ls -lh /tmp/softon-entregas/acme/
unzip -l /tmp/softon-entregas/acme/*.zip
```

Estrutura esperada:

```
documento-acme-dtecld-1.0.0.pdf
modulos/dtec-web/dtec-web-1.0.0.war
```

---

## §9 — Próxima release automática (test loop)

Pra validar que tudo está fluido, repita:

```bash
cd dtec-lite-mock
# edite o index.html pra mudar a mensagem
echo "<h1>v1.1.0</h1>" > src/main/webapp/index.html
git commit -am "chore: bump"
git tag v1.1.0
git push && git push origin v1.1.0
```

E no portal:

1. **Nova release** `1.1.0` → publica
2. **Aba Artefatos → Sincronizar do GitHub**
3. **Nova entrega** → FROM `1.0.0` TO `1.1.0`
4. Pacote gerado em segundos

Esse é o loop de operação real — **zero upload manual**.

---

## §10 — Onde cada peça fica

```
┌─────────────────────────────┐    git push v1.1.0
│ Repo GitHub                 │ ◄────────────── você
│ seuuser/dtec-lite-mock      │
└──────┬──────────────────────┘
       │ webhook (ngrok)
       ▼
┌─────────────────────────────┐
│ Jenkins (localhost:8090)    │
│  - dtec-ld-build            │
│  - tools: jdk-21, maven-3.9 │
│  - cred: github-pat-softon  │
│  - cred: softon-portal-...  │
└──────┬──────────────────────┘
       │ gh release upload (PAT)
       │
       │  POST /webhooks/jenkins (PORTAL_SECRET)
       ▼
┌─────────────────────────────┐    REST GitHub API (PAT)
│ Portal API (localhost:8080) │ ──────────────► GitHub Releases
│  - release.buildStatus      │
│  - syncFromGithub()         │ ◄──────────────  WAR baixado
│  - PublicacaoRetryJob       │
└──────┬──────────────────────┘
       │ render UI
       ▼
┌─────────────────────────────┐
│ Frontend (localhost:4200)   │
│  - badge build no header    │
│  - sync de artefatos        │
│  - wizard 5 passos          │
└─────────────────────────────┘
```

---

## §11 — Quando algo dá errado

| Sintoma | Onde olhar primeiro |
|---|---|
| Jenkins não builda | Console output do job; tipicamente é `pom.xml` quebrado ou plugin faltando |
| `git push` não dispara webhook | GitHub → repo → Settings → Webhooks → Recent Deliveries: deve ter resposta 200 |
| Build OK mas asset não foi pro GitHub | Credencial `github-pat-softon` ausente ou PAT sem escopo `repo` |
| Build OK, asset OK, badge não aparece | `PORTAL_WEBHOOK_URL` usa `localhost` em vez de `host.docker.internal` |
| Webhook bate, portal devolve 403 | Secret divergente entre `RELEASE_ORCHESTRATOR_WEBHOOKS_JENKINS_SECRET` (portal) e credencial `softon-portal-webhook-secret` (Jenkins) |
| Portal "Sincronizar do GitHub" não acha release | Tag não casa com regex configurado no produto (`^v\d+\.\d+\.\d+$`) |
| Sync OK mas asset não baixa | PAT no portal sem escopo `repo`, ou nome do asset ≠ `{PRODUTO_SIGLA}-{MODULO}-{VERSAO}.{EXT}` |
| Wizard passo 3 não lista o módulo | Cliente não tem produto contratado, ou módulo `ativo=false` |
| Wizard passo 4 não acha delta | A release no portal tem 0 artefatos — confira o sync e cheque que `ArtefatoReleaseModulo` foi criado |

---

## §12 — Próximos níveis

Quando esse fluxo estiver redondo, evolua:

1. **Conectar destino remoto real** (SFTP do cliente ou MinIO) — guia 09 §7 opções 2 e 3
2. **Cadastrar módulos BANCO e KETTLE** com `caminhoRepo` apontando pra pastas reais no repo → testa delta multi-arquivo
3. **Múltiplos clientes** com versões atuais diferentes → testa cálculo de delta com gap (`1.0.0 → 1.3.0` pulando intermediários)
4. **Webhook real (sem ngrok)** quando for pra produção — runbook 08 cobre
5. **Múltiplos produtos** repetindo §4-§6 — cada produto vira um repo + um job
6. **Pipeline com testes + cobertura** — adicionar `mvn test` antes do package no Jenkinsfile

---

## §13 — Resetar tudo (se precisar)

```bash
# Para tudo
docker stop jenkins softon-postgres
docker rm jenkins softon-postgres
docker volume rm jenkins_home softon-pgdata

# Portal + frontend: Ctrl+C nos terminais

# Artefatos gerados
rm -rf softon-portal-api/storage/* /tmp/softon-entregas
```

Volta pra §2 e refaz limpo.
