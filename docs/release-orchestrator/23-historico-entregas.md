# 23 — Histórico de Entregas

## 1. Papel da tela

**Consulta rastreável das entregas realizadas**. Listagem cronológica com filtros poderosos, base para auditoria, suporte e relatórios.

Acessível em `/orchestrator/entregas`. Atalho a partir do dashboard e da visão geral do cliente.

---

## 2. Objetivos

| Objetivo | Como atende |
|---|---|
| Auditar entregas | Lista completa + filtros |
| Saber o que cada cliente recebeu | Filtro por cliente |
| Consultar versões por módulo | Drill-down em detalhe |
| Detectar problemas | Status + filtro de problemáticas |
| Suporte (cliente liga reportando) | Busca rápida + filtros |
| Identificar reentregas | Coluna + filtro |

---

## 3. Layout

```text
┌────────────────────────────────────────────────────────────────────────┐
│ Histórico de Entregas                            [Exportar CSV]        │
├────────────────────────────────────────────────────────────────────────┤
│ 🔍 [Busca: ID, cliente, produto...]                                    │
│                                                                        │
│ Período: [Últimos 90 dias ▼]  Cliente: [Todos ▼]  Produto: [Todos ▼]  │
│ Status: [Todos ▼]  Módulo: [Todos ▼]  Responsável: [Todos ▼]          │
│ ☐ Apenas problemáticas      ☐ Apenas reentregas       [Limpar tudo]    │
├────────────────────────────────────────────────────────────────────────┤
│ ┌──────────────────────────────────────────────────────────────────┐  │
│ │ ID         │ Cliente │ Produto │ Vers │ Data    │ Status │ Reent│  │
│ ├──────────────────────────────────────────────────────────────────┤  │
│ │ ENTR-0142 │ ACME    │ NEXUSLD  │ 1.5.0│ 31/05/26│ ✅     │ —    │  │
│ │ ENTR-0141 │ BETA    │ FOLHA   │ 2.0.0│ 30/05/26│ ✅     │ —    │  │
│ │ ENTR-0140 │ GAMMA   │ CONTAS  │ 0.9.0│ 28/05/26│ ⚠️ Prob│ —    │  │
│ │ ENTR-0139 │ ACME    │ NEXUSLD  │ 1.4.0│ 15/05/26│ ✅     │ #138 │  │
│ │ ENTR-0138 │ ACME    │ NEXUSLD  │ 1.4.0│ 14/05/26│ ❌ Falh│ —    │  │
│ │ ENTR-0137 │ DELTA   │ NEXUSCR  │ 1.0.0│ 10/05/26│ ✅     │ —    │  │
│ │ ENTR-0136 │ ACME    │ INTPAG  │ 1.0.0│ 05/05/26│ ✅     │ —    │  │
│ └──────────────────────────────────────────────────────────────────┘  │
│                                                                        │
│ Mostrando 1-15 de 142   [< 1 2 3 ... 10 >]                            │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Filtros

### Busca
- ID curto (`ENTR-0142`), cliente, produto, versão.

### Período
- Default: últimos 90 dias.
- Atalhos: hoje, esta semana, este mês, este ano, customizado.

### Cliente, Produto, Ambiente, Responsável
- Multi-select.

### Status
| Status | Cor |
|---|---|
| CONCLUIDA | Verde |
| EM_GERACAO | Azul |
| RASCUNHO | Cinza |
| AGUARDANDO_APROVACAO | Amarelo |
| ERRO | Vermelho |
| CANCELADA | Cinza escuro |

### Módulo
- Filtra entregas que incluíram um módulo específico.
- Útil: "todas entregas que tocaram nexus-db-ddl".

### Avançados
- Apenas problemáticas.
- Apenas reentregas.
- Por versão entregue.

### Persistência
- Querystring.
- Reset paginação ao mudar.

---

## 5. Tabela

### Colunas

| Coluna | Conteúdo | Sortable |
|---|---|---|
| ID | Código curto (ENTR-XXXX) | ✅ |
| Cliente | Sigla + nome | ✅ |
| Produto | Sigla + cor | ✅ |
| Versão | Versão entregue | — |
| Data | Data de publicação | ✅ |
| Status | Badge colorido | ✅ |
| Responsável | Nome | ✅ |
| Reentrega de | ID da entrega original (se aplicável) | — |
| Ações | Menu | — |

### Sort default
`dataCriacao DESC`.

### Click na linha
- Vai para `22-detalhes-entrega.md`.

### Indicadores
- ⚠️ Badge "Problemática".
- 🔄 Indicador de reentrega (link para original).
- ❌ Linha vermelha para FALHA.

---

## 6. Ações

### Toolbar
- **Exportar CSV** — download com filtros aplicados.
- **Exportar Excel** — opcional.

### Inline (kebab)
- **Detalhes** — vai para `22`.
- **Download pacote** — atalho direto.
- **Reentregar** — atalho.

### Permissões

| Ação | ADMIN | EDITOR | LEITOR |
|---|---|---|---|
| Listar e ver | ✅ | ✅ | ✅ |
| Download | ✅ | ✅ | ✅ |
| Reentregar | ✅ | ✅ | ❌ |
| Exportar | ✅ | ✅ | ❌ |

---

## 7. Regras

### 7.1 Imutabilidade
- Histórico **nunca é apagado fisicamente**.
- LGPD: anonimização possível, mas registros permanecem.

### 7.2 Base para delta
- Histórico é fonte da "versão atual do cliente" para cálculo de delta.
- Última entrega CONCLUIDA do cliente para módulo X define versão atual.

### 7.3 Reentregas
- Reentregas têm referência ao original.
- Filtro pode mostrar só reentregas ou só originais.

### 7.4 Status RASCUNHO
- Aparecem com cor diferente (cinza).
- Operador pode continuar de rascunho.

### 7.5 Performance em volume
- > 100k entregas → paginação cursor-based.
- Materialized view para agregados.

---

## 8. Contratos de API

### Listar

```
GET /api/v1/orchestrator/entregas
```

Query params: vários filtros (igual aos da tela).

Response:
```json
{
  "items": [
    {
      "id": "uuid",
      "codigo": "ENTR-2026-0142",
      "clienteId": "uuid", "clienteSigla": "ACME",
      "produtoId": "uuid", "produtoSigla": "NEXUSLD", "produtoCor": "#2563eb",
      "versao": "1.5.0",
      "ambiente": "PROD",
      "status": "CONCLUIDA",
      "dataCriacao": "2026-05-31T14:32:00Z",
      "dataPublicacao": "2026-05-31T14:35:00Z",
      "responsavel": "joao.silva",
      "entregaOriginalId": null,
      "ehReentrega": false,
      "problematica": false,
      "totalModulos": 6,
      "tamanhoBytes": 53216789
    }
  ],
  "page": 1, "size": 15, "totalElements": 142, "totalPages": 10
}
```

### Exportar CSV

```
GET /api/v1/orchestrator/entregas/export?formato=CSV
```

Aceita mesmos filtros. Stream de CSV.

### Buscar por código

```
GET /api/v1/orchestrator/entregas/codigo/{codigo}
```

Útil para suporte: cliente liga com código → busca direta.

---

## 9. DTOs

```java
public record EntregaListaResponse(
    UUID id, String codigo,
    UUID clienteId, String clienteSigla, String clienteNome,
    UUID produtoId, String produtoSigla, String produtoCor,
    String versao, String ambiente,
    StatusEntrega status,
    OffsetDateTime dataCriacao, OffsetDateTime dataPublicacao,
    String responsavel,
    UUID entregaOriginalId, boolean ehReentrega,
    boolean problematica,
    int totalModulos, long tamanhoBytes
) {}
```

---

## 10. Performance

### Volumes
- Crescimento típico: ~100 entregas/mês.
- Em 5 anos: ~6000.

### Otimizações
- Índices: `(cliente_id, data_criacao)`, `(produto_id, data_criacao)`, `(status)`.
- Materialized view para contadores.
- Paginação cursor-based em volumes grandes.

---

## 11. Estados e edge cases

### Sem entregas
- Empty: "Nenhuma entrega ainda."
- CTA: "Planejar primeira entrega".

### Filtro sem resultado
- "Nenhuma entrega corresponde aos filtros."
- "Limpar filtros".

### Suporte ligou com código
- Busca rápida no topo.
- "ENTR-2026-0142" → resultado imediato.

---

## 12. Acessibilidade

- Tabela semântica.
- Sort buttons com `aria-sort`.
- Indicadores de status com `aria-label`.

---

## 13. Auditoria

| Ação | Detalhes |
|---|---|
| `ENTREGA_HISTORICO_EXPORTADO` | filtros aplicados |
| `ENTREGA_BUSCADA_POR_CODIGO` | quem (útil para suporte tracking) |

---

## 14. Cross-reference

- [`22-detalhes-entrega.md`](22-detalhes-entrega.md) — Drill-down.
- [`27-suporte-operacional.md`](27-suporte-operacional.md) — Suporte rápido.
- [`26-relatorios.md`](26-relatorios.md) — Análises agregadas.
- [`32-modelo-dados-sugerido.md`](32-modelo-dados-sugerido.md) — Entidade Entrega.
- [`99-padroes-tela.md`](99-padroes-tela.md) — Padrões.
