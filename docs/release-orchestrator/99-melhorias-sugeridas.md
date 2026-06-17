# 99 — Melhorias Sugeridas (Release Orchestrator)

Backlog específico do **Release Orchestrator** cobrindo **back + front**. Para roadmap consolidado, ver [`../ROADMAP.md`](../ROADMAP.md). Para naming, ver [`../naming-suggestions.md`](../naming-suggestions.md).

> Categorias: A. Modelo | B. APIs | C. Performance | D. Segurança | E. Testes | F. Observabilidade | G. UX/Frontend | H. Operação | I. Documentação

---

## A. Modelo de domínio

### A.1 Conceito de "envelope" da entrega
- **Problema**: hoje `Entrega` tem N artefatos, mas não há entidade "Envelope" para representar o pacote final (zip único).
- **Proposta**: adicionar `EntregaPacote` com `caminhoArmazenado`, `sha256`, `tamanhoBytes`, `criadoEm`. Permite reentregar sem regerar e auditar conteúdo.
- **Esforço**: M.

### A.2 `EntregaArtefato` (snapshot do que foi enviado)
- **Problema**: ao reentregar mais tarde, qual SHA foi usado? Hoje rastreável só por inferência.
- **Proposta**: tabela snapshot que registra cada artefato incluído na entrega com SHA e tamanho. Independente do `ArtefatoReleaseModulo` original.
- **Esforço**: M.
- **Por quê**: imutabilidade real do que foi entregue.

### A.3 Versionamento de `Cliente.ConfiguracaoEntrega`
- **Problema**: mudar config de cliente afeta retroativamente.
- **Proposta**: snapshot da config no momento da entrega (`Entrega.configEntregaSnapshot`).
- **Esforço**: S.

### A.4 Status de `Cliente`
- **Estado atual**: implícito (`ativo`?).
- **Proposta**: enum `StatusCliente { ATIVO, PAUSADO, ENCERRADO, EM_ANALISE }` com regras (PAUSADO bloqueia novas entregas).
- **Esforço**: S.

### A.5 Estratégia de delta como FSM
- **Problema**: cálculo de delta tem várias regras espalhadas.
- **Proposta**: encapsular em strategy por tipo de módulo (`DeltaStrategy` para BANCO, KETTLE, etc.).
- **Esforço**: M.

### A.6 `Entrega` ↔ `ReleaseModuloVersao` explícito
- **Proposta**: tabela `EntregaModulo` que vincula entrega ao par (release, moduloProduto, versao). Hoje pode ser derivado mas tabela explícita facilita queries.
- **Esforço**: M.

### A.7 `Cliente.codigoExterno`
- **Proposta**: campo opcional para casar cliente do orchestrator com ID em sistemas externos (CRM, ERP).
- **Esforço**: S.

### A.8 `Domínio.compartilhado`
- **Problema**: hoje cada produto tem seu catálogo de domínios. Há sobreposição (ex.: "Usuários" existe em todo produto).
- **Proposta**: tabela `DominioGlobal` de templates, com `Dominio` por produto podendo herdar.
- **Esforço**: M.

### A.9 `Funcionalidade.dependencia`
- **Proposta**: campo `requeresFuncionalidadeIds` indicando dependências (não habilita "Bloquear" sem "Visualizar").
- **Esforço**: M.

### A.10 Auditoria expandida
- **Proposta**: tabela `OrchestratorAuditoria` similar ao `release_historico` para clientes/entregas. Hoje cada entidade pode ter seu mecanismo — centralizar.
- **Esforço**: M.

---

## B. APIs

### B.1 GraphQL para queries complexas (sugestão)
- **Problema**: a tela de dashboard precisa de muitos dados em paralelo. Em REST → N chamadas.
- **Proposta**: expor um endpoint GraphQL **apenas para reads** de dashboards e relatórios.
- **Esforço**: L.
- **Decisão**: só se a complexidade do dashboard crescer.

### B.2 Endpoint `/api/v1/orchestrator/clientes/{id}/dashboard`
- Agregado: total entregas, última entrega, próxima, módulos contratados, alertas.
- **Esforço**: M.
- **Benefício**: reduz chatter na tela de visão geral.

### B.3 Idempotency-Key em geração e reentrega
- **Esforço**: M.
- **Benefício**: cliente pode retry sem duplicar geração.

### B.4 Endpoint de teste de configuração de entrega
- **Proposta**: `POST /clientes/{id}/configuracao-entrega/testar` valida credenciais e conexão.
- **Esforço**: M (depende da Fase 3).

### B.5 Webhooks de entrega
- **Proposta**: `POST /webhooks/entregas` que notifica sistemas externos ao concluir entrega.
- **Esforço**: M.
- **Casos**: integração CRM, automação interna.

### B.6 Bulk endpoints
- Habilitar funcionalidades em massa para cliente.
- Marcar/desmarcar produtos contratados em batch.
- **Esforço**: M.

### B.7 Resposta padronizada de erro (Problem Details)
- Ver `release-orchestrator/16-erros-padroes.md`. Aplicar mesmo padrão no orchestrator.
- **Esforço**: M (mesmo enum compartilhado).

### B.8 OpenAPI auto-gerado
- Mesmo que release-orchestrator.
- **Esforço**: M.

---

## C. Performance

### C.1 Geração de pacote como streaming
- **Hoje**: tudo em memória/disco temporário.
- **Proposta**: streaming direto para o ZIP final (sem intermediário). Reduz memória e disco.
- **Esforço**: M.

### C.2 Paralelização da montagem
- Módulos independentes (WEB, BATCH, KETTLE) podem ser empacotados em paralelo.
- **Esforço**: M.
- **Stack**: `CompletableFuture` ou Project Reactor.

### C.3 Cache de delta calculado
- **Hoje**: calculado a cada nova entrega.
- **Proposta**: cache por `(cliente, modulo, versaoInicial, versaoFinal)`. Invalida ao mudar artefato.
- **Esforço**: M.

### C.4 Índices PostgreSQL para queries comuns
- `cliente_produto_modulo (cliente_id, produto_id)`.
- `entrega (cliente_id, criada_em DESC)`.
- `proxima_entrega (data_planejada, status)`.
- **Esforço**: S.

### C.5 Paginação cursor-based em históricos
- Hoje provavelmente offset-based. Para >50k entregas, cursor é melhor.
- **Esforço**: M.

### C.6 Background jobs com cron
- Limpeza de pacotes antigos.
- Retry de entregas falhadas.
- Recompute de stats de cliente.
- **Stack**: Spring `@Scheduled` ou Quartz.
- **Esforço**: M.

---

## D. Segurança

### D.1 Credenciais cifradas (Jasypt)
- Critical para Fase 3 (FTP/SFTP/Bucket).
- **Esforço**: M.
- Ver `33-decisoes-tecnicas.md`.

### D.2 Vault para secrets em prod
- Hashicorp Vault ou AWS Secrets Manager.
- **Esforço**: L.

### D.3 Permissões granulares
- Já documentadas em `33`. Implementar.
- **Esforço**: M.

### D.4 Audit de acesso a dados sensíveis
- Registrar quem viu credenciais de cliente.
- **Esforço**: S.

### D.5 Rate limiting
- Endpoints de geração e download — limitar por usuário.
- **Esforço**: M (Bucket4j).

### D.6 Validação de path traversal
- `caminhoArmazenado` jamais aceita input direto do usuário. Sempre derivado.
- Validar via `Path.normalize()`.

### D.7 Antivírus em artefatos uploadados (futuro)
- ClamAV ou similar antes de aceitar.
- **Esforço**: M.

### D.8 Não logar conteúdo de credenciais
- Mascarar tokens, senhas. Logback filter.

---

## E. Testes

### E.1 Cobertura mínima 70% no service layer
- Atualmente 0% (módulo não existe).
- **Esforço**: M (acompanhar implementação).

### E.2 Testes de integração com Testcontainers
- PostgreSQL real + filesystem.
- Fluxo: criar cliente → planejar entrega → gerar pacote → verificar SHA.
- **Esforço**: M.

### E.3 Testes de estratégia de delta
- Mock múltiplas releases entre cliente atual e novo target.
- Validar que delta inclui apenas scripts entre versões.
- **Esforço**: M.

### E.4 Testes E2E (Cypress/Playwright)
- Cobertura: ciclo completo end-to-end pela UI.
- **Esforço**: L.

### E.5 Testes de carga
- 100 entregas concorrentes geradas.
- Métricas: tempo médio, p95, p99, falhas.
- **Stack**: k6 ou Gatling.
- **Esforço**: M.

### E.6 Testes de migração
- Cada migration roda clean em DB vazio e em DB com dados.
- **Esforço**: S (Testcontainers).

### E.7 Property-based tests
- Cálculo de delta com versões geradas aleatoriamente.
- **Esforço**: M (jqwik).

---

## F. Observabilidade

Ver `34-observabilidade.md` (novo). Resumo:

### F.1 Logs estruturados (JSON)
- MDC com `correlationId`, `clienteId`, `entregaId`.

### F.2 Métricas Micrometer
- Entregas criadas/concluídas/falhas.
- Tempo médio de geração.
- Tamanho médio de pacote.
- Gauge de gerações em curso.

### F.3 Tracing OTLP
- Spans para cada estágio: delta → download → empacotamento → publicação.

### F.4 Healthchecks
- Storage de pacotes.
- DB.
- (Fase 3) Conectividade SFTP/FTP.

### F.5 Alertas
- Falha em geração > 5%.
- Tempo médio sobe 50%.
- Disco < 20%.

---

## G. UX / Frontend (módulo Orchestrator a criar no `softon-portal-web`)

### G.1 Wizard de Nova Entrega genuinamente multi-step
- Hoje provavelmente form longo. Wizard com progress bar.
- **Esforço**: M.

### G.2 Drag-and-drop para reordenar módulos no pacote
- **Esforço**: M.

### G.3 Preview do pacote antes de gerar
- Mostra árvore do ZIP que será produzido.
- **Esforço**: M.

### G.4 Comparação visual de entregas
- "O que mudou entre a entrega de Janeiro e a de Maio?"
- **Esforço**: L.

### G.5 Dashboard com gráficos
- Entregas por cliente, por produto, no tempo.
- **Stack**: PrimeNG Chart, Apex Charts.
- **Esforço**: M.

### G.6 Notificação em tempo real do status de geração
- WebSocket ou SSE.
- **Esforço**: M.

### G.7 Filtros avançados em histórico
- Por cliente, produto, versão de módulo, status, data.
- **Esforço**: M.

### G.8 Tela de "Próximas 7 dias" no dashboard
- Calendário visual de entregas planejadas.
- **Esforço**: M.

### G.9 Modal de credencial mascarada
- Mostra `****` com botão "alterar".
- Não exibe credencial existente.
- **Esforço**: S.

### G.10 Confirmação dupla em reentrega
- "Tem certeza que quer republicar para X?".
- **Esforço**: S.

### G.11 Exportar histórico como CSV/Excel
- **Esforço**: M.

### G.12 Indicador de "última atualização" em listagens
- "Atualizado há 30s" — usuário sabe se dado está fresh.
- **Esforço**: S.

---

## H. Operação

### H.1 Backup automatizado do storage de pacotes
- **Esforço**: S (Ops/cron).

### H.2 Política de retenção de pacotes
- Entregas > 1 ano: mover para storage frio.
- > 5 anos: arquivar com criptografia.
- **Esforço**: M.

### H.3 Cleanup de pacotes órfãos
- Pacotes no FS sem registro DB ou entrega cancelada.
- Job semanal.
- **Esforço**: M.

### H.4 Migração para object storage (S3-compatible)
- FS local não escala bem horizontalmente.
- Abstração `PacoteStorage` permite trocar impl.
- **Esforço**: M (quando crescer).

### H.5 Disaster recovery
- RPO/RTO definidos.
- Backup DB + storage.
- Plano documentado.
- **Esforço**: L.

### H.6 Monitoramento de credenciais expirando
- GitHub tokens, Jenkins tokens, SFTP keys.
- Alerta 30 dias antes de expirar.
- **Esforço**: M.

---

## I. Documentação

### I.1 OpenAPI publicado em rota interna
- `/api/v1/orchestrator/openapi.yaml`.
- **Esforço**: S após F.B.8.

### I.2 Tutoriais "como fazer X"
- Como cadastrar cliente novo.
- Como configurar entrega via SFTP.
- Como gerar pacote para cliente.
- **Esforço**: M.

### I.3 Diagramas Mermaid em vez de ASCII
- Visualização melhor em GitHub e ferramentas.
- **Esforço**: S por diagrama.

### I.4 Glossário centralizado
- Termos do domínio: Cliente, Entrega, Pacote, Módulo, Domínio, Funcionalidade.
- Ver `38-glossario.md` (novo).
- **Esforço**: S.

### I.5 Decisões arquiteturais (ADRs)
- Pasta `docs/adrs/` com decisões importantes.
- **Esforço**: M.

### I.6 Exemplos de payload completos
- Cada endpoint com request/response real.
- **Esforço**: M (junto com OpenAPI).

---

## Prioridades sugeridas (matriz impacto × esforço)

### Quick wins (alto impacto, baixo/médio esforço)
- A.4 — Status de Cliente
- C.4 — Índices PostgreSQL
- D.1 — Credenciais cifradas (crítico Fase 3)
- F.2 — Métricas básicas
- G.9 — Modal de credencial mascarada
- I.4 — Glossário

### Médio prazo
- A.2, A.3 — Snapshots de artefato e configuração
- B.3, B.7 — Idempotency e Problem Details
- C.2 — Paralelização
- E.1, E.2 — Cobertura de testes
- G.1, G.5 — Wizard e dashboard

### Longo prazo / quando crescer
- B.1 — GraphQL
- C.5 — Cursor pagination
- D.2 — Vault
- H.4 — Object storage
- H.5 — Disaster recovery

---

## Cross-reference

- `softon-portal-web/docs/release-orchestrator/` — Documentação do frontend (por tela).
- [`../ROADMAP.md`](../ROADMAP.md) — Roadmap consolidado.
- [`../naming-suggestions.md`](../naming-suggestions.md) — Renomeações.
- [`34-observabilidade.md`](34-observabilidade.md) — Detalhes de F.
- [`35-testes-qa.md`](35-testes-qa.md) — Detalhes de E.
- [`36-deploy-operacao.md`](36-deploy-operacao.md) — Detalhes de H.
- [`37-contratos-openapi.md`](37-contratos-openapi.md) — Detalhes de B.8.
