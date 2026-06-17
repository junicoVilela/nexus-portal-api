# 18 — Nova Entrega — Assistente

## 1. Papel da tela

**Wizard multi-step** para criar uma entrega real ao cliente. Guia o operador desde a seleção de cliente até a confirmação da geração do pacote.

Acessível em:
- `/orchestrator/entregas/nova` (manual).
- Atalho da agenda (`16`) ou próxima entrega (`17`) — pré-preenchido.
- Atalho do detalhe do cliente (`04`) — pré-preenchido com cliente.

---

## 2. Objetivos

| Objetivo | Como atende |
|---|---|
| Iniciar nova entrega | Wizard simplifica fluxo |
| Validar pré-requisitos | Cada step valida antes de avançar |
| Reduzir erros | Pré-visualização antes de confirmar |
| Auditar decisões | Cada step gera entrada no histórico |
| Permitir abandono | "Salvar como rascunho" em qualquer step |

---

## 3. Layout (wizard com sidebar de passos)

```text
┌────────────────────────────────────────────────────────────────────────┐
│ < Voltar       Nova Entrega                                            │
├──────────┬─────────────────────────────────────────────────────────────┤
│ Passos:  │                                                             │
│          │  Passo 3 de 5: Versões                                      │
│ ① Cliente│  ─────────────────────────────────────                      │
│ ② Produto│                                                             │
│ ③ Versões│  Release vinculada                                          │
│ ④ Módulos│  ◉ DTEC-LD v1.5.0 (APROVADA, 15/05/2026)                   │
│ ⑤ Revisão│  ○ DTEC-LD v1.4.5 (PUBLICADA, 02/04/2026)                   │
│          │                                                             │
│          │  Range automático: v1.4.0 → v1.5.0                          │
│          │  ☑ Pré-vincular próxima entrega #1234                       │
│          │                                                             │
│          │  Observações                                                │
│          │  [Janela ajustada com cliente para sábado 22h.    ]         │
│          │                                                             │
│          │              [Voltar]    [Salvar rascunho]    [Avançar →]   │
└──────────┴─────────────────────────────────────────────────────────────┘
```

---

## 4. Etapas do wizard

### Step 1: Cliente
- Select de cliente (apenas ATIVOS).
- Preview rápido: produtos contratados, última entrega.
- Pré-preenchido se vem de outras telas.

### Step 2: Produto + Ambiente
- Select de produto **filtrado pelos contratados pelo cliente**.
- Ambiente filtrado conforme contratação.
- Preview: módulos contratados, última versão entregue.

### Step 3: Versões
- Lista releases disponíveis (APROVADA + PUBLICADA) do produto.
- Vinculação opcional com Próxima Entrega (se houver pendente).
- Mostra range automático (versão atual cliente → versão alvo).
- Observações livres.

### Step 4: Módulos
- Vai para tela `19-selecao-modulos.md`.
- Permite ajuste fino de módulos a enviar + modo (Automático/Manual).

### Step 5: Revisão e geração
- Resumo completo da entrega.
- Pré-visualização do pacote (módulos, deltas estimados, tamanho).
- Botão "Gerar pacote" → confirma e dispara geração assíncrona.

---

## 5. Sidebar de passos

- Indicador visual do progresso (números 1-5).
- Passos completos ✅, atual ◉, pendentes ⚪.
- Click em passo anterior → volta (mantém dados).
- Click em passo futuro → bloqueado (não pulável).

---

## 6. Campos por passo

### Passo 1: Cliente

| Campo | Tipo | Obrigatório |
|---|---|---|
| Cliente | select | ✅ |

Display de info:
- Sigla, nome, status.
- Total de produtos contratados.
- Última entrega (data + status).

### Passo 2: Produto + Ambiente

| Campo | Tipo | Obrigatório |
|---|---|---|
| Produto | select | ✅ |
| Ambiente | select | ✅ |

Display:
- Módulos contratados.
- Última versão entregue (geral).

### Passo 3: Versões

| Campo | Tipo | Obrigatório |
|---|---|---|
| Release | radio list | ✅ |
| Próxima entrega vinculada | checkbox/select | ❌ |
| Observações | textarea | ❌ |

### Passo 4: Módulos
- Ver tela `19`.

### Passo 5: Revisão
- Confirmação final + botão de gerar.

---

## 7. Regras de negócio

### 7.1 Pré-condições para iniciar
- Cliente ATIVO.
- Produto contratado pelo cliente, ativo.
- Pelo menos 1 release `APROVADA` ou `PUBLICADA` do produto.
- Cliente com configuração de entrega válida (warning se incompleta).

### 7.2 Pré-preenchimento
- Vindo da agenda: cliente, produto, release, observações.
- Vindo do cliente: cliente.
- Vindo da próxima entrega: tudo.

### 7.3 Salvar rascunho
- A qualquer momento.
- Cria `Entrega` em status `RASCUNHO`.
- Permite continuar depois.

### 7.4 Avançar passo
- Validações específicas por passo.
- Não permite avançar se inválido.

### 7.5 Voltar passo
- Mantém dados do passo atual em memória.

### 7.6 Geração final
- Verifica novamente todos os pré-requisitos.
- Cria entrega em status `PENDENTE`.
- Dispara job assíncrono (ver `21-geracao-pacote.md`).
- Redireciona para `22-detalhes-entrega.md` com polling de status.

---

## 8. Contratos de API

### Iniciar wizard (criar rascunho)

```
POST /api/v1/orchestrator/entregas/rascunho
```

```json
{
  "proximaEntregaId": "uuid",   // opcional, se origem é agenda
  "clienteId": "uuid"           // opcional, pré-preenchimento
}
```

Response: ID da entrega em rascunho.

### Salvar passo do wizard

```
PATCH /api/v1/orchestrator/entregas/{id}/wizard
```

```json
{
  "passo": 3,
  "dados": {
    "releaseId": "uuid",
    "proximaEntregaIdVinculada": "uuid",
    "observacoes": "..."
  }
}
```

### Finalizar (disparar geração)

```
POST /api/v1/orchestrator/entregas/{id}/gerar
```

Body opcional com configurações de geração.

Response: entrega com status `PENDENTE`, job id.

### Buscar rascunho

```
GET /api/v1/orchestrator/entregas/{id}
```

Retorna dados acumulados do wizard.

### Cancelar rascunho

```
DELETE /api/v1/orchestrator/entregas/{id}
```

Apenas se status = `RASCUNHO`.

---

## 9. DTOs

```java
public record EntregaWizardSalvarRequest(
    @Min(1) @Max(5) int passo,
    @NotNull JsonNode dados
) {}

public record EntregaResponse(
    UUID id,
    StatusEntrega status,
    UUID clienteId, String clienteSigla,
    UUID produtoId, String produtoSigla,
    String ambiente,
    UUID releaseId, String versao,
    UUID proximaEntregaIdVinculada,
    List<ModuloEntrega> modulos,
    GeracaoStatus geracaoStatus,
    String observacoes,
    int passoAtualWizard,
    OffsetDateTime createdAt,
    String createdBy
) {}
```

---

## 10. Estados e edge cases

### Sem produtos contratados pelo cliente
- Bloqueia step 2.
- CTA: "Configurar produtos do cliente".

### Sem release disponível
- Bloqueia step 3.
- CTA: "Ir ao Release Orchestrator criar release".

### Cliente com config de entrega incompleta
- Aviso amarelo no step 1.
- "Finalize a configuração antes da geração [Configurar]".

### Wizard interrompido (browser fechado)
- Próximo acesso: lista rascunhos pendentes.
- "Continuar entrega rascunho #1234?"

### Entrega já em geração
- Não pode mexer.
- Vai direto para detalhe.

---

## 11. Performance

- Cada passo carrega dados sob demanda.
- Cache local entre passos.
- Pré-visualização (step 5) pode demorar — mostra loading.

---

## 12. Acessibilidade

- Steps navegáveis por teclado.
- Cada passo é uma região anunciada (`role="region"` + `aria-label`).
- Botões "Avançar/Voltar" mantêm foco.

---

## 13. Auditoria

| Ação | Detalhes |
|---|---|
| `ENTREGA_RASCUNHO_CRIADA` | wizard origem |
| `ENTREGA_WIZARD_PASSO_SALVO` | passo + dados |
| `ENTREGA_GERACAO_DISPARADA` | configurações |
| `ENTREGA_RASCUNHO_DESCARTADA` | motivo |

---

## 14. Cross-reference

- [`16-proximas-entregas-agenda.md`](16-proximas-entregas-agenda.md) — Origem comum.
- [`17-proximas-entregas-cadastro.md`](17-proximas-entregas-cadastro.md) — Próxima entrega.
- [`19-selecao-modulos.md`](19-selecao-modulos.md) — Step 4.
- [`20-range-manual-delta.md`](20-range-manual-delta.md) — Customização avançada.
- [`21-geracao-pacote.md`](21-geracao-pacote.md) — Execução pós-wizard.
- [`22-detalhes-entrega.md`](22-detalhes-entrega.md) — Tela seguinte.
- [`99-padroes-tela.md`](99-padroes-tela.md) — Padrões.
