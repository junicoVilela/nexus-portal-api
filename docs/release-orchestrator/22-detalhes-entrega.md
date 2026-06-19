# 22 — Detalhes da Entrega

## 1. Papel da tela

**Consulta completa de uma entrega gerada**. Visualização imutável do que foi entregue, com ações para reentrega, regeneração e auditoria.

Acessível em `/orchestrator/entregas/:id`.

---

## 2. Objetivos

| Objetivo | Como atende |
|---|---|
| Ver dados gerais | Cabeçalho + aba Visão Geral |
| Ver módulos enviados | Aba Módulos |
| Baixar pacote | Botão "Download pacote" |
| Ver arquivos individuais | Aba Arquivos |
| Ver logs técnicos | Aba Logs |
| Reentregar | Ação principal |
| Regerar documento | Atalho para PDF |
| Auditoria | Aba Auditoria |

---

## 3. Layout

```text
┌────────────────────────────────────────────────────────────────────────┐
│ < Entregas    #ENTR-2026-0142   ACME LTDA • DTEC-LD v1.5.0             │
│ Status: ✅ CONCLUÍDA • Publicada em 31/05/2026 14:35                    │
├────────────────────────────────────────────────────────────────────────┤
│ [⬇️ Download pacote] [🔄 Reentregar] [📄 Regerar PDF] [⋮ Mais]         │
├────────────────────────────────────────────────────────────────────────┤
│ [Visão Geral] [Módulos] [Arquivos] [Logs] [Auditoria]                  │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│  Cliente:        ACME LTDA (ACME)                                      │
│  Produto:        DTEC-LD (DTECLD)                                      │
│  Ambiente:       PROD                                                  │
│  Release:        v1.5.0 (MINOR, publicada 15/05/2026)                  │
│  Tamanho pacote: 53.2 MB                                               │
│  Checksums:      verificados ✅                                         │
│                                                                        │
│  Gerada por:     João Silva (joao.silva)                               │
│  Gerada em:      31/05/2026 14:32 (duração 3min 12s)                   │
│  Publicada em:   /var/lib/softon/entregas/cliente-acme/                │
│                                                                        │
│  Próxima entrega vinculada: #1234 (CONVERTIDA)                         │
│                                                                        │
│ ┌─ Resumo de módulos ──────────────────────────────────────────────┐ │
│ │ ✅ dtec-web         WEB    1.4.0 → 1.5.0                          │ │
│ │ ✅ dtec-batch       BATCH  1.4.0 → 1.4.0 (sem mudança)            │ │
│ │ ✅ dtec-db-ddl      BANCO  1.4.0 → 1.5.0 (+15 SQL)                │ │
│ │ ✅ dtec-db-dml      BANCO  1.4.0 → 1.5.0 (+8 SQL)                 │ │
│ │ ✅ dtec-funcs       FUNC   auto (23 funcionalidades)              │ │
│ │ ✅ dtec-regras      REGRAS auto                                   │ │
│ └──────────────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Cabeçalho

### Identidade
- ID curto + UUID (em tooltip).
- Cliente: sigla + nome.
- Produto: sigla + versão.
- Status com ícone visual.
- Data de publicação.

### Ações principais (botões grandes)
- **⬇️ Download pacote** (sempre disponível se CONCLUIDO).
- **🔄 Reentregar** (gera nova entrega reutilizando pacote).
- **📄 Regerar PDF** (atalho para PDF).
- **⋮ Mais**: copiar ID, exportar manifest, ver release relacionada, marcar como problemática.

### Permissões

| Ação | ADMIN | EDITOR | LEITOR |
|---|---|---|---|
| Ver detalhes | ✅ | ✅ | ✅ |
| Download pacote | ✅ | ✅ | ✅ |
| Reentregar | ✅ | ✅ | ❌ |
| Regerar PDF | ✅ | ✅ | ❌ |
| Marcar problemática | ✅ | ❌ | ❌ |

---

## 5. Abas

### Aba 1: Visão Geral (default)
- Cabeçalho + resumo de módulos.

### Aba 2: Módulos
- Lista detalhada de cada módulo entregue.
- Por módulo: tipo, versão before/after, modo, artefatos.
- Click em módulo → detalhe técnico (arquivos, sha256).

### Aba 3: Arquivos
- Árvore do conteúdo do pacote.
- Por arquivo: nome, tamanho, sha256.
- Download individual.

### Aba 4: Logs
- Todos os logs estruturados da geração.
- Filtro por nível e etapa.
- Export para análise.

### Aba 5: Auditoria
- Eventos relacionados a esta entrega na `orchestrator_auditoria`.
- Quem fez o quê, quando.

---

## 6. Painel "Resumo de módulos"

Por módulo:
- Ícone de status (✅ ok, ⚠️ aviso).
- Código + nome.
- Tipo (badge).
- Versão before → after.
- Resumo de mudança ("+15 SQL", "asset .war").

Click → vai para aba Módulos com expansão na linha.

---

## 7. Aba Arquivos (estrutura)

```text
📦 ACME_DTECLD_1.5.0_20260531-1432.zip  (53.2 MB)
├── 📁 web/
│   └── 📄 dtec-web-1.5.0.war           47.2 MB  sha256:abc...
├── 📁 batch/
│   └── 📄 dtec-batch-1.4.0.jar         12.0 MB  sha256:def...
├── 📁 banco/
│   └── 📁 oracle/
│       ├── 📄 DDL.sql                   45 KB   sha256:ghi...
│       └── 📄 DML.sql                   23 KB   sha256:jkl...
├── 📁 funcionalidades/
│   └── 📄 funcionalidades.sql           12 KB   sha256:mno...
├── 📁 regras/
│   └── 📄 regras.sql                    8 KB    sha256:pqr...
├── 📁 docs/
│   └── 📄 release-notes.pdf             340 KB  sha256:stu...
├── 📄 manifest.json                     2 KB    sha256:vwx...
└── 📄 SHA256SUMS.txt                    1 KB    sha256:yza...
```

Cada arquivo:
- Click → download individual.
- Ver detalhe (data, modificação, etc.).

---

## 8. Aba Logs

```text
┌────────────────────────────────────────────────────────────────────────┐
│ Filtros: Nível: [Todos ▼]  Etapa: [Todas ▼]  [Exportar JSON]           │
├────────────────────────────────────────────────────────────────────────┤
│ 14:32:01 INFO  [validar] Validação OK                                  │
│ 14:32:02 INFO  [reservar-id] Entrega #ENTR-2026-0142 reservada         │
│ 14:32:02 INFO  [coletar-artefatos] Coletando 6 artefatos da release    │
│ 14:32:14 INFO  [coletar-artefatos] Concluído: 6 arquivos, 59.2 MB      │
│ 14:32:14 INFO  [coletar-sql] Concluído: DDL 12, DML 8                  │
│ 14:32:15 INFO  [gerar-funcs] Gerando scripts de 23 funcionalidades     │
│ 14:32:16 INFO  [gerar-funcs] Concluído                                 │
│ 14:32:18 INFO  [render-pdf] Renderizando release-notes.pdf             │
│ 14:32:24 INFO  [render-pdf] PDF gerado: 340KB                          │
│ 14:32:25 INFO  [manifest] Manifest gerado                              │
│ 14:32:26 INFO  [checksums] SHA256 calculados                           │
│ 14:32:27 INFO  [publicar] Publicando em pasta local                    │
│ 14:32:28 INFO  [publicar] Pacote publicado                             │
│ 14:32:28 INFO  [atualizar-historico] Versão atual de 6 módulos atual.. │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 9. Contratos de API

### Buscar entrega

```
GET /api/v1/orchestrator/entregas/{id}
```

Response: `EntregaDetalheResponse`.

### Download pacote

```
GET /api/v1/orchestrator/entregas/{id}/pacote/download
```

Stream binary ZIP. ETag = sha256 do pacote.

### Download manifest

```
GET /api/v1/orchestrator/entregas/{id}/manifest
```

JSON.

### Download arquivo individual

```
GET /api/v1/orchestrator/entregas/{id}/arquivos/{caminho:.+}
```

Stream binary.

### Logs

```
GET /api/v1/orchestrator/entregas/{id}/logs?nivel=INFO&etapa=...
```

### Reentregar

```
POST /api/v1/orchestrator/entregas/{id}/reentregar
```

```json
{
  "regerarPacote": false,
  "novaDataPlanejada": "2026-06-15T22:00:00-03:00",
  "motivo": "Cliente reportou falha na primeira aplicação"
}
```

Cria nova `Entrega` com `entregaOriginalId` apontando para esta.
- `regerarPacote=false`: reutiliza pacote existente.
- `regerarPacote=true`: gera novo pacote (mesma seleção).

### Regerar PDF

```
POST /api/v1/orchestrator/entregas/{id}/pdf/regerar
```

```json
{ "visibilidade": "CLIENTE" }
```

### Marcar como problemática

```
POST /api/v1/orchestrator/entregas/{id}/problematica
```

```json
{ "motivo": "Script DDL_010 falhou no cliente" }
```

---

## 10. DTOs

```java
public record EntregaDetalheResponse(
    UUID id, String codigo,
    StatusEntrega status,
    UUID clienteId, String clienteSigla, String clienteNome,
    UUID produtoId, String produtoSigla, String produtoNome,
    String ambiente,
    UUID releaseId, String versao, TipoRelease tipoRelease,
    LocalDate releaseDataPublicacao,
    long tamanhoBytes,
    boolean checksumsValidos,
    String geradoPor, OffsetDateTime geradoEm,
    long duracaoGeracaoMs,
    OffsetDateTime publicadoEm, String pathPublicacao,
    UUID proximaEntregaIdVinculada,
    UUID entregaOriginalId,            // se reentrega
    List<UUID> reentregasIds,         // se foi reentregue
    List<ModuloEntregue> modulos,
    boolean problematica, String motivoProblematica
) {
    public record ModuloEntregue(
        UUID moduloProdutoId, String codigo, String nome, TipoModulo tipo,
        String versaoBefore, String versaoAfter,
        ModoSelecao modo,
        List<ArquivoEntregue> artefatos,
        String mudancaResumo
    ) {}

    public record ArquivoEntregue(String caminho, long tamanhoBytes, String sha256) {}
}
```

---

## 11. Regras

### 11.1 Reentrega
- Reutiliza pacote existente quando possível.
- Se cliente mudou de versão entre primeira entrega e reentrega: regerar.
- Gera nova `Entrega` com `entregaOriginalId`.
- Histórico mantém os dois.

### 11.2 Imutabilidade
- Entrega CONCLUIDA não pode ser editada.
- Pacote pode ser baixado ad infinitum.

### 11.3 Marcar problemática
- Flag visível na listagem.
- Aparece com badge vermelho.
- Útil para sinalizar entrega que precisa investigação.

### 11.4 Histórico de PDF
- Regeração do PDF gera novo snapshot.
- Anteriores permanecem disponíveis (em `release_pdf_snapshot`).

---

## 12. Performance

### Download
- Stream binary, sem carregar em memória.
- ETag para suporte a 304.

### Logs
- Paginação se > 1000 entradas.

---

## 13. Estados e edge cases

### Entrega cancelada/falha
- Mostra estado mas botões de reentrega disabled.
- Atalho para "Refazer".

### Pacote deletado (limpeza)
- Mensagem: "Pacote arquivado. Use 'Refazer pacote' para regenerar."

### Reentrega em curso
- Botão "Reentregar" disabled.
- Link para reentrega ativa.

### Cliente mudou de status
- Aviso se cliente PAUSADO ou ENCERRADO.

---

## 14. Acessibilidade

- Cabeçalho com hierarquia clara (h1, h2).
- Abas com `role="tablist"`.
- Tabela de arquivos semântica.
- Botões com labels.

---

## 15. Auditoria

| Ação | Detalhes |
|---|---|
| `ENTREGA_DOWNLOAD` | quem, quando |
| `ENTREGA_PACOTE_BAIXADO` | usuário |
| `ENTREGA_PDF_REGERADO` | visibilidade |
| `ENTREGA_REENTREGUE` | nova entregaId, motivo |
| `ENTREGA_MARCADA_PROBLEMATICA` | motivo |

---

## 16. Cross-reference

- [`21-geracao-pacote.md`](21-geracao-pacote.md) — Tela anterior.
- [`23-historico-entregas.md`](23-historico-entregas.md) — Volta para listagem.
- [`25-documento-release-md-pdf.md`](25-documento-release-md-pdf.md) — PDF.
- [`32-modelo-dados-sugerido.md`](32-modelo-dados-sugerido.md) — Entidade Entrega.
- [`99-padroes-tela.md`](99-padroes-tela.md) — Padrões.
