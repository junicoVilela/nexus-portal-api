# `infra/docker` — Stack Docker local do Softon Portal

Toda a infraestrutura de **desenvolvimento local** numa pasta só:
Postgres, Jenkins (com plugins + JCasC), MinIO e SFTP. Cada serviço
opcional fica atrás de um **profile** do Docker Compose — você sobe só
o que precisa.

```
infra/docker/
├── docker-compose.yml      # orquestra tudo
├── .env.example            # copie para .env e preencha
├── .gitignore              # ignora .env
├── README.md               # você está aqui
├── jenkins/
│   ├── Dockerfile          # FROM lts-jdk21 + gh CLI + plugins.txt
│   ├── plugins.txt         # lista versionada de plugins
│   └── casc.yaml           # JCasC: admin, tools, credenciais
└── postgres/
    └── init/               # SQLs/scripts de inicialização (vazio hoje)
```

---

## Pré-requisitos

| Ferramenta | Versão | Verificar |
|---|---|---|
| Docker Engine | 20.10+ | `docker --version` |
| Docker Compose | v2 (plugin do Docker) | `docker compose version` |

---

## Quickstart

```bash
cd infra/docker
cp .env.example .env
# Edite o .env: GITHUB_USER, GITHUB_PAT, PORTAL_WEBHOOK_SECRET
docker compose up -d                  # Postgres apenas (mínimo)
```

Os outros serviços ficam atrás de profiles:

```bash
docker compose --profile ci up -d         # + Jenkins
docker compose --profile bucket up -d     # + MinIO (S3)
docker compose --profile sftp up -d       # + SFTP
docker compose --profile all up -d        # tudo de uma vez
```

> **Profiles cumulam.** Subir com `--profile ci` não derruba o Postgres
> que já estava de pé; só **adiciona** o Jenkins.

---

## Mapa de portas

| Serviço | Porta host | Para que |
|---|---|---|
| Postgres | `5432` | Banco do portal API |
| Jenkins UI | `8090` | Acesso à interface (evita conflito com portal 8080) |
| Jenkins agentes | `50000` | Comunicação com agentes (não exponha em rede) |
| MinIO API | `9000` | Endpoint S3 — configure como `endpoint` no destino BUCKET |
| MinIO Console | `9001` | UI web pra criar bucket/inspecionar objetos |
| SFTP | `2222` | Destino remoto pra testar publicação SFTP |

Todas customizáveis via `.env`.

---

## Variáveis sensíveis

O Compose só lê o `.env` — **nunca commite esse arquivo**. O `.gitignore`
local já cobre. Variáveis críticas:

| Variável | Onde usar |
|---|---|
| `GITHUB_PAT` | PAT classic com escopos `repo` + `workflow`. Vira credencial `github-pat-softon` no Jenkins via JCasC. |
| `PORTAL_WEBHOOK_SECRET` | **Tem que ser o mesmo valor** de `RELEASE_ORCHESTRATOR_WEBHOOKS_JENKINS_SECRET` no backend. Gere com `openssl rand -hex 32`. |
| `JENKINS_ADMIN_PASSWORD` | Default `admin` — troque em qualquer cenário que não seja `localhost`. |
| `POSTGRES_PASSWORD` | Bate com `application-dev.yml` por default; mude **junto** se trocar. |

---

## Operação

### Subir/derrubar
```bash
docker compose up -d                     # sobe Postgres (default)
docker compose --profile ci up -d        # sobe + Jenkins
docker compose down                      # derruba tudo (preserva volumes)
```

### Ver logs
```bash
docker compose logs -f postgres
docker compose logs -f jenkins
```

### Restartar 1 serviço
```bash
docker compose restart jenkins
```

### Rebuildar Jenkins após mudar `plugins.txt` ou `casc.yaml`
```bash
docker compose build jenkins
docker compose --profile ci up -d        # recria com a imagem nova
```

### Resetar tudo (apaga dados!)
```bash
docker compose --profile all down -v
docker volume rm $(docker volume ls -q --filter name=softon-) 2>/dev/null
```

---

## Como o Jenkins é provisionado

A imagem custom `softon-portal/jenkins:latest` é construída pelo Compose
a partir de `jenkins/Dockerfile`:

1. **Base** `jenkins/jenkins:lts-jdk21` — já vem com JDK 21.
2. **gh CLI** instalado via APT (Jenkinsfile template usa pra subir
   releases; cai para `curl` na API REST se faltar).
3. **Skip setup wizard** via `JAVA_OPTS`. JCasC cria o admin no boot.
4. **Plugins** instalados via `jenkins-plugin-cli --plugin-file plugins.txt`.
5. **JCasC** carrega `casc.yaml` no boot:
   - `securityRealm` local com admin lido de `JENKINS_ADMIN_USER`/`PASSWORD`
   - 3 credenciais lidas de env: `github-pat-softon`, `github-user-pat`,
     `softon-portal-webhook-secret`
   - tools `jdk-21` (Adoptium auto-install) e `maven-3.9` (3.9.9 auto-install)
   - timestamper ligado em todos os pipelines

Os IDs das credenciais e os nomes das tools são **referenciados
literalmente** pelo template `docs/release-orchestrator/templates/Jenkinsfile`.

---

## Atualizar plugins

```bash
# 1. Edite plugins.txt mudando a versão
$EDITOR jenkins/plugins.txt

# 2. Rebuilde a imagem
docker compose build jenkins

# 3. Suba com a imagem nova
docker compose --profile ci up -d
```

> Pra descobrir a versão mais nova de um plugin, veja em
> https://plugins.jenkins.io/{plugin-id}/ ou rode dentro do container:
>
> ```bash
> docker exec softon-jenkins jenkins-plugin-cli --list \
>   | grep workflow-aggregator
> ```

---

## Atualizar credenciais sem rebuildar

JCasC é lido toda vez que o Jenkins boota. Pra atualizar uma credencial:

```bash
# 1. Edite o .env (não o casc.yaml — os valores reais ficam em env)
$EDITOR .env

# 2. Restart pra forçar reload do JCasC
docker compose restart jenkins
```

Se precisar **mudar a estrutura** do JCasC (nova credencial, nova tool),
edite `jenkins/casc.yaml`, rebuilde:

```bash
docker compose build jenkins
docker compose --profile ci up -d
```

---

## Testar conexão de cada serviço

```bash
# Postgres
docker compose exec postgres pg_isready -U softon_intranet

# Jenkins
curl -fs http://localhost:8090/login >/dev/null && echo "Jenkins OK"

# MinIO API
curl -fs http://localhost:9000/minio/health/live && echo "MinIO OK"

# SFTP (precisa do client)
sftp -P 2222 foo@localhost
```

---

## Integração com o backend e frontend

Estes serviços **não são iniciados pelo Compose** — você roda fora,
direto da IDE ou shell, pra ter hot-reload:

```bash
# Backend (em outro terminal)
cd ../../              # volta pra raiz do softon-portal-api
./mvnw -pl application spring-boot:run -Dspring-boot.run.profiles=dev

# Frontend (em outro terminal)
cd ../softon-portal-web/frontend
npm start
```

| Componente | Onde roda | Aponta para |
|---|---|---|
| Backend Spring Boot | host (`./mvnw`) | `localhost:5432` (Postgres do Compose) |
| Frontend Angular | host (`npm start`) | proxy reverso `/api/*` → `localhost:8080` |
| Jenkins | Compose | `host.docker.internal:8080` (portal API no host) |

> O `extra_hosts: host.docker.internal:host-gateway` no Compose
> resolve esse alias até em Linux puro.

---

## Diagnóstico

| Sintoma | O que checar |
|---|---|
| `connection refused` no backend | `docker compose ps postgres` — container está up? |
| Jenkins demora ~2 min pra ficar UP | Normal no boot (instala tools, carrega JCasC). Veja logs. |
| Webhook do Jenkins devolve 403 no portal | `PORTAL_WEBHOOK_SECRET` no `.env` ≠ do backend |
| Build no Jenkins não acha o portal | `PORTAL_WEBHOOK_URL` no Jenkinsfile precisa ser `http://host.docker.internal:8080/...` |
| Plugin não instala | Versão pinada em `plugins.txt` ficou indisponível — atualize pra outra LTS |
| Porta 8090 já em uso | Mude `JENKINS_PORT` no `.env` |
| Porta 5432 já em uso | Mude `POSTGRES_PORT` no `.env` (lembre de exportar `DB_URL` pro backend) |

---

## Próximos passos

- Use o [`docs/jornadas/10-fluxo-local-completo-com-jenkins.md`](../../docs/jornadas/10-fluxo-local-completo-com-jenkins.md)
  pra fazer o fluxo end-to-end.
- Pra produção, veja
  [`docs/jornadas/08-runbook-primeira-publicacao.md`](../../docs/jornadas/08-runbook-primeira-publicacao.md).
- O template Jenkinsfile fica em
  [`docs/release-orchestrator/templates/Jenkinsfile`](../../docs/release-orchestrator/templates/Jenkinsfile).
