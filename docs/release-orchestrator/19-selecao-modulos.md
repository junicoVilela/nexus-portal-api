# 19 — Seleção de Módulos

> **MVP**: a "nova versão" de cada módulo é determinada pelo **artefato uploadado na release** (ver `14-release-orchestrator-detalhe.md`) — sem cálculo de delta automático via Git. O modo Manual com FROM/TO de tag descrito abaixo (e detalhado em `20-range-manual-delta.md`) é **pós-MVP** (depende de integração GitHub).

## 1. Papel da tela

**Passo 4 do assistente de Nova Entrega** (`18`). Define **quais módulos do produto entram no pacote** desta entrega e **qual versão de cada um**.

Acessível como passo do wizard, ou diretamente em `/orchestrator/entregas/:id/modulos`.

---

## 2. Pré-condições

- Cliente, produto, release-alvo já selecionados nos passos anteriores.
- Sistema carregou:
  - Módulos do produto (de `10-produtos-modulos-artefatos.md`).
  - Contratação de módulos pelo cliente (de `06-cliente-produtos-contratados.md`).
  - Última versão enviada de cada módulo a este cliente (do histórico `23-historico-entregas.md`).
  - Versão da release-alvo por módulo (do Release Orchestrator).
  - Artefatos uploadados na release (de `14`).

---

## 3. Layout

```text
┌────────────────────────────────────────────────────────────────────────┐
│ Seleção de Módulos                          Modo: ◉ Automático ○ Manual│
├────────────────────────────────────────────────────────────────────────┤
│ Resumo: 5 módulos do produto, 4 contratados, 3 com mudança detectada   │
├────────────────────────────────────────────────────────────────────────┤
│ ┌──────────────────────────────────────────────────────────────────┐  │
│ │ Env│Módulo         │Tipo │Contr.│Atual │Nova  │Mudança│Obs    │  │
│ ├──────────────────────────────────────────────────────────────────┤  │
│ │ ☑ │dtec-db-ddl    │BANCO│ Sim  │1.4.0 │1.5.0 │+15 SQL│[___]  │  │
│ │ ☑ │dtec-db-dml    │BANCO│ Sim  │1.4.0 │1.5.0 │+8 SQL │[___]  │  │
│ │ ☑ │dtec-funcs     │FUNC │ Sim  │ —    │ —    │auto   │[___]  │  │
│ │ ☑ │dtec-regras    │REGRA│ Sim  │ —    │ —    │auto   │[___]  │  │
│ │ ☑ │dtec-web       │WEB  │ Sim  │1.4.0 │1.5.0 │.war   │[___]  │  │
│ │ ☐ │dtec-batch     │BATCH│ Sim  │1.4.0 │1.4.0 │nenhuma│[___]  │  │
│ │ ▒ │dtec-etl       │KETL │ Não  │ —    │ —    │N/C    │       │  │
│ └──────────────────────────────────────────────────────────────────┘  │
│                                                                        │
│ ☑ Avisos:                                                              │
│ • dtec-batch: nenhuma mudança detectada (versão atual = nova).         │
│ • dtec-etl: módulo não contratado.                                     │
│                                                                        │
│              [Voltar]    [Salvar rascunho]    [Continuar →]            │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Tabela principal

### Colunas

| Coluna | Conteúdo |
|---|---|
| Enviar | Toggle (☑/☐). Desabilitado para não-contratado. |
| Módulo | Código + tooltip com nome completo |
| Tipo | Badge colorido por tipo |
| Contratado | Sim/Não (do cliente) |
| Versão atual no cliente | Última versão entregue. Vazio se nunca recebeu. |
| Nova versão | Tag da release-alvo. Editável em modo Manual. |
| Mudança | Resumo do que muda (+X SQLs, +.war, etc.) |
| Observação | Campo livre por módulo |

### Linha visualmente
- **Branco**: contratado e selecionado.
- **Cinza claro**: contratado mas desmarcado pelo operador.
- **Cinza escuro/listrado**: não contratado (disabled).

---

## 5. Modos de seleção da versão

### 5.1 Automático (default)

| Tipo de módulo | Comportamento |
|---|---|
| `WEB` / `BATCH` | Nova versão = tag da release-alvo. Sem delta — baixa o asset inteiro. |
| `BANCO` / `KETTLE` | FROM = versão atual no cliente, TO = tag da release-alvo. Sistema calcula delta (ver `20`). |
| `FUNCIONALIDADES` / `REGRAS` | Lê configuração do cliente atual (ver `05`), gera scripts no momento da geração do pacote. |

### 5.2 Manual (pós-MVP)

- Usuário sobrescreve FROM e TO por módulo.
- Confirmar leva à tela `20-range-manual-delta.md`.
- Exige justificativa textual.
- Altera status da entrega para exigir aprovação adicional (se config do cliente exige).

---

## 6. Mudança de modo

- Toggle global "Automático ↔ Manual" no topo.
- Mudança preserva seleção de módulos.
- Mudança gera audit log.

---

## 7. Regras

### 7.1 Contratação
- Módulo **não contratado** pelo cliente fica **desabilitado** (não pode marcar "Enviar").
- Tooltip: "Módulo não contratado por este cliente."

### 7.2 Inativos
- Módulo **desativado no produto** (ver `10`) **não aparece** na lista.
- Módulo **inativo no cliente** (ver `06`) aparece em cinza, desmarcado.

### 7.3 Pelo menos um
- Pelo menos um módulo deve estar marcado para prosseguir.
- Erro inline se nenhum selecionado.

### 7.4 Sem mudanças
- Se "versão atual" = "nova versão" E módulo é delta (`BANCO`/`KETTLE`):
  - Sistema marca como "sem mudanças".
  - Operador pode desmarcar (pacote menor).
  - Operador pode forçar (mantém marcado para re-aplicar).

### 7.5 Primeira entrega
- Se "versão atual" estiver vazia (cliente nunca recebeu):
  - Modo Automático: usa primeira tag conhecida do repositório como FROM, ou trata como "full" se configurado.
  - Mensagem clara: "Primeira entrega — incluindo tudo desde a versão inicial."

### 7.6 Mudança Auto → Manual
- Status da entrega exige aprovação adicional (config do cliente).
- Logado em auditoria.

---

## 8. Indicadores e avisos

### Resumo no topo
```
Resumo: 5 módulos do produto, 4 contratados, 3 com mudança detectada
```

### Avisos por módulo
- "Nenhuma mudança detectada" (banco/kettle iguais).
- "Módulo não contratado".
- "Sem artefato uploadado" (warning crítico).
- "Versão regredindo (rollback)".

### Avisos globais
- "Cliente está sem janela ativa agora."
- "Próxima entrega vinculada está pendente de aprovação."

---

## 9. Contratos de API

### Carregar dados para seleção

```
GET /api/v1/orchestrator/entregas/{id}/modulos-disponiveis
```

Response:
```json
{
  "modulos": [
    {
      "moduloProdutoId": "uuid",
      "codigo": "dtec-db-ddl",
      "nome": "Banco DDL",
      "tipo": "BANCO",
      "contratado": true,
      "ativoNoProduto": true,
      "ativoNoCliente": true,
      "versaoAtualCliente": "1.4.0",
      "novaVersao": "1.5.0",
      "modo": "AUTOMATICO",
      "fromTag": "v1.4.0",
      "toTag": "v1.5.0",
      "mudancaResumo": "+15 SQL",
      "podeMarcar": true,
      "avisos": []
    }
  ]
}
```

### Salvar seleção

```
PUT /api/v1/orchestrator/entregas/{id}/modulos
```

```json
{
  "modo": "AUTOMATICO",
  "modulos": [
    {
      "moduloProdutoId": "uuid",
      "enviar": true,
      "modo": "AUTOMATICO",
      "fromTag": "v1.4.0",
      "toTag": "v1.5.0",
      "observacao": ""
    }
  ]
}
```

---

## 10. DTOs

```java
public record ModuloDisponivel(
    UUID moduloProdutoId, String codigo, String nome, TipoModulo tipo,
    boolean contratado, boolean ativoNoProduto, boolean ativoNoCliente,
    String versaoAtualCliente, String novaVersao,
    ModoSelecao modo, String fromTag, String toTag,
    String mudancaResumo,
    boolean podeMarcar,
    List<AvisoModulo> avisos
) {}

public record SalvarSelecaoModulosRequest(
    @NotNull ModoSelecaoGlobal modo,
    @NotEmpty @Valid List<ModuloSelecionado> modulos,
    @Size(max = 500) String justificativaManual    // obrigatória se modo MANUAL
) {
    public record ModuloSelecionado(
        @NotNull UUID moduloProdutoId,
        @NotNull Boolean enviar,
        ModoSelecao modo,
        String fromTag, String toTag,
        @Size(max = 200) String observacao
    ) {}
}
```

---

## 11. Performance

- Cálculo de delta resumo (linha "Mudança") pode ser lento.
- Backend retorna estimativa rápida e detalhe sob demanda (ver `20`).

---

## 12. Estados e edge cases

### Sem nenhum módulo contratado
- Erro: "Cliente não contratou nenhum módulo deste produto."
- Bloqueia passo.

### Todos sem mudança
- Aviso: "Nenhum módulo tem mudança a entregar. Continuar gera pacote vazio."

### Release sem artefatos (MVP)
- Banco/Kettle calculado normal.
- WEB/BATCH bloqueado se sem artefato uploadado.

### Tipo FUNCIONALIDADES/REGRAS sem catálogo
- Aviso: "Cliente sem funcionalidades habilitadas. Pacote gerará scripts vazios."

---

## 13. Acessibilidade

- Tabela semântica.
- Toggle com `role="switch"`.
- Avisos com `aria-live`.
- Mudança de modo anunciada.

---

## 14. Auditoria

| Ação | Detalhes |
|---|---|
| `ENTREGA_MODULOS_SELECIONADOS` | lista + modo |
| `ENTREGA_MODO_MANUAL_USADO` | justificativa |

---

## 15. Cross-reference

- [`18-nova-entrega-assistente.md`](18-nova-entrega-assistente.md) — Wizard mãe.
- [`20-range-manual-delta.md`](20-range-manual-delta.md) — Detalhe do modo manual.
- [`10-produtos-modulos-artefatos.md`](10-produtos-modulos-artefatos.md) — Catálogo.
- [`06-cliente-produtos-contratados.md`](06-cliente-produtos-contratados.md) — Contratação.
- [`21-geracao-pacote.md`](21-geracao-pacote.md) — Próximo passo.
- [`99-padroes-tela.md`](99-padroes-tela.md) — Padrões.
