# 01 — Dashboard

## 1. Papel da tela

Tela inicial do orchestrator. Oferece **visão executiva e operacional consolidada** do ecossistema de releases e entregas, e atalhos para fluxos comuns.

Acessível em `/orchestrator` ou rota equivalente. É a primeira tela vista após login do usuário com perfil operacional.

---

## 2. Objetivos

| Objetivo | Como atende |
|---|---|
| Indicadores gerais (KPIs) | Cards de números no topo |
| Status atual da operação | Painel de entregas em andamento + falhas |
| Próximas ações importantes | Painel de próximas entregas |
| Histórico recente | Painel de atividades recentes |
| Atalhos a fluxos comuns | Botões "criar cliente", "nova entrega" |
| Saúde do sistema | Indicador de healthcheck (admin) |

---

## 3. Layout (sugerido)

```text
┌──────────────────────────────────────────────────────────────────┐
│ Dashboard                              👤 João (ADMIN) [logout]   │
├──────────────────────────────────────────────────────────────────┤
│ Período: [Últimos 30 dias ▼]   [Atualizar 🔄]                     │
├──────────────────────────────────────────────────────────────────┤
│ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐│
│ │Concluídas│ │ Em curso │ │ Em rev.  │ │ Próximas │ │ Falhas   ││
│ │   42     │ │    3     │ │    7     │ │   12     │ │    1     ││
│ └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘│
├──────────────────────────────────────────────────────────────────┤
│ ┌─ Entregas por período (linha) ──┐ ┌─ Status (pizza) ──────────┐│
│ │                                 │ │                            ││
│ │  [gráfico mensal]               │ │  [pizza por status]        ││
│ │                                 │ │                            ││
│ └─────────────────────────────────┘ └────────────────────────────┘│
├──────────────────────────────────────────────────────────────────┤
│ ┌─ Próximas entregas (lista) ────────────────────────────────────┐│
│ │ Hoje    │ Cliente Acme    │ DTEC-LD 1.5.0 │ APROVADA           ││
│ │ Amanhã  │ Cliente Beta    │ FOLHA 2.1.0   │ APROVADA           ││
│ │ +3 dias │ Cliente Gamma   │ DTEC-LD 1.5.0 │ PLANEJADA          ││
│ │                                          [Ver agenda completa] ││
│ └────────────────────────────────────────────────────────────────┘│
├──────────────────────────────────────────────────────────────────┤
│ ┌─ Atividades recentes ─────────┐ ┌─ Atalhos ────────────────────┐│
│ │ 14:30 João publicou DTEC 1.5.0│ │ [+ Cliente]                  ││
│ │ 14:15 Maria aprovou prox X    │ │ [+ Produto]                  ││
│ │ 13:00 Pacote Acme: SUCESSO    │ │ [+ Release]                  ││
│ │ 11:42 Pacote Beta: FALHA      │ │ [→ Nova entrega]             ││
│ │                               │ │ [→ Release Orchestrator]             ││
│ └───────────────────────────────┘ └──────────────────────────────┘│
└──────────────────────────────────────────────────────────────────┘
```

---

## 4. KPIs (cards superiores)

### Por período (filtro no topo)
Default: últimos 30 dias. Opções: hoje, 7 dias, 30 dias, este mês, este ano, customizado.

| Card | Conteúdo | Detalhe |
|---|---|---|
| **Entregas Concluídas** | Total de entregas com status `CONCLUIDA` no período | Click: lista filtrada de entregas concluídas |
| **Em Curso** | Entregas com `GeracaoStatus.PROCESSANDO` agora | Click: lista de execuções ativas |
| **Releases em Revisão** | Releases com status `EM_REVISAO` agora | Click: vai para Release Orchestrator filtrado |
| **Próximas Entregas** | `ProximaEntrega` com `dataPlanejada` nos próximos 7 dias | Click: agenda |
| **Falhas Recentes** | Entregas com `GeracaoStatus.ERRO` ou publicação falhada no período | Click: detalhe das falhas |

### Indicadores visuais
- Verde: tudo normal.
- Amarelo: requer atenção (próximas atrasadas, alertas).
- Vermelho: ação imediata (falhas, falhas críticas).

### Trend
Cada card mostra também a tendência (↑ +12% / ↓ -3%) comparando com período anterior do mesmo tamanho.

---

## 5. Gráficos

### Entregas por período (linha)
- Eixo X: dia / semana / mês (auto-ajusta com período do filtro).
- Eixo Y: número de entregas.
- 3 séries: Concluídas (verde), Em curso (azul), Falhas (vermelho).
- Tooltip mostra detalhe por ponto.

### Status (pizza)
- Distribuição: CONCLUIDA, PROCESSANDO, ERRO, CANCELADO.
- Slice click: filtra a lista abaixo por status.

### Top 5 produtos (barra horizontal) — opcional
- Produtos com mais entregas no período.

### Top 5 clientes (barra horizontal) — opcional
- Clientes com mais entregas no período.

---

## 6. Painel: Próximas Entregas

### Conteúdo
Lista das próximas 10 entregas planejadas/aprovadas, ordenadas por `dataPlanejada` ASC.

| Coluna | Conteúdo |
|---|---|
| Quando | "Hoje" / "Amanhã" / "+3 dias" / data específica |
| Cliente | Sigla + nome |
| Produto + versão | DTEC-LD v1.5.0 |
| Status | PLANEJADA / APROVADA / EM_GERACAO |
| Ações | Botão de início rápido (se EDITOR/ADMIN) |

### Regras
- Entregas atrasadas (data passada e ainda PLANEJADA): destacar em vermelho no topo.
- "Ver agenda completa" → vai para `16-proximas-entregas-agenda.md`.

---

## 7. Painel: Atividades Recentes

### Conteúdo
Últimas 20 ações do orchestrator (agregado da `orchestrator_auditoria`), ordenadas por `created_at DESC`.

| Coluna | Conteúdo |
|---|---|
| Hora | "14:30" se hoje; "ontem 16:00" se ontem; "12/05 10:00" se mais antigo |
| Usuário | Avatar/iniciais + nome |
| Ação | Verbo em PT: "publicou", "aprovou", "criou" |
| Recurso | Nome + ID curto (clicável) |

### Filtros (opcionais)
- Por usuário.
- Por tipo de ação.

### Real-time (futuro)
- WebSocket/SSE para atualizar a lista em tempo real conforme ações acontecem.
- "1 nova atividade" link para refresh.

---

## 8. Atalhos rápidos

Botões grandes/destacados:

- **+ Cliente** → cadastro de cliente (`03-clientes-cadastro.md`).
- **+ Produto** → cadastro de produto (`09-produtos-cadastro.md`).
- **+ Release** → nova release no Release Orchestrator (link externo).
- **→ Nova Entrega** → assistente (`18-nova-entrega-assistente.md`).
- **→ Release Orchestrator** → tela do release flow.

Mostrar apenas atalhos cujas ações o usuário tem permissão para executar.

---

## 9. Regras de visibilidade

### Por perfil
- **ADMIN**: vê tudo, sem restrição.
- **EDITOR**: vê tudo, sem alguns admin-only (ex.: configurações).
- **VIEWER**: vê KPIs e listas, mas atalhos de criação ocultos. Ações inline ocultas.

### Por contexto
- Falhas críticas (5xx no backend, healthcheck DEGRADED) aparecem como banner no topo, independente do filtro.

### Privacidade
- Dashboard agrega dados — não expõe detalhes de clientes a usuários sem permissão de leitura desses clientes (multitenancy futuro).

---

## 10. Backend — endpoint `/dashboard`

Para evitar N chamadas, criar endpoint agregado:

```
GET /api/v1/orchestrator/dashboard?periodo=ULTIMOS_30_DIAS
```

### Response
```json
{
  "periodo": { "inicio": "2026-05-01", "fim": "2026-05-31" },
  "periodoAnterior": { "inicio": "2026-04-01", "fim": "2026-04-30" },
  "kpis": {
    "entregasConcluidas": { "atual": 42, "anterior": 38, "variacao": 0.105 },
    "emCurso": { "atual": 3 },
    "releasesRevisao": { "atual": 7 },
    "proximasEntregas7Dias": { "atual": 12 },
    "falhasRecentes": { "atual": 1, "anterior": 0 }
  },
  "graficos": {
    "entregasPorPeriodo": [
      { "data": "2026-05-01", "concluidas": 1, "emCurso": 0, "falhas": 0 },
      { "data": "2026-05-02", "concluidas": 2, "emCurso": 0, "falhas": 0 },
      ...
    ],
    "statusAtual": {
      "concluida": 42,
      "processando": 3,
      "erro": 1,
      "cancelada": 0
    }
  },
  "proximasEntregas": [
    {
      "id": "uuid",
      "dataPlanejada": "2026-05-31",
      "cliente": { "sigla": "ACME", "nome": "Acme LTDA" },
      "produto": { "sigla": "DTECLD", "nome": "DTEC-LD" },
      "versao": "1.5.0",
      "status": "APROVADA"
    },
    ...
  ],
  "atividadesRecentes": [
    {
      "id": "uuid",
      "createdAt": "2026-05-31T14:30:00Z",
      "usuario": "joao.silva",
      "acao": "ENTREGA_GERADA",
      "entidadeTipo": "ENTREGA",
      "entidadeId": "uuid",
      "descricao": "Entrega gerada para Acme LTDA - DTEC-LD 1.5.0"
    },
    ...
  ]
}
```

### Cache
- Cache de 30s no servidor (mesmos dados para múltiplos usuários no mesmo período).
- ETag para suporte a 304 Not Modified.

---

## 11. Performance

### Cálculo dos KPIs
- Queries pesadas: agregação por `created_at` com `count(*)`.
- Para volume > 100k: criar **tabela materializada** atualizada por job (ex.: a cada 5 min).
- Stack: `pg_cron` + materialized view, ou `@Scheduled` Spring.

### Refresh button
- Permite usuário forçar refresh.
- Mostra hora do último refresh.
- Auto-refresh opcional configurável (off por default — não desperdiçar requests).

---

## 12. Erros

### Falha ao carregar
- Erro em uma seção não bloqueia as outras.
- Cada painel tem seu próprio loading/erro.

### Sem permissão
- Card vazio com mensagem: "Sem permissão para visualizar essa métrica."

### Sem dados
- KPI mostra "—" em vez de "0" quando não há dados.
- Gráfico vazio mostra mensagem clara: "Sem entregas no período."

---

## 13. Acessibilidade

- Cards são `<article>` com `aria-label`.
- Gráficos têm fallback textual (lista de valores).
- Navegação por teclado: Tab passa pelos cards e gráficos.
- Atalhos têm `aria-keyshortcuts` documentado.

---

## 14. Métricas para acompanhar uso do dashboard

| Métrica | Insight |
|---|---|
| Acessos diários ao dashboard | Quanto a equipe consulta |
| Tempo médio na tela | Engajamento |
| Cliques em "Nova entrega" no dashboard | Atalho realmente útil? |
| Filtro de período mais usado | Ajustar default |

---

## 15. Variações por perfil

### Operador (foco em "o que preciso fazer hoje")
- Painel principal: próximas entregas.
- KPIs secundários.

### Gestor (foco em "como estamos performando")
- KPIs em destaque.
- Gráficos de tendência.

### Suporte (foco em "alguém ligou e precisa de info")
- Atalho para suporte operacional.
- Busca rápida de cliente/entrega no topo.

> Decisão futura: dashboards customizáveis ou tabs/visões dedicadas por perfil.

---

## 16. Cross-reference

- [`16-proximas-entregas-agenda.md`](16-proximas-entregas-agenda.md) — Agenda completa.
- [`23-historico-entregas.md`](23-historico-entregas.md) — Histórico para a base dos KPIs.
- [`26-relatorios.md`](26-relatorios.md) — Análises mais profundas.
- [`27-suporte-operacional.md`](27-suporte-operacional.md) — Busca rápida.
- [`34-observabilidade.md`](34-observabilidade.md) — Métricas do sistema.
- [`99-padroes-tela.md`](99-padroes-tela.md) — Padrões aplicáveis.
