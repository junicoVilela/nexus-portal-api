# 03 — Plano 90 dias + tarefas (S0–S7)

Complemento de [`02-checklist-por-sprint.md`](02-checklist-por-sprint.md): **marcos fixos**, **paralelismo** e **backlog pronto para Jira/Linear**.

**Início sugerido:** 2026-06-16 · **Fim MVP (Fase 1):** 2026-09-14 (90 dias)  
**Produto piloto:** NEXUS-LD · **Cliente piloto:** ACME

**Estimativa:** story points Fibonacci (SP). Conversão orientativa: **1 SP ≈ 0,5 dia-dev** (dev full-stack maduro no projeto).

**Labels sugeridas:** `docflow`, `orchestrator`, `f0`, `f1`, `infra`, `qa`

---

## Premissas de capacidade

| Papel | Alocação | Observação |
|---|---|---|
| Dev backend | 1,0 FTE | release-orchestrator + doc-flow |
| Dev frontend | 1,0 FTE | docflow + orchestrator |
| DevOps | 0,25 FTE | Jenkins/GitHub a partir da semana 5 (trilha paralela) |
| QA / operador | 0,25 FTE | DoD + roteiro demo |
| PO / gestor release | pontual | Aprovação release, validação jornada |

**Compressão vs checklist original:** S0–S7 em 8×2 semanas = 16 semanas. No plano de 90 dias, **backend e frontend avançam em paralelo** a partir da semana 3; DevOps não bloqueia o MVP manual.

---

## Marcos fixos (90 dias)

| Marco | Data alvo | Entregável | Sprint ref. |
|---|---|---|---|
| **M0** Kickoff | 2026-06-16 | Ambiente local OK, piloto NEXUS-LD + ACME definidos | — |
| **M1** DocFlow integrado | 2026-06-30 | Login JWT + publicação ZIP end-to-end | S0 |
| **M2** Release com artefatos | 2026-07-14 | Módulos catalogados + upload + PDF release 1.5.0 rascunho | S1–S2 |
| **M3** Cliente operacional | 2026-07-28 | ACME cadastrado (API); produtos/módulos/funcionalidades | S3 |
| **M4** Pacote gerado (API) | 2026-08-11 | `POST` gerar entrega → ZIP local + versão atualizada | S4 |
| **M5** UI entregas completa | 2026-08-25 | Wizard + agenda + histórico sem Postman | S5–S6 |
| **M6** MVP Fase 1 | 2026-09-14 | Demo jornada ACME ([`00-cenario-feliz-acme.md`](00-cenario-feliz-acme.md)) | S7 |

### Calendário semanal (visão)

```
        Jun          Jul          Aug          Sep
Sem  W24 W25 W26 W27 W28 W29 W30 W31 W32 W33 W34 W35 W36 W37
     ├──S0──┤
         ├──S1 BE──┤
              ├──S2 FE+PDF──┤
                   ├──S3 BE──┤
                        ├──S4 BE gen──┤
              (DevOps F2.0 inicia ~W28, paralelo)
                             ├──S5 FE clientes──┤
                                  ├──S6 FE wizard──┤
                                       ├──S7 integração──┤
                                                            ▲ M6
```

---

## Épicos (Jira/Linear)

| ID | Épico | Marco |
|---|---|---|
| EP-01 | DocFlow — integração API e auth | M1 |
| EP-02 | Orchestrator F0 — módulos, artefatos, PDF | M2 |
| EP-03 | Orchestrator F1 — cadastros cliente | M3 |
| EP-04 | Orchestrator F1 — geração pacote MVP | M4 |
| EP-05 | Orchestrator F1 — UI clientes e agenda | M5 |
| EP-06 | Orchestrator F1 — UI assistente e geração | M5 |
| EP-07 | Integração e demo ACME | M6 |
| EP-08 | (Paralelo) Jenkins + GitHub piloto | pós-M6 / S8 |

---

## Backlog detalhado — S0 (EP-01) · Marco M1

| ID | Título | SP | Dep. | Critério de aceite |
|---|---|---:|---|---|
| DF-001 | Auditar contrato API DocFlow vs front | 2 | — | Matriz path/método documentada; gaps listados |
| DF-002 | Ajustar proxy dev (`/api/v1/docflow`) | 1 | DF-001 | Chamadas locais batem no backend |
| DF-003 | Auth: login JWT real | 3 | DF-002 | Token armazenado; logout limpa sessão |
| DF-004 | Auth: interceptor + refresh 401 | 2 | DF-003 | Requests autenticadas; redirect login se expirado |
| DF-005 | Auth: `/auth/me` e menu por permissão | 3 | DF-003 | Menu oculta rotas sem permissão |
| DF-006 | Config: logo empresa via API | 2 | DF-002 | Upload/list logo sem mock |
| DF-007 | Smoke test DocFlow (QA) | 2 | DF-003–006 | Roteiro M1 verde |

**Total S0:** 15 SP (~7,5 dia-dev) · **1 dev front + 0,5 back** em 2 semanas.

---

## Backlog detalhado — S1 (EP-02) · parte M2

| ID | Título | SP | Dep. | Critério de aceite |
|---|---|---:|---|---|
| RO-101 | Migration `ModuloProduto` + enum tipos | 3 | — | CRUD repository + testes migration |
| RO-102 | Migration `ReleaseModuloVersao` | 2 | RO-101 | Vincular versão por módulo na release |
| RO-103 | Migration `ArtefatoReleaseModulo` + storage | 5 | RO-101 | Path configurável; SHA256 persistido |
| RO-104 | API CRUD módulos por produto | 3 | RO-101 | OpenAPI alinhado spec `10` |
| RO-105 | API upload/list/delete artefato | 5 | RO-103 | Multipart; rejeita se release PUBLICADA |
| RO-106 | Testes service  imutabilidade release publicada | 3 | RO-105 | Cobertura cenários feliz + erro |
| RO-107 | Collection Postman / exemplos curl | 1 | RO-105 | DoD S1 reproduzível |

**Total S1:** 22 SP (~11 dia-dev backend).

---

## Backlog detalhado — S2 (EP-02) · parte M2

| ID | Título | SP | Dep. | Critério de aceite |
|---|---|---:|---|---|
| RO-201 | Renderer MD → PDF (3 tipos) | 8 | RO-102 | PDF INTERNO/CLIENTE/SUPORTE |
| RO-202 | API `GET /releases/{id}/pdf` | 2 | RO-201 | Content-Disposition download |
| RO-203 | FE: CRUD módulos por produto | 5 | RO-104 | Lista/criar/editar módulos NEXUS-LD |
| RO-204 | FE: aba Artefatos na release | 5 | RO-105 | Upload por módulo; progresso; delete |
| RO-205 | FE: versão por módulo na release | 3 | RO-102 | Campos versão atual/nova visíveis |
| RO-206 | FE: botão gerar/baixar PDF | 2 | RO-202 | Preview ou download direto |
| RO-207 | QA: release 1.5.0 rascunho completa | 3 | RO-203–206 | war + sql + PDF OK |

**Total S2:** 28 SP (~14 dia-dev; **paralelo** BE PDF + FE artefatos).

---

## Backlog detalhado — S3 (EP-03) · Marco M3

| ID | Título | SP | Dep. | Critério de aceite |
|---|---|---:|---|---|
| RO-301 | Migrations Cliente, Contato, ConfigEntrega | 5 | — | Flyway/Liquibase idempotente |
| RO-302 | Migrations Domínio, Funcionalidade | 3 | RO-301 | Por produto |
| RO-303 | Migrations ClienteProduto, ClienteProdutoModulo | 5 | RO-301 | `versaoAtual` por módulo |
| RO-304 | Migrations ClienteFuncionalidade | 2 | RO-302 | Matriz N:N |
| RO-305 | API CRUD Cliente + Contatos | 5 | RO-301 | Spec `02`, `03` |
| RO-306 | API Domínios/Funcionalidades + matriz cliente | 5 | RO-304 | Spec `05` |
| RO-307 | API produtos/módulos contratados | 5 | RO-303 | Spec `06` |
| RO-308 | API ConfigEntrega (destino PASTA) | 3 | RO-301 | Spec `07` |
| RO-309 | Seed ACME + testes integração | 5 | RO-305–308 | Dados demo reproduzíveis |

**Total S3:** 38 SP (~19 dia-dev backend) · pode dividir em 2 devs ou estender 1 semana se necessário.

---

## Backlog detalhado — S4 (EP-04) · Marco M4

| ID | Título | SP | Dep. | Critério de aceite |
|---|---|---:|---|---|
| RO-401 | Migrations ProximaEntrega, Entrega, auditoria | 5 | RO-303 | Status máquina de estados |
| RO-402 | API wizard: rascunho + salvar passos | 5 | RO-401 | Spec `18` passos 1–5 |
| RO-403 | Seleção módulos: contratado vs release | 5 | RO-402 | Spec `19` lógica MVP |
| RO-404 | Delta MVP (metadados, sem Git) | 3 | RO-403 | Lista mudanças por módulo |
| RO-405 | Job @Async geração pacote (11 etapas) | 13 | RO-403 | Spec `21`; status por etapa |
| RO-406 | Etapa FUNC/REGRAS: templates + matriz | 5 | RO-405 | Arquivos no ZIP |
| RO-407 | manifest.json + SHA256SUMS | 3 | RO-405 | Validação checksum |
| RO-408 | Atualizar versaoAtual pós-CONCLUIDA | 3 | RO-405 | Idempotente |
| RO-409 | Reentrega (`entregaOriginalId`) | 3 | RO-405 | Nova entrega ligada à original |
| RO-410 | Testes geração + falha parcial | 5 | RO-405 | Retry/cancel documentado |

**Total S4:** 50 SP — **épico crítico**; priorizar RO-405 em pair programming; considerar spike 1 dia no início.

---

## Backlog detalhado — S5 (EP-05) · parte M5

| ID | Título | SP | Dep. | Critério de aceite |
|---|---|---:|---|---|
| RO-501 | Shell rotas `/orchestrator/*` + menu | 3 | DF-005 | Guards alinhados roles |
| RO-502 | Lista + formulário Cliente | 5 | RO-305 | Spec `02` |
| RO-503 | Detalhe cliente — visão geral | 3 | RO-502 | Spec `04` |
| RO-504 | Aba funcionalidades (matriz) | 5 | RO-306 | Toggle por funcionalidade |
| RO-505 | Aba produtos contratados + módulos | 5 | RO-307 | Versão instalada editável |
| RO-506 | Aba config entrega | 3 | RO-308 | Destino pasta local |
| RO-507 | Agenda próximas entregas | 5 | RO-401 | Spec `16`, `17` |
| RO-508 | Dashboard KPIs entregas (P1 mínimo) | 3 | RO-507 | Contadores status |

**Total S5:** 32 SP (~16 dia-dev front).

---

## Backlog detalhado — S6 (EP-06) · parte M5

| ID | Título | SP | Dep. | Critério de aceite |
|---|---|---:|---|---|
| RO-601 | Assistente passo 1–2 (cliente, produto) | 3 | RO-502 | Navegação wizard |
| RO-602 | Assistente passo 3–4 (versões, módulos) | 5 | RO-403 | Chama API seleção |
| RO-603 | Assistente passo 5 (revisão + finalizar) | 3 | RO-602 | Cria entrega GERANDO |
| RO-604 | Tela geração — progresso 11 etapas | 5 | RO-405 | Polling 2–5s |
| RO-605 | Detalhe entrega + download ZIP | 3 | RO-604 | Spec `22` |
| RO-606 | Histórico entregas + filtros | 3 | RO-605 | Spec `23` |
| RO-607 | Reentregar a partir do histórico | 2 | RO-409 | Botão + confirmação |
| RO-608 | QA wizard completo | 3 | RO-601–607 | ACME v1.5.0 só pela UI |

**Total S6:** 27 SP (~13,5 dia-dev front).

---

## Backlog detalhado — S7 (EP-07) · Marco M6

| ID | Título | SP | Dep. | Critério de aceite |
|---|---|---:|---|---|
| INT-701 | Roteiro demo ACME (script 30 min) | 2 | RO-608 | Passo a passo personas |
| INT-702 | Release 1.5.0 PUBLICADA (dados reais piloto) | 2 | RO-207 | Aprovação gestor |
| INT-703 | Entrega ACME CONCLUIDA + DocFlow publicado | 3 | RO-608, DF-007 | ZIP + manual ZIP |
| INT-704 | Bug bash integração (2 dias) | 5 | INT-703 | Zero blocker; P1 documentados |
| INT-705 | Retrospectiva + go/no-go Fase 2 | 1 | INT-704 | Ata com decisão S8 |

**Total S7:** 13 SP + buffer timeboxed.

---

## Trilha paralela DevOps (EP-08) — não bloqueia M6

Iniciar **semana 5** (≈ 2026-07-21); concluir idealmente até **2026-09-30**.

| ID | Título | SP | Dep. | Critério de aceite |
|---|---|---:|---|---|
| INF-801 | Jenkinsfile NEXUS-LD (build-on-tag) | 5 | — | Build local ou CI sandbox OK |
| INF-802 | Job Jenkins + credencial PAT | 3 | INF-801 | Trigger tag `v*.*.*` |
| INF-803 | Tag teste v0.0.1 → GitHub Release | 3 | INF-802 | Asset `.war` nomeado |
| INF-804 | VERSIONING.md + estrutura db/kettle | 2 | — | Spec `40` no repo |
| INF-805 | Tag v1.5.0 piloto pós-M2 | 2 | M2, INF-803 | Assets na Release real |

---

## Resumo de esforço S0–S7

| Sprint | SP total | Foco principal |
|---|---:|---|
| S0 | 15 | DocFlow auth + API |
| S1 | 22 | Backend artefatos |
| S2 | 28 | PDF + FE artefatos |
| S3 | 38 | Backend cadastros |
| S4 | 50 | Geração pacote |
| S5 | 32 | FE clientes |
| S6 | 27 | FE wizard |
| S7 | 13 | Integração + demo |
| **Total** | **225 SP** | ~112 dia-dev ≈ **56 semanas×1 dev** ou **~14 semanas×2 devs** com paralelismo |

Com **2 devs** (1 BE + 1 FE) e paralelismo do calendário, **90 dias é exequível** se S4 não estourar; reservar **1 semana buffer** antes de M6 para S4/S6 slip.

---

## Template de issue (copiar no Jira/Linear)

```markdown
## Contexto
[Link spec / jornada / ROADMAP item]

## Descrição
Como [persona], preciso [ação] para [valor].

## Critérios de aceite
- [ ] ...
- [ ] ...

## Dependências
- Blocks: RO-xxx
- Blocked by: RO-yyy

## Estimativa
X SP

## Labels
orchestrator, f1, backend|frontend
```

### Exemplo preenchido — RO-405

```markdown
## Contexto
Spec `21-geracao-pacote-entrega.md`, ROADMAP F1.15

## Descrição
Como operador, preciso disparar a geração assíncrona do pacote de entrega
para que o cliente receba ZIP com artefatos, delta MVP, PDF e manifest.

## Critérios de aceite
- [ ] POST /entregas/{id}/gerar retorna 202 + jobId
- [ ] 11 etapas persistidas com status (PENDENTE/EXECUTANDO/OK/ERRO)
- [ ] ZIP final em pasta configurada em ConfigEntrega
- [ ] Falha em etapa marca entrega ERRO com mensagem
- [ ] Testes cobrem caminho feliz ACME NEXUS-LD 1.4.0→1.5.0

## Dependências
- Blocked by: RO-403, RO-103

## Estimativa
13 SP

## Labels
orchestrator, f1, backend
```

---

## Riscos e mitigação (90 dias)

| Risco | Impacto | Mitigação |
|---|---|---|
| S4 geração estoura estimativa | Atraso M4/M6 | Spike RO-405 dia 1; MVP com 7 etapas primeiro |
| DocFlow auth mais complexo que S0 | Atraso M1 | Timebox DF-001 em 1 dia; escopo mínimo JWT |
| Dois modelos de permissão | Retrabalho menu | Alinhar roles ADMIN/EDITOR vs CLIENTE:LER na M1 |
| Falta dados demo ACME | Demo fraca | RO-309 seed automatizado na M3 |
| DevOps indisponível | S8 atrasa | Não impacta M6; upload manual explícito no demo |

---

## Referências

- Checklist sprint: [`02-checklist-por-sprint.md`](02-checklist-por-sprint.md)
- Jornada demo: [`00-cenario-feliz-acme.md`](00-cenario-feliz-acme.md)
- ROADMAP: [`../ROADMAP.md`](../ROADMAP.md)
