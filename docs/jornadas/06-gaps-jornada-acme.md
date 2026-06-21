# 06 — Gaps da jornada ACME pós-S6

Auditoria das telas, rotas e endpoints listados em
[`00-cenario-feliz-acme.md`](00-cenario-feliz-acme.md) contra o código
atual (fim da S6). Cada item tem **caminho**, **classificação** e
**ação sugerida**.

Classificação:
- 🔴 **Bloqueante** — impede uma cena da demo de funcionar.
- 🟡 **UX/funcional** — cena funciona mas com fricção ou substituto.
- 🟢 **Backlog** — fora do MVP, planejado para fase futura.

---

## 1. Resumo executivo

| Tema | Status | Impacto na demo |
|---|---|---|
| Release builder + revisão + publicação | ✅ Completo | OK |
| Upload manual de artefatos | ✅ Completo | OK |
| Cadastro de cliente | ✅ Completo | OK |
| Agenda de próximas entregas | ✅ Completo | OK |
| Wizard 5 passos + geração + polling | ✅ Completo | OK |
| Detalhe entrega + download PDF + reentregar | ✅ Completo | OK |
| Visão geral cliente (abas Produtos / Funcionalidades / Histórico) | 🟡 Parcial | Cena 2.2 do `00` reduzida |
| Suporte (`27`) | 🟡 Ausente | Substituído por histórico filtrado |
| Indicadores agregados (`/proximas-entregas/indicadores`, `/calendario`) | 🟡 Ausente | KPI calculado client-side |
| Spec `19` (seleção módulos avançada) | 🟢 Backlog F2 | Wizard MVP basta |
| Spec `21` (progresso 11 etapas) | 🟢 Backlog F2 | Polling simples basta |
| Spec `20` (delta com GitHub tags) | 🟢 Backlog F2 | Sem GitHub no MVP |
| Download direto do ZIP pelo UI | 🟡 Path exposto, sem endpoint binário | Copy/paste do caminho |

Nenhum gap 🔴 **na cena principal** (4 do `00`). Demo curta roda.

---

## 2. Detalhes por área

### 2.1 Cliente — abas pendentes

**Spec:** [`04-cliente-visao-geral.md`](../release-orchestrator/04-cliente-visao-geral.md) §7
**Front:** `frontend/src/app/modules/release-orchestrator/pages/clientes/cliente-detalhe/cliente-detalhe.component.html`
**Hoje:** 4 abas (Visão geral / Contatos / Config. entrega / Próximas entregas).
**Falta:** Produtos contratados, Funcionalidades por domínio, Histórico de auditoria.

**Backend ausente:**
- `ClienteProdutoContratado` entity + `/clientes/{id}/produtos-contratados`
- `ClienteFuncionalidade` queries por cliente
- `Auditoria` por entidade cliente

**Ação sugerida:** próxima sprint backend — começar por `ClienteProdutoContratado`
(desbloqueia spec `06` e o wizard ganha "produtos contratados do cliente"
para filtrar).

Classificação: 🟡

---

### 2.2 Indicadores e calendário da agenda

**Spec:** [`16-proximas-entregas-agenda.md`](../release-orchestrator/16-proximas-entregas-agenda.md) §4, §7
**Front:** `proximas-entregas-list.component.ts` (sem cards de indicadores)
**Hoje:** Lista filtrada com paginação. Sem cards superiores (Esta semana / Críticas / Atrasadas).

**Endpoints ausentes:**
- `GET /proximas-entregas/indicadores?inicio=&fim=`
- `GET /proximas-entregas/calendario?mes=2026-06`
- `GET /proximas-entregas/proximas-48h`

**Workaround atual:** o dashboard calcula "Atrasadas" client-side filtrando
`status` ativo e `dataPrevista < hoje`. Suficiente para volumes pequenos.

**Ação sugerida:** quando passar de ~100 entregas ativas, criar endpoint
agregado para evitar listar tudo. Por enquanto não bloqueia.

Classificação: 🟢

---

### 2.3 Visão geral agregada do cliente

**Spec:** [`04-cliente-visao-geral.md`](../release-orchestrator/04-cliente-visao-geral.md) §9
**Endpoint ausente:** `GET /clientes/{id}/visao-geral` (resposta agregada com
última entrega, próxima, contagem de produtos, alertas).

**Hoje:** `cliente-detalhe.component.ts` faz `forkJoin` de 4 chamadas
(cliente + contatos + config + próximas entregas). Funciona, custa
4 round-trips.

**Ação sugerida:** criar quando otimização de tela passar de hipotética
para mensurada. Sem urgência.

Classificação: 🟢

---

### 2.4 Tela de suporte

**Spec:** `27-suporte.md` (referenciada no `00`, ainda não desenhada em detalhe)
**Hoje:** Inexistente.
**Workaround:** Histórico de entregas (`/entregas`) com filtro por cliente
cobre 80% do caso "o que ACME recebeu?".

**Ação sugerida:** post-MVP. Quando a área de suporte abrir tickets de
verdade, desenhar a tela com base no caso real.

Classificação: 🟡

---

### 2.5 Download direto do ZIP

**Backend:** `Entrega.arquivoPacoteCaminho` guarda path em disco. Não há
controller binário tipo `GET /entregas/{id}/pacote/download` retornando
o ZIP.

**Frontend:** detalhe da entrega mostra o caminho como `code`.

**Hoje no MVP:** operador copia o caminho e baixa via SSH/SCP.
**Para demo:** funciona se a pasta de saída estiver montada localmente.

**Ação sugerida:** expor controller streaming do arquivo com
`Content-Disposition: attachment`, semelhante ao `DocumentoEntregaController`,
mas com `application/zip`.

Classificação: 🟡 (5 linhas de código, valor alto na demo)

---

### 2.6 Seleção de módulos avançada

**Spec:** [`19-selecao-modulos.md`](../release-orchestrator/19-selecao-modulos.md)
- Modo Automático vs Manual
- Tabela com `FROM_TAG`/`TO_TAG` editável (delta Git)
- Pré-visualização de SQL/Kettle

**Hoje:** Wizard passo 4 só permite toggle `selecionado` por módulo.
**Substituto:** suficiente para o MVP — toda decisão de versão já vem do
contrato + release. Editar `FROM/TO` manualmente só faz sentido com Git.

Classificação: 🟢 (F2)

---

### 2.7 Progresso de geração com 11 etapas

**Spec:** [`21-geracao-pacote.md`](../release-orchestrator/21-geracao-pacote.md)
- Stream Server-Sent Events ou WebSocket por etapa
- Visualização tipo "log invertido"

**Hoje:** Polling em `GET /entregas/{id}` a cada 5 s. Status binário
`EM_GERACAO` → `CONCLUIDA`/`FALHA`.

**Substituto:** suficiente para geração rápida (~5-30 s no MVP). Quando
geração ultrapassar minutos (delta Git real, KETTLE complexo), virar
SSE.

Classificação: 🟢 (F2)

---

### 2.8 Range automático cliente → alvo

**Spec:** wizard passo 3 deve mostrar "Range automático: v1.4.0 → v1.5.0"
baseado na versão já instalada no cliente.

**Hoje:** wizard mostra só a release alvo. O passo 4 (módulos) mostra
`versaoFrom`/`versaoTo` calculado pelo backend, então a informação **existe**
— só não está no resumo do passo 3.

**Ação sugerida:** small UX win — exibir `versaoFrom` agregada na tela
de revisão (passo 5). 30 min de trabalho.

Classificação: 🟡

---

### 2.9 Integração DocFlow ↔ Orchestrator

**Spec:** `00` §"Lacunas conhecidas" mencionava paths `/api/doc-flow/*` vs
`/api/v1/docflow/*` e auth mock vs real.

**Auditoria atual:**
- `proxy.conf.json` já reescreve `/api/doc-flow` → `/api/v1/docflow` em dev. ✅
- `AuthApiService` em `seguranca` chama `POST /api/v1/auth/login` real
  (não mock). ✅
- `ConfiguracaoService` consome `EmpresaController` real. ✅

Os três itens da seção "Lacunas conhecidas" do `00` já estão **fechados**.
A nota no `00` está desatualizada.

**Ação sugerida:** atualizar `00-cenario-feliz-acme.md` removendo a seção
"Lacunas conhecidas" (ou apontando para este documento).

Classificação: 🟡 (doc rot)

---

## 3. Próximas sprints — sugestão de ordem

Priorizando por desbloqueio da demo + valor de negócio:

1. **Controller de download do ZIP** (🟡, 1 dia) — fecha a única fricção
   real da cena 4.
2. **Range automático no passo 5** (🟡, ½ dia) — UX win, dado já existe.
3. **`ClienteProdutoContratado` + aba Produtos** (🟡, 3-5 dias) — desbloqueia
   spec `06` e cliente passa a ter histórico operacional visível.
4. **Indicadores agregados da agenda** (🟢, 1-2 dias) — só quando volume
   pedir.
5. **SSE de progresso + spec `21`** (🟢) — junto com F2 (Jenkins/GitHub).

---

## 4. Critérios DoD F1 ainda em aberto

Da [`02-checklist-por-sprint.md`](02-checklist-por-sprint.md) §Sprint 7:

- [x] João: builder release 1.5.0 → revisão → PUBLICADA (✅ implementado)
- [x] Upload manual artefatos na release (✅ aba Artefatos)
- [x] Carlos: agenda → Pedro: assistente → pacote ACME (✅ wizard + atalho da agenda)
- [x] Suporte: consulta histórico (✅ via filtros do histórico)
- [ ] **Roteiro único documentado (15–30 min demo)** → entregue em
  [`05-demo-roteiro-acme.md`](05-demo-roteiro-acme.md)
- [ ] Bugs críticos integração DocFlow resolvidos (S0) — sem bug crítico
  aberto neste momento; depende de execução real da demo para validar.

DoD da fase 1 está coberto **com os workarounds documentados acima**.
