# 10 — Produtos — Módulos

> **MVP**: todos os artefatos chegam via **upload manual na release** (ver `14-release-orchestrator-detalhe.md` e as seções abaixo). As colunas "Origem do artefato" e "Mecanismo de empacotamento" descritas abaixo refletem o desenho **pós-MVP** (com GitHub/Jenkins). No MVP, o operador é responsável por subir o artefato correspondente ao tipo de cada módulo, e a "config específica por tipo" (diretório do repo, padrão de tag, etc.) só é exigida em pós-MVP.

## 1. Papel da tela

**Cadastro livre dos módulos que compõem um produto**. Define o que pode ser empacotado e como o delta é calculado durante a geração da entrega.

Acessível em `/orchestrator/produtos/:produtoId/modulos`.

---

## 2. Conceito

- Cada produto tem N módulos cadastráveis.
- Cada módulo tem um **tipo** (categoria fixa do sistema) que determina como o pacote é montado.
- **Vários projetos web** no mesmo produto = **vários módulos `WEB`** (não é necessário novo tipo).
- O cliente contrata módulos individualmente (ver `06-cliente-produtos-contratados.md`).

```text
Produto NEXUS-LD
├─ Módulo nexus-portal      (WEB)   ← app principal (.war)
├─ Módulo nexus-api         (WEB)   ← API REST (.jar ou .war)
├─ Módulo nexus-front-spa   (WEB)   ← build estático (.zip)
├─ Módulo nexus-batch       (BATCH)
├─ Módulo nexus-db-ddl      (BANCO)
├─ Módulo nexus-db-dml      (BANCO)
├─ Módulo nexus-etl         (KETTLE)
├─ Módulo nexus-funcs       (FUNCIONALIDADES)
└─ Módulo nexus-regras      (REGRAS)
```

> Um módulo `WEB`/`BATCH` = **um slot de artefato** na release (upload no MVP; asset da `TO_TAG` no pós-MVP). Cada um com `destinoPacote` distinto no ZIP final.

---

## 3. Tipos de módulo (fixos)

| Tipo | Origem do artefato | Mecanismo de empacotamento |
|---|---|---|
| `WEB` | GitHub Release asset — MVP: upload manual | Baixa o asset da `TO_TAG` (pós-MVP). Substitui inteiro (sem delta). |
| `BATCH` | GitHub Release asset — MVP: upload manual | Baixa o asset da `TO_TAG` (pós-MVP). Substitui inteiro. |
| `BANCO` | Diretório de scripts SQL no repositório — MVP: upload `.zip`/`.sql` | Coleta SQL entre `FROM_TAG` e `TO_TAG`, ordena DDL → DML, gera `DDL.sql` e `DML.sql` unificados. **Gera delta.** |
| `KETTLE` | Diretório `.ktr`/`.kjb` no repositório — MVP: upload `.zip` | Coleta arquivos modificados entre `FROM_TAG` e `TO_TAG`. **Gera delta.** |
| `FUNCIONALIDADES` | Configuração do cliente (`05-cliente-dominios-funcionalidades.md`) | Gera scripts a partir das funcionalidades habilitadas. Sempre re-gera. |
| `REGRAS` | Configuração do cliente | Gera scripts de regras (matriz de permissões/grupos). |

### Extensões aceitas por tipo

Validadas no upload (MVP) e no `padraoAsset` / download GitHub (pós-MVP). Lista **default** se `extensoesAceitas` não estiver na config do módulo.

| Tipo | Extensões default | Exemplos de uso |
|---|---|---|
| `WEB` | `.war`, `.jar`, `.zip`, `.tar.gz`, `.tgz`, `.ear` | WAR Tomcat, JAR Spring Boot, SPA estático em ZIP, pacote legado EAR |
| `BATCH` | `.jar`, `.zip`, `.tar.gz`, `.tgz` | JAR batch, pacote com libs + scripts |
| `BANCO` | `.sql`, `.zip` | Scripts avulsos ou ZIP com DDL/DML |
| `KETTLE` | `.ktr`, `.kjb`, `.zip` | Jobs avulsos ou ZIP com delta |

- `extensoesAceitas` na config **restringe** (subset) ou **expande** (superset explícito) o default do tipo — validado server-side.
- Comparação **case-insensitive** (`.WAR` = `.war`).
- MIME type é informativo; a validação primária é pela extensão.

### Defaults por tipo

| Tipo | `geraDelta` (default) | `obrigatorio` (default) |
|---|---|---|
| WEB | false | true |
| BATCH | false | true |
| BANCO | true | true |
| KETTLE | true | false |
| FUNCIONALIDADES | false | true |
| REGRAS | false | true |

---

## 4. Layout

```text
┌────────────────────────────────────────────────────────────────────────┐
│ < NEXUS-LD       Módulos                                [+ Novo módulo] │
├────────────────────────────────────────────────────────────────────────┤
│ ☰ (drag para reordenar)                                                │
│ ┌──────────────────────────────────────────────────────────────────┐  │
│ │ Ord │ Status │ Código        │ Nome             │ Tipo  │ Δ │ ⚙️ │  │
│ ├──────────────────────────────────────────────────────────────────┤  │
│ │ 1   │ ✅     │ nexus-db-ddl   │ Banco DDL        │ BANCO │✓ │ ⋮  │  │
│ │ 2   │ ✅     │ nexus-db-dml   │ Banco DML        │ BANCO │✓ │ ⋮  │  │
│ │ 3   │ ✅     │ nexus-funcs    │ Funcionalidades  │ FUNC  │  │ ⋮  │  │
│ │ 4   │ ✅     │ nexus-regras   │ Regras           │ REGRAS│  │ ⋮  │  │
│ │ 5   │ ✅     │ nexus-portal   │ Portal Web       │ WEB   │  │ ⋮  │  │
│ │ 6   │ ✅     │ nexus-api      │ API REST         │ WEB   │  │ ⋮  │  │
│ │ 7   │ ✅     │ nexus-batch    │ Processador      │ BATCH │  │ ⋮  │  │
│ │ 8   │ ❌     │ nexus-etl      │ ETL Kettle       │ KETTLE│✓ │ ⋮  │  │
│ └──────────────────────────────────────────────────────────────────┘  │
│ ℹ️  Ordem afeta sequência de montagem do pacote.                      │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 5. Tabela de módulos

### Colunas

| Coluna | Conteúdo |
|---|---|
| Ord | Drag handle + número da ordem |
| Status | ✅ ativo / ❌ inativo |
| Código | Slug único no produto (ex.: `nexus-web`) |
| Nome | Nome amigável |
| Tipo | Badge colorido por tipo |
| Δ | Indica se gera delta |
| Obrig. | Indica se obrigatório no pacote |
| Ações | Menu kebab |

### Ordenação
- Drag-and-drop para reordenar.
- Salva via endpoint dedicado.
- Default: ordem alfabética por código se não setada.

---

## 6. Formulário de módulo (modal ou tela)

### Campos

| Campo | Tipo | Obrigatório | Validação |
|---|---|---|---|
| Nome | text | ✅ | max 200 |
| Código (slug) | text | ✅ | regex `[a-z0-9-]+`, UNIQUE por produto, **imutável** |
| Tipo | select | ✅ | enum TipoModulo |
| Gera delta | checkbox | ✅ | default por tipo |
| Obrigatório | checkbox | ✅ | default false |
| Ordem | number | ❌ | auto se vazio |
| Ativo | checkbox | ✅ | default true |
| Config específica | JSON dynamic | depende | validado por tipo |

### Config específica por tipo

Campos comuns em `WEB` e `BATCH`:

| Campo | Obrigatório | Descrição |
|---|---|---|
| `destinoPacote` | recomendado | Pasta relativa no ZIP final (ex.: `web/portal/`) |
| `extensoesAceitas` | ❌ | Lista de extensões permitidas; default = tabela §3 |
| `padraoAsset` | pós-MVP | Glob do asset na GitHub Release (ex.: `nexus-portal-*.war`) |
| `repositorioGithub` | pós-MVP | `owner/repo` **por módulo**; se omitido, usa o do produto (`09`) |
| `jenkinsJob` | pós-MVP | Job Jenkins que publica o asset deste módulo |

> **Repositório por módulo (pós-MVP):** quando cada app web tem repo próprio, configure `repositorioGithub` no módulo. A `TO_TAG` da release é a mesma (ex.: `v1.5.0`); o orchestrator busca o asset em cada repo/tag conforme `padraoAsset`.

#### WEB — exemplos

Portal Tomcat (repo dedicado):
```json
{
  "repositorioGithub": "nexus/nexus-portal",
  "padraoAsset": "nexus-portal-*.war",
  "extensoesAceitas": [".war"],
  "jenkinsJob": "nexus-portal-build",
  "destinoPacote": "web/portal/"
}
```

API Spring Boot (mesmo repo do produto, asset distinto):
```json
{
  "padraoAsset": "nexus-api-*.jar",
  "extensoesAceitas": [".jar", ".war"],
  "destinoPacote": "web/api/"
}
```

Frontend estático (ZIP de build):
```json
{
  "repositorioGithub": "nexus/nexus-front",
  "padraoAsset": "nexus-front-*.zip",
  "extensoesAceitas": [".zip", ".tar.gz"],
  "destinoPacote": "web/static/"
}
```

#### BATCH — exemplo
```json
{
  "padraoAsset": "nexus-batch-*.jar",
  "extensoesAceitas": [".jar", ".zip"],
  "jenkinsJob": "nexus-batch-build",
  "destinoPacote": "batch/"
}
```

#### BANCO
```json
{
  "diretorioRaiz": "db/scripts",
  "dialeto": "oracle",
  "prefixoDdl": "DDL_",
  "prefixoDml": "DML_",
  "ordenacao": "alfabetica",
  "destinoPacote": "db/"
}
```

#### KETTLE
```json
{
  "diretorioRaiz": "etl/jobs",
  "incluirDependencias": true,
  "destinoPacote": "etl/"
}
```

#### FUNCIONALIDADES
```json
{
  "templateScriptId": "uuid",
  "destinoPacote": "func/"
}
```

#### REGRAS
```json
{
  "templateScriptId": "uuid",
  "destinoPacote": "rules/"
}
```

---

## 7. Regras

### 7.1 Imutabilidade
- **Código (slug) é imutável** após criação.
- **Tipo é imutável** após primeira entrega usar o módulo.

### 7.2 Exclusão
- Apenas se nenhuma `Entrega` histórica usou o módulo.
- E nenhum `ArtefatoReleaseModulo` referencia.
- Alternativa: inativar.

### 7.3 Inativação
- Módulo inativo não aparece na seleção de nova entrega.
- Continua visível em entregas históricas.
- Não pode receber upload de artefato.

### 7.4 Reordenação
- Ordem afeta sequência de montagem.
- Tipicamente: BANCO antes de WEB antes de BATCH (mas operador decide).
- Drag-and-drop com endpoint dedicado.

### 7.5 Defaults
- `geraDelta` usa default do tipo.
- `obrigatorio` default false (exceto tipos críticos).

### 7.6 Tipos sem upload
- FUNCIONALIDADES e REGRAS **não aceitam upload**. Frontend esconde botão.
- Sempre presentes no pacote.

### 7.7 Config específica
- Validada server-side por tipo.
- JSON inválido → erro detalhado.

### 7.8 Vários módulos WEB/BATCH no mesmo produto
- Permitido e esperado quando há múltiplas aplicações deployáveis.
- Códigos (slug) únicos por produto: `nexus-portal`, `nexus-api`, etc.
- Cada módulo tem upload/artefato **independente** na release.
- `destinoPacote` deve ser **único** por módulo para evitar sobrescrita no ZIP.
- Cliente pode contratar subset (ver `06`); módulos não contratados não entram na entrega.

### 7.9 Extensões de artefato
- Upload e download validam extensão contra `extensoesAceitas` (ou default do tipo).
- Extensão desconhecida → `422` com mensagem listando as permitidas.
- Um artefato ativo por módulo na release (reupload substitui o anterior).
- `padraoAsset` (pós-MVP) deve ser compatível com `extensoesAceitas` (validação na gravação da config).

---

## 8. Contratos de API

Ver detalhe completo nas seções acima (§5).

### Listar

```
GET /api/v1/release-orchestrator/produtos/{produtoId}/modulos
```

### Criar

```
POST /api/v1/release-orchestrator/produtos/{produtoId}/modulos
```

### Atualizar

```
PUT /api/v1/release-orchestrator/produtos/{produtoId}/modulos/{id}
```

### Reordenar

```
PUT /api/v1/release-orchestrator/produtos/{produtoId}/modulos/reordenar
```

### Toggle status

```
PATCH /api/v1/release-orchestrator/produtos/{produtoId}/modulos/{id}/status
```

### Excluir

```
DELETE /api/v1/release-orchestrator/produtos/{produtoId}/modulos/{id}
```

---

## 9. DTOs

Ver seções acima (§6).

---

## 10. Performance

### Tamanhos
- 5-10 módulos por produto típico.
- Render em página única.

---

## 11. Estados e edge cases

### Produto novo sem módulos
- Empty state: "Produto sem módulo. Cadastre o primeiro."
- CTA: "+ Novo módulo".

### Módulo com artefatos
- Botão excluir disabled. Tooltip: "Módulo tem artefatos. Inative em vez de excluir."

### Mudança de tipo bloqueada
- Form: select de tipo desabilitado se já usado em entrega.
- Tooltip explica.

### Config específica vazia
- Aceitar e usar defaults do tipo.

---

## 12. Acessibilidade

- Drag handle com `aria-label="Reordenar"`.
- Modal de form com foco trap.
- Tipo select com descrição (`aria-describedby`).

---

## 13. Auditoria

| Ação | Detalhes |
|---|---|
| `MODULO_PRODUTO_CRIADO` | snapshot |
| `MODULO_PRODUTO_EDITADO` | diff |
| `MODULO_PRODUTO_REORDENADO` | ordens antes/depois |
| `MODULO_PRODUTO_STATUS_ALTERADO` | de → para |
| `MODULO_PRODUTO_EXCLUIDO` | snapshot |

---

## 14. Cross-reference

- [`08-produtos-lista.md`](08-produtos-lista.md) — Listagem.
- [`09-produtos-cadastro.md`](09-produtos-cadastro.md) — Cadastro do produto.
- [`19-selecao-modulos.md`](19-selecao-modulos.md) — Onde módulos aparecem na entrega.
- [`21-geracao-pacote.md`](21-geracao-pacote.md) — Como tipo afeta empacotamento.
- Spec técnica de módulos e artefatos — este documento.
- [`11-produtos-catalogo-funcional.md`](11-produtos-catalogo-funcional.md) — Catálogo funcional (domínios/funcionalidades por produto).
- [`14-release-orchestrator-detalhe.md`](14-release-orchestrator-detalhe.md) — Upload de artefatos na release.
- [`99-padroes-tela.md`](99-padroes-tela.md) — Padrões.
