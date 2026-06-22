# 02 — Checklist por sprint

Plano executável em sprints de **~2 semanas**. Ajuste a duração conforme o time.

**Legenda:** `[ ]` pendente · `[x]` concluído · **🔧** repo/infra (fora do portal) · **📋** portal

**Produto piloto sugerido:** DTEC-LD (`DTECLD`)

---

## Visão rápida

| Sprint | Foco | Saída verificável |
|---|---|---|
| S0 | PRÉ — DocFlow integrado | Login real → publicação funciona |
| S1 | F0 backend — módulos + artefatos | API upload por módulo na release |
| S2 | F0 front — artefatos + PDF | Tela upload + PDF na release |
| S3 | F1 backend — cadastros cliente | CRUD cliente + produtos contratados |
| S4 | F1 backend — entrega core | Assistente API + geração pacote local |
| S5 | F1 front — clientes + agenda | Telas cliente + próximas entregas |
| S6 | F1 front — assistente + geração | Wizard 5 passos + polling geração |
| S7 | DocFlow + entrega integrada | Jornada ACME manual end-to-end |
| S8 | 🔧 F2.0 — Jenkins + GitHub (piloto) | Tag v0.0.1 → asset na Release |
| S9 | F2 portal — GitHub adapter | Download asset TO_TAG na geração |
| S10 | F2 portal — delta Git | Diff BANCO/KETTLE FROM..TO |
| S11 | F2 front + operação | Cadastro GitHub/Jenkins + build status |
| S12+ | F3 / F4 | FTP, observabilidade (conforme prioridade) |

**Trilha paralela 🔧 (S2–S8):** Jenkinsfile nos repos — não bloqueia S1–S7 se MVP usa upload manual.

---

## Sprint 0 — PRÉ: DocFlow integrado

**Objetivo:** manual operando contra API real.

### Backend
- [x] Confirmar endpoints em `/api/v1/docflow/*` documentados
- [x] Validar login JWT `POST /api/v1/auth/login` (admin/editor seed)
- [x] `GET /api/v1/auth/me` para usuário autenticado

### Frontend
- [x] Corrigir proxy ou prefixo services (`/api/doc-flow` → `/api/v1/docflow`)
- [x] Migrar `AuthApiService` de mock para HttpClient real
- [x] Implementar ou adaptar `GET /auth/me` (permissões do catálogo RBAC na base)
- [x] `ConfiguracaoService` → `EmpresaController` (logo empresa)
- [x] Interceptor JWT em todas as chamadas DocFlow

### QA (DoD)
- [x] Login `admin/admin` → dashboard DocFlow
- [x] Menu filtrado por `/auth/me` (ex.: **Configurações** visível com `CONFIGURACAO:EDITAR`)
- [x] CRUD cliente DocFlow (+ vínculos via API)
- [x] Página até **PUBLICADO** → publicação **SUCESSO** → download ZIP

_Validado em 2026-06-19: smoke test end-to-end — login JWT de `admin`/`editor`/`revisor`, `/auth/me` retornando grupos + permissões do catálogo RBAC, proxy `/api/doc-flow/*` rewriting correto pra `/api/v1/{public,preview-tokens,preview,docflow}/*`, e backend tests 31/31 verdes. Hotfix nesse mesmo dia: V12 gravava hash BCrypt errado pro `revisor` (`fix(rbac): move ajustes do REVISOR para V17`)._

**Spec:** [`../doc-flow/README.md`](../doc-flow/README.md) § Integração

---

## Sprint 1 — F0 backend: módulos e artefatos

**Objetivo:** release sabe quais módulos tem e aceita upload.

### 🛡️ Higiene de repo (1–2 dias antes de F0.3)

Bloco curto no início da sprint pra fechar dívida técnica que ficou exposta no fim da S0 (o bug do hash BCrypt do `revisor` só apareceu no smoke manual — CI teria pegado em PR).

- [x] GitHub Actions: workflow `ci.yml` rodando `./mvnw test` em push/PR pra `main` (`softon-portal-api`) — _job `Test (Java 21)`_
- [x] GitHub Actions: workflow `ci.yml` rodando `ng test --watch=false --browsers=ChromeHeadless` em push/PR pra `main` (`softon-portal-web`) — _job `Test (Node 22, Chrome headless)`_
- [ ] ~~Branch protection em `main` (ambos repos)~~ — **bloqueado pelo plano:** branch protection e rulesets exigem GitHub Pro pra repos privados. Reabrir se decidirmos upgrade ou tornar repos públicos.
- [x] Teste de integração com Testcontainers: `AuthSeedsIntegrationTest` aplica V1–V17 num Postgres efêmero e valida login dos 3 seeds (`admin/admin`, `editor/editor`, `revisor/revisor`) — pega regressões nos seeds em CI antes do smoke manual

### Backend (release-orchestrator)
- [x] Migration + entidade `ModuloProduto` (tipos WEB/BATCH/BANCO/KETTLE/FUNC/REGRAS) — F0.3
- [x] Migration + entidade `ReleaseModuloVersao` — F0.4
- [x] Migration + entidade `ArtefatoReleaseModulo` + storage filesystem — F0.5
- [x] CRUD módulos por produto — F0.6 part 1
- [x] Endpoints upload / listar / excluir artefato por release+módulo — F0.6 part 2
- [x] Testes service: upload, validação tipo, release publicada = imutável (10 testes em ArtefatoReleaseModuloServiceTest)

### Frontend
- [ ] (Opcional neste sprint) stub ou API-only — UI na S2

### DoD
- [x] CI rodando em ambos repos, teste de seeds verde (branch protection adiada — exige Pro)
- [ ] Postman/curl: criar módulo WEB em DTEC-LD, upload `.war` na release em rascunho (validação manual pendente — endpoints prontos)

**Specs:** `10-produtos-modulos-artefatos.md`, ROADMAP F0.3–F0.6

---

## Sprint 2 — F0 front: artefatos + PDF

**Objetivo:** release completa visualmente + PDF.

### Backend
- [x] Renderer Markdown → PDF (openhtmltopdf + commonmark + Thymeleaf) — F0.7
- [x] `GET /releases/{id}/pdf?tipo=INTERNO|CLIENTE|SUPORTE` — F0.8
- [x] (P1) `ROLE_LEITOR` + GET-only onde aplicável — F0.9 (renomeado de VIEWER pra alinhar com seed)
- [x] CRUD `ReleaseModuloVersao` (vínculo versão por módulo) — endpoints REST

### Frontend
- [x] CRUD módulos por produto (`/release-orchestrator/produtos/:id/modulos`) — F0.14
- [x] Aba **Artefatos** no detalhe da release — upload + listar + download + excluir — F0.12
- [x] Botão gerar/preview PDF no detalhe (3 tipos: Cliente/Suporte/Interno) — F0.8 front
- [x] Vincular versão por módulo na release (input dentro do card do módulo na aba Artefatos)

### DoD
- [ ] Release DTEC-LD 1.5.0: módulos catalogados, `.war` + `.zip` sql uploadados, PDF baixado (validação manual pendente)

**Specs:** `14-release-orchestrator-detalhe.md`, `25-documento-release-md-pdf.md`, ROADMAP F0.7–F0.14

---

## Sprint 3 — F1 backend: cadastros de cliente

**Objetivo:** orchestrator conhece clientes operacionais.

### Backend
- [x] Migrations: Cliente, Contato, ConfigEntrega, Dominio, Funcionalidade — F1.2a/b, F1.3
- [x] Migrations: ClienteFuncionalidade, ClienteProduto, ClienteProdutoModulo — F1.4, F1.5
- [x] CRUD Cliente + Contatos — F1.2a/b
- [x] CRUD Domínio/Funcionalidade por produto — F1.3
- [x] Matriz ClienteFuncionalidade — F1.4
- [x] Produtos e módulos contratados + `versaoAtual` por módulo — F1.5
- [x] Config entrega (MVP: destino PASTA local) — F1.2b

### Frontend
- [ ] (Opcional) telas na S5 — validar via API neste sprint

### DoD
- [ ] Cliente ACME cadastrado: DTEC-LD contratado, módulos WEB+BANCO, v1.4.0 instalada, 23 funcionalidades marcadas (validação manual via Postman — endpoints prontos)

**Specs:** `02`–`07`, ROADMAP F1.2–F1.6

---

## Sprint 4 — F1 backend: entrega e geração

**Objetivo:** job assíncrono monta pacote MVP (upload manual).

### Backend
- [x] Migrations: ProximaEntrega, Entrega + EntregaModulo + EntregaModuloArtefato — F1.7, F1.8, F1.9, F1.10
- [x] API wizard entrega: rascunho (criar), salvar passo (atualizarRascunho), finalizar (geracao/iniciar) — F1.8, F1.11
- [x] Seleção módulos: contratados vs release, versão atual vs nova — F1.9
- [x] **Delta MVP:** metadados + artefatos uploadados (sem Git) — F1.10
- [x] Geração @Async: copiar artefatos release → montar ZIP — F1.11
- [x] Etapa FUNC/REGRAS: render templates + ClienteFuncionalidade — F1.12
- [x] PDF release-notes filtrado cliente — F1.13
- [x] manifest.json + SHA256SUMS.txt — F1.14
- [x] Atualizar `ClienteProdutoModulo.versaoAtual` ao concluir — F1.11
- [x] Reentrega (`entregaOriginalId`) — F1.15

### DoD
- [ ] `POST` gerar entrega ACME → pacote em pasta local → status CONCLUIDA → versão 1.5.0 registrada (validação manual via Postman — endpoints prontos)

**Specs:** `18`–`21`, ROADMAP F1.7–F1.15

---

## Sprint 5 — F1 front: clientes e agenda

**Objetivo:** operador planeja entregas.

### Frontend
- [ ] Shell `/orchestrator/*` (ou prefixo acordado)
- [ ] Lista + cadastro clientes (`02`, `03`)
- [ ] Visão geral cliente (`04`)
- [ ] Abas: funcionalidades (`05`), produtos contratados (`06`), config entrega (`07`)
- [ ] Agenda próximas entregas (`16`, `17`)
- [ ] Dashboard entregas KPIs básicos (`01` orchestrator) — P1

### DoD
- [ ] Gestor cadastra ACME, planeja entrega v1.5.0 para sábado, status PLANEJADA/APROVADA

---

## Sprint 6 — F1 front: assistente e geração

**Objetivo:** wizard completo + acompanhamento.

### Frontend
- [ ] Assistente 5 passos (`18`): Cliente → Produto → Versões → Módulos → Revisão
- [ ] Tela seleção módulos (`19`): contratado, versão atual/nova, mudança detectada
- [ ] Tela geração com progresso 11 etapas (`21`)
- [ ] Detalhe entrega (`22`): download, reentregar
- [ ] Histórico entregas (`23`)
- [ ] Polling status GERANDO (padrão DocFlow publicações)

### DoD
- [ ] Operador gera pacote ACME pelo wizard sem Postman; baixa ZIP; histórico visível

---

## Sprint 7 — Integração: jornada ACME (MVP manual)

**Objetivo:** validar [`00-cenario-feliz-acme.md`](00-cenario-feliz-acme.md) sem GitHub.

### Release + entrega
- [ ] João: builder release 1.5.0 → revisão → PUBLICADA
- [ ] Upload manual artefatos na release (war, sql zip)
- [ ] Carlos: agenda → Pedro: assistente → pacote ACME
- [ ] Suporte: consulta histórico (mínimo lista filtrada)

### DocFlow (paralelo)
- [ ] Lucia: páginas PUBLICADO alinhadas à release
- [ ] Publicação DocFlow ACME `2026.06.1`

### QA
- [ ] Roteiro único documentado (15–30 min demo)
- [ ] Bugs críticos integração DocFlow resolvidos (S0)

### DoD Fase 1
- [ ] Critérios ROADMAP F1 atendidos (cadastro, planejar, gerar, histórico, reentregar)

---

## Sprint 8 — 🔧 F2.0: Jenkins + GitHub (piloto DTEC-LD)

**Objetivo:** tag no repo → asset no GitHub Release. **Fora do portal.**

### Jenkins from zero (runbook `07` §0)
- [ ] Servidor/container Jenkins instalado (docker `jenkins/jenkins:lts-jdk21` ou pacote LTS)
- [ ] Setup wizard concluído: admin, URL pública definida
- [ ] Plugins: Pipeline, GitHub, GitHub Branch Source, Credentials Binding, Generic Webhook Trigger, Timestamper
- [ ] Tools globais: `maven-3.9`, `jdk-21`
- [ ] `gh` CLI no agente (opcional — fallback para curl)
- [ ] Smoke test `smoke-test` rodando java + mvn com sucesso

### Por repositório (spec `39`)
- [ ] Repo WEB `softon/dtec-ld`: `Jenkinsfile` build-on-tag
- [ ] (Se separado) repo DB: pastas `db/`, convenção DDL_### / DML_###
- [ ] (Se separado) repo Kettle: pasta `kettle/` ou `pdi/`
- [ ] Job Jenkins `dtec-ld-build` apontando ao repo
- [ ] Credencial GitHub PAT no Jenkins (`repo` + upload release)
- [ ] Tag teste `v0.0.1` → GitHub Release com asset nomeado `{sigla}-{versao}.war`
- [ ] Publicar `docs/VERSIONING.md` no repo (baseado em spec `40`)

### Checklist operacional tag real
- [ ] Release PUBLICADA no portal **antes** da tag (processo time)
- [ ] `git tag -a v1.5.0 -m "..."` + push
- [ ] Jenkins SUCCESS → assets na Release v1.5.0
- [ ] Validar manualmente assets (war, sql, kettle zip)

### DoD
- [ ] Um produto piloto com pipeline repo→Jenkins→GitHub Release funcionando

**Specs:** `39-entregaveis-cicd-repositorios.md`, `40-guia-versao-tag.md`

---

## Sprint 9 — F2 portal: GitHub Releases adapter

**Objetivo:** geração baixa assets do GitHub (TO_TAG).

### Backend
- [ ] Campos Produto: `repositorioGithub`, `branchPadrao`, `padraoTag`
- [ ] `GitHubReleasesAdapter` + credencial (env/vault)
- [ ] Download asset → storage local (cache)
- [ ] Geração etapa 3: WEB/BATCH from GitHub em vez de upload
- [ ] (Opcional) sync automático pós PUBLICADA release

### Frontend
- [ ] Form produto: seção GitHub (`09`) + teste conexão

### DoD
- [ ] Entrega ACME com WEB baixado da Release v1.5.0 (upload manual desligado para WEB)

**ROADMAP:** F2.1–F2.4, F2.9

---

## Sprint 10 — F2 portal: delta Git (BANCO + KETTLE)

**Objetivo:** diff `FROM_TAG..TO_TAG` por cliente.

### Backend
- [ ] Serviço delta: resolve commits das tags via GitHub API
- [ ] BANCO: listar `.sql` alterados → classificar DDL/DML → unificar arquivos
- [ ] KETTLE: listar `.ktr`/`.kjb` alterados + dependências
- [ ] `POST /entregas/{id}/delta/calcular` + persistir preview
- [ ] Geração etapas 4–5 usam diff (fallback upload se tag ausente)
- [ ] Config por módulo: pasta repo, dialeto oracle/sqlserver

### Frontend
- [ ] Tela range/delta (`20`): FROM/TO, preview arquivos, recalcular
- [ ] Wizard passo 4 modo Automático mostra "+N SQL" real

### DoD
- [ ] ACME v1.4.0→v1.5.0: preview lista scripts corretos; pacote contém só delta SQL

**Specs:** `19`, `20`, ROADMAP F2.8

---

## Sprint 11 — F2: Jenkins no portal + operação

**Objetivo:** visibilidade de build; webhook opcional.

### Backend
- [ ] Campos Produto: `jenkinsUrl`, `jenkinsJob`, `triggerMode`
- [ ] Cliente Jenkins + trigger build (P2)
- [ ] Webhook GitHub/Jenkins → atualizar status artefato na release (P2)

### Frontend
- [ ] Seção Jenkins no produto (`09`)
- [ ] Badge "Build em andamento" / SUCCESS / FAILED na release (F2.10)
- [ ] Suporte operacional (`27`) — P1 se não feito

### DoD
- [ ] Tag push → Jenkins roda → portal mostra build OK → entrega usa assets fresh

**ROADMAP:** F2.5–F2.7, F2.10

---

## Sprint 12+ — F3 Distribuição remota

**Objetivo:** publicar no destino do cliente.

- [ ] `PublishStrategy` + FTP + SFTP
- [ ] Credenciais cifradas (Jasypt) + tela teste conexão
- [ ] Bucket S3/MinIO (P2)
- [ ] Retry job + notificação Slack/e-mail
- [ ] DoD: pacote publicado no FTP do ACME automaticamente

**ROADMAP:** F3.*

---

## Sprint 13+ — F4 Observabilidade

- [ ] Logs JSON + correlationId na geração
- [ ] Métricas: tempo geração, falhas, delta duration
- [ ] Health: github, jenkins, storage
- [ ] E2E Playwright: release → entrega → download
- [ ] Backup + retenção pacotes

**ROADMAP:** F4.*

---

## Checklist rápido — ambiente externo (copiar por produto)

Use para **cada** produto (DTEC-LD, DTEC-CR, …):

```markdown
### Produto: ___________  Repo: ___________

#### GitHub
- [ ] Branch padrão definida (main)
- [ ] Regex tag: ^v\d+\.\d+\.\d+$
- [ ] Estrutura db/ / kettle/ conforme spec 39
- [ ] VERSIONING.md no repo (spec 40)

#### Jenkins
- [ ] Job criado: ___________
- [ ] Trigger tag v*.*.*
- [ ] Credencial GitHub PAT
- [ ] Pipeline: checkout → build → gh release upload
- [ ] Tag teste v0.0.1 OK

#### Portal (após S9)
- [ ] Produto cadastrado com repositorioGithub
- [ ] Módulos WEB/BANCO/KETTLE com paths configurados
- [ ] Release PUBLICADA + tag vX.Y.Z + assets na Release
- [ ] Entrega teste cliente piloto FROM..TO
```

---

## Matriz: quem faz o quê

| Atividade | Time dev portal | Time produto/DevOps | Operador |
|---|---|---|---|
| Jenkinsfile no repo | — | 🔧 | — |
| Job Jenkins | — | 🔧 | — |
| Tag Git | — | colabora | dispara após release PUBLICADA |
| Upload manual MVP | — | — | ✅ S1–S7 |
| Cadastro cliente | 📋 | — | ✅ |
| Assistente entrega | 📋 | — | ✅ |
| DocFlow manual | 📋 | — | ✅ |
| Delta Git | 📋 backend | — | revisa preview |

---

## Referências

- Jornada: [`00-cenario-feliz-acme.md`](00-cenario-feliz-acme.md)
- GitHub/Jenkins: [`01-github-jenkins-delta.md`](01-github-jenkins-delta.md)
- ROADMAP numerado: [`../ROADMAP.md`](../ROADMAP.md)
- Plano 90 dias + tarefas: [`03-plano-90-dias-e-tarefas.md`](03-plano-90-dias-e-tarefas.md)
- Índice jornadas: [`README.md`](README.md)
