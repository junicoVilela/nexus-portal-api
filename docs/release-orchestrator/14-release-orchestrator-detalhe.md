# 14 — Release Orchestrator — Detalhe (abas Artefatos + PDF)

> **Estado**: as abas **Itens**, **Histórico** e **Validação** já estão implementadas no backend (`ReleaseController`, `ReleaseItemController`, `ReleaseService.validar`). Este documento cobre **o que falta** para fechar o detalhe da release no contexto pré-orchestrator:
> - **Aba Artefatos** (MVP) — upload manual de artefatos por módulo da release.
> - **Aba PDF** (MVP) — geração do PDF da release com filtro de visibilidade.

---

## 1. Contexto

A tela de detalhe da release (`/release-orchestrator/releases/:id`) ganha **duas novas abas** para destravar o orchestrator. Ambas dependem das entidades da Fase 0 (`ModuloProduto`, `ArtefatoReleaseModulo`, `ReleasePdfSnapshot`).

Detalhes técnicos completos em:
- [`10-produtos-modulos-artefatos.md`](10-produtos-modulos-artefatos.md) — Upload de artefatos.
- [`25-documento-release-md-pdf.md`](25-documento-release-md-pdf.md) — Renderer de PDF.

---

## 2. Layout do detalhe da release

```text
┌────────────────────────────────────────────────────────────────────────┐
│ < Releases     DTEC-LD v1.5.0      Status: APROVADA  [Ações ⋮]         │
├────────────────────────────────────────────────────────────────────────┤
│ [Visão Geral] [Itens] [Artefatos*] [PDF*] [Histórico] [Validação]     │
│                       ^^^^^^^^^^^^ ^^^^^^                              │
│                       NOVO MVP     NOVO MVP                            │
├────────────────────────────────────────────────────────────────────────┤
│ (conteúdo da aba ativa)                                                │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Aba Artefatos (MVP)

### 3.1 Objetivo

Permitir ao operador anexar arquivos físicos da release por módulo do produto, para que o Orchestrator monte o pacote sem depender de integração GitHub/Jenkins.

### 3.2 Layout

```text
┌────────────────────────────────────────────────────────────────────────┐
│ Artefatos da Release DTEC-LD v1.5.0                                    │
├────────────────────────────────────────────────────────────────────────┤
│ Resumo: 4/6 módulos com artefato. 2 pendentes obrigatórios.            │
├────────────────────────────────────────────────────────────────────────┤
│ ┌─ dtec-db-ddl (BANCO) ────────────────── Obrigatório ─ ✅ 1 arq.  ─┐ │
│ │ DDL_001_users.sql  •  12KB  •  sha256: abc..ef0   [⬇️] [🗑️]      │ │
│ │ Anexar: [Choose file]  Obs: [_______________]   [+ Adicionar]    │ │
│ └──────────────────────────────────────────────────────────────────┘ │
│                                                                        │
│ ┌─ dtec-db-dml (BANCO) ────────────────── Obrigatório ─ ⚠️ Pendente─┐ │
│ │ Nenhum artefato anexado.                                          │ │
│ │ Anexar: [Choose file]  Obs: [_______________]   [+ Adicionar]    │ │
│ └──────────────────────────────────────────────────────────────────┘ │
│                                                                        │
│ ┌─ dtec-portal (WEB) ──────────────────── Obrigatório ─ ✅ 1 arq.  ─┐ │
│ │ dtec-portal-1.5.0.war  •  47MB  •  sha256: 123..789  [⬇️] [🗑️]   │ │
│ │ ✅ Aceita: .war, .jar, .zip, .tar.gz, .tgz, .ear (config módulo)  │ │
│ └──────────────────────────────────────────────────────────────────┘ │
│                                                                        │
│ ┌─ dtec-api (WEB) ─────────────────────── Obrigatório ─ ✅ 1 arq.  ─┐ │
│ │ dtec-api-1.5.0.jar  •  38MB  •  sha256: abc..012  [⬇️] [🗑️]     │ │
│ └──────────────────────────────────────────────────────────────────┘ │
│                                                                        │
│ ┌─ dtec-funcs (FUNCIONALIDADES) ──────── Auto-gerado ─ N/A          ┐│
│ │ ℹ️  Tipo não aceita upload. Gerado a partir do cliente na entrega.│ │
│ └──────────────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────────┘
```

### 3.3 Comportamento por tipo de módulo

| Tipo | Aceita upload? | O que pode ser anexado |
|---|---|---|
| `WEB` | Sim | **Um** artefato por módulo; extensões default: `.war`, `.jar`, `.zip`, `.tar.gz`, `.tgz`, `.ear` — ou lista `extensoesAceitas` na config do módulo (ver `10` §3) |
| `BATCH` | Sim | **Um** artefato; default: `.jar`, `.zip`, `.tar.gz`, `.tgz` |
| `BANCO` | Sim | Múltiplos `.sql` ou um `.zip` contendo `.sql` |
| `KETTLE` | Sim | Múltiplos `.ktr` / `.kjb` ou um `.zip` |
| `FUNCIONALIDADES` | **Não** | Gerado a partir da config do cliente |
| `REGRAS` | **Não** | Gerado a partir da config do cliente |

> Vários apps web = vários módulos `WEB` na release, cada um com slot de upload próprio (`10` §7.8).

### 3.4 Campos por upload

| Campo | Tipo | Obrigatório | Validação |
|---|---|---|---|
| Arquivo | binário | ✅ | extensão compatível, tamanho ≤ limite |
| Observação | text | depende do status | obrigatória em EM_REVISAO/APROVADA |
| Substituir existente | checkbox | ❌ | usado para reupload |

### 3.5 Sistema preenche automaticamente

- `sha256` (calculado server-side).
- `tamanhoBytes`.
- `contentType`.
- `uploadedAt` = now().
- `uploadedBy` = usuário autenticado.

### 3.6 Regras por status

| Status da release | Pode upload? | Pode remover? | Justificativa? |
|---|---|---|---|
| RASCUNHO | ✅ | ✅ | Não |
| EM_DESENVOLVIMENTO | ✅ | ✅ | Não |
| EM_REVISAO | ✅ | ✅ | **Sim** |
| APROVADA | ✅ | ✅ | **Sim** |
| PUBLICADA | ❌ (imutável) | ❌ | — |
| CANCELADA | ❌ | ❌ | — |

### 3.7 Validações

- Extensão do arquivo deve casar com tipo do módulo.
- Tamanho ≤ `releaseorchestrator.artefatos.tamanho-maximo-mb` (default 500MB).
- SHA-256 não duplicado para o mesmo (release, módulo).
- Release em status que permite upload.
- Módulo deve aceitar upload (tipo).

### 3.8 Integração com validação pré-publicação

A tela de **Validação** (já implementada) deve ser expandida:

```
Pendências:
✗ A release não possui itens cadastrados.
✗ Módulo obrigatório "dtec-db-dml" sem artefato uploadado.
✗ Módulo obrigatório "dtec-batch" sem artefato uploadado.

Alertas:
⚠ A release não possui data prevista definida.
⚠ Módulo opcional "dtec-etl" sem artefato.
```

Publicação bloqueada se há pendência.

### 3.9 Endpoints (recapitulando)

```
POST   /api/v1/release-orchestrator/releases/{id}/modulos/{moduloId}/artefatos    (multipart)
GET    /api/v1/release-orchestrator/releases/{id}/artefatos
GET    /api/v1/release-orchestrator/releases/{id}/artefatos/{artefatoId}/download
DELETE /api/v1/release-orchestrator/releases/{id}/artefatos/{artefatoId}
POST   /api/v1/release-orchestrator/releases/{id}/artefatos/{artefatoId}/verificar
```

Detalhe completo em [`10-produtos-modulos-artefatos.md`](10-produtos-modulos-artefatos.md).

---

## 4. Aba PDF (MVP)

### 4.1 Objetivo

Gerar e baixar o PDF da release a partir do conteúdo já cadastrado (release + itens + visibilidade).

### 4.2 Layout

```text
┌────────────────────────────────────────────────────────────────────────┐
│ PDF da Release DTEC-LD v1.5.0                                          │
├────────────────────────────────────────────────────────────────────────┤
│ Template: [DTEC-LD Padrão ▼]   Visibilidade: [Cliente ▼]               │
│                                                                        │
│ [Pré-visualizar]   [Baixar PDF]   [Gerar snapshot]                     │
├────────────────────────────────────────────────────────────────────────┤
│ ┌─ Pré-visualização ────────────────────────────────────────────────┐│
│ │                                                                    ││
│ │   DTEC-LD - Release Notes                                          ││
│ │   Versão 1.5.0 • MINOR • Publicada em 15/05/2026                   ││
│ │                                                                    ││
│ │   ## Novidades                                                     ││
│ │   - Nova função de exportação CSV                                  ││
│ │   - Suporte a SSO                                                  ││
│ │                                                                    ││
│ │   ## Correções                                                     ││
│ │   - Corrigido cálculo de juros em prazos > 12 meses                ││
│ │                                                                    ││
│ │   [...]                                                            ││
│ └────────────────────────────────────────────────────────────────────┘│
├────────────────────────────────────────────────────────────────────────┤
│ Snapshots existentes:                                                  │
│ • PDF Cliente — sha256:abc123 — gerado 15/05 14:30 por João  [⬇️]     │
│ • PDF Suporte — sha256:def456 — gerado 15/05 14:30 por João  [⬇️]     │
└────────────────────────────────────────────────────────────────────────┘
```

### 4.3 Funcionalidades

- **Selecionar template** de PDF (default = template do produto; ver `24-documentos-templates.md`).
- **Pré-visualizar** o documento renderizado in-browser (PDF embed).
- **Filtrar conteúdo por visibilidade** (Cliente / Suporte / Interno).
- **Baixar PDF** gerado on-demand.
- **Gerar snapshot** (release publicada): congela PDF para auditoria.
- **Listar snapshots** existentes da release.

### 4.4 Conteúdo do PDF

| Seção | Conteúdo |
|---|---|
| Cabeçalho | Logo, produto, sigla, versão, tipo, data prevista/publicação |
| Resumo | Texto livre da release |
| Itens agrupados por categoria | Novidade, Melhoria, Correção, Segurança, Performance, Documentação, Ajuste Técnico, Impacto Operacional, Importante |
| Por item | Título, descrição, ticket, PR (se visível ao público-alvo) |
| Observações | Texto livre da release |
| Rodapé | Data de geração, usuário, hash do PDF |

### 4.5 Filtros de visibilidade

| Filtro | Itens incluídos |
|---|---|
| **Cliente** | Visibilidade = `TODOS` |
| **Suporte** | Visibilidade = `TODOS` + `SUPORTE` |
| **Interno (técnico)** | Todos os itens (inclui `TECNICO`) |

### 4.6 Regras

- PDF gerado é **versionado**: cada snapshot tem `sha256` e timestamp.
- Release publicada gera **3 snapshots automáticos** (Cliente, Suporte, Interno) no momento da publicação.
- Mudança de template após publicação exige nova geração explícita.
- Snapshots são **imutáveis** (não podem ser editados).
- Geração on-demand para release não publicada usa cache (TTL configurável).
- O renderer de PDF deste módulo é o mesmo usado pelo Orchestrator (ver `25-documento-release-md-pdf.md`).

### 4.7 Endpoints (recapitulando)

```
GET   /api/v1/release-orchestrator/releases/{id}/pdf?visibilidade={CLIENTE|SUPORTE|INTERNO}
GET   /api/v1/release-orchestrator/releases/{id}/pdf/preview?visibilidade=...
POST  /api/v1/release-orchestrator/releases/{id}/pdf/snapshot
GET   /api/v1/release-orchestrator/releases/{id}/pdf/snapshots
GET   /api/v1/release-orchestrator/releases/{id}/pdf/snapshots/{snapshotId}
```

Detalhe completo em [`25-documento-release-md-pdf.md`](25-documento-release-md-pdf.md).

### 4.8 Performance

- Pré-visualização usa endpoint `/preview` que retorna PDF inline (não download).
- Cache no servidor: TTL 60 minutos, invalidado em mudança da release/itens.
- Geração média: <2s (release pequena); 2-5s (release grande).

---

## 5. Estados visuais

### Aba Artefatos
- Badge "Pendente" se há módulo obrigatório sem artefato.
- Cor de aviso (amarelo) no badge.

### Aba PDF
- Badge "Snapshots" com contagem se houver.
- Verde se PDF Cliente snapshot existe (release publicada).

---

## 6. Permissões

| Ação | ADMIN | EDITOR | LEITOR |
|---|---|---|---|
| Ver artefatos | ✅ | ✅ | ✅ |
| Upload artefato | ✅ | ✅ | ❌ |
| Remover artefato | ✅ | ✅ | ❌ |
| Baixar artefato | ✅ | ✅ | ❌ |
| Ver PDF | ✅ | ✅ | ✅ |
| Gerar PDF on-demand | ✅ | ✅ | ✅ |
| Criar snapshot | ✅ | ✅ | ❌ |
| Baixar snapshot | ✅ | ✅ | ✅ |

---

## 7. Erros e edge cases

### Upload em release publicada
- Frontend esconde botão.
- Backend: 409 `RELEASE_PUBLICADA_IMUTAVEL`.

### Tamanho excedido
- Validação client-side (antes do upload).
- Backend: 413 com mensagem amigável.

### SHA-256 duplicado
- Backend: 409 `ARTEFATO_DUPLICADO`.
- Frontend: "Esse arquivo já foi enviado anteriormente."

### PDF render falha
- Toast erro + correlation ID.
- Pré-visualização não exibida.

### Template excluído
- Fallback para template global.

---

## 8. Auditoria

| Ação | Registrado |
|---|---|
| Upload artefato | `ARTEFATO_ADICIONADO` em `release_historico` |
| Remoção artefato | `ARTEFATO_REMOVIDO` + motivo |
| Geração PDF on-demand | log estruturado (sem audit) |
| Snapshot PDF | `PDF_GERADO` em `release_historico` |

---

## 9. Cross-reference

- [`25-documento-release-md-pdf.md`](25-documento-release-md-pdf.md) — Spec técnica do PDF.
- [`10-produtos-modulos-artefatos.md`](10-produtos-modulos-artefatos.md) — Catálogo de módulos.
- [`10-produtos-modulos-artefatos.md`](10-produtos-modulos-artefatos.md) — Spec técnica de artefatos.
- [`23-historico-entregas.md`](23-historico-entregas.md) — Histórico e auditoria.
- [`24-documentos-templates.md`](24-documentos-templates.md) — Templates de PDF.
- [`25-documento-release-md-pdf.md`](25-documento-release-md-pdf.md) — Renderer.
- [`28-configuracoes.md`](28-configuracoes.md) — Limites de upload.
- [`99-padroes-tela.md`](99-padroes-tela.md) — Padrões.
