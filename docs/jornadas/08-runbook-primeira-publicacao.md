# 08 — Runbook de primeira publicação e configuração do ambiente

Guia operacional **end-to-end** para subir o Nexus Portal pela primeira vez,
fazer os cadastros mínimos e disparar a primeira entrega real para um cliente.

Pré-requisito: você terminou de ler o [07-runbook-s8-jenkins-github.md](07-runbook-s8-jenkins-github.md)
(Jenkins + GitHub from zero — esse runbook assume que o produto já tem
Jenkinsfile + releases automáticas, conforme S8).

> **Premissas de deploy:**
> - Storage local em disco do servidor do portal (memo `storage-disco-local-servidor.md`).
> - Sem Kubernetes (memo `deployment-sem-kubernetes.md`).
> - PostgreSQL 15+ acessível ao backend.

---

## §1 — Provisionar a máquina do portal

Servidor recomendado (MVP): 1× VM Linux, 4 vCPU, 8 GB RAM, 200 GB SSD.

```bash
# 1.1 — Java 21 (Temurin recomendado)
curl -fsSL https://get.sdkman.io | bash
sdk install java 21.0.11-tem

# 1.2 — PostgreSQL 15 (Debian/Ubuntu)
sudo apt-get update
sudo apt-get install -y postgresql-15 postgresql-client-15

# 1.3 — Node 20 + npm (pro build do frontend)
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo bash -
sudo apt-get install -y nodejs

# 1.4 — Pastas de runtime
sudo mkdir -p /var/lib/nexus/{artefatos,entregas,backup}
sudo chown -R nexus:nexus /var/lib/nexus
```

### 1.1 — Banco

```sql
-- Como superuser postgres
CREATE USER nexus_portal WITH PASSWORD 'TROQUE_AQUI';
CREATE DATABASE nexus_portal OWNER nexus_portal ENCODING 'UTF8';
\c nexus_portal
GRANT ALL ON SCHEMA public TO nexus_portal;
```

Flyway roda as migrations (V1..V14) automaticamente no startup.

---

## §2 — Variáveis de ambiente sensíveis

Crie `/etc/nexus-portal/portal.env` (modo 600, dono `nexus`):

```bash
# Banco
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/nexus_portal
SPRING_DATASOURCE_USERNAME=nexus_portal
SPRING_DATASOURCE_PASSWORD=TROQUE_AQUI

# Profile (ativa logs JSON, retenção real, etc)
SPRING_PROFILES_ACTIVE=prod

# Storage
RELEASE_ORCHESTRATOR_STORAGE_ARTEFATOS_DIR=/var/lib/nexus/artefatos
RELEASE_ORCHESTRATOR_STORAGE_ENTREGAS_DIR=/var/lib/nexus/entregas
RELEASE_ORCHESTRATOR_STORAGE_RETENCAO_DIAS=90

# Cifragem AES-GCM para senhas FTP/SFTP/S3 e tokens GitHub/Jenkins
# Gere com: openssl rand -base64 32
RELEASE_ORCHESTRATOR_ENCRYPTION_KEY=cole_aqui_a_chave_base64

# Webhook Jenkins (S11 P2) — gere com: openssl rand -hex 32
RELEASE_ORCHESTRATOR_WEBHOOKS_JENKINS_SECRET=cole_aqui_o_secret_hex

# Retry de publicação remota (F3 P2) — opcionais (defaults razoáveis)
RELEASE_ORCHESTRATOR_PUBLICACAO_MAX_TENTATIVAS=5
RELEASE_ORCHESTRATOR_PUBLICACAO_BACKOFF_INICIAL_MINUTOS=5
RELEASE_ORCHESTRATOR_PUBLICACAO_BACKOFF_MAX_MINUTOS=240

# JWT secret (auth)
SECURITY_JWT_SECRET=cole_aqui_outra_chave_base64
```

> ⚠ Anote a `RELEASE_ORCHESTRATOR_ENCRYPTION_KEY` em cofre **fora** do servidor.
> Se você perder, todas as senhas cifradas viram lixo e precisam ser
> recadastradas. Nunca rotacione sem ter um plano de re-cifragem.

---

## §3 — Build e deploy do backend

```bash
# 3.1 — Clone e build
git clone https://github.com/junicoVilela/nexus-portal-api.git
cd nexus-portal-api
./mvnw -pl application -am package -DskipTests

# 3.2 — JAR final
ls application/target/nexus-application-*.jar
# Copie para /opt/nexus-portal/portal.jar
```

### 3.1 — systemd unit

`/etc/systemd/system/nexus-portal.service`:

```ini
[Unit]
Description=Nexus Portal API
After=network.target postgresql.service
Wants=postgresql.service

[Service]
Type=simple
User=nexus
Group=nexus
EnvironmentFile=/etc/nexus-portal/portal.env
WorkingDirectory=/opt/nexus-portal
ExecStart=/home/nexus/.sdkman/candidates/java/current/bin/java \
  -Xms512m -Xmx2g \
  -jar /opt/nexus-portal/portal.jar
Restart=on-failure
RestartSec=10
StandardOutput=append:/var/log/nexus-portal/portal.log
StandardError=append:/var/log/nexus-portal/portal.log

[Install]
WantedBy=multi-user.target
```

```bash
sudo mkdir -p /var/log/nexus-portal && sudo chown nexus:nexus $_
sudo systemctl daemon-reload
sudo systemctl enable --now nexus-portal
sudo systemctl status nexus-portal
curl http://localhost:8080/actuator/health   # esperado: {"status":"UP",...}
```

---

## §4 — Build e deploy do frontend

```bash
# 4.1 — Clone, build de produção
git clone https://github.com/junicoVilela/nexus-portal-web.git
cd nexus-portal-web/frontend
npm ci
npm run build -- --configuration production

# 4.2 — Publicar via nginx
sudo cp -r dist/frontend/browser/* /var/www/nexus-portal/
```

`/etc/nginx/sites-available/nexus-portal`:

```nginx
server {
  listen 80;
  server_name portal.nexus.local;

  root /var/www/nexus-portal;
  index index.html;

  # SPA fallback
  location / {
    try_files $uri $uri/ /index.html;
  }

  # Proxy reverso para a API
  location /api/ {
    proxy_pass http://localhost:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
  }
}
```

```bash
sudo ln -s /etc/nginx/sites-available/nexus-portal /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

Acesse `http://portal.nexus.local` — deve abrir a tela de login.

---

## §5 — Cadastros iniciais no portal

### 5.1 — Login do admin seed

Usuário inicial criado pelas migrations do `seguranca`:

| Campo | Valor |
|---|---|
| Email | `admin@nexus.local` |
| Senha | `nexus` (**troque na primeira sessão!**) |

Vá em **Configurações → Usuários** e:

1. Troque a senha do admin.
2. Crie usuários para o time:
   - **Operadores de release** — grupo `RBAC_RELEASE_ORCHESTRATOR_OP`
   - **Suporte/leitura** — grupo `LEITOR`
   - **Devs de produto** — grupo `RBAC_DOCFLOW_EDITOR` (se forem mexer em manuais)

### 5.2 — Cadastrar um produto

**Menu:** Release Orchestrator → Produtos → **Novo produto**

| Campo | Exemplo |
|---|---|
| Sigla | `NEXUSLD` (CAIXA-ALTA, usado em assets e nomes de pacote) |
| Nome | `Nexus Lite` |
| Cor | `#2563eb` (badges na UI) |
| Ativo | `true` |

**Aba GitHub** (S9):
- Repositório: `nexus/nexus-lite`
- Branch padrão: `main`
- Regex de tag: `^v\d+\.\d+\.\d+$`
- Token GitHub: PAT com escopo `repo` (cifrado no banco — nunca devolvido)
- Clique **Testar conexão** — deve listar últimas 5 releases.

**Aba Jenkins** (S11):
- URL base: `https://jenkins.nexus.local`
- Nome do job: `nexus-lite-build`
- Usuário + API token do Jenkins
- Clique **Testar conexão**.

**Aba Módulos:** cadastre os módulos do produto (ex: `nexus-web`, `nexus-batch`,
`nexus-banco`, `nexus-kettle`) com `tipo` (WEB/BATCH/BANCO/KETTLE) e
`caminhoRepo`/`prefixoDDL`/`prefixoDML` (BANCO) ou `caminhoRepo` (KETTLE).

### 5.3 — Cadastrar o cliente

**Menu:** Release Orchestrator → Clientes → **Novo cliente**

| Campo | Exemplo |
|---|---|
| Sigla | `ACME` |
| Nome | `ACME Indústria Ltda` |
| CNPJ | `00.000.000/0001-00` |
| Ambiente padrão | `PROD` |
| Tipo banco | `ORACLE` |
| Codificação | `UTF-8` |

**Aba Contatos:** adicione pelo menos um contato técnico e um operacional
(receberão e-mails de notificação).

**Aba Produtos:** **Contratar produto** → escolha `NEXUSLD`. Para cada módulo,
informe a `versão atual` que o cliente está rodando (ex: `1.4.0`). Isso é o
ponto de partida do delta.

**Aba Funcionalidades:** marque as funcionalidades que o cliente tem licença
de usar (matriz por domínio).

**Aba Config. entrega:** escolha o tipo de destino:

| Tipo | Quando usar | Campos |
|---|---|---|
| `PASTA` | Cliente compartilha pasta local/montada | Caminho base absoluto |
| `FTP` | Servidor legado | Host, porta, usuário, senha, modo passivo |
| `SFTP` | Default seguro | Host, porta, usuário, senha, validar fingerprint |
| `BUCKET` | S3 ou MinIO | Bucket, endpoint (vazio = AWS), região, access key, secret key, path-style (ON pra MinIO) |

Clique **Testar conexão** antes de salvar. Marque **Exigir aprovação** se a
entrega só puder ser publicada depois de validada por um segundo operador.

---

## §6 — Primeira release publicada

> Esta etapa pressupõe que o repositório do produto já tem `Jenkinsfile`
> conforme o template `docs/release-orchestrator/templates/Jenkinsfile`
> e o job Jenkins está disparando build-on-tag (passo §6 do runbook 07).

### 6.1 — Cadastrar a release no portal

**Menu:** Release Orchestrator → Releases → **Nova release**

| Campo | Exemplo |
|---|---|
| Produto | `NEXUSLD` |
| Versão | `1.5.0` |
| Tipo | `MINOR` |
| Status | `RASCUNHO` |
| Título | `Sprint 23 — relatórios novos` |

Na aba **Itens**, adicione as novidades/melhorias/correções (categoria +
título + descrição + visibilidade). Esse é o conteúdo que vai sair no
release-notes PDF.

Avance status: `RASCUNHO → EM_REVISAO → APROVADA → PUBLICADA`.

### 6.2 — Disparar o build

No repo do produto:

```bash
git tag v1.5.0
git push origin v1.5.0
```

O Jenkins detecta a tag, builda, faz upload do asset na GitHub Release, e ao
final dispara o webhook para o portal. No detalhe da release você verá o
badge **Build: Sucesso · #N** apontando para o build no Jenkins.

### 6.3 — Verificar assets

Na aba **Artefatos** da release, clique **Sincronizar do GitHub**. O portal
baixa os assets WEB/BATCH e cacheia em
`/var/lib/nexus/artefatos/github-cache/{produto}/{release}/{módulo}/`.

---

## §7 — Primeira entrega ao cliente

**Menu:** Release Orchestrator → Entregas → **Nova entrega** (ou via wizard
em 5 passos)

### Passo 1 — Cliente + produto + release
- Cliente: `ACME`
- Produto: `NEXUSLD`
- Release: `1.5.0`

### Passo 2 — Módulos a entregar
Marque quais módulos vão no pacote. Para módulos com versão atual no
contrato, o sistema sugere `FROM = versão atual`, `TO = 1.5.0`.

### Passo 3 — Calcular delta
Clique **Calcular delta**. O backend:
- WEB/BATCH: identifica artefatos novos vs versão FROM.
- BANCO: chama `GithubDeltaBancoService` — concatena DDL.sql + DML.sql por
  dialeto. Para multi-dialeto (ORACLE+SQLSERVER) sai `oracle/DDL.sql` etc.
- KETTLE: chama `GithubDeltaKettleService` — pega `.ktr`/`.kjb` e suas
  dependências por BFS.

Revise a contagem por módulo. Se houver gap (ex: cliente em 1.2.0, hoje
você libera 1.5.0), o sistema concatena os deltas das versões intermediárias
automaticamente.

### Passo 4 — Documento entregue
Edite o documento (markdown) com o que será publicado. PDF gerado no
empacotamento usa esse conteúdo.

### Passo 5 — Geração + publicação
Clique **Gerar pacote**. Em background:
1. `EmpacotadorEntrega` cria ZIP em `/var/lib/nexus/entregas/acme/nexusld/1.5.0/`.
2. Status: `EM_GERACAO → CONCLUIDA`. SHA-256 salvo em audit.
3. Se `ConfigEntrega.tipoDestino != PASTA`, status_publicacao vira `PENDENTE`
   e a primeira tentativa é agendada para "agora".
4. `PublicacaoRetryJob` (a cada 2 min) tenta enviar para FTP/SFTP/S3. Se
   sucesso → `OK`. Se falha → backoff 5/10/20/40/80 min, esgotando vira `FALHA`.

No detalhe da entrega:
- Badge **Status entrega** (CONCLUIDA).
- Card **Publicação remota** com badge (PENDENTE/OK/FALHA), tentativas,
  próxima tentativa, destino final.
- Botão **Tentar agora** força nova tentativa sem esperar o backoff.
- Botão **Baixar pacote** salva ZIP local (útil pra suporte).

---

## §8 — Verificações pós-go-live

### 8.1 — Health
```bash
curl http://localhost:8080/actuator/health | jq
# Deve ter: storage UP, github UP, jenkins UP (cache 1 min).
```

### 8.2 — Métricas
```bash
curl http://localhost:8080/actuator/prometheus | grep entrega_geracao
# entrega.geracao.duration (P50/95/99)
# entrega.geracao.resultado{status=sucesso|falha|cancelada}
```

Aponte um Prometheus/Grafana para o endpoint se quiser dashboards.

### 8.3 — Logs JSON
```bash
tail -f /var/log/nexus-portal/portal.log | jq
# Procure por entregaId/clienteId via MDC enrichment.
```

### 8.4 — Retenção
Job roda diário às 03:00 (configurável via `release-orchestrator.storage.retencao-cron`).
Pacotes mais velhos que `retencao-dias` (default 90) viram registros sem
arquivo físico — SHA-256 e metadados preservados para auditoria.

### 8.5 — Backup
**Banco:** `pg_dump` diário do schema. Sem o banco você perde todo o
histórico, contratos, deltas e cifragens.

**Storage local:** rsync de `/var/lib/nexus/entregas/` para máquina
secundária (rclone, restic, etc). Pacotes antigos já são reproduzíveis a
partir do banco + GitHub, então backup é desejável mas não crítico.

**Chaves:** `RELEASE_ORCHESTRATOR_ENCRYPTION_KEY`,
`RELEASE_ORCHESTRATOR_WEBHOOKS_JENKINS_SECRET` e `SECURITY_JWT_SECRET` em
cofre off-server (1Password, Vault, etc).

---

## §9 — Checklist de "tudo pronto pra cliente"

Antes de comunicar a entrega ao cliente:

- [ ] Entrega `status = CONCLUIDA`
- [ ] `statusPublicacao = OK` (se destino remoto) **ou** ZIP baixado/copiado manualmente (se PASTA local)
- [ ] PDF do release-notes revisado (`PDF Cliente`)
- [ ] Lista de itens da release confere com o que foi prometido
- [ ] Versões dos módulos no contrato do cliente foram atualizadas (`ClienteProdutoModulo.versaoAtual = 1.5.0`)
- [ ] E-mail enviado para os contatos da aba **Contatos** com link do PDF + SHA-256 do ZIP

---

## §10 — Atalhos para diagnóstico rápido

| Sintoma | Onde olhar |
|---|---|
| Build sumiu / portal mostra build antigo | `/actuator/health` da integração Jenkins; webhook secret correto no Jenkinsfile? |
| Falha "Token GitHub inválido" | Re-cifre o token no produto (PAT pode ter expirado) |
| `statusPublicacao` preso em PENDENTE | Veja `ultima_falha_publicacao` no detalhe da entrega; teste conexão na config |
| Pacote sumiu do disco | Cron de retenção. Confira `data_conclusao` da entrega vs `retencao-dias` |
| `403` em todo webhook | `RELEASE_ORCHESTRATOR_WEBHOOKS_JENKINS_SECRET` ausente ou diferente do Jenkins |
| Logs sem campos JSON | Profile errado — confirme `SPRING_PROFILES_ACTIVE=prod` |

---

## Próximos passos sugeridos

1. **Replicar §5.2 para todos os produtos** que vão entrar no portal.
2. **Replicar §5.3 para todos os clientes** que vão consumir releases.
3. **Configurar Prometheus + Grafana** apontando para `/actuator/prometheus`.
4. **Aplicar template de Jenkinsfile** em cada repo de produto (runbook 07 §0.4).
5. **Treinar operadores** com a [roteiro de demo](05-demo-roteiro-acme.md).
