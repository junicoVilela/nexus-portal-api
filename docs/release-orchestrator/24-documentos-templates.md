# 24 — Documentos e Templates

## 1. Papel da tela

**Gestão dos modelos (templates) usados para gerar documentos PDF de release e entrega**. Centraliza os templates Markdown que alimentam o renderer (`25-documento-release-md-pdf.md`).

Acessível em `/orchestrator/documentos/templates`.

---

## 2. Objetivos

| Objetivo | Como atende |
|---|---|
| Criar templates | Botão "+ Novo template" |
| Editar modelos por produto | Form com associação a produto |
| Versionar modelos | Histórico de versões + diff |
| Reutilizar entre produtos | Templates globais |
| Garantir consistência visual | Mesmo renderer para todos |

---

## 3. Layout

```text
┌────────────────────────────────────────────────────────────────────────┐
│ Templates de Documentos                          [+ Novo template]     │
├────────────────────────────────────────────────────────────────────────┤
│ Filtros: [Apenas ativos]  Tipo: [Todos ▼]  Produto: [Todos ▼]          │
├────────────────────────────────────────────────────────────────────────┤
│ ┌──────────────────────────────────────────────────────────────────┐  │
│ │ Nome           │ Tipo      │ Produto   │ Versão │ Status │ Ações │  │
│ ├──────────────────────────────────────────────────────────────────┤  │
│ │ NEXUS-LD Padrão │ Base      │ NEXUS-LD   │ v3     │ ATIVO  │ ⋮     │  │
│ │ NEXUS-LD Cliente│ Cliente   │ NEXUS-LD   │ v2     │ ATIVO  │ ⋮     │  │
│ │ FOLHA Padrão   │ Base      │ FOLHA-WEB │ v1     │ ATIVO  │ ⋮     │  │
│ │ Global Padrão  │ Base      │ —         │ v5     │ ATIVO  │ ⋮     │  │
│ │ DDL Banco      │ Banco     │ —         │ v2     │ INATIVO│ ⋮     │  │
│ └──────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Tipos de template

| Tipo | Uso |
|---|---|
| **Base** | Template estrutural completo (cabeçalho + corpo + rodapé) |
| **Cliente** | Filtrado para visibilidade pública (`TODOS` + `SUPORTE`) |
| **Interno** | Inclui itens técnicos (`TECNICO`) |
| **Novidades** | Apenas seção de novidades (parcial reutilizável) |
| **Correções** | Apenas seção de correções (parcial) |
| **Banco de dados** | Instruções DDL/DML |
| **Instruções** | Guia de implantação |

---

## 5. Form de template

### Campos

| Campo | Tipo | Obrigatório |
|---|---|---|
| Nome | text | ✅ |
| Tipo | select | ✅ |
| Produto vinculado | select | ❌ (vazio = global) |
| Descrição | text | ❌ |
| Conteúdo Markdown | textarea / editor | ✅ |
| Variáveis disponíveis | (info) | — |
| Status | toggle | ✅ |

### Editor

- Editor Markdown lado-a-lado com preview.
- Syntax highlight.
- Auto-complete de variáveis.
- Fullscreen mode.

### Variáveis disponíveis

```
{{cliente.nome}}        {{cliente.sigla}}        {{cliente.ambiente}}
{{produto.nome}}        {{produto.sigla}}        {{produto.cor}}
{{release.versao}}      {{release.tipo}}         {{release.dataPublicacao}}
{{release.resumo}}      {{release.observacoes}}
{{entrega.codigo}}      {{entrega.dataGeracao}}  {{entrega.totalModulos}}
{{itens.novidades}}     {{itens.correcoes}}      {{itens.melhorias}}
{{modulos}}             {{scripts.ddl.total}}    {{scripts.dml.total}}
```

Resolvidas com Thymeleaf no momento da geração.

---

## 6. Regras

### 6.1 Produto vinculado
- Vazio = **template global** (disponível para qualquer produto).
- Preenchido = **template específico do produto**.

### 6.2 Templates ativos
- Apenas ativos aparecem no seletor durante geração.
- Templates inativos preservam histórico.

### 6.3 Versionamento
- Cada edição cria nova **versão** (`v1`, `v2`, ...).
- Versões antigas mantidas.
- Geração de PDF usa versão atual (ou específica se override).

### 6.4 Templates padrão por produto
- Produto pode ter template **default** definido em `09-produtos-cadastro.md`.
- Default usado se não especificado na geração.
- Fallback para template global se produto não tem.

### 6.5 Exclusão
- Soft delete (`ativo=false`).
- Templates usados em entregas históricas preservam referência.

---

## 7. Contratos de API

### Listar

```
GET /api/v1/orchestrator/templates
```

### Buscar

```
GET /api/v1/orchestrator/templates/{id}
```

### Criar

```
POST /api/v1/orchestrator/templates
```

```json
{
  "nome": "NEXUS-LD Cliente",
  "tipo": "CLIENTE",
  "produtoId": "uuid",
  "descricao": "Template do cliente final para NEXUS-LD",
  "conteudoMarkdown": "# Release {{produto.sigla}} {{release.versao}}\n\n## Novidades\n{{itens.novidades}}\n\n..."
}
```

### Atualizar

```
PUT /api/v1/orchestrator/templates/{id}
```

Cria nova versão.

### Toggle status

```
PATCH /api/v1/orchestrator/templates/{id}/status
```

### Histórico de versões

```
GET /api/v1/orchestrator/templates/{id}/versoes
```

### Preview com dados de exemplo

```
POST /api/v1/orchestrator/templates/{id}/preview
```

Body opcional com mocks. Retorna PDF.

---

## 8. DTOs

```java
public record TemplateRequest(
    @NotBlank @Size(max = 200) String nome,
    @NotNull TipoTemplate tipo,
    UUID produtoId,
    @Size(max = 500) String descricao,
    @NotBlank String conteudoMarkdown,
    Boolean ativo
) {}

public record TemplateResponse(
    UUID id, String nome, TipoTemplate tipo,
    UUID produtoId, String produtoSigla,
    String descricao, String conteudoMarkdown,
    int versao,
    boolean ativo,
    OffsetDateTime createdAt, OffsetDateTime updatedAt,
    String createdBy, String updatedBy
) {}
```

---

## 9. Estados e edge cases

### Template sem produto e sem global
- Mensagem: "Produto não tem template e não há template global. Crie um."

### Edição com variável inválida
- Linter inline destaca variável desconhecida.

### Sintaxe Markdown inválida
- Warning na preview.

---

## 10. Acessibilidade

- Editor com keyboard shortcuts padrão.
- Preview com `aria-live="polite"`.
- Variáveis com `<datalist>` para autocomplete.

---

## 11. Auditoria

| Ação | Detalhes |
|---|---|
| `TEMPLATE_CRIADO` | snapshot |
| `TEMPLATE_NOVA_VERSAO` | versão anterior, diff |
| `TEMPLATE_STATUS_ALTERADO` | de → para |

---

## 12. Cross-reference

- [`25-documento-release-md-pdf.md`](25-documento-release-md-pdf.md) — Renderer.
- [`09-produtos-cadastro.md`](09-produtos-cadastro.md) — Default por produto.
- [`14-release-orchestrator-detalhe.md`](14-release-orchestrator-detalhe.md) — Aba PDF da release.
- [`25-documento-release-md-pdf.md`](25-documento-release-md-pdf.md) — Stack de PDF (Markdown → PDF).
- [`99-padroes-tela.md`](99-padroes-tela.md) — Padrões.
