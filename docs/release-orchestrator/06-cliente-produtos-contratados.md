# 06 — Cliente — Produtos Contratados

## 1. Papel da tela

Define **quais produtos** o cliente possui, **quais módulos de cada produto** estão contratados, e **qual versão atual** está instalada em cada módulo no ambiente do cliente.

Acessível em:
- `/orchestrator/clientes/:id/produtos`.
- Aba "Produtos" em `04-cliente-visao-geral.md`.

---

## 2. Objetivos

| Objetivo | Como atende |
|---|---|
| Catalogar produtos contratados | Tabela de produtos por cliente |
| Controlar módulos por produto | Sub-tabela ou expansão por linha |
| Registrar versão atual | Coluna "versão atual" editável (ou via entrega) |
| Servir de base para delta | Versão atual + versão alvo → calcula delta |
| Controlar status (ativo/inativo) | Toggle por módulo |
| Histórico de versões | Drill-down por módulo mostra histórico |

---

## 3. Conceitos

### ClienteProduto
Vínculo entre Cliente e Produto. Define ambiente e ativação.

### ClienteProdutoModulo
Para cada módulo do produto contratado, registra:
- Se está contratado/ativo.
- Versão atual instalada.
- Última entrega que afetou esse módulo.

```text
Cliente ACME
├─ ClienteProduto(NEXUS-LD, ambiente=PROD)
│  ├─ ClienteProdutoModulo(nexus-web,       versaoAtual=1.4.0, ativo=true)
│  ├─ ClienteProdutoModulo(nexus-batch,     versaoAtual=1.4.0, ativo=true)
│  ├─ ClienteProdutoModulo(nexus-db-ddl,    versaoAtual=1.4.0, ativo=true)
│  ├─ ClienteProdutoModulo(nexus-db-dml,    versaoAtual=1.4.0, ativo=true)
│  └─ ClienteProdutoModulo(nexus-etl,       versaoAtual=null,  ativo=false)
└─ ClienteProduto(FOLHA, ambiente=PROD)
   └─ ...
```

---

## 4. Layout

```text
┌────────────────────────────────────────────────────────────────────────┐
│ Produtos Contratados                              [+ Adicionar produto]│
├────────────────────────────────────────────────────────────────────────┤
│ Filtros: [Apenas ativos]  [Por produto: NEXUS-LD ▼]                    │
├────────────────────────────────────────────────────────────────────────┤
│ ▼ NEXUS-LD       (PROD)   3/5 módulos ativos    Última: v1.4.0 ✅      │
│ ┌──────────────────────────────────────────────────────────────────┐  │
│ │ Módulo          │ Tipo │ Versão atual │ Última entrega │ Ações    │  │
│ ├──────────────────────────────────────────────────────────────────┤  │
│ │ ✅ nexus-web     │ WEB  │ 1.4.0        │ 2026-05-15     │ ⋮        │  │
│ │ ✅ nexus-batch   │ BATCH│ 1.4.0        │ 2026-05-15     │ ⋮        │  │
│ │ ✅ nexus-db-ddl  │ BANCO│ 1.4.0        │ 2026-05-15     │ ⋮        │  │
│ │ ❌ nexus-etl     │ KETTLE│ —           │ —              │ ⋮        │  │
│ │ ❌ nexus-func    │ FUNC │ —            │ —              │ ⋮        │  │
│ └──────────────────────────────────────────────────────────────────┘  │
│                                                                        │
│ ▼ FOLHA        (PROD)   4/4 módulos ativos     Última: v2.0.0 ✅      │
│ ┌──────────────────────────────────────────────────────────────────┐  │
│ │ ...                                                              │  │
│ └──────────────────────────────────────────────────────────────────┘  │
│                                                                        │
│ ▶ CONTAS       (HOM)    1/3 módulos ativos     Última: v0.9.0 ✅      │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 5. Estrutura

### Por produto (expansível)
- Cabeçalho: sigla, ambiente, contagem de módulos ativos, última versão entregue.
- Click no cabeçalho: expande/colapsa a sub-tabela.
- Estado expandido persistido em localStorage.

### Sub-tabela de módulos
Por linha:
- Status (✅ ativo / ❌ inativo).
- Código do módulo.
- Tipo (WEB, BATCH, BANCO, KETTLE, FUNCIONALIDADES, REGRAS).
- Versão atual instalada.
- Data da última entrega que tocou este módulo.
- Menu de ações.

---

## 6. Ações por produto

### Adicionar produto
- Modal: select de produtos disponíveis (filtra os já contratados).
- Define ambiente.
- Pré-marca todos os módulos do produto como ativos (com toggle individual).
- Salvar cria `ClienteProduto` + `ClienteProdutoModulo` para cada selecionado.

### Editar produto contratado
- Mudar ambiente.
- Ativar/inativar produto inteiro (cascateia para módulos).

### Remover produto contratado
- Confirm dupla.
- Move para soft-delete (`ativo=false`).
- Não exclui histórico de entregas.

---

## 7. Ações por módulo

### Toggle ativo/inativo
- Click → confirm: "Inativar módulo X? Não receberá novas entregas."
- Cascade: módulo inativo é excluído da seleção em nova entrega.

### Editar versão atual manualmente
- Modal com input de versão (semver).
- Útil para corrigir desvios (cliente instalou manualmente, ajustar registro).
- Pede motivo.
- Vai para auditoria.

### Ver histórico do módulo
- Modal listando todas as entregas que tocaram esse módulo no cliente.
- Versão antes/depois, data, entrega ID.

---

## 8. Regras de negócio

### 8.1 Adicionar produto
- Produto deve estar ativo no catálogo.
- Não pode duplicar (mesmo produto + ambiente).
- Permitido mesmo produto em ambientes diferentes (PROD + HOM).

### 8.2 Versão atual
- Atualizada automaticamente pela conclusão de entrega bem-sucedida (publicada).
- Edição manual permitida com motivo → auditoria.
- Versão atual = null → módulo nunca foi entregue → delta = tudo.

### 8.3 Módulos inativos
- Não aparecem na seleção de nova entrega.
- Continuam visíveis no histórico.
- Reativação possível.

### 8.4 Vínculo cliente-produto inativo
- Bloqueia novas entregas para esse produto.
- Histórico preservado.
- Reativação possível.

### 8.5 Tipos FUNCIONALIDADES/REGRAS
- Sempre presentes (gerados a partir da matriz de funcionalidades).
- Versão atual = `null` se cliente nunca recebeu, senão = versão da release de origem.

---

## 9. Contratos de API

### Listar produtos contratados

```
GET /api/v1/orchestrator/clientes/{id}/produtos
```

Response:
```json
{
  "produtos": [
    {
      "id": "uuid-cliente-produto",
      "produtoId": "uuid",
      "produtoSigla": "NEXUSLD",
      "produtoNome": "NEXUS-LD",
      "produtoCor": "#2563eb",
      "ambiente": "PROD",
      "ativo": true,
      "modulosAtivos": 3,
      "modulosTotal": 5,
      "ultimaVersao": "1.4.0",
      "ultimaDataEntrega": "2026-05-15",
      "modulos": [
        {
          "id": "uuid-cpm",
          "moduloProdutoId": "uuid-modulo",
          "codigo": "nexus-web",
          "nome": "Nexus Web",
          "tipo": "WEB",
          "versaoAtual": "1.4.0",
          "ativo": true,
          "ultimaEntregaId": "uuid",
          "ultimaEntregaData": "2026-05-15"
        }
      ]
    }
  ]
}
```

### Adicionar produto

```
POST /api/v1/orchestrator/clientes/{id}/produtos
```

```json
{
  "produtoId": "uuid",
  "ambiente": "PROD",
  "modulos": [
    { "moduloProdutoId": "uuid-1", "ativo": true },
    { "moduloProdutoId": "uuid-2", "ativo": false }
  ]
}
```

### Editar produto contratado

```
PUT /api/v1/orchestrator/clientes/{id}/produtos/{clienteProdutoId}
```

```json
{ "ambiente": "PROD", "ativo": true }
```

### Toggle status produto

```
PATCH /api/v1/orchestrator/clientes/{id}/produtos/{clienteProdutoId}/status
```

```json
{ "ativo": false, "motivo": "Cliente solicitou pausa do produto" }
```

### Toggle status módulo

```
PATCH /api/v1/orchestrator/clientes/{id}/produtos/{clienteProdutoId}/modulos/{cpmId}/status
```

```json
{ "ativo": false, "motivo": "Módulo não usado pelo cliente" }
```

### Atualizar versão atual

```
PATCH /api/v1/orchestrator/clientes/{id}/produtos/{clienteProdutoId}/modulos/{cpmId}/versao
```

```json
{
  "versaoAtual": "1.4.2",
  "motivo": "Correção manual após instalação no cliente"
}
```

### Histórico do módulo

```
GET /api/v1/orchestrator/clientes/{id}/produtos/{clienteProdutoId}/modulos/{cpmId}/historico
```

Response: lista de entregas que afetaram esse módulo (versão anterior, nova, data, entregaId).

---

## 10. DTOs

```java
public record ClienteProdutosResponse(
    List<ProdutoContratado> produtos
) {
    public record ProdutoContratado(
        UUID id,
        UUID produtoId, String produtoSigla, String produtoNome, String produtoCor,
        String ambiente, boolean ativo,
        int modulosAtivos, int modulosTotal,
        String ultimaVersao, LocalDate ultimaDataEntrega,
        List<ModuloContratado> modulos
    ) {}

    public record ModuloContratado(
        UUID id, UUID moduloProdutoId, String codigo, String nome,
        TipoModulo tipo, String versaoAtual, boolean ativo,
        UUID ultimaEntregaId, OffsetDateTime ultimaEntregaData
    ) {}
}

public record AdicionarProdutoRequest(
    @NotNull UUID produtoId,
    @NotBlank String ambiente,
    @NotEmpty List<ModuloVinculo> modulos
) {
    public record ModuloVinculo(@NotNull UUID moduloProdutoId, @NotNull Boolean ativo) {}
}

public record AtualizarVersaoModuloRequest(
    @NotBlank @Pattern(regexp = "^\\d+\\.\\d+(\\.\\d+)?(-\\w+)?$") String versaoAtual,
    @NotBlank @Size(max = 500) String motivo
) {}
```

---

## 11. Integração com geração de entrega

### Cálculo de delta
- Entrega usa `ClienteProdutoModulo.versaoAtual` como ponto de partida.
- Versão alvo vem da release selecionada.
- Strategy por tipo de módulo calcula o delta.

### Atualização pós-entrega
- Entrega concluída com sucesso → atualiza `versaoAtual` de cada módulo entregue.
- Falha → não atualiza (mantém estado anterior).
- Cancelamento → não atualiza.

### Sincronização manual
- Se backend perde sincronia (cliente recebeu por fora), operador atualiza via "Editar versão atual".

---

## 12. Performance

### Tamanhos
- Cliente típico: 3-5 produtos × 5-10 módulos = 15-50 módulos.
- 1 GET resolve tudo.

### Cache
- Cache local: 60s.
- Invalida ao mutar.

---

## 13. Estados e edge cases

### Nenhum produto contratado
- Empty state: "Cliente sem produto contratado."
- CTA: "Adicionar primeiro produto".

### Produto inativo
- Linha cinza, badge "INATIVO".
- Módulos colapsados por default.

### Módulo nunca entregue
- `versaoAtual` = `—`.
- Última entrega = `—`.

### Catálogo de módulos mudou
- Cliente tinha módulo X, mas X foi excluído do produto.
- Aparece como "Módulo descontinuado: X" + opção de remover do cliente.

### Versão manual diferente da última entrega
- Indicador "⚠️ Atual diferente do último entregue (1.4.2 vs 1.4.0)".
- Tooltip explica que foi ajuste manual.

---

## 14. Acessibilidade

- Tabelas expansíveis com `aria-expanded`.
- Click no cabeçalho do produto: keyboard equivalente (Enter).
- Status badges com `aria-label`.
- Toggle com confirm modal trapped.

---

## 15. Auditoria

| Ação | Detalhes |
|---|---|
| `CLIENTE_PRODUTO_CONTRATADO` | produtoId, ambiente, modulos selecionados |
| `CLIENTE_PRODUTO_INATIVADO` | motivo |
| `CLIENTE_MODULO_INATIVADO` | motivo |
| `CLIENTE_MODULO_VERSAO_AJUSTADA_MANUALMENTE` | de → para + motivo |

---

## 16. Cross-reference

- [`04-cliente-visao-geral.md`](04-cliente-visao-geral.md) — Tela mãe.
- [`05-cliente-dominios-funcionalidades.md`](05-cliente-dominios-funcionalidades.md) — Outra aba.
- [`10-produtos-modulos-artefatos.md`](10-produtos-modulos-artefatos.md) — Catálogo de módulos do produto.
- [`19-selecao-modulos.md`](19-selecao-modulos.md) — Onde versão atual é usada.
- [`20-range-manual-delta.md`](20-range-manual-delta.md) — Cálculo de delta.
- [`32-modelo-dados-sugerido.md`](32-modelo-dados-sugerido.md) — Entidades.
- [`99-padroes-tela.md`](99-padroes-tela.md) — Padrões.
