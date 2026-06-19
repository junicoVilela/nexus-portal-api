# 27 — Suporte Operacional

## 1. Papel da tela

**Monitoramento técnico de falhas, jobs e reprocessamentos**. Ferramenta de diagnóstico para operador/DevOps quando algo dá errado ou precisa intervenção.

Acessível em `/orchestrator/suporte`.

---

## 2. Objetivos

| Objetivo | Como atende |
|---|---|
| Ver filas de execução | Aba "Fila" |
| Ver falhas recentes | Aba "Falhas" |
| Reprocessar etapas | Ações inline |
| Consultar logs técnicos | Aba "Logs" |
| Auditoria | Aba "Auditoria" |
| Busca rápida | Topo da tela |

---

## 3. Layout

```text
┌────────────────────────────────────────────────────────────────────────┐
│ Suporte Operacional                                                    │
├────────────────────────────────────────────────────────────────────────┤
│ 🔍 [Buscar: ID entrega, cliente, código...]                            │
├────────────────────────────────────────────────────────────────────────┤
│ [Fila] [Falhas] [Reprocessamentos] [Logs] [Auditoria] [Saúde]          │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│ Aba: Falhas (últimas 24h)                                              │
│                                                                        │
│ ┌──────────────────────────────────────────────────────────────────┐ │
│ │ Quando│ Entrega │ Cliente │ Etapa que falhou │ Causa            │ │
│ ├──────────────────────────────────────────────────────────────────┤ │
│ │ 13:42 │ ENTR-138│ ACME    │ Render PDF       │ Timeout          │ │
│ │ 11:20 │ ENTR-130│ DELTA   │ Coletar SQL      │ Tag não existe   │ │
│ │ 09:15 │ ENTR-128│ GAMMA   │ Publicar         │ Disco cheio      │ │
│ └──────────────────────────────────────────────────────────────────┘ │
│                                                                        │
│ Ações em entrega selecionada:                                          │
│ [🔄 Recalcular delta] [📄 Regerar PDF] [⬆️ Republicar pacote] [✓ Verif]│
└────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Abas

### Aba 1: Fila de execução
Mostra entregas atualmente em processamento ou aguardando:
- Em fila (PENDENTE).
- Em execução (PROCESSANDO).
- Posição na fila.
- Tempo estimado.
- Worker atribuído.

### Aba 2: Falhas
Entregas com `GeracaoStatus.ERRO` recentes:
- Filtro por período (default: 24h).
- Filtro por etapa que falhou.
- Filtro por causa.
- Drill-down para detalhe.

### Aba 3: Reprocessamentos
Histórico de operações de reprocessamento:
- Quem, quando, o quê, motivo.
- Status (sucesso, falha).
- Reprocessamentos repetidos para mesma entrega.

### Aba 4: Logs
Logs estruturados de todo o sistema:
- Filtro por nível, módulo, correlationId.
- Busca textual.
- Export.

### Aba 5: Auditoria
Eventos sensíveis registrados em `orchestrator_auditoria`:
- Mudanças de credenciais.
- Acessos a dados sensíveis.
- Exclusões/anulações.

### Aba 6: Saúde
Healthchecks em tempo real:
- DB, storage, async pool.
- Integrações (GitHub, Jenkins — pós-MVP).
- Espaço em disco.

---

## 5. Ações disponíveis

### Por entrega
- **Recalcular delta** — refaz cálculo de delta sem regerar pacote inteiro.
- **Regerar documento** — refaz só o PDF.
- **Republicar pacote** — copia novamente para destino (caso primeira tentativa falhou).
- **Revalidar checksum** — verifica integridade do pacote existente.
- **Refazer entrega completa** — joga fora e reinicia.
- **Marcar como ignorada** — apenas ADMIN, audit pesado.

### Em massa
- **Reprocessar falhas em lote** — repete N entregas falhas.
- **Limpar fila** — apenas ADMIN, com confirm.

---

## 6. Regras

### 6.1 Justificativa
- Reprocessamento exige **motivo** (texto livre).
- Vai para auditoria.

### 6.2 Permissões
- Aba Fila: LEITOR+.
- Aba Falhas: EDITOR+.
- Aba Logs: ADMIN.
- Aba Auditoria: ADMIN + LEITOR (read-only).
- Aba Saúde: LEITOR+.
- Ações de reprocessamento: EDITOR+.
- Ações destrutivas (limpar fila): ADMIN.

### 6.3 Rate limiting
- Reprocessamento manual: máx 10/min por usuário.
- Evita "tempestade" durante incidente.

### 6.4 Bloqueio se em curso
- Não permite reprocessar entrega em `PROCESSANDO` (esperar/cancelar).

---

## 7. Contratos de API

### Fila

```
GET /api/v1/orchestrator/suporte/fila
```

Response:
```json
{
  "ativas": [
    { "entregaId": "uuid", "codigo": "ENTR-142", "etapa": 7, "etapaNome": "Render PDF", "worker": "orch-gen-1" }
  ],
  "pendentes": [
    { "entregaId": "uuid", "codigo": "ENTR-143", "posicaoFila": 1, "estimativaMs": 30000 }
  ]
}
```

### Falhas

```
GET /api/v1/orchestrator/suporte/falhas?inicio=&fim=&etapa=
```

### Logs

```
GET /api/v1/orchestrator/suporte/logs?nivel=ERROR&correlationId=&q=&inicio=&fim=
```

### Ações

```
POST /api/v1/orchestrator/entregas/{id}/recalcular-delta
POST /api/v1/orchestrator/entregas/{id}/regerar-documento
POST /api/v1/orchestrator/entregas/{id}/republicar
POST /api/v1/orchestrator/entregas/{id}/revalidar-checksum
POST /api/v1/orchestrator/entregas/{id}/refazer
POST /api/v1/orchestrator/entregas/{id}/marcar-ignorada
```

Body com `motivo` obrigatório.

### Saúde

```
GET /api/v1/orchestrator/suporte/saude
```

Resposta similar a `/actuator/health` mas com detalhes operacionais.

---

## 8. DTOs

```java
public record FilaResponse(
    List<EntregaAtiva> ativas,
    List<EntregaPendente> pendentes
) {
    public record EntregaAtiva(UUID entregaId, String codigo, int etapa, String etapaNome, String worker) {}
    public record EntregaPendente(UUID entregaId, String codigo, int posicaoFila, long estimativaMs) {}
}

public record FalhasResponse(List<FalhaEntrega> falhas) {
    public record FalhaEntrega(
        UUID entregaId, String codigo,
        String clienteSigla, String produtoSigla,
        int etapaFalhou, String etapaNome, String causa, String detalheErro,
        OffsetDateTime quando
    ) {}
}

public record AcaoOperacionalRequest(@NotBlank @Size(max = 500) String motivo) {}
```

---

## 9. Performance

### Polling
- Fila/Saúde atualizam a cada 5s.
- WebSocket/SSE em pós-MVP.

### Logs
- Paginação obrigatória.
- Stream se export.

---

## 10. Estados e edge cases

### Sem falhas
- Empty state: "Tudo funcionando ✅".

### Fila vazia
- "Sem entregas em processamento."

### Tempestade de erros
- Banner crítico no topo: "20 falhas nos últimos 5 min".
- Sugestão: investigar antes de reprocessar em massa.

### Disco cheio
- Banner vermelho persistente até resolver.

---

## 11. Acessibilidade

- Tabelas semânticas.
- Status com `aria-label`.
- Auto-refresh anunciado por `aria-live`.

---

## 12. Auditoria

| Ação | Detalhes |
|---|---|
| `ENTREGA_RECALCULADA` | motivo |
| `ENTREGA_DOC_REGERADO` | motivo |
| `ENTREGA_REPUBLICADA` | motivo |
| `ENTREGA_CHECKSUM_REVALIDADO` | resultado |
| `ENTREGA_REFEITA` | motivo |
| `ENTREGA_IGNORADA` | motivo (audit pesado) |
| `LOGS_CONSULTADOS` | filtros, quem (opcional) |

---

## 13. Cross-reference

- [`22-detalhes-entrega.md`](22-detalhes-entrega.md) — Detalhe de cada entrega.
- [`21-geracao-pacote.md`](21-geracao-pacote.md) — Etapas.
- [`23-historico-entregas.md`](23-historico-entregas.md) — Histórico completo.
- [`26-relatorios.md`](26-relatorios.md) — Relatórios consolidados.
- [`34-observabilidade.md`](34-observabilidade.md) — Métricas e tracing.
- [`36-deploy-operacao.md`](36-deploy-operacao.md) — Operação.
- [`99-padroes-tela.md`](99-padroes-tela.md) — Padrões.
