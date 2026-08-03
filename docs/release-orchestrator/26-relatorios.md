# 26 — Relatórios

## 1. Papel da tela

**Relatórios gerenciais, operacionais e de auditoria**. Visão analítica sobre todas as operações do orchestrator. Suporta tomada de decisão e prestação de contas.

Acessível em `/orchestrator/relatorios`.

---

## 2. Objetivos

| Objetivo | Como atende |
|---|---|
| Medir desempenho da operação | Relatórios numéricos + gráficos |
| Auditar histórico | Filtros + exportação |
| Exportar para stakeholders | PDF, Excel, CSV |
| Comparar períodos | Side-by-side |
| Detectar tendências | Gráficos temporais |

---

## 3. Layout

```text
┌────────────────────────────────────────────────────────────────────────┐
│ Relatórios                                                             │
├──────────┬─────────────────────────────────────────────────────────────┤
│ Categorias│  Período: [Últimos 90 dias ▼]   [Aplicar]                  │
│          │                                                             │
│ ▶ Operac.│  Relatório: Entregas por Cliente                            │
│ ▶ Cliente│  ─────────────────────────────────────                      │
│ ▶ Produto│                                                             │
│ ▶ Qualid.│  ┌─ Top 10 clientes ─────────────────┐  ┌─ Gráfico ──────┐ │
│ ▶ Audit. │  │ ACME LTDA      12 entregas        │  │  [barras]      │ │
│          │  │ BETA S.A.      8                   │  │                │ │
│ Favoritos│  │ GAMMA Inc.     5                   │  │                │ │
│ • Entreg.│  │ ...                                │  └────────────────┘ │
│   p/Clien│  └────────────────────────────────────┘                     │
│          │                                                             │
│          │  [⬇️ Excel] [⬇️ CSV] [⬇️ PDF] [📅 Agendar envio]            │
└──────────┴─────────────────────────────────────────────────────────────┘
```

---

## 4. Categorias de relatórios

### Operacionais
- **Entregas por período**: total, por status, por dia.
- **Tempo médio de geração**: por produto, por tipo.
- **Throughput**: entregas/hora, /dia, /mês.
- **Falhas**: contagem, por etapa, por causa.
- **Reentregas**: contagem, taxa.
- **Carga de fila**: histórico do pool async.

### Por cliente
- **Entregas por cliente**: top N, distribuição.
- **Última entrega**: por cliente.
- **Sem entrega há > N dias**.
- **Funcionalidades por cliente**: heatmap.
- **Versão atual por cliente**: tabela cruzada.

### Por produto
- **Releases por produto**: contagem, status.
- **Entregas por produto**: total.
- **Adoção**: clientes contratando.
- **Módulos mais entregues**.

### Qualidade
- **Taxa de sucesso**: % entregas concluídas.
- **MTBF**: tempo médio entre falhas.
- **Tempo médio de resolução**: falha → reentrega ok.
- **Tendência de problemáticas**.

### Auditoria
- **Ações por usuário**: contagem por tipo.
- **Mudanças de credenciais**.
- **Acessos a dados sensíveis**.
- **Exportações realizadas**.

---

## 5. Estrutura de cada relatório

### Conteúdo
- **Filtros específicos** do relatório.
- **Gráfico** (quando aplicável).
- **Tabela** com dados detalhados.
- **Resumo executivo** no topo (1-2 linhas).

### Exportação
- PDF (formatado com cabeçalho).
- Excel (com gráficos embutidos).
- CSV (dados brutos).
- JSON (para integração).

---

## 6. Filtros globais

| Filtro | Aplicável a |
|---|---|
| Período | Todos |
| Cliente | Cliente, Operacionais |
| Produto | Produto, Operacionais |
| Status | Operacionais, Qualidade |
| Usuário | Auditoria |

---

## 7. Favoritos e agendamento

### Favoritar relatório
- Marcar como favorito + parâmetros salvos.
- Aparece na lateral.

### Agendamento (futuro)
- Enviar por e-mail diário/semanal/mensal.
- Destinatários múltiplos.
- Formato (PDF/CSV).

---

## 8. Contratos de API

### Listar tipos de relatório

```
GET /api/v1/orchestrator/relatorios/tipos
```

### Gerar relatório

```
POST /api/v1/orchestrator/relatorios/{tipo}/gerar
```

Body com filtros específicos.

Response: dados + metadados.

### Exportar

```
POST /api/v1/orchestrator/relatorios/{tipo}/exportar?formato=PDF
```

Response: stream binário.

### Agendar

```
POST /api/v1/orchestrator/relatorios/agendados
```

```json
{
  "tipo": "entregas-por-cliente",
  "parametros": {...},
  "frequencia": "DIARIA",
  "destinatarios": ["gerencia@nexus.com"],
  "formato": "PDF"
}
```

---

## 9. DTOs

```java
public record RelatorioResultadoResponse(
    String tipoRelatorio,
    String periodoDescricao,
    JsonNode resumoExecutivo,
    JsonNode dadosGrafico,
    List<JsonNode> linhas,
    int totalLinhas,
    OffsetDateTime geradoEm,
    String geradoPor
) {}
```

---

## 10. Performance

### Cache
- Relatórios pesados (volume grande): cache 5 min.
- Refresh manual disponível.

### Geração pesada
- Async se demora > 5s.
- Notificação quando pronto.

### Volumes
- Materialized views para agregados.
- Recalc noturno via cron.

---

## 11. Permissões

| Categoria | ADMIN | EDITOR | LEITOR |
|---|---|---|---|
| Operacionais | ✅ | ✅ | ✅ |
| Por cliente/produto | ✅ | ✅ | ✅ |
| Qualidade | ✅ | ✅ | ✅ |
| Auditoria | ✅ | ❌ | ✅ |
| Exportar | ✅ | ✅ | ❌ |
| Agendar | ✅ | ❌ | ❌ |

---

## 12. Acessibilidade

- Gráficos com fallback textual.
- Tabelas semânticas.
- Filtros labelados.

---

## 13. Auditoria

| Ação | Detalhes |
|---|---|
| `RELATORIO_GERADO` | tipo, parâmetros |
| `RELATORIO_EXPORTADO` | formato |
| `RELATORIO_AGENDADO` | configuração |

---

## 14. Cross-reference

- [`23-historico-entregas.md`](23-historico-entregas.md) — Base de dados.
- [`27-suporte-operacional.md`](27-suporte-operacional.md) — Diagnóstico.
- [`34-observabilidade.md`](34-observabilidade.md) — Métricas internas.
- [`99-padroes-tela.md`](99-padroes-tela.md) — Padrões.
