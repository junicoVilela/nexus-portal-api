# 04 — Kickoff + como pedir execução (Cursor)

One-pager para alinhar o time e **prompts prontos** para colar no chat do Cursor e iniciar cada fase.

> Piloto: produto **NEXUS-LD**, cliente **ACME**, MVP **manual** (upload + pacote local) até M6.

---

## Decisões já fechadas

| Item | Decisão |
|---|---|
| Produto piloto | NEXUS-LD (`NEXUSLD`) |
| Cliente piloto | ACME |
| MVP entregas | Upload manual de artefatos; sem GitHub/Jenkins até M6 |
| DocFlow | Integrar antes do Orchestrator (S0 / M1) |
| Plano de execução | [`03-plano-90-dias-e-tarefas.md`](03-plano-90-dias-e-tarefas.md) |
| DoD por sprint | [`02-checklist-por-sprint.md`](02-checklist-por-sprint.md) |
| Demo final | [`00-cenario-feliz-acme.md`](00-cenario-feliz-acme.md) |

---

## Documentos de referência (ordem)

1. **Executar** → [`03-plano-90-dias-e-tarefas.md`](03-plano-90-dias-e-tarefas.md)
2. **Validar sprint** → [`02-checklist-por-sprint.md`](02-checklist-por-sprint.md)
3. **Entender fluxo** → [`00-cenario-feliz-acme.md`](00-cenario-feliz-acme.md)
4. **Detalhe técnico** → specs em [`../release-orchestrator/`](../release-orchestrator/README.md) e [`../doc-flow/`](../doc-flow/README.md)
5. **Fases longas** → [`../ROADMAP.md`](../ROADMAP.md)

---

## Como pedir execução no Cursor

### Regra geral

Sempre inclua:

1. **O que executar** — sprint, marco ou ID de tarefa (`DF-003`, `RO-405`)
2. **Escopo** — backend, frontend ou ambos
3. **Documentos** — caminho do plano + spec
4. **Critério de pronto** — DoD do checklist ou critérios de aceite da tarefa
5. **Restrições** — diff mínimo, não commitar, seguir convenções do repo

### Prompt base (copiar e adaptar)

```text
Execute o [S0 / M1 / tarefa RO-405] conforme a documentação:

- Plano: nexus-portal-api/docs/jornadas/03-plano-90-dias-e-tarefas.md
- Checklist DoD: nexus-portal-api/docs/jornadas/02-checklist-por-sprint.md (seção S__)
- Spec técnica: [caminho da spec, se houver]

Escopo: [backend | frontend | ambos]
Repositório(s): [nexus-portal-api | nexus-portal-web | ambos]

Faça:
1. Ler o código existente antes de alterar
2. Implementar só o escopo desta tarefa/sprint
3. Rodar testes/build relevantes
4. Marcar no checklist o que foi concluído (se pedido)

Não fazer: commits, refactors fora do escopo, novas libs sem necessidade.
Critério de pronto: [colar DoD ou critérios de aceite da tarefa]
```

---

## Prompts prontos por fase

### Início do projeto (kickoff completo)

```text
Inicie o projeto Release Orchestrator + integração DocFlow seguindo:

- nexus-portal-api/docs/jornadas/04-kickoff-e-prompts-execucao.md
- nexus-portal-api/docs/jornadas/03-plano-90-dias-e-tarefas.md

Comece pelo Sprint S0 (Marco M1): integração DocFlow.
Leia nexus-portal-api/docs/doc-flow/README.md e o código em doc-flow/ e frontend docflow/.
Implemente as tarefas DF-001 a DF-007. Valide login JWT, proxy /api/v1/docflow e publicação ZIP.
Não commite até eu pedir.
```

### Sprint 0 — DocFlow (M1)

```text
Execute Sprint S0 — DocFlow integrado (Marco M1).

Referências:
- nexus-portal-api/docs/jornadas/03-plano-90-dias-e-tarefas.md (tarefas DF-001 a DF-007)
- nexus-portal-api/docs/jornadas/02-checklist-por-sprint.md (seção Sprint 0)
- nexus-portal-api/docs/doc-flow/README.md

Escopo: nexus-portal-web (auth, proxy, services) + ajustes mínimos no API se necessário.
Critério de pronto: login → dashboard → CRUD cliente → página PUBLICADO → publicação GERANDO → SUCESSO → download ZIP.
```

### Sprint 1 — Backend artefatos (M2)

```text
Execute Sprint S1 — F0 backend módulos e artefatos.

Referências:
- nexus-portal-api/docs/jornadas/03-plano-90-dias-e-tarefas.md (RO-101 a RO-107)
- nexus-portal-api/docs/jornadas/02-checklist-por-sprint.md (Sprint 1)
- nexus-portal-api/docs/release-orchestrator/10-produtos-modulos-artefatos.md
- nexus-portal-api/docs/ROADMAP.md (F0.3–F0.6)

Escopo: nexus-portal-api (release-orchestrator).
Critério de pronto: via API, criar módulo WEB em NEXUS-LD e fazer upload .war em release rascunho; testes passando.
```

### Sprint 2 — PDF + front artefatos (M2)

```text
Execute Sprint S2 — F0 front artefatos + PDF release.

Referências:
- nexus-portal-api/docs/jornadas/03-plano-90-dias-e-tarefas.md (RO-201 a RO-207)
- nexus-portal-api/docs/jornadas/02-checklist-por-sprint.md (Sprint 2)
- nexus-portal-api/docs/release-orchestrator/25-documento-release-md-pdf.md
- nexus-portal-web/docs/release-orchestrator/14-release-orchestrator-detalhe.md

Escopo: backend PDF + frontend aba Artefatos e botão PDF.
Critério de pronto: release 1.5.0 rascunho com war + sql uploadados e PDF baixado pela UI.
```

### Sprint 3 — Cadastros cliente (M3)

```text
Execute Sprint S3 — F1 backend cadastros de cliente.

Referências:
- nexus-portal-api/docs/jornadas/03-plano-90-dias-e-tarefas.md (RO-301 a RO-309)
- nexus-portal-api/docs/jornadas/02-checklist-por-sprint.md (Sprint 3)
- nexus-portal-api/docs/release-orchestrator/02 a 07 (specs cliente)

Escopo: nexus-portal-api.
Critério de pronto: cliente ACME via API com NEXUS-LD, módulos WEB+BANCO v1.4.0, funcionalidades marcadas; seed + testes.
```

### Sprint 4 — Geração pacote (M4) — crítico

```text
Execute Sprint S4 — F1 backend entrega e geração de pacote MVP.

Referências:
- nexus-portal-api/docs/jornadas/03-plano-90-dias-e-tarefas.md (RO-401 a RO-410)
- nexus-portal-api/docs/jornadas/02-checklist-por-sprint.md (Sprint 4)
- nexus-portal-api/docs/release-orchestrator/18, 19, 21 (assistente, módulos, geração)

Escopo: nexus-portal-api. Priorize RO-405 (job @Async 11 etapas).
MVP: delta lógico sem Git; artefatos de upload manual.
Critério de pronto: POST gerar entrega ACME → ZIP em pasta local → CONCLUIDA → versaoAtual 1.5.0.
```

### Sprint 5 — Front clientes (M5)

```text
Execute Sprint S5 — F1 front clientes e agenda.

Referências:
- nexus-portal-api/docs/jornadas/03-plano-90-dias-e-tarefas.md (RO-501 a RO-508)
- nexus-portal-api/docs/jornadas/02-checklist-por-sprint.md (Sprint 5)
- nexus-portal-web/docs/release-orchestrator/ (specs 02–07, 16–17)

Escopo: nexus-portal-web.
Critério de pronto: gestor cadastra ACME e planeja entrega v1.5.0 só pela UI.
```

### Sprint 6 — Wizard entrega (M5)

```text
Execute Sprint S6 — F1 front assistente e geração.

Referências:
- nexus-portal-api/docs/jornadas/03-plano-90-dias-e-tarefas.md (RO-601 a RO-608)
- nexus-portal-api/docs/jornadas/02-checklist-por-sprint.md (Sprint 6)
- nexus-portal-web/docs/release-orchestrator/ (specs 18–23)

Escopo: nexus-portal-web.
Critério de pronto: operador gera pacote ACME pelo wizard, baixa ZIP, vê histórico; polling GERANDO.
```

### Sprint 7 — Demo integrada (M6)

```text
Execute Sprint S7 — integração e validação jornada ACME.

Referências:
- nexus-portal-api/docs/jornadas/00-cenario-feliz-acme.md
- nexus-portal-api/docs/jornadas/02-checklist-por-sprint.md (Sprint 7)

Não implementar features novas. Corrigir blockers da jornada end-to-end.
Entregar roteiro demo 30 min e lista do que passou/falhou no bug bash.
Critério de pronto: demo M6 reproduzível sem Postman.
```

### Uma tarefa só (recomendado para sessões curtas)

```text
Implemente apenas a tarefa [RO-405] do plano:
nexus-portal-api/docs/jornadas/03-plano-90-dias-e-tarefas.md

Leia a spec linkada na tabela da tarefa e o código existente.
Escopo mínimo; testes para o comportamento novo.
Critério de pronto: [colar critérios de aceite da linha RO-405].
```

### Continuar de onde parou

```text
Continue a execução do Sprint [S4] onde paramos.

Leia o checklist nexus-portal-api/docs/jornadas/02-checklist-por-sprint.md
e git status/diff para ver o que já foi feito.
Implemente só os itens ainda [ ] pendentes deste sprint.
```

### Pedir commit (quando quiser versionar)

```text
Revise as alterações deste sprint [S0] e crie um commit seguindo o estilo do repositório.
Não incluir arquivos sensíveis. Mensagem focada no porquê.
```

---

## Fluxo recomendado no dia a dia

```text
1. Escolher sprint ou tarefa no plano 90 dias
2. Colar prompt correspondente acima (ou prompt base)
3. Revisar diff no Cursor
4. Pedir "continue" ou próxima tarefa
5. No fim do sprint: pedir validação contra checklist S__
6. No M6: pedir demo seguindo 00-cenario-feliz-acme.md
```

---

## Dicas

| Situação | O que pedir |
|---|---|
| Sessão longa | Sprint inteiro (ex.: S0) |
| Sessão curta | Uma tarefa (`RO-105`) |
| Só entender | "Explique o Sprint S4 sem implementar" |
| Só backend | Dizer explicitamente `Escopo: nexus-portal-api` |
| Evitar escopo creep | "Implemente só RO-405; ignore RO-406 por agora" |
| Testes | "Rode testes do módulo alterado e corrija falhas" |

---

## Referências

- Índice jornadas: [`README.md`](README.md)
- Plano: [`03-plano-90-dias-e-tarefas.md`](03-plano-90-dias-e-tarefas.md)
- Checklist: [`02-checklist-por-sprint.md`](02-checklist-por-sprint.md)
