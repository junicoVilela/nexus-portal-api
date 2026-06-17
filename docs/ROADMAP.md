# Softon Intranet — Roadmap Consolidado

Roadmap do **Release Orchestrator** cobrindo backend (`softon-portal-api`) e frontend (`softon-portal-web`).

> Documento vivo. Última revisão: 2026-05-31.
> Convenção: cada item tem **prioridade** (P0/P1/P2/P3), **esforço** (S/M/L/XL) e **dependências**.

---

## Visão geral por fases

```text
┌─────────────────────────────────────────────────────────────────┐
│ FASE 0 — Fundação (módulo release-orchestrator precisa amadurecer) │
│ Pré-req do Orchestrator. Inclui ModuloProduto, ArtefatoModulo,   │
│ ReleaseModuloVersao, renderer PDF, naming cleanup, VIEWER role.  │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ FASE 1 — Orchestrator MVP                                        │
│ Clientes + ProximaEntrega + assistente de nova entrega + delta   │
│ + geração de pacote local + PDF + manifest + reentrega.          │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ FASE 2 — Automação                                               │
│ GitHub Releases (download de assets) + Jenkins build-on-tag      │
│ + delta automático via Git.                                      │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ FASE 3 — Distribuição remota                                     │
│ Publicação FTP/SFTP/Bucket + credenciais cifradas + Vault.       │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ FASE 4 — Observabilidade + Hardening                             │
│ Métricas, traces, alertas, performance, segurança avançada.      │
└─────────────────────────────────────────────────────────────────┘
```

---

## FASE 0 — Pré-Orchestrator

| # | Item | Lado | Prio | Esforço | Depende de |
|---|---|---|---|---|---|
| F0.1 | Aplicar `naming-suggestions.md` (ProdutoRh → Produto + Rh*/Hub*) | Back+Front | P1 | M | — |
| F0.2 | Atualizar `DATABASE_STANDARDS.md` (sem prefixo `tb_`) | Docs | P2 | S | — |
| F0.3 | Entidade `ModuloProduto` (catálogo livre por produto + tipo: WEB/BATCH/BANCO/KETTLE/FUNCIONALIDADES/REGRAS) | Back | P0 | M | F0.1 |
| F0.4 | Entidade `ReleaseModuloVersao` (vínculo release ↔ versão por módulo) | Back | P0 | M | F0.3 |
| F0.5 | Entidade `ArtefatoReleaseModulo` (upload manual MVP) + storage local | Back | P0 | M | F0.3 |
| F0.6 | Endpoint upload/listar/excluir artefatos + frontend | Back+Front | P0 | M | F0.5 |
| F0.7 | Renderer Markdown → PDF (openhtmltopdf + commonmark-java + Thymeleaf) | Back | P0 | L | — |
| F0.8 | Endpoint `GET /releases/{id}/pdf` + ação no frontend | Back+Front | P0 | M | F0.7 |
| F0.9 | Adicionar `ROLE_VIEWER` ao Spring Security + endpoints GET | Back | P0 | S | — |
| F0.10 | Permissões granulares (enum `Permissao` + `@auth.has('...')`) | Back | P1 | M | F0.9 |
| F0.11 | Templates PDF customizáveis por produto | Back | P2 | M | F0.7 |
| F0.12 | Frontend: tela de upload de artefatos por módulo na release | Front | P0 | M | F0.6 |
| F0.13 | Frontend: preview de PDF na detalhe da release | Front | P0 | M | F0.8 |
| F0.14 | Frontend: catálogo de módulos por produto (CRUD) | Front | P0 | M | F0.3 |

**Critério de saída da Fase 0**: release-orchestrator consegue:
- ter módulos cadastrados por produto;
- ter artefatos uploadados por módulo;
- gerar PDF formatado;
- responder ao Orchestrator com versão por módulo.

---

## FASE 1 — Orchestrator MVP

| # | Item | Lado | Prio | Esforço | Depende de |
|---|---|---|---|---|---|
| F1.1 | Estender módulo Maven `release-orchestrator` com domínio de entregas (mesmo módulo, novo pacote) | Back | P0 | S | F0.* |
| F1.2 | Migrations Flyway para entidades novas (Cliente, Contato, ConfigEntrega, Dominio, Funcionalidade, ClienteFuncionalidade, ClienteProduto, ClienteProdutoModulo, ProximaEntrega, Entrega) | Back | P0 | M | F1.1 |
| F1.3 | CRUD Cliente + Contato + ConfigEntrega | Back+Front | P0 | L | F1.2 |
| F1.4 | CRUD Domínio + Funcionalidade (por produto) | Back+Front | P0 | M | F1.2 |
| F1.5 | Matriz ClienteFuncionalidade (habilitar/desabilitar) | Back+Front | P0 | M | F1.4 |
| F1.6 | Produtos contratados por cliente (ClienteProduto + ClienteProdutoModulo) | Back+Front | P0 | M | F1.2 |
| F1.7 | Próximas Entregas — agenda + cadastro | Back+Front | P0 | M | F1.6 |
| F1.8 | Assistente de Nova Entrega (multi-step) | Front | P0 | L | F1.7 |
| F1.9 | Seleção de módulos (auto-marcar contratados) | Back+Front | P0 | M | F1.8 |
| F1.10 | Cálculo de delta (versão atual cliente vs versão release) | Back | P0 | M | F1.9 |
| F1.11 | Geração de pacote local (@Async + storage filesystem) | Back | P0 | L | F1.10 |
| F1.12 | Status de geração (`GeracaoStatus`) + polling/WebSocket no frontend | Back+Front | P0 | M | F1.11 |
| F1.13 | Documento da entrega (release-notes.pdf por cliente) | Back | P0 | M | F0.7 |
| F1.14 | Manifest + checksums dos artefatos | Back | P0 | S | F1.11 |
| F1.15 | Histórico de entregas + reentrega (FK `entregaOriginalId`) | Back+Front | P0 | M | F1.11 |
| F1.16 | Dashboard do orchestrator (KPIs: próximas, em geração, concluídas) | Front | P1 | M | F1.7 |
| F1.17 | Relatórios básicos (entrega por cliente, por produto, por período) | Back+Front | P1 | M | F1.15 |
| F1.18 | Suporte operacional (consulta rápida: o que foi entregue p/ X?) | Front | P1 | S | F1.15 |

**Critério de saída da Fase 1**: usuário consegue, end-to-end:
- cadastrar cliente com configuração;
- planejar próxima entrega;
- gerar pacote local + PDF;
- consultar histórico;
- reentregar.

---

## FASE 2 — Automação

| # | Item | Lado | Prio | Esforço | Depende de |
|---|---|---|---|---|---|
| F2.0 | **Repos de produto**: Jenkinsfile build-on-tag, assets no GitHub Release, guia versão/tag por repo (checklist ~2 dias/entregável — ver `39`, `40`) | Repo | P0 | L | — |
| F2.1 | Campos GitHub no `Produto` (`repositorioGithub`, `branchPadrao`, `padraoTag`) | Back | P1 | S | F1.* |
| F2.2 | Cliente HTTP para GitHub API + abstração `GitHubReleasesAdapter` | Back | P1 | M | F2.1 |
| F2.3 | Download automático de assets do GitHub Releases para storage local | Back | P1 | M | F2.2 |
| F2.4 | Substituir upload manual de artefatos por trigger pós-release-publicada | Back | P1 | M | F2.3 |
| F2.5 | Campos Jenkins no `Produto` (`jenkinsUrl`, `jenkinsJob`, `triggerMode`) | Back | P1 | S | F2.1 |
| F2.6 | Cliente Jenkins + trigger de build on tag | Back | P2 | L | F2.5 |
| F2.7 | Webhook GitHub → Jenkins → Notificação Release Orchestrator | Back | P2 | L | F2.6 |
| F2.8 | Cálculo automático de delta via diff Git (tags / commits) | Back | P2 | L | F2.3 |
| F2.9 | Frontend: campo GitHub/Jenkins no formulário de Produto | Front | P1 | S | F2.1, F2.5 |
| F2.10 | Frontend: indicador de "build em andamento" na release | Front | P2 | M | F2.6 |

---

## FASE 3 — Distribuição remota

| # | Item | Lado | Prio | Esforço | Depende de |
|---|---|---|---|---|---|
| F3.1 | Strategy de publicação (`PublishStrategy` interface) | Back | P1 | M | F1.11 |
| F3.2 | Implementação `FtpPublisher` | Back | P1 | M | F3.1 |
| F3.3 | Implementação `SftpPublisher` | Back | P1 | M | F3.1 |
| F3.4 | Implementação `BucketPublisher` (S3/MinIO compatível) | Back | P2 | M | F3.1 |
| F3.5 | Credenciais cifradas (Jasypt) — coluna `credencial_ref` + cofre | Back | P0 | M | F3.1 |
| F3.6 | Frontend: tela de credenciais (mascarada, com teste de conexão) | Front | P0 | M | F3.5 |
| F3.7 | Job de retry para publicação falhada | Back | P1 | M | F3.1 |
| F3.8 | Notificação por e-mail/Slack ao concluir publicação | Back | P2 | S | F3.1 |

---

## FASE 4 — Observabilidade + Hardening

| # | Item | Lado | Prio | Esforço | Depende de |
|---|---|---|---|---|---|
| F4.1 | Logs estruturados (JSON) com correlationId por requisição | Back | P1 | M | — |
| F4.2 | Métricas Micrometer (Prometheus): contagem de releases, tempo de geração, falhas | Back | P1 | M | — |
| F4.3 | Tracing OpenTelemetry para fluxo de geração de pacote | Back | P2 | L | F4.2 |
| F4.4 | Healthchecks específicos (`/actuator/health/{db,storage,github,jenkins}`) | Back | P1 | S | — |
| F4.5 | Rate limiting nos endpoints de geração e download | Back | P2 | M | — |
| F4.6 | Auditoria expandida (quem viu o quê, quem baixou o quê) | Back | P2 | M | — |
| F4.7 | Backup automático do storage de artefatos e pacotes | Ops | P0 | S | F1.11 |
| F4.8 | Política de retenção de pacotes antigos | Back | P2 | M | F1.11 |
| F4.9 | Frontend: erro detalhado de geração com link para logs | Front | P1 | M | F4.1 |
| F4.10 | E2E tests Cypress/Playwright para fluxo completo de entrega | Front | P1 | L | F1.* |

---

## Melhorias transversais (não dependem de fase)

### Backend
- **Testes**: hoje provavelmente baixa cobertura — fazer Cypress dos services públicos e ReleaseService (transições, validações, publicação). Meta: 70%+ no service layer.
- **OpenAPI/Swagger**: gerar spec automática (`springdoc-openapi`). Frontend pode usar para gerar clients tipados.
- **Validação cross-field** em DTOs (ex: `dataPublicacao > dataPrevista`).
- **Idempotência** em ações POST que efetivam mudança (`Idempotency-Key` header).
- **Database constraints redundantes**: hoje algumas validações estão só no service (ex: versão única). Reforçar com `UNIQUE INDEX` quando aplicável.
- **`@EntityGraph` consistente** para evitar N+1 em `Release.produto`.
- **Limites de tamanho** explícitos (upload, descrição, observações) em property file.

### Frontend
- **Empty/Loading/Error states uniformes** em todas as listagens (criar shared `ListStateComponent`).
- **Confirmação universal** em ações destrutivas (criar `ConfirmDialogService`).
- **Toast/Snackbar** unificado para feedback de sucesso/erro.
- **Tipagem completa** dos models (alinhar com response DTOs do backend).
- **Padrão de URL**: query state preservado em filtros (já existe `query-state.ts` em shared/utils).
- **Loading skeletons** em vez de spinners onde fizer sentido.
- **Atalhos de teclado** no builder de release (próximo item, salvar, etc.).
- **Acessibilidade** (a11y): foco visível, labels, contraste — auditar com Lighthouse.
- **Mobile**: hoje o shell + sidebar parece desktop-first. Definir comportamento responsivo.
- **Module Federation**: já existe `tsconfig.federation.json` — decidir se é caminho real ou remover.

### Documentação / DX
- **Sync de specs entre repos**: `release-orchestrator/` aparece em ambos. Pensar em ferramenta (script de sync ou git submodule de docs/) — ou explicitar quais arquivos divergem por design (front vs back).
- **Plano de testes manuais** documentado para cada release dos próprios módulos.
- **Glossário** de termos do domínio (Release, Entrega, Pacote, Módulo, Domínio, Funcionalidade) num único arquivo.

### Cross-cutting
- **CI**: GitHub Actions / GitLab CI rodando build + testes + lint em cada PR.
- **Pré-commit hooks** (formatter Java, ESLint+Prettier frontend).
- **Versionamento semântico** dos próprios módulos (`softon-portal-api 1.0.0`, etc.).

---

## Métricas para acompanhar entrega do roadmap

- **Throughput**: releases publicadas / mês.
- **Tempo médio**: criação → publicação.
- **Tempo médio**: planejamento entrega → pacote pronto.
- **Erros**: % de gerações que falham.
- **Adoção**: clientes ativos com pelo menos 1 entrega no mês.

---

## Documentos relacionados

- [`naming-suggestions.md`](naming-suggestions.md) — propostas de renomeação aplicáveis em Fase 0.
- [`jornadas/README.md`](jornadas/README.md) — jornadas de uso (cenário ACME, GitHub/Jenkins/delta).
- [`doc-flow/README.md`](doc-flow/README.md) — DocFlow backend (API).
- [`release-orchestrator/`](release-orchestrator/) — specs do backend (release-orchestrator + orchestrator são o mesmo projeto; esta é a referência única).
- [`release-orchestrator/39-entregaveis-cicd-repositorios.md`](release-orchestrator/39-entregaveis-cicd-repositorios.md) — entregáveis nos repos (Jenkins/GitHub).
- [`release-orchestrator/40-guia-versao-tag.md`](release-orchestrator/40-guia-versao-tag.md) — guia operacional de tags.
- [`release-orchestrator/99-melhorias-sugeridas.md`](release-orchestrator/99-melhorias-sugeridas.md) — backlog do backend.
- `softon-portal-web/docs/release-orchestrator/` — specs do frontend (por tela).
