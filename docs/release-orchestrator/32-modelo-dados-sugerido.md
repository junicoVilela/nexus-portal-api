# 32 — Modelo de Dados Sugerido

> Este modelo reflete o que o **Orchestrator** precisa adicionar. As entidades já existentes no `release-orchestrator` do backend (`ProdutoRh`, `Release`, `ReleaseItem`, `ReleaseHistorico`, `ReleaseTemplate`) **não estão duplicadas aqui** — são consumidas via API interna do release-orchestrator.

## Convenções
- Todas as entidades têm `id` (UUID), `createdAt`, `updatedAt`, `createdBy`, `updatedBy` (auditável).
- Status são enums fixos (não cadastráveis).
- Soft delete via `ativo` quando aplicável; histórico nunca é apagado fisicamente.

---

## Entidades já existentes (`release-orchestrator`)
Apenas referenciadas. Estão prontas no backend:
- `ProdutoRh` — produtos (renomear conceitual para `Produto` ao expor via API). Hoje tem: nome, sigla, descrição, cor, responsavelId, ativo.
- `Release` — release do produto (versão única atual).
- `ReleaseItem` — item da release com categoria, visibilidade, ticket, commit, PR.
- `ReleaseHistorico` — auditoria de transições.
- `ReleaseTemplate` — template estrutural.

### Ajustes necessários em `release-orchestrator` (fase 0 do plano)
- **MVP:**
  - Nova entidade `ModuloProduto` (catálogo livre por produto — ver abaixo).
  - Vincular `Release` a versões por módulo: nova `ReleaseModuloVersao`.
  - Nova entidade `ArtefatoReleaseModulo` (upload manual de artefatos por módulo da release — ver abaixo).
  - Renderer de PDF da Release.
- **Pós-MVP:**
  - Adicionar campos de integração ao Produto: `repositorioGithub`, `branchPadrao`, `padraoTag`, `jenkinsUrl`, `jenkinsJob`, `triggerMode`.

---

## Entidades novas (`release-orchestrator`)

### `Cliente`
- nome, razaoSocial, cnpj, sigla (única), status
- ambientePadrao, tipoBanco, codificacao, fusoHorario
- responsavelComercialId, responsavelTecnicoId
- emailNotificacoes
- ativo

### `ClienteContato`
- clienteId, nome, email, telefone, papel

### `ConfiguracaoEntrega`
- clienteId, tipoDestino (`PASTA` | `FTP` | `SFTP` | `BUCKET`)
- caminhoBase, usuario, credencialRef (referência cifrada)
- janelaPermitida (cron-like ou texto), aprovacaoObrigatoria

### `ModuloProduto` (catálogo livre, em `release-orchestrator`)
- produtoId, nome, codigo (slug único no produto)
- tipo (`WEB` | `BATCH` | `BANCO` | `KETTLE` | `FUNCIONALIDADES` | `REGRAS`)
- geraDelta (boolean, default por tipo)
- obrigatorio, ordem, ativo
- configEspecifica (JSON: diretório, dialeto, prefixos DDL/DML, etc. — ver tela 10)

### `ReleaseModuloVersao` (em `release-orchestrator`)
- releaseId, moduloProdutoId, versao (tag GitHub no pós-MVP; rótulo livre no MVP)

### `ArtefatoReleaseModulo` (em `release-orchestrator`, MVP)
- releaseId, moduloProdutoId
- nomeArquivo, caminhoArmazenado, sha256, tamanhoBytes
- observacao (texto livre)
- uploadedBy, uploadedAt
- Uma release pode ter N artefatos por módulo (ex.: múltiplos `.sql` em `BANCO`). O conjunto fica imutável após `PUBLICADA`.

### `ClienteProduto`
- clienteId, produtoId, ativo, ambiente

### `ClienteProdutoModulo`
- clienteProdutoId, moduloProdutoId, versaoAtual, ativo, ultimaEntregaId

### `DominioProduto` *(tabela `tb_dominio_produto`)*
- produtoId, nome, codigo (`NM_MODELO_DOMINIO`), **codigoLegado** (`CD_DOMINIO_FUNCIONAL`), descricao, ordem, ativo
- Spec de tela: [`11-produtos-catalogo-funcional.md`](11-produtos-catalogo-funcional.md) §17
- **Por que `Produto` no nome:** o RBAC de segurança (em `doc-flow`) já ocupa `tb_dominio` / `Dominio` — ver [`33-decisoes-tecnicas.md`](33-decisoes-tecnicas.md) §Autorização.

### `FuncionalidadeProduto` *(tabela `tb_funcionalidade_produto`)*
- dominioProdutoId, nome, codigo, **codigoLegado** (`CD_FUNCIONALIDADE`), **codigoOperacao** (`CD_OPERACAO`), descricao, critica, ordem, ativo
- Distinta da `Funcionalidade` do RBAC (que representa item de segurança como `USUARIO`, `CLIENTE`, `RELEASE`).

### `ClienteFuncionalidadeProduto` *(tabela `tb_cliente_funcionalidade_produto`)*
- clienteId, funcionalidadeProdutoId, habilitada, origem (`MANUAL` | `TEMPLATE` | `HERDADA`)
- Granularidade: **por funcionalidade do produto**. Domínios “possuídos” pelo cliente são **derivados** (`COUNT habilitada=true` por domínio do produto) — ver `05` §2 e §8.
- **Espelho no banco destino:** `TB_CLIENTE_FUNCIONALIDADE` (`CD_FUNCIONALIDADE` PK) — tabela **nova**, ambiente **single-tenant** (um cliente por banco), ver `11` §17.1.

> **Nota sobre nomenclatura:** o sufixo `Produto` evita colisão com o RBAC. Na UI, comunicação informal e na tela `11` os termos seguem **Domínio** e **Funcionalidade** (do produto) — o sufixo aparece apenas no modelo persistido / API interna. O domínio do RBAC trata de **segurança** (`USUARIO`, `CLIENTE`, `RELEASE`, …); o domínio do produto trata de **catálogo funcional** (`Usuários`, `Relatórios`, …).

### `ProximaEntrega`
- clienteId, produtoId, releaseId, dataPrevista, ambiente
- prioridade (`BAIXA` | `MEDIA` | `ALTA` | `CRITICA`)
- status — enum **próprio**:
  - `PLANEJADA`, `AGENDADA`, `REPLANEJADA`, `ATRASADA`, `CONVERTIDA`, `CANCELADA`
- responsavelId, observacoes, entregaConvertidaId (FK quando vira Entrega)

### `Entrega`
- clienteId, produtoId, releaseId, proximaEntregaId (FK opcional)
- status — enum `GeracaoStatus` (ver 33):
  - `PENDENTE`, `PROCESSANDO`, `CONCLUIDO`, `ERRO`, `CANCELADO`
- dataCriacao, dataConclusao, responsavelId
- caminhoPacote, checksumPacote
- erroDetalhe (quando `ERRO`)
- entregaOriginalId (FK opcional, preenchido quando esta entrega é uma **reentrega** de outra; reentrega pode reutilizar o pacote da entrega original ou disparar nova geração)

### `EntregaModulo`
- entregaId, moduloProdutoId
- fromTag, toTag, modoCalculo (`AUTOMATICO` | `MANUAL`)
- justificativa (quando `MANUAL`)
- statusEtapa (`PENDENTE` | `OK` | `FALHA`)
- artefatos (JSON: lista de paths + sha256)

### `Pacote`
- entregaId, caminho, tamanhoBytes, sha256, geradoEm

### `ArtefatoPacote`
- pacoteId, modulo, tipoArtefato (`WAR` | `JAR` | `SQL` | `KTR` | `KJB` | `PDF` | `JSON` | `TXT`)
- nome, caminhoRelativo, sha256, tamanhoBytes

### `EntregaHistorico`
- entregaId, acao, descricao, statusAnterior, statusNovo
- usuario, createdAt

### `TemplateDocumento` (orchestrator)
- nome, produtoId, tipo (`CLIENTE` | `INTERNO` | `INSTRUCOES` | `NOVIDADES` | `BANCO`)
- conteudoMarkdown, ativo

---

## Enums consolidados

### Tipo de módulo
```text
WEB | BATCH | BANCO | KETTLE | FUNCIONALIDADES | REGRAS
```

### Status de Próxima Entrega
```text
PLANEJADA | AGENDADA | REPLANEJADA | ATRASADA | CONVERTIDA | CANCELADA
```

### Status de Entrega / Geração (execução)
Ver detalhes em 33. Implementado como `GeracaoStatus`:
```text
PENDENTE | PROCESSANDO | CONCLUIDO | ERRO | CANCELADO
```
> Reentrega não é status — é uma nova `Entrega` com FK `entregaOriginalId`.

### Tipo de destino
```text
PASTA | FTP | SFTP | BUCKET
```

### Modo de cálculo de delta
```text
AUTOMATICO | MANUAL
```

### Status de Cliente
```text
ATIVO | PAUSADO | EM_ANALISE | ENCERRADO
```

### Severidade de alerta
```text
INFO | WARN | CRITICAL
```

### Papel de contato
```text
TECNICO | COMERCIAL | FINANCEIRO | OUTRO
```

### Origem de habilitação de funcionalidade
```text
MANUAL | TEMPLATE | HERDADA
```

### Trigger mode de integração (Produto, pós-MVP)
```text
MANUAL | BUILD_ON_TAG | BUILD_ON_PR
```

### Comportamento fora da janela (ConfiguracaoEntrega)
```text
BLOQUEAR | ALERTAR
```

### Visibilidade de item de release
```text
TODOS | SUPORTE | TECNICO
```

### Filtro de visibilidade de PDF
```text
CLIENTE | SUPORTE | INTERNO
```

---

## Relações principais (resumo)
```text
Cliente 1—N ClienteProduto N—1 Produto
ClienteProduto 1—N ClienteProdutoModulo N—1 ModuloProduto
Produto 1—N ModuloProduto
Produto 1—N Release
Release 1—N ReleaseModuloVersao N—1 ModuloProduto
Release 1—N ReleaseItem
ProximaEntrega 0..1 — 0..1 Entrega
Entrega 1—N EntregaModulo
Entrega 1—1 Pacote 1—N ArtefatoPacote
Cliente 1—N ClienteFuncionalidadeProduto N—1 FuncionalidadeProduto N—1 DominioProduto N—1 Produto
```

---

## Convenções gerais

- **Todas as entidades**: `id` (UUID), `createdAt`, `updatedAt`, `createdBy`, `updatedBy` (via `AuditableEntity` do shared).
- **Status**: enums fixos, persistidos como STRING.
- **Soft delete**: via `ativo: boolean` quando aplicável; histórico nunca apagado fisicamente.
- **Auditoria**: tabela `orchestrator_auditoria` separada (ver `34-observabilidade.md` §9).
- **FKs**: explícitas no banco para integridade referencial.
- **CASCADE**: apenas para histórico e dependências fortes (artefatos de pacote).

---

## Tabelas auxiliares

### `orchestrator_auditoria`
Audit trail centralizado. Ver `34-observabilidade.md` §9 para schema completo.

### `entrega_pdf_snapshot`
Versões de PDF gerado por entrega (similar a `release_pdf_snapshot` no release-orchestrator).

### `notification_log`
Histórico de notificações enviadas (e-mail, Slack).

---

## Cross-reference

- [`33-decisoes-tecnicas.md`](33-decisoes-tecnicas.md) — Stack, storage, async, autorização (RBAC tabular em `doc-flow`).
- [`34-observabilidade.md`](34-observabilidade.md) — Schema de auditoria.
- Entidades reutilizadas do release-orchestrator (Produto, Release, ReleaseItem) — consolidadas neste módulo.
- [`10-produtos-modulos-artefatos.md`](10-produtos-modulos-artefatos.md) — Detalhe de `ModuloProduto` e `ArtefatoReleaseModulo`.
- [`11-produtos-catalogo-funcional.md`](11-produtos-catalogo-funcional.md) — UI de `DominioProduto` / `FuncionalidadeProduto`.
- [`../naming-suggestions.md`](../naming-suggestions.md) — `ProdutoRh` → `Produto`.
- [`../MIGRATIONS.md`](../MIGRATIONS.md) — Baseline Flyway. As novas tabelas (`tb_dominio_produto`, `tb_funcionalidade_produto`, `tb_cliente_funcionalidade_produto`) entram em `V?__release_orchestrator__05_alter_*`.
- [`38-glossario.md`](38-glossario.md) — Termos do domínio.
