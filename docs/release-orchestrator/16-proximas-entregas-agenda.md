# 16 — Próximas Entregas — Agenda

## 1. Papel da tela

**Agenda operacional das entregas planejadas**. Ponto central onde gestores e operadores acompanham o que está por vir, priorizam o trabalho e identificam atrasos.

Acessível em `/orchestrator/proximas-entregas`. Atalho a partir do dashboard.

---

## 2. Objetivos

| Objetivo | Como atende |
|---|---|
| Visualizar entregas futuras | Tabela + calendário |
| Identificar atrasos | Indicador visual + filtro |
| Priorizar trabalho | Coluna de prioridade + ordenação |
| Replanejar | Ação inline para mudar data |
| Iniciar execução | "Converter em entrega" pré-preenche `18` |
| Acompanhar progresso | Status visíveis |

---

## 3. Layout

```text
┌────────────────────────────────────────────────────────────────────────┐
│ Próximas Entregas                              [+ Planejar entrega]    │
├────────────────────────────────────────────────────────────────────────┤
│ Indicadores:                                                           │
│ ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐          │
│ │ Esta semana│ │ Críticas   │ │ Atrasadas  │ │ Replanej.  │          │
│ │     12     │ │     3      │ │     2      │ │     1      │          │
│ └────────────┘ └────────────┘ └────────────┘ └────────────┘          │
├────────────────────────────────────────────────────────────────────────┤
│ Período: [Próximas 30 dias ▼]  Cliente: [Todos ▼]  Produto: [Todos ▼] │
│ Status: [Pendentes ▼]                            [Limpar filtros]      │
├────────────────────────────────────────────────────────────────────────┤
│ ┌─ Tabela ─────────────────────────┐ ┌─ Calendário / Próx 48h ────┐  │
│ │ Data  Cliente  Produto Vers Stat │ │  Maio 2026                 │  │
│ │ 01/06 ACME    NEXUSLD 1.5.0 ⚠️PLA│ │  S  T  Q  Q  S  S  D       │  │
│ │ 02/06 BETA    FOLHA  2.1.0 ✅APR│ │  1  2  3  4  5  6  7       │  │
│ │ 02/06 GAMMA   NEXUSLD 1.5.0 🟡PLA│ │  ●  ●●         ●           │  │
│ │ 05/06 DELTA   CONTAS 0.9.0 ⚠️CRI│ │  8  9 10 11 12 13 14       │  │
│ │ 10/06 ACME    INTPAG 1.0.0 ✅PLA│ │     ●                 ●   │  │
│ │                                  │ │  ...                       │  │
│ │ [< 1 2 3 >]                      │ │  Próximas 48h:             │  │
│ │                                  │ │  • ACME NEXUS-LD 1.5.0      │  │
│ │                                  │ │  • BETA FOLHA 2.1.0        │  │
│ └──────────────────────────────────┘ └────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Indicadores (cards superiores)

| KPI | Definição | Cor |
|---|---|---|
| **Esta semana** | Próximas entregas com dataPlanejada na semana atual | Azul |
| **Críticas** | Prioridade `CRITICA` em qualquer período visível | Vermelho |
| **Atrasadas** | dataPlanejada < hoje E status != CONVERTIDA/CANCELADA | Vermelho |
| **Replanejadas** | Status `REPLANEJADA` no período | Amarelo |

Click no card → filtra a tabela.

---

## 5. Filtros

### Período
- Hoje, próximas 7 dias, próximas 30 dias, próximos 90 dias, customizado.
- Default: próximas 30 dias.

### Cliente
- Multi-select.

### Produto
- Multi-select.

### Ambiente
- Multi-select.

### Status
- Multi-select.
- Default: PLANEJADA + AGENDADA + REPLANEJADA + ATRASADA (pendentes).

### Prioridade
- BAIXA, MEDIA, ALTA, CRITICA.

### Avançados
- Apenas com problemas (atrasada, sem responsável).
- Por responsável.

### Persistência
- Querystring.
- Reset paginação ao mudar.

---

## 6. Tabela

### Colunas

| Coluna | Conteúdo | Sortable |
|---|---|---|
| Data | dataPlanejada + tempo relativo ("amanhã") | ✅ |
| Cliente | Sigla + nome | ✅ |
| Produto | Sigla + cor | ✅ |
| Versão | Versão prevista | — |
| Ambiente | PROD/HOM | ✅ |
| Prioridade | Badge colorido | ✅ |
| Responsável | Nome | ✅ |
| Status | Badge | ✅ |
| Ações | Menu kebab | — |

### Sort default
`dataPlanejada ASC`.

### Indicadores na linha
- **Vermelho**: atrasada.
- **Amarelo**: replanejada.
- **Roxo**: crítica.
- **Cinza**: cancelada/convertida.

### Click
- Click linha → modal/tela de detalhe (`17-proximas-entregas-cadastro.md` modo view).
- Click "Converter em entrega" → wizard `18` pré-preenchido.

---

## 7. Painel lateral

### Mini-calendário
- Mês corrente com pontos nos dias que têm entregas.
- Click dia → filtra tabela por essa data.
- Navegação mês anterior/próximo.

### Próximas 48 horas
- Lista das entregas nas próximas 48h (todos clientes).
- Cor por urgência.
- Click → detalhe.

---

## 8. Ações

### Toolbar
- **+ Planejar entrega** → `17-proximas-entregas-cadastro.md` modo criar.
- **Exportar CSV** → download com filtros.

### Inline (kebab)
- **Detalhe**.
- **Editar** → modo edit.
- **Replanejar** → modal de nova data + motivo.
- **Converter em entrega** → wizard 18 pré-preenchido.
- **Cancelar** → confirm + motivo.
- **Duplicar** → cria nova para outro cliente/data.

### Permissões

| Ação | ADMIN | EDITOR | LEITOR |
|---|---|---|---|
| Ver agenda | ✅ | ✅ | ✅ |
| Planejar | ✅ | ✅ | ❌ |
| Editar/Replanejar | ✅ | ✅ | ❌ |
| Cancelar | ✅ | ❌ | ❌ |
| Converter | ✅ | ✅ | ❌ |

---

## 9. Status de Próxima Entrega

| Status | Significado | Visual |
|---|---|---|
| `PLANEJADA` | Criada, sem data confirmada ou em planejamento | ⚪ Cinza |
| `AGENDADA` | Data confirmada e validada | 🔵 Azul |
| `APROVADA` | Aprovada para gerar | ✅ Verde |
| `REPLANEJADA` | Data alterada após confirmação | 🟡 Amarelo |
| `ATRASADA` | Data passou e não foi convertida nem cancelada | 🔴 Vermelho |
| `EM_GERACAO` | Conversão iniciada (entrega real existindo) | 🟦 Azul-escuro |
| `CONVERTIDA` | Virou Entrega real concluída | ✅ Verde + check |
| `CANCELADA` | Cancelada antes da execução | ⚫ Cinza-escuro |

### Diferença entre Próxima Entrega e Entrega
- **Próxima Entrega**: planejamento. Existe **antes** de gerar pacote.
- **Entrega**: execução. Existe **a partir do início da geração**.
- Status de execução (PROCESSANDO, CONCLUIDO, ERRO) vivem em `Entrega`, não aqui.

---

## 10. Contratos de API

### Listar agenda

```
GET /api/v1/orchestrator/proximas-entregas
```

#### Query params

| Param | Tipo | Default |
|---|---|---|
| inicio | Date | hoje |
| fim | Date | hoje + 30d |
| clienteId | UUID list | — |
| produtoId | UUID list | — |
| ambiente | String list | — |
| status | StatusPE list | [PLANEJADA, AGENDADA, REPLANEJADA, ATRASADA] |
| prioridade | Prioridade list | — |
| responsavelId | UUID | — |
| page, size, sort, direction | | |

#### Response

```json
{
  "items": [
    {
      "id": "uuid",
      "clienteId": "uuid", "clienteSigla": "ACME", "clienteNome": "ACME LTDA",
      "produtoId": "uuid", "produtoSigla": "NEXUSLD", "produtoCor": "#2563eb",
      "releaseId": "uuid", "versao": "1.5.0",
      "ambiente": "PROD",
      "dataPlanejada": "2026-06-01T22:00:00-03:00",
      "prioridade": "ALTA",
      "responsavelId": "uuid", "responsavelNome": "Maria",
      "status": "AGENDADA",
      "atrasada": false,
      "dependencias": [],
      "observacoes": "Janela acordada com cliente"
    }
  ],
  "page": 1,
  "size": 15,
  "totalElements": 47
}
```

### Indicadores

```
GET /api/v1/orchestrator/proximas-entregas/indicadores?inicio=&fim=
```

Response:
```json
{
  "estaSemana": 12,
  "criticas": 3,
  "atrasadas": 2,
  "replanejadas": 1
}
```

### Calendário (mini)

```
GET /api/v1/orchestrator/proximas-entregas/calendario?mes=2026-06
```

Response:
```json
{
  "mes": "2026-06",
  "dias": [
    { "data": "2026-06-01", "total": 2, "criticas": 0 },
    { "data": "2026-06-02", "total": 3, "criticas": 1 }
  ]
}
```

### Próximas 48h

```
GET /api/v1/orchestrator/proximas-entregas/proximas-48h
```

---

## 11. DTOs

Ver `17-proximas-entregas-cadastro.md` para `ProximaEntregaResponse` completo.

---

## 12. Regras

### 12.1 Atrasada (calculado)
- `dataPlanejada < now()` E `status NOT IN (CONVERTIDA, CANCELADA, EM_GERACAO)`.
- Calculado server-side; frontend não recalcula.

### 12.2 Conversão
- Botão "Converter em entrega" só aparece se status `APROVADA` e release `PUBLICADA`.
- Conversão muda status para `EM_GERACAO` e abre wizard.

### 12.3 Replanejamento
- Mudança de data: exige motivo (texto).
- Status muda para `REPLANEJADA` mantendo histórico.
- Replanejamentos repetidos somam (ex: "replanejada 3x").

### 12.4 Cancelamento
- Pede motivo.
- Status `CANCELADA` (terminal).
- Histórico preservado.

### 12.5 Dependências
- Próxima entrega pode depender de outras (ordem de execução).
- Não pode converter se dependência não está `CONVERTIDA`.

---

## 13. Performance

### Otimizações
- Endpoint agregado para indicadores + lista.
- Cache TTL 30s para lista.
- Calendário pré-computado por mês.

---

## 14. Estados e edge cases

### Sem próximas entregas no período
- Empty: "Nenhuma entrega planejada no período."
- CTA: "Planejar primeira entrega".

### Todas atrasadas
- Banner vermelho no topo.
- Suggestion: "Replaneje as atrasadas".

### Conversão bloqueada
- Tooltip explicando: "Release ainda em revisão. Aprove a release primeiro."

---

## 15. Acessibilidade

- Tabela semântica.
- Mini-calendário com `<table>` + `aria-label`.
- Click dia → keyboard equivalente (Enter).
- Indicadores com `aria-live` em mudanças.

---

## 16. Auditoria

| Ação | Detalhes |
|---|---|
| `PROXIMA_ENTREGA_CRIADA` | snapshot |
| `PROXIMA_ENTREGA_REPLANEJADA` | de → para + motivo |
| `PROXIMA_ENTREGA_APROVADA` | usuário |
| `PROXIMA_ENTREGA_CANCELADA` | motivo |
| `PROXIMA_ENTREGA_CONVERTIDA` | entregaId gerada |

---

## 17. Cross-reference

- [`01-dashboard.md`](01-dashboard.md) — Cards do dashboard.
- [`17-proximas-entregas-cadastro.md`](17-proximas-entregas-cadastro.md) — Form de cadastro.
- [`18-nova-entrega-assistente.md`](18-nova-entrega-assistente.md) — Wizard de geração.
- [`23-historico-entregas.md`](23-historico-entregas.md) — Entregas concluídas.
- [`32-modelo-dados-sugerido.md`](32-modelo-dados-sugerido.md) — Entidade ProximaEntrega.
- [`99-padroes-tela.md`](99-padroes-tela.md) — Padrões.
