# 25 — Documento de Release (Markdown → PDF)

## 1. Papel da tela

Pré-visualizar e gerar o **documento PDF da entrega ao cliente** — diferente do PDF da release técnica (ver `14-release-orchestrator-detalhe.md`).

Acessível em `/orchestrator/entregas/:id/documento`.

---

## 2. Distinção dos dois PDFs

| PDF | Origem | Conteúdo | Quem vê |
|---|---|---|---|
| **PDF da Release** (`14`) | Release Orchestrator | Todos os itens da versão (filtráveis por visibilidade) | Equipe interna / suporte |
| **PDF da Entrega** (este) | Orchestrator | Conteúdo da release + escopo do cliente (módulos enviados, scripts, instruções) | Cliente final |

> Ambos usam o **mesmo renderer** (Markdown → PDF) para manter consistência visual. Stack detalhada nas seções abaixo.

---

## 3. Objetivos

| Objetivo | Como atende |
|---|---|
| Selecionar template | Dropdown no topo |
| Editar markdown final | Editor lado-a-lado |
| Pré-visualizar | Painel direito atualiza em tempo real |
| Gerar PDF final | Botão "Gerar PDF" |
| Versionar | Histórico de geração |

---

## 4. Layout

```text
┌────────────────────────────────────────────────────────────────────────┐
│ Documento da Entrega — ENTR-2026-0142                                  │
├────────────────────────────────────────────────────────────────────────┤
│ Template: [NEXUS-LD Cliente v2 ▼]  [Restaurar default]  [Gerar PDF]    │
├────────────────────────────────────────────────────────────────────────┤
│ ┌─ Markdown ───────────────────┐ ┌─ Preview PDF ───────────────────┐ │
│ │ # Release NEXUS-LD 1.5.0       │ │                                  │ │
│ │                               │ │  Release NEXUS-LD 1.5.0           │ │
│ │ ## Cliente: ACME LTDA         │ │  ════════════════════            │ │
│ │ ## Ambiente: PROD             │ │                                  │ │
│ │ ## Data: 31/05/2026           │ │  Cliente: ACME LTDA              │ │
│ │                               │ │  Ambiente: PROD                  │ │
│ │ ## Novidades                  │ │  Data: 31/05/2026                │ │
│ │ {{itens.novidades}}           │ │                                  │ │
│ │                               │ │  Novidades                       │ │
│ │ ## Correções                  │ │  ─────────                       │ │
│ │ {{itens.correcoes}}           │ │  • Nova função de exportação CSV │ │
│ │                               │ │  • Suporte a SSO                 │ │
│ │ ## Módulos enviados           │ │                                  │ │
│ │ {{modulos}}                   │ │  Correções                       │ │
│ │                               │ │  ─────────                       │ │
│ │ ## Scripts                    │ │  • Cálculo de juros corrigido   │ │
│ │ DDL: {{scripts.ddl.total}}    │ │                                  │ │
│ │ DML: {{scripts.dml.total}}    │ │  ...                             │ │
│ └───────────────────────────────┘ └──────────────────────────────────┘ │
│                                                                        │
│ Versões geradas: v1 31/05 14:32 [⬇️]  v2 31/05 14:40 [⬇️]  v3 atual    │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 5. Conteúdo do documento (montado automaticamente)

### Cabeçalho
- Nome do cliente + sigla.
- Produto + versão.
- Data prevista de implantação.
- Ambiente alvo.

### Resumo
- Resumo da release (do Release Orchestrator).

### Itens
- Novidades, melhorias e correções (itens com visibilidade `TODOS`).
- Cada item: título + descrição.

### Módulos enviados
Para cada módulo:
- Nome + tipo.
- FROM_TAG → TO_TAG (ou versões equivalentes no MVP).
- Modo (auto/manual).
- Tamanho do artefato.

### Scripts inclusos
- Quantidade de DDL/DML por dialeto.
- Lista resumida de scripts (ou link para detalhe).

### Instruções de implantação
- Ordem sugerida de execução (do `manifest.json`).
- Pré-requisitos (do template).

### Observações
- Da release.
- Da entrega (campo livre).

### Rodapé
- ID da entrega.
- Hash do pacote.
- Data/hora de geração.
- Usuário gerador.

---

## 6. Ações

### Editar Markdown
- Alterações ficam vinculadas só a esta entrega.
- Não altera o template original.
- Markdown editado fica salvo no DB.

### Voltar para template padrão
- Descarta edição.
- Confirm: "Descartar customizações desta entrega?".

### Gerar PDF
- Renderiza markdown atual.
- Substitui PDF no pacote (se ainda não publicado).
- Cria nova versão (`v1`, `v2`, ...).
- Mantém versão anterior no histórico.

### Regerar PDF
- Mesmo que gerar, mas explícito após mudança.

### Baixar PDF
- Download da versão atual ou histórica.

---

## 7. Regras

### 7.1 Estado da geração
- Documento sempre reflete o estado da entrega + release **no momento da geração**.
- Geração captura snapshot dos dados.

### 7.2 Versionamento
- Regeração mantém histórico de versões do PDF (auditoria).
- Versões antigas continuam baixáveis.
- Pacote da entrega referencia versão atual.

### 7.3 Override de markdown
- Markdown editado pelo operador é **override** desta entrega.
- Não afeta o template original.
- Outras entregas continuam usando template puro.

### 7.4 Renderer compartilhado
- Mesmo renderer Markdown → PDF descrito neste documento.
- Mudança de estilo afeta os dois PDFs.

### 7.5 Entrega CONCLUIDA
- PDF pode continuar sendo regerado para fins de comunicação.
- Pacote físico no destino não muda — apenas snapshot novo.

### 7.6 Variáveis sem dado
- `{{...}}` sem valor é substituído por "—" ou texto vazio.
- Linter avisa antes da geração.

---

## 8. Contratos de API

### Carregar markdown atual + dados

```
GET /api/v1/orchestrator/entregas/{id}/documento
```

```json
{
  "templateAtualId": "uuid",
  "templateNome": "NEXUS-LD Cliente",
  "templateVersao": 2,
  "markdownAtual": "# Release...",  // já resolvido OU bruto
  "modoEdicao": "TEMPLATE_PURO" | "OVERRIDE_ENTREGA",
  "versoesPdf": [
    { "versao": 1, "geradoEm": "...", "sha256": "...", "tamanhoBytes": 340000 },
    { "versao": 2, "geradoEm": "...", "sha256": "...", "tamanhoBytes": 342000 }
  ]
}
```

### Atualizar markdown override

```
PUT /api/v1/orchestrator/entregas/{id}/documento/markdown
```

```json
{ "markdown": "# ..." }
```

### Trocar template

```
PUT /api/v1/orchestrator/entregas/{id}/documento/template
```

```json
{ "templateId": "uuid" }
```

### Restaurar default

```
POST /api/v1/orchestrator/entregas/{id}/documento/restaurar
```

### Preview (HTML para iframe)

```
GET /api/v1/orchestrator/entregas/{id}/documento/preview?versao=atual
```

Retorna HTML renderizado ou PDF inline.

### Gerar PDF

```
POST /api/v1/orchestrator/entregas/{id}/documento/gerar
```

Response: nova versão criada.

### Download versão específica

```
GET /api/v1/orchestrator/entregas/{id}/documento/{versao}/download
```

---

## 9. DTOs

```java
public record DocumentoEntregaResponse(
    UUID entregaId,
    UUID templateAtualId, String templateNome, int templateVersao,
    String markdownAtual,
    ModoEdicaoMarkdown modoEdicao,
    List<VersaoPdf> versoesPdf
) {
    public record VersaoPdf(int versao, OffsetDateTime geradoEm, String sha256, long tamanhoBytes) {}
}
```

---

## 10. Performance

### Preview
- Renderiza em <2s para documento típico.
- Cache do markdown resolvido.

### Geração de PDF
- Async se documento grande (>5 páginas).
- Status visível.

---

## 11. Estados e edge cases

### Cliente sem dados completos
- Variáveis vazias substituídas por "—".

### Template excluído
- Fallback para global.
- Aviso no topo.

### Markdown com sintaxe inválida
- Preview mostra HTML simplificado + erro.

### Geração falha
- Erro detalhado (Thymeleaf trace, etc.).
- Permite tentar de novo.

---

## 12. Acessibilidade

- Editor com keyboard shortcuts.
- Preview com `aria-live`.
- Versões com `<ol>` semântico.

---

## 13. Auditoria

| Ação | Detalhes |
|---|---|
| `DOCUMENTO_TEMPLATE_TROCADO` | de → para |
| `DOCUMENTO_MARKDOWN_EDITADO` | diff |
| `DOCUMENTO_PDF_GERADO` | versão, sha256 |
| `DOCUMENTO_RESTAURADO` | versão restaurada |

---

## 14. Cross-reference

- [`24-documentos-templates.md`](24-documentos-templates.md) — Templates.
- [`14-release-orchestrator-detalhe.md`](14-release-orchestrator-detalhe.md) — PDF da release.
- [`21-geracao-pacote.md`](21-geracao-pacote.md) — Onde PDF entra no pacote.
- [`22-detalhes-entrega.md`](22-detalhes-entrega.md) — Origem.
- Stack de renderização Markdown → PDF — este documento.
- [`99-padroes-tela.md`](99-padroes-tela.md) — Padrões.
