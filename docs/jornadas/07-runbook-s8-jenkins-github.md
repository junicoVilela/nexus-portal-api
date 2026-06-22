# 07 — Runbook S8: piloto Jenkins + GitHub (DTEC-LD)

Operacional para fechar a [Sprint 8](02-checklist-por-sprint.md) — **fora do
portal** — colocando o piloto DTEC-LD em build-on-tag com publicação no GitHub
Releases. Cobre instalação do Jenkins do zero, PAT, job, primeira tag de
teste e validação.

> Templates de arquivos: [`../release-orchestrator/templates/`](../release-orchestrator/templates/)
> Convenção mestre: [`../release-orchestrator/40-guia-versao-tag.md`](../release-orchestrator/40-guia-versao-tag.md)

---

## 0. Instalação do Jenkins (from zero)

Pule esta seção se já existir um Jenkins acessível ao time. Caso contrário,
o caminho mais rápido é Docker; se houver requisito de servidor dedicado,
ver §0.5.

### 0.1 Container Docker (recomendado para PoC e times pequenos)

```bash
# Volume nomeado para persistir configs e jobs entre reinícios
docker volume create jenkins_home

docker run -d \
  --name jenkins \
  --restart unless-stopped \
  -p 8080:8080 -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  jenkins/jenkins:lts-jdk21
```

- `8080`: UI HTTP. Coloque atrás de um nginx/Traefik se for expor publicamente.
- `50000`: porta de agentes (não precisa expor externamente no MVP).
- Volume `jenkins_home` guarda tudo — não deletar.
- Imagem `lts-jdk21` já vem com JDK 21; evita configurar tool depois.

### 0.2 Senha inicial e setup wizard

```bash
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

1. Abra `http://localhost:8080` (ou o host onde o container roda).
2. Cole a senha. **Install suggested plugins** — já inclui Pipeline, Git,
   GitHub, Credentials Binding.
3. Crie o usuário admin com senha forte.
4. URL do Jenkins: confirme a URL pública (será usada no webhook GitHub).

### 0.3 Plugins adicionais necessários

Após o wizard, em **Manage Jenkins → Plugins → Available plugins**, instale:

| Plugin | Para que serve |
|---|---|
| `Pipeline` | Já vem com "suggested" — confirme |
| `GitHub` | Webhooks `/github-webhook/` |
| `GitHub Branch Source` | Trigger por tag |
| `Generic Webhook Trigger` | Alternativa flexível pro disparo por tag |
| `Credentials Binding` | Bind do PAT em variável de ambiente |
| `Pipeline: Stage View` (opcional) | UI bonita do pipeline |
| `Timestamper` | Timestamps nos logs (já usado no Jenkinsfile template) |

Reinicie ao final da instalação (checkbox "Restart Jenkins when installation
is complete and no jobs are running").

### 0.4 Tools globais

**Manage Jenkins → Tools**:

- **JDK installations**:
  - Name: `jdk-21`
  - Install automatically (Adoptium) ou caminho local
- **Maven installations**:
  - Name: `maven-3.9`
  - Install automatically (Apache Maven 3.9.x)

> Estes nomes são referenciados literalmente no
> [`../release-orchestrator/templates/Jenkinsfile`](../release-orchestrator/templates/Jenkinsfile)
> — se mudar, ajuste o template também.

### 0.5 gh CLI no agente

Se for usar o `gh` no upload de assets (mais limpo que `curl`):

```bash
# Para o container LTS oficial (Debian-based):
docker exec -u root jenkins bash -c '
  curl -fsSL https://cli.github.com/packages/githubcli-archive-keyring.gpg \
    | dd of=/usr/share/keyrings/githubcli-archive-keyring.gpg && \
  echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/githubcli-archive-keyring.gpg] \
    https://cli.github.com/packages stable main" \
    > /etc/apt/sources.list.d/github-cli.list && \
  apt update && apt install -y gh
'
docker exec jenkins gh --version
```

Se faltar `gh`, o Jenkinsfile template cai para `curl` direto na API REST.

### 0.6 Instalação em servidor dedicado (alternativa)

Quando o container não couber (firewall corporativo, requisito de SSO via
LDAP, agentes Windows etc.), instale o pacote `.deb`/`.rpm` oficial. Resumo
do passo-a-passo:

1. JDK 21 disponível no sistema (`apt install temurin-21-jdk` ou equivalente).
2. Adicionar repositório APT/YUM do Jenkins LTS:
   `https://www.jenkins.io/doc/book/installing/linux/`
3. `systemctl enable --now jenkins`.
4. Senha inicial em `/var/lib/jenkins/secrets/initialAdminPassword`.
5. Repetir §0.3 e §0.4 dentro da UI.

### 0.7 Smoke test do Jenkins

Antes de seguir, valide que tudo funciona com um job dummy:

1. **New Item → Pipeline**, nome `smoke-test`.
2. Pipeline script:
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
3. **Build Now** → console deve mostrar Java 21 e Maven 3.9 ativos.

Se OK, apague o job e siga para §1.

---

## 0. Pré-requisitos (resumo)

Após §0.1–§0.7, você deve ter:

| Item | Onde | Como conferir |
|---|---|---|
| Jenkins acessível | URL pública do Jenkins | login funciona, Console online |
| Plugins | Jenkins → "Manage Plugins" | `Pipeline`, `GitHub`, `GitHub Branch Source`, `Credentials Binding`, `Generic Webhook Trigger`, `Timestamper` |
| Tool `maven-3.9` | Jenkins → "Tools" | versão instalada e nomeada `maven-3.9` |
| Tool `jdk-21` | idem | JDK 21 nomeado `jdk-21` |
| `gh` CLI (opcional) | agente do Jenkins | `gh --version` ≥ 2.0 |
| Smoke test `smoke-test` | Jenkins → Build History | SUCCESS, mostra java + mvn |

Se faltar `gh`, o `Jenkinsfile` cai para `curl` direto na API REST.

---

## 1. Criar o Personal Access Token (PAT)

1. Em github.com → seu perfil → **Settings → Developer settings → Personal access tokens → Tokens (classic)**.
2. **Generate new token (classic)** com:
   - Nome: `softon-jenkins-{{ produto-sigla }}-build`
   - Expiration: 90 dias (renovar via lembrete no calendário)
   - Escopos: `repo` (todos os subitens) + `workflow`
3. Guardar o token em local seguro (não vai aparecer de novo).

> Para um único PAT compartilhado entre repos do mesmo time, use o nome
> genérico `softon-jenkins` e armazene em vault.

---

## 2. Registrar credencial no Jenkins

1. Jenkins → **Manage Jenkins → Credentials → System → Global credentials**.
2. **Add Credentials**:
   - Kind: `Secret text`
   - ID: `github-pat-softon` (este ID está hardcoded no Jenkinsfile template)
   - Secret: cole o PAT
   - Description: `GitHub PAT - upload de releases (escopo repo)`
3. Salvar.

---

## 3. Preparar o repositório do produto

Considerando `softon/dtec-ld` como exemplo:

```bash
cd softon/dtec-ld
git checkout -b chore/cicd-setup
cp ../../softon-portal-api/docs/release-orchestrator/templates/Jenkinsfile .
mkdir -p docs
cp ../../softon-portal-api/docs/release-orchestrator/templates/VERSIONING.md docs/

# ajuste os placeholders {{ }} no Jenkinsfile e no VERSIONING.md:
$EDITOR Jenkinsfile docs/VERSIONING.md

git add Jenkinsfile docs/VERSIONING.md
git commit -m "chore(cicd): pipeline build-on-tag + versionamento"
git push origin chore/cicd-setup
# abrir PR e mergear na main
```

Variáveis a preencher (em `Jenkinsfile`):

| Variável | DTEC-LD |
|---|---|
| `PRODUTO_SIGLA` | `dtecld` |
| `MODULO_CODIGO` | `dtec-web` |
| `ARTIFACT_EXT` | `war` |
| `BUILD_CMD` | `mvn -B -ntp clean package -DskipTests` |
| `BUILD_OUTPUT_DIR` | `target` |

---

## 4. Criar o job no Jenkins

1. **New Item → Pipeline**, nome: `dtec-ld-build`.
2. **General → GitHub project**: `https://github.com/softon/dtec-ld`.
3. **Build Triggers**: marcar **GitHub hook trigger for GITScm polling**.
4. **Pipeline**:
   - Definition: `Pipeline script from SCM`
   - SCM: Git
   - Repository URL: `https://github.com/softon/dtec-ld.git`
   - Credentials: outra credencial (Username/Password) para clone — pode reutilizar o PAT como password com qualquer usuário.
   - Branch Specifier: `refs/tags/v*.*.*`
   - Script Path: `Jenkinsfile`
5. Salvar.

> Se preferir build automático em qualquer tag, use o
> [Generic Webhook Trigger](https://plugins.jenkins.io/generic-webhook-trigger/)
> com filtro de `ref` começando em `refs/tags/v`.

---

## 5. Configurar o webhook no GitHub

Em `softon/dtec-ld → Settings → Webhooks → Add webhook`:

- **Payload URL**: `https://jenkins.softon.{{ tld }}/github-webhook/`
- **Content type**: `application/json`
- **Events**: `Just the push event` (suficiente; tags são push events).
- Ative SSL verification.

Após adicionar, o GitHub envia um ping; confirmar status verde.

---

## 6. Tag de teste

```bash
cd softon/dtec-ld
git checkout main && git pull --ff-only
git tag -a v0.0.1 -m "Teste do pipeline build-on-tag"
git push origin v0.0.1
```

Acompanhar:

1. Jenkins: `dtec-ld-build` deve aparecer em "Build History" rodando.
2. Console deve mostrar:
   - `Build da tag v0.0.1`
   - `mvn package` rodando
   - `Locate artifact` achando o `.war`
   - `Publish to GitHub Release` com `Asset uploaded`
3. GitHub → `softon/dtec-ld/releases/tag/v0.0.1` → **deve conter** o asset `dtecld-dtec-web-0.0.1.war`.

Se falhar, ver troubleshooting em `VERSIONING.md` §Troubleshooting.

---

## 7. Limpar a tag de teste

A tag `v0.0.1` é descartável. Antes de continuar para a tag real:

```bash
# remove no remoto
git push --delete origin v0.0.1
# remove local
git tag -d v0.0.1
# no GitHub: apagar manualmente a Release v0.0.1 também
```

---

## 8. Primeira tag real `v1.5.0`

Pré-requisito: release `1.5.0` deve estar **PUBLICADA** no Release Orchestrator (`/release-orchestrator/releases`).

```bash
cd softon/dtec-ld
git checkout main && git pull --ff-only
git tag -a v1.5.0 -m "Release 1.5.0 — corresponde à release publicada no portal"
git push origin v1.5.0
```

Conferir:
- Jenkins `SUCCESS`
- Asset `dtecld-dtec-web-1.5.0.war` no GitHub Release
- SHA-256 do asset (campo `Digest`)

---

## 9. DoD da Sprint 8

Da [`02-checklist-por-sprint.md`](02-checklist-por-sprint.md) §S8:

- [x] Repo `softon/dtec-ld` com `Jenkinsfile` build-on-tag — passo 3
- [x] Job Jenkins `dtec-ld-build` apontando ao repo — passo 4
- [x] Credencial GitHub PAT no Jenkins — passo 2
- [x] Tag teste `v0.0.1` → GitHub Release com asset nomeado `{sigla}-{modulo}-{versao}.war` — passo 6
- [x] `docs/VERSIONING.md` publicado no repo — passo 3
- [ ] Repo BD (se existir separadamente) — usar checklist BANCO em [`templates/README.md`](../release-orchestrator/templates/README.md#banco)
- [ ] Repo Kettle (se existir) — usar checklist KETTLE

DoD principal: **um produto piloto com pipeline repo → Jenkins → GitHub Release funcionando**. Atingido com DTEC-LD.

---

## 10. Próximos repos

Para cada novo produto (DTEC-CR, DTEC-ONLINE, FOLHA-WEB, etc.):

1. Repetir os passos 3–6 com as variáveis do produto.
2. Reutilizar o PAT `github-pat-softon`.
3. Reutilizar a credencial Jenkins.
4. Tempo estimado por repo: **2 horas** se o produto já compila com Maven/Gradle padrão.

---

## 11. O que isso desbloqueia (S9)

Com a pipeline rodando, a S9 do portal pode:

- Adicionar campos `repositorioGithub`, `branchPadrao`, `padraoTag` no cadastro de Produto.
- Implementar `GitHubReleasesAdapter` no backend para baixar assets de uma tag.
- Substituir o upload manual de WEB pela busca automática no GitHub.

Ver [`02-checklist-por-sprint.md`](02-checklist-por-sprint.md) §Sprint 9.
