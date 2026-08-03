# 08 — Produtos — Listagem

## 1. Papel da tela

Tela de **consulta e administração de produtos** gerenciados pelo Release Orchestrator / Orchestrator. Cada produto tem ciclo próprio de releases e pode ser contratado por múltiplos clientes.

Acessível em `/orchestrator/produtos`. Também acessível pelo Release Orchestrator.

---

## 2. Objetivos

| Objetivo | Como atende |
|---|---|
| Listar produtos | Tabela paginada |
| Identificar status e saúde | Coluna status + indicadores |
| Acessar configuração de módulos | Botão "Módulos" por linha |
| Acessar catálogo funcional | Botão "Catálogo" por linha → `11` |
| Iniciar novo cadastro | Botão "+ Novo produto" |
| Navegar para releases do produto | Click → vai para release flow filtrado |
| Avaliar uso (quantos clientes contratam) | Coluna "Clientes" com contagem |

---

## 3. Layout

```text
┌────────────────────────────────────────────────────────────────────────┐
│ Produtos                                            [+ Novo produto]   │
├────────────────────────────────────────────────────────────────────────┤
│ 🔍 [Busca por nome, sigla...]                                          │
│ Status: [Ativos ▼]  Tipo: [Todos ▼]                  [Limpar filtros]  │
├────────────────────────────────────────────────────────────────────────┤
│ ┌──────────────────────────────────────────────────────────────────┐  │
│ │ Sigla  │ Produto       │ Tipo    │ Módulos │ Clientes │ Releases │  │
│ ├──────────────────────────────────────────────────────────────────┤  │
│ │ 🟦NEXUSLD│ NEXUS-LD       │ Sistema │ 5       │ 12       │ 1.4.0 ✅ │  │
│ │ 🟩FOLHA │ FOLHA-WEB     │ Sistema │ 4       │ 8        │ 2.0.0 ✅ │  │
│ │ 🟧CONTAS│ CONTAS        │ Sistema │ 3       │ 3        │ 0.9.0 🟡 │  │
│ │ 🟪INTPAG│ Integração Pag│ Integr. │ 2       │ 5        │ —        │  │
│ │ ⚫ LIB-X │ Lib Util      │ Library │ 1       │ —        │ —        │  │
│ └──────────────────────────────────────────────────────────────────┘  │
│ Mostrando 1-15 de 23   [< 1 2 >]                                       │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Filtros

### Busca
- Texto livre.
- Pesquisa em: `nome`, `sigla`, `descricao`.
- Debounce 400ms.

### Status
- Multi-select: ATIVO (default), INATIVO.

### Tipo
- Multi-select: SISTEMA, INTEGRACAO, LIBRARY, OUTRO (extensível).

### Avançados
- Sem release publicada nos últimos N dias.
- Sem clientes contratantes.

---

## 5. Tabela

### Colunas

| Coluna | Conteúdo | Sortable |
|---|---|---|
| Cor/Sigla | Badge colorido com sigla | ✅ (sigla) |
| Produto | Nome completo | ✅ |
| Tipo | SISTEMA, INTEGRACAO, etc. | ✅ |
| Módulos | Contagem (clicável) | ✅ |
| Clientes | Contagem de contratantes (clicável) | ✅ |
| Última release | Versão + status | — |
| Status | Badge ATIVO/INATIVO | ✅ |
| Ações | Menu kebab | — |

### Sort default
`nome ASC`.

### Click na linha
- Vai para detalhe do produto (catálogo de módulos, releases, clientes).
- Atalho: vai direto para release-orchestrator filtrado.

### Indicadores
- Badge colorido (`Produto.cor`) com sigla.
- Última release: status visual (✅ publicada, 🟡 em revisão, ❌ falhou).

---

## 6. Ações

### Toolbar
- **+ Novo produto** — ADMIN, EDITOR — vai para `09-produtos-cadastro.md`.
- **⚙️ Exportar CSV** — ADMIN, EDITOR.

### Inline (kebab ⋮)
- **Editar** — vai para `09-produtos-cadastro.md` modo edit.
- **Módulos** — vai para `10-produtos-modulos-artefatos.md`.
- **Catálogo funcional** — vai para `11-produtos-catalogo-funcional.md`.
- **Ver releases** — vai para Release Orchestrator filtrado.
- **Ver clientes** — vai para `02-clientes-lista.md` filtrado por este produto.
- **Inativar / Ativar** — ADMIN — toggle status.

### Permissões

| Ação | ADMIN | EDITOR | LEITOR |
|---|---|---|---|
| Listar | ✅ | ✅ | ✅ |
| Criar | ✅ | ❌ | ❌ |
| Editar | ✅ | ✅ | ❌ |
| Módulos | ✅ | ✅ | ❌ |
| Catálogo funcional | ✅ | ✅ | 👁️ |
| Inativar | ✅ | ❌ | ❌ |

---

## 7. Regras de negócio

### Visibilidade
- ATIVOS por default.
- INATIVOS aparecem se filtro inclui.
- **Não há exclusão física** de produto (apenas INATIVO).

### Indicadores de saúde
- ⚠️ Produto sem módulo cadastrado.
- ⚠️ Produto sem release publicada há > 6 meses.
- ⚠️ Produto contratado mas sem release recente.

### Contagens
- **Módulos**: total ativos.
- **Clientes**: total contratantes ativos.
- **Última release**: versão da última PUBLICADA, status.

---

## 8. Contratos de API

### Listar

```
GET /api/v1/release-orchestrator/produtos
```

(já implementado parcialmente — ver `37-contratos-openapi.md`).

Para a tela ampliada (com contagens agregadas), endpoint específico do orchestrator:

```
GET /api/v1/orchestrator/produtos
```

Response:
```json
{
  "items": [
    {
      "id": "uuid",
      "sigla": "NEXUSLD",
      "nome": "NEXUS-LD",
      "descricao": "Sistema de legislação digital",
      "cor": "#2563eb",
      "tipo": "SISTEMA",
      "responsavelId": "uuid",
      "ativo": true,
      "totalModulos": 5,
      "totalClientes": 12,
      "ultimaRelease": {
        "id": "uuid",
        "versao": "1.4.0",
        "status": "PUBLICADA",
        "dataPublicacao": "2026-05-15"
      },
      "alertas": [
        { "tipo": "SEM_MODULO", "severidade": "WARN", "mensagem": "..." }
      ]
    }
  ],
  "page": 1,
  "size": 15,
  "totalElements": 23
}
```

---

## 9. DTOs

```java
public record ProdutoListaResponse(
    UUID id, String sigla, String nome, String descricao, String cor,
    TipoProduto tipo, UUID responsavelId, boolean ativo,
    int totalModulos, int totalClientes,
    UltimaReleaseInfo ultimaRelease,
    List<AlertaProduto> alertas
) {
    public record UltimaReleaseInfo(UUID id, String versao, ReleaseStatus status, LocalDate dataPublicacao) {}
    public record AlertaProduto(TipoAlertaProduto tipo, Severidade severidade, String mensagem) {}
}
```

---

## 10. Performance

### Volumes esperados
- 10-50 produtos.

### Agregações
- Cache de contagens (TTL 5 min).
- Materialized view para `total_clientes` e `ultima_release`.

---

## 11. Estados e edge cases

### Sem produtos
- Empty state: "Nenhum produto cadastrado."
- CTA: "Cadastrar primeiro produto".

### Produto sem módulos
- Linha com alerta visual.
- Tooltip: "Produto sem módulo cadastrado. Não pode receber entregas."

### Produto sem clientes
- Coluna `Clientes` = `—`.

---

## 12. Acessibilidade

- Tabela semântica.
- Sort buttons com `aria-sort`.
- Click row → keyboard equivalente (Enter).

---

## 13. Auditoria

| Ação | Detalhes |
|---|---|
| `PRODUTO_STATUS_ALTERADO` | de → para |

---

## 14. Cross-reference

- [`09-produtos-cadastro.md`](09-produtos-cadastro.md) — Cadastro.
- [`10-produtos-modulos-artefatos.md`](10-produtos-modulos-artefatos.md) — Catálogo de módulos.
- [`37-contratos-openapi.md`](37-contratos-openapi.md) — Contratos de API (produtos).
- [`99-padroes-tela.md`](99-padroes-tela.md) — Padrões.
