# 17 — Próximas Entregas — Cadastro

## 1. Papel da tela

**Planejar uma entrega futura** antes da geração real do pacote. Form para criação e edição de `ProximaEntrega`.

Acessível em:
- `/orchestrator/proximas-entregas/nova` (criação).
- `/orchestrator/proximas-entregas/:id/editar` (edição).
- `/orchestrator/proximas-entregas/:id` (detalhe — modo read).

---

## 2. Distinção importante

- **Próxima Entrega**: item de planejamento/agenda. Tem ciclo de vida próprio.
- **Entrega**: execução real (pacote gerado e publicado). Tem outro ciclo (ver `22-detalhes-entrega.md`).
- Quando uma Próxima Entrega vira Entrega real, ela passa ao status `CONVERTIDA` e fica imutável.

```text
PLANEJADA → AGENDADA → APROVADA → EM_GERACAO → CONVERTIDA  (caminho feliz)
                                              \
                                               → CANCELADA (a qualquer momento)
                                              /
                  REPLANEJADA (mudança de data)
                  ATRASADA (calculado, não pelo usuário)
```

---

## 3. Layout

```text
┌────────────────────────────────────────────────────────────────────────┐
│ < Voltar       Planejar Próxima Entrega                  [Salvar]      │
├────────────────────────────────────────────────────────────────────────┤
│ ┌─ Vínculos ──────────────────────────────────────────────────────┐  │
│ │ Cliente*                                                         │  │
│ │ [ACME LTDA (ACME) ▼]                                             │  │
│ │                                                                  │  │
│ │ Produto*                            Ambiente*                    │  │
│ │ [NEXUS-LD ▼]                         [PROD ▼]                     │  │
│ │                                                                  │  │
│ │ Release vinculada*  (status: APROVADA ou PUBLICADA)              │  │
│ │ [NEXUS-LD v1.5.0 - APROVADA ▼]                                    │  │
│ │ ℹ️  Versão prevista: 1.5.0                                        │  │
│ └──────────────────────────────────────────────────────────────────┘  │
│                                                                        │
│ ┌─ Agenda ────────────────────────────────────────────────────────┐  │
│ │ Data prevista*               Horário                             │  │
│ │ [01/06/2026]                 [22:00]                             │  │
│ │ Fuso horário: America/Sao_Paulo (do cliente)                     │  │
│ │                                                                  │  │
│ │ Prioridade*                  Responsável                         │  │
│ │ [Média ▼]                    [Select usuário ▼]                  │  │
│ └──────────────────────────────────────────────────────────────────┘  │
│                                                                        │
│ ┌─ Dependências (opcional) ───────────────────────────────────────┐  │
│ │ Outras próximas entregas que precisam acontecer antes:           │  │
│ │ ☑ #1234 - BETA FOLHA 2.0.0 (planejada 30/05)                    │  │
│ │ ☐ #1235 - GAMMA NEXUS-LD 1.5.0 (planejada 02/06)                 │  │
│ └──────────────────────────────────────────────────────────────────┘  │
│                                                                        │
│ ┌─ Observações ───────────────────────────────────────────────────┐  │
│ │ [Janela acordada com cliente: madrugada de sábado.            ]  │  │
│ │ [____________________________________________________________ ] │  │
│ └──────────────────────────────────────────────────────────────────┘  │
│                                                                        │
│              [Cancelar]   [Salvar como rascunho]   [Salvar e agendar]  │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Seções e campos

### 4.1 Vínculos

| Campo | Tipo | Obrigatório | Validação |
|---|---|---|---|
| Cliente | select | ✅ | apenas ATIVO |
| Produto | select | ✅ | apenas produtos contratados pelo cliente |
| Ambiente | select | ✅ | apenas ambientes do produto contratado |
| Release vinculada | select | ✅ | release `APROVADA` ou `PUBLICADA` do produto |

**Filtros dinâmicos:**
- Cliente selecionado → produto reseta + filtra por contratados.
- Produto selecionado → ambiente reseta + filtra por ambientes contratados.
- Cliente + produto → release filtra pelo produto.

### 4.2 Agenda

| Campo | Tipo | Obrigatório | Validação |
|---|---|---|---|
| Data prevista | date | ✅ | futura (warn se passada) |
| Horário | time | ❌ | dentro da janela do cliente |
| Prioridade | select | ✅ | BAIXA, MEDIA, ALTA, CRITICA |
| Responsável | select usuário | ❌ | usuário válido |

**Validação de janela:**
- Se cliente tem janela definida e horário está fora: warning.
- Se comportamento da janela = BLOQUEAR: rejeita.

### 4.3 Dependências (opcional)

- Lista de próximas entregas do mesmo cliente ou correlatas.
- Marcação por checkbox.
- Conversão só permitida se dependências `CONVERTIDA`.

### 4.4 Observações

- Texto livre.
- Útil para anotar janela acordada, validações pendentes, etc.

---

## 5. Botões

### Cancelar
- Volta para `16-proximas-entregas-agenda.md`.
- Discard guard se sujo.

### Salvar como rascunho
- Persiste com status `PLANEJADA`.
- Permite salvar com dados parciais.

### Salvar e agendar
- Persiste com status `AGENDADA` (valida campos obrigatórios).
- Retorna para agenda.

---

## 6. Regras de negócio

### 6.1 Cliente
- Apenas clientes ATIVOS aparecem.
- Cliente PAUSADO/ENCERRADO: bloqueia criação.
- Click "Novo cliente" no select abre modal de cadastro (atalho).

### 6.2 Produto
- Apenas produtos contratados pelo cliente.
- Se cliente não tem produto: erro "Cliente sem produto contratado. [Configurar produtos]".

### 6.3 Release
- Apenas releases do produto com status `APROVADA` ou `PUBLICADA`.
- Sort: por data de criação DESC.
- Display: sigla + versão + status + dataPublicação.

### 6.4 Conversão para Entrega
- Disponível só com release `PUBLICADA`.
- Botão "Converter em entrega" disabled se release ainda `APROVADA`.
- Conversão muda status para `EM_GERACAO` e abre wizard `18`.

### 6.5 Cancelamento
- Pede motivo (texto).
- Status muda para `CANCELADA`.
- Histórico preservado.

### 6.6 Replanejamento
- Edição da `dataPlanejada` em `AGENDADA/APROVADA` muda status para `REPLANEJADA`.
- Pede motivo.
- Histórico mantém todas as datas.

### 6.7 Atrasada
- Cálculo automático: `dataPlanejada < now()` E status pendente.
- Não é status persistido — derivado.

### 6.8 Dependências
- Validar que dependências são do mesmo cliente (ou autorizadas cross-cliente).
- Detectar ciclos.

---

## 7. Contratos de API

### Criar

```
POST /api/v1/orchestrator/proximas-entregas
```

```json
{
  "clienteId": "uuid",
  "produtoId": "uuid",
  "ambiente": "PROD",
  "releaseId": "uuid",
  "dataPlanejada": "2026-06-01T22:00:00-03:00",
  "prioridade": "ALTA",
  "responsavelId": "uuid",
  "dependenciaIds": ["uuid-1234"],
  "observacoes": "Janela acordada com cliente"
}
```

Response 201: `ProximaEntregaResponse`.

### Atualizar

```
PUT /api/v1/orchestrator/proximas-entregas/{id}
```

Status calculado conforme regras (REPLANEJADA se data mudou).

### Replanejar (atalho dedicado)

```
PATCH /api/v1/orchestrator/proximas-entregas/{id}/replanejar
```

```json
{ "novaData": "...", "motivo": "Cliente solicitou adiamento" }
```

### Cancelar

```
POST /api/v1/orchestrator/proximas-entregas/{id}/cancelar
```

```json
{ "motivo": "Cliente decidiu não atualizar" }
```

### Aprovar

```
POST /api/v1/orchestrator/proximas-entregas/{id}/aprovar
```

Status → APROVADA. Necessário antes da conversão.

### Converter em Entrega

```
POST /api/v1/orchestrator/proximas-entregas/{id}/converter
```

Cria entrada de `Entrega` com `proximaEntregaId` e status inicial. Retorna ID da Entrega criada.

### Buscar

```
GET /api/v1/orchestrator/proximas-entregas/{id}
```

Response: `ProximaEntregaResponse` completa.

---

## 8. DTOs

```java
public record ProximaEntregaRequest(
    @NotNull UUID clienteId,
    @NotNull UUID produtoId,
    @NotBlank String ambiente,
    @NotNull UUID releaseId,
    @NotNull @FutureOrPresent OffsetDateTime dataPlanejada,
    @NotNull Prioridade prioridade,
    UUID responsavelId,
    List<UUID> dependenciaIds,
    @Size(max = 1000) String observacoes
) {}

public record ProximaEntregaResponse(
    UUID id,
    UUID clienteId, String clienteSigla, String clienteNome,
    UUID produtoId, String produtoSigla, String produtoCor,
    String ambiente,
    UUID releaseId, String versao, ReleaseStatus releaseStatus,
    OffsetDateTime dataPlanejada,
    Prioridade prioridade,
    UUID responsavelId, String responsavelNome,
    StatusProximaEntrega status,
    boolean atrasada,
    List<ProximaEntregaResumo> dependencias,
    String observacoes,
    List<ReplanejamentoHistorico> historicoReplanejamentos,
    UUID entregaConvertidaId,         // se já convertida
    OffsetDateTime createdAt, OffsetDateTime updatedAt,
    String createdBy, String updatedBy
) {
    public record ProximaEntregaResumo(UUID id, String label, StatusProximaEntrega status) {}
    public record ReplanejamentoHistorico(OffsetDateTime dataAnterior, OffsetDateTime dataNova,
                                          String motivo, String usuario, OffsetDateTime quando) {}
}

public record ReplanejarRequest(
    @NotNull OffsetDateTime novaData,
    @NotBlank @Size(max = 500) String motivo
) {}

public record CancelarProximaEntregaRequest(
    @NotBlank @Size(max = 500) String motivo
) {}
```

---

## 9. Performance

- Endpoint leve.
- Listagens dependentes (clientes, produtos, releases) carregadas sob demanda.
- Cache de releases publicadas com TTL 1 min.

---

## 10. Estados e edge cases

### Cliente sem produto contratado
- Erro inline no campo cliente.
- CTA: "Configurar produtos do cliente".

### Produto sem release publicada
- Select de release vazio.
- Mensagem: "Nenhuma release disponível para esse produto."

### Data no passado
- Warning (mas permite — pode estar regularizando histórico).

### Data fora da janela do cliente
- Warning amarelo.
- Se janela.comportamento = BLOQUEAR: erro.

### Dependência cíclica
- Backend detecta e rejeita.
- Erro: "Dependência cíclica detectada."

### Conversão bloqueada
- Tooltip explicando por quê:
  - "Release ainda APROVADA — aguarde publicação."
  - "Dependência #1234 ainda não foi convertida."
  - "Próxima entrega já CANCELADA."

---

## 11. Acessibilidade

- Form com campos labelados.
- Selects com filtro de busca.
- Confirm modal trapped.

---

## 12. Auditoria

| Ação | Detalhes |
|---|---|
| `PROXIMA_ENTREGA_CRIADA` | snapshot |
| `PROXIMA_ENTREGA_EDITADA` | diff |
| `PROXIMA_ENTREGA_REPLANEJADA` | de → para + motivo |
| `PROXIMA_ENTREGA_APROVADA` | quem |
| `PROXIMA_ENTREGA_CANCELADA` | motivo |
| `PROXIMA_ENTREGA_CONVERTIDA` | entregaId |

---

## 13. Cross-reference

- [`16-proximas-entregas-agenda.md`](16-proximas-entregas-agenda.md) — Agenda.
- [`18-nova-entrega-assistente.md`](18-nova-entrega-assistente.md) — Wizard pós-conversão.
- [`02-clientes-lista.md`](02-clientes-lista.md) — Seleção de cliente.
- [`07-cliente-configuracoes-entrega.md`](07-cliente-configuracoes-entrega.md) — Janelas do cliente.
- [`32-modelo-dados-sugerido.md`](32-modelo-dados-sugerido.md) — Entidade ProximaEntrega.
- [`99-padroes-tela.md`](99-padroes-tela.md) — Padrões.
