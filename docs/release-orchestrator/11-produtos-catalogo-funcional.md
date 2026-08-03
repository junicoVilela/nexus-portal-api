# 11 — Produtos — Catálogo de Domínios e Funcionalidades

> **Estado**: 📋 Especificado — cadastro mestre por **produto**. A matriz por **cliente** (habilitar/desabilitar) continua em [`05-cliente-dominios-funcionalidades.md`](05-cliente-dominios-funcionalidades.md).  
> **Anexo de scripts:** estrutura Oracle MERGE documentada na **§17** (`TB_DOMINIO_FUNCIONAL`, `TB_FUNCIONALIDADE`, **`TB_CLIENTE_FUNCIONALIDADE`** — tabela nova a criar no banco destino).

## 1. Papel da tela

**Cadastro mestre** de domínios e funcionalidades de um produto (ex.: NEXUS-LD). Define o vocabulário funcional que:

- aparece na matriz de cada cliente (`05`);
- alimenta os módulos `FUNCIONALIDADES` e `REGRAS` na geração do pacote (`21`).

Acessível em:
- `/orchestrator/produtos/:produtoId/catalogo-funcional` (tela dedicada).
- Aba **Catálogo funcional** no detalhe do produto (junto a Módulos / Releases).
- Atalho a partir da matriz do cliente quando o catálogo estiver vazio (`05` §13).

---

## 2. Conceito e separação de responsabilidades

```text
CAMADA A — Catálogo (ESTA SPEC, por PRODUTO)
  Domínio "Usuários"
   ├─ Funcionalidade "Inserir"      (codigo: inserir)
   ├─ Funcionalidade "Bloquear"      (codigo: bloquear)
   └─ Funcionalidade "Exportar"      (codigo: exportar)

CAMADA B — Matriz (spec 05, por CLIENTE + produto)
  Cliente ACME + NEXUS-LD
   ├─ Usuários.Inserir    ✅ habilitada
   ├─ Usuários.Bloquear   ❌ desabilitada
   └─ Usuários.Exportar   ✅ habilitada
```

| O quê | Onde | Quem edita |
|---|---|---|
| Criar domínio / funcionalidade | **Catálogo do produto** (esta tela) | ADMIN, EDITOR |
| Habilitar/desabilitar para um cliente | Matriz do cliente (`05`) | ADMIN, EDITOR |
| RBAC do portal (`CLIENTE:LER`, etc.) | Módulo Segurança | Outro bounded context |

**Regra:** a matriz do cliente **não cria** domínios/funcionalidades novas — apenas referencia o catálogo do produto e define o **subconjunto habilitado** por cliente (ver exemplo Cliente X / Cliente Y em [`05`](05-cliente-dominios-funcionalidades.md) §2).

### Exemplo — catálogo completo vs subconjuntos por cliente

```text
CATÁLOGO DO PRODUTO (todos os clientes enxergam na edição)
  Domínio A → func 1, 2, 4
  Domínio B → func 1, 2, 3
  Domínio C → func 2, 3
  Domínio T → func 2, 3, 5

CLIENTE X possui          CLIENTE Y possui
  domínios: A, B, C         domínios: T, B, C
  funcs: 1, 2               funcs: 2, 3
```

Visão lado a lado de todos os clientes: [`05`](05-cliente-dominios-funcionalidades.md) §8 — rota `/orchestrator/clientes/resumo-funcionalidades`.

---

## 3. Layout

Master-detail em três colunas (referência visual: matriz do módulo Segurança).

```text
┌────────────────────────────────────────────────────────────────────────┐
│ < NEXUS-LD     Catálogo funcional                    [+ Domínio] [⋯]   │
├────────────────────────────────────────────────────────────────────────┤
│ 🔍 [Buscar domínio ou funcionalidade...]     Resumo: 7 dom. • 48 func. │
├────────────────────────────────────────────────────────────────────────┤
│ ┌─ Domínios ───────────┐ ┌─ Funcionalidades ─────────────────────────┐ │
│ │ ☰ Usuários      10   │ │ Domínio: Usuários          [+ Funcional.]│ │
│ │ ☰ Grupos Acesso  8   │ ├───────────────────────────────────────────┤ │
│ │ ☰ Clientes       6   │ │ ☰ Inserir      inserir    ✅ Ativa  [⋮]  │ │
│ │ ☰ Relatórios     8   │ │ ☰ Atualizar    atualizar  ✅ Ativa  [⋮]  │ │
│ │ ☰ Integrações    5   │ │ ☰ Excluir      excluir    ✅ Crítica [⋮]  │ │
│ │ ☰ Parâmetros     5   │ │ ☰ Exportar     exportar   ❌ Inativa [⋮]  │ │
│ └──────────────────────┘ └───────────────────────────────────────────┘ │
│ ┌─ Detalhe (painel inferior ou drawer) ─────────────────────────────┐ │
│ │ Funcionalidade: Excluir                                            │ │
│ │ Código: excluir • Ordem: 40 • Crítica: sim                         │ │
│ │ Descrição: Remove permanentemente um usuário do sistema.           │ │
│ │ Script (ref.): ver §17 — catálogo (tipo A) + habilitação cliente (tipo B) │ │
│ └────────────────────────────────────────────────────────────────────┘ │
│ ℹ️  Alterações no catálogo afetam novas entregas; entregas passadas    │
│    usam snapshot da matriz do cliente no momento da geração.           │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Domínio — campos e formulário

| Campo | Tipo | Obrigatório | Validação |
|---|---|---|---|
| Nome | text | ✅ | max 200 |
| Código (slug / modelo) | text | ✅ | regex `[a-z0-9_]+`, UNIQUE por produto, **imutável** — mapeia `NM_MODELO_DOMINIO` |
| Código legado | number | ✅ | `CD_DOMINIO_FUNCIONAL` — inteiro UNIQUE por produto, **imutável** |
| Descrição | textarea | ❌ | max 500 |
| Ordem | number | ❌ | default auto (última + 10) |
| Ativo | checkbox | ✅ | default true |

### Ações
- **+ Domínio** — modal ou inline.
- **Editar** — nome, descrição, ordem, ativo (código imutável).
- **Inativar** — preferido em vez de excluir se já usado em cliente ou entrega.
- **Excluir** — só se nenhuma `Funcionalidade` vinculada e nenhum `ClienteFuncionalidade` referencia funcionalidades do domínio.
- **Reordenar** — drag na lista de domínios.

---

## 5. Funcionalidade — campos e formulário

| Campo | Tipo | Obrigatório | Validação |
|---|---|---|---|
| Domínio | select (contexto) | ✅ | FK do produto |
| Nome | text | ✅ | max 200 — mapeia `NM_FUNCIONALIDADE` |
| Código (slug) | text | ✅ | regex `[a-z0-9_]+`, UNIQUE por domínio, **imutável** |
| Código legado | number | ✅ | `CD_FUNCIONALIDADE` — inteiro UNIQUE por produto, **imutável** |
| Código operação | text | ✅ | 1–3 chars — mapeia `CD_OPERACAO` (ex.: `P` = Pesquisar) |
| Descrição | textarea | ❌ | max 500 — mapeia `DS_FUNCIONALIDADE` |
| Crítica | checkbox | ✅ | default false — exige confirmação extra ao desabilitar na matriz (`05`) |
| Ordem | number | ❌ | default auto |
| Ativo | checkbox | ✅ | default true |

> **Export SQL:** o portal pode gerar o bloco MERGE Oracle (§17) a partir destes campos — botão "Exportar catálogo SQL" (fase 2).

### Ações
- **+ Funcionalidade** — no domínio selecionado.
- **Editar** — campos mutáveis acima.
- **Inativar / Excluir** — mesmas regras do domínio; exclusão bloqueada se `ClienteFuncionalidade` ou snapshot de entrega referenciar.
- **Reordenar** — drag dentro do domínio.

### Identificador composto (somente leitura)

Exibido na UI e usado em logs/scripts:

```text
{dominio.codigo}.{funcionalidade.codigo}
→ usuarios.inserir
```

---

## 6. Regras de negócio

### 6.1 Imutabilidade
- **Código** de domínio e funcionalidade imutável após criação.
- Renomear **nome** ou **descrição** não quebra vínculos.

### 6.2 Produto inativo
- Catálogo somente leitura se produto inativo.

### 6.3 Inativação vs exclusão
- **Inativar** funcionalidade: some da matriz padrão de clientes novos; clientes existentes mantêm estado até edição manual.
- **Inativar** domínio: inativa em cascata funcionalidades filhas (confirm).

### 6.4 Impacto em clientes
- Ao **criar** funcionalidade nova: opcional checkbox "Habilitar por padrão para clientes existentes" (default **false**).
- Ao **inativar** funcionalidade com clientes habilitados: warning com contagem + confirm.

### 6.5 Impacto em entregas
- Entregas já geradas usam **snapshot** — catálogo editado depois não altera pacotes passados.
- Reentrega usa matriz **atual** do cliente + catálogo **atual** (funcionalidades inativas ignoradas).

### 6.6 Unicidade
- `(produtoId, dominio.codigo)` único.
- `(dominioId, funcionalidade.codigo)` único.

### 6.7 Ordenação
- Domínios e funcionalidades ordenados por `ordem`, depois `nome`.

---

## 7. Importação e exportação (fase 2)

Reservado para quando a estrutura de scripts (§17) estiver fechada.

| Ação | Formato | Uso |
|---|---|---|
| Exportar catálogo | JSON / CSV | backup, diff entre ambientes |
| Importar catálogo | JSON | carga inicial ou sync com repo |
| Sincronizar do repo | job/manual | ler manifesto versionado no repositório do produto |

Até §17: cadastro manual via UI ou seed Flyway por produto piloto.

---

## 8. Contratos de API

Base: `/api/v1/release-orchestrator/produtos/{produtoId}`

### Listar catálogo completo

```
GET /api/v1/release-orchestrator/produtos/{produtoId}/catalogo-funcional
```

Response:
```json
{
  "produtoId": "uuid",
  "produtoSigla": "NEXUSLD",
  "resumo": { "dominios": 7, "funcionalidades": 48, "funcionalidadesAtivas": 45 },
  "dominios": [
    {
      "id": "uuid",
      "nome": "Via",
      "codigo": "via",
      "codigoLegado": 1,
      "descricao": "Domínio de vias.",
      "ordem": 10,
      "ativo": true,
      "totalFuncionalidades": 10,
      "funcionalidades": [
        {
          "id": "uuid",
          "nome": "Pesquisar",
          "codigo": "pesquisar",
          "codigoLegado": 2,
          "codigoOperacao": "P",
          "descricao": "Pesquisar registros de via.",
          "critica": false,
          "ordem": 10,
          "ativo": true,
          "codigoComposto": "via.pesquisar"
        }
      ]
    }
  ]
}
```

### Domínios — CRUD

```
POST   /api/v1/release-orchestrator/produtos/{produtoId}/dominios
PUT    /api/v1/release-orchestrator/produtos/{produtoId}/dominios/{id}
PATCH  /api/v1/release-orchestrator/produtos/{produtoId}/dominios/{id}/status
DELETE /api/v1/release-orchestrator/produtos/{produtoId}/dominios/{id}
PUT    /api/v1/release-orchestrator/produtos/{produtoId}/dominios/reordenar
```

### Funcionalidades — CRUD

```
POST   /api/v1/release-orchestrator/produtos/{produtoId}/dominios/{dominioId}/funcionalidades
PUT    /api/v1/release-orchestrator/produtos/{produtoId}/funcionalidades/{id}
PATCH  /api/v1/release-orchestrator/produtos/{produtoId}/funcionalidades/{id}/status
DELETE /api/v1/release-orchestrator/produtos/{produtoId}/funcionalidades/{id}
PUT    /api/v1/release-orchestrator/produtos/{produtoId}/dominios/{dominioId}/funcionalidades/reordenar
```

### Reordenar (body)

```json
{
  "itens": [
    { "id": "uuid-1", "ordem": 10 },
    { "id": "uuid-2", "ordem": 20 }
  ]
}
```

---

## 9. DTOs

```java
public record CatalogoFuncionalResponse(
    UUID produtoId,
    String produtoSigla,
    ResumoCatalogo resumo,
    List<DominioCatalogoResponse> dominios
) {
    public record ResumoCatalogo(int dominios, int funcionalidades, int funcionalidadesAtivas) {}
}

public record DominioRequest(
    @NotBlank @Size(max = 200) String nome,
    @NotBlank @Pattern(regexp = "[a-z0-9_]+") String codigo,
    @NotNull Integer codigoLegado,
    @Size(max = 500) String descricao,
    Integer ordem,
    @NotNull Boolean ativo
) {}

public record FuncionalidadeRequest(
    @NotBlank @Size(max = 200) String nome,
    @NotBlank @Pattern(regexp = "[a-z0-9_]+") String codigo,
    @NotNull Integer codigoLegado,
    @NotBlank @Size(max = 3) String codigoOperacao,
    @Size(max = 500) String descricao,
    @NotNull Boolean critica,
    Integer ordem,
    @NotNull Boolean ativo,
    Boolean habilitarClientesExistentes
) {}
```

---

## 10. Integração com geração de pacote

Na etapa **Gerar funcionalidades** (`21`):

1. Carrega matriz do cliente para o produto (`ClienteFuncionalidade`).
2. Resolve metadados no catálogo (`Dominio`, `Funcionalidade` ativos).
3. Gera scripts conforme **§17** (tipo A catálogo + tipo B habilitação na `TB_CLIENTE_FUNCIONALIDADE`).
4. Combina com módulo `REGRAS` (grupos/permissões do cliente).

Campos do catálogo usados na geração:

| Campo | Uso na geração |
|---|---|
| `codigoComposto` | chave no manifest e logs (`via.pesquisar`) |
| `codigoLegado` | `CD_FUNCIONALIDADE` / `CD_DOMINIO_FUNCIONAL` nos MERGE |
| `codigoOperacao` | `CD_OPERACAO` no MERGE de funcionalidade |
| `critica` | validação — não desabilitar em massa sem audit |
| `ativo` | funcionalidades inativas ignoradas na geração |

---

## 11. Performance

- Produto típico: 7 domínios × ~8 funcionalidades ≈ 56 itens — uma única carga GET.
- Edições granulares (POST/PUT por item); sem round-trip por toggle.
- Busca client-side ou query `q=` no GET quando catálogo > 200 itens.

---

## 12. Estados e edge cases

### Catálogo vazio
- Empty state: "Nenhum domínio cadastrado. [+ Criar primeiro domínio]".
- Link na matriz do cliente (`05`) aponta para esta tela.

### Domínio sem funcionalidades
- Permitido temporariamente; warning na listagem.
- Bloqueia publicação de entrega se módulo `FUNCIONALIDADES` for obrigatório e catálogo efetivo vazio.

### Código duplicado
- `409 CODIGO_DUPLICADO` com sugestão de slug alternativo.

### Exclusão bloqueada
- Tooltip: "Funcionalidade em uso por N clientes ou em M entregas. Inative em vez de excluir."

---

## 13. Acessibilidade

- Listas com `role="listbox"` / itens com `role="option"`.
- Drag handle com `aria-label="Reordenar domínio"` / `aria-label="Reordenar funcionalidade"`.
- Badge "Crítica" com `aria-describedby` explicando impacto na matriz do cliente.

---

## 14. Auditoria

| Ação | Detalhes |
|---|---|
| `DOMINIO_CATALOGO_CRIADO` | snapshot |
| `DOMINIO_CATALOGO_EDITADO` | diff |
| `DOMINIO_CATALOGO_STATUS_ALTERADO` | de → para |
| `DOMINIO_CATALOGO_EXCLUIDO` | snapshot |
| `DOMINIO_CATALOGO_REORDENADO` | ordens antes/depois |
| `FUNCIONALIDADE_CATALOGO_CRIADA` | snapshot + dominioId |
| `FUNCIONALIDADE_CATALOGO_EDITADA` | diff |
| `FUNCIONALIDADE_CATALOGO_STATUS_ALTERADA` | de → para |
| `FUNCIONALIDADE_CATALOGO_EXCLUIDA` | snapshot |
| `FUNCIONALIDADE_CATALOGO_REORDENADA` | ordens antes/depois |

---

## 15. Permissões

| Ação | Roles |
|---|---|
| Visualizar catálogo | ADMIN, EDITOR, LEITOR |
| Criar/editar/inativar | ADMIN, EDITOR |
| Excluir | ADMIN |

---

## 16. Cross-reference

- [`05-cliente-dominios-funcionalidades.md`](05-cliente-dominios-funcionalidades.md) — Matriz por cliente (habilitar/desabilitar).
- [`08-produtos-lista.md`](08-produtos-lista.md) — Entrada produtos.
- [`09-produtos-cadastro.md`](09-produtos-cadastro.md) — Identidade do produto.
- [`10-produtos-modulos-artefatos.md`](10-produtos-modulos-artefatos.md) — Módulos WEB/BATCH/BANCO (empacotamento).
- [`21-geracao-pacote.md`](21-geracao-pacote.md) — Consumo na entrega.
- [`32-modelo-dados-sugerido.md`](32-modelo-dados-sugerido.md) — Entidades `Dominio`, `Funcionalidade`.
- [`99-padroes-tela.md`](99-padroes-tela.md) — Padrões de UI.

---

## 17. Anexo — Estrutura de scripts Oracle (catálogo + cliente)

> **Dialeto:** Oracle (MERGE + `DUAL`). Produtos que usam SQL Server terão template equivalente (`MERGE` / `IF NOT EXISTS`) — mesmos campos lógicos.  
> **Deploy:** cada **ambiente** (DEV, HML, PRD) é de **um único cliente** — banco dedicado; a tabela de habilitação **não** precisa identificar tenant.

### 17.1 Tabelas no banco destino

| Tabela | Origem | Papel |
|---|---|---|
| `TB_DOMINIO_FUNCIONAL` | **Legado** (já existe no produto) | Catálogo de domínios |
| `TB_FUNCIONALIDADE` | **Legado** (já existe no produto) | Catálogo de funcionalidades |
| `TB_CLIENTE_FUNCIONALIDADE` | **Nova** — criada pelo time para facilitar entregas | Funcionalidades habilitadas neste ambiente (espelha matriz `05` do cliente dono do ambiente) |

> **Importante:** não existe hoje tabela de vínculo no banco do produto. A `TB_CLIENTE_FUNCIONALIDADE` é **proposta nova**. Como **cada ambiente pertence a um único cliente**, a tabela guarda apenas `CD_FUNCIONALIDADE` — sem coluna de sigla/tenant. O orchestrator gera o script tipo B a partir de `ClienteFuncionalidade` no portal; a sigla do cliente aparece no **nome do arquivo** e no **manifest JSON** para auditoria.

#### `TB_DOMINIO_FUNCIONAL` (legado)

| Coluna | Origem no portal | Exemplo |
|---|---|---|
| `CD_DOMINIO_FUNCIONAL` | `Dominio.codigoLegado` | `1` |
| `NM_DOMINIO_FUNCIONAL` | `Dominio.nome` | `Via` |
| `NM_MODELO_DOMINIO` | `Dominio.codigo` | `via` |

#### `TB_FUNCIONALIDADE`

| Coluna | Origem no portal | Exemplo |
|---|---|---|
| `CD_FUNCIONALIDADE` | `Funcionalidade.codigoLegado` | `2` |
| `NM_FUNCIONALIDADE` | `Funcionalidade.nome` | `Pesquisar` |
| `DS_FUNCIONALIDADE` | `Funcionalidade.descricao` | `Pesquisar` |
| `CD_OPERACAO` | `Funcionalidade.codigoOperacao` | `P` |
| `CD_DOMINIO_FUNCIONAL` | `Dominio.codigoLegado` (FK) | `1` |

#### `TB_CLIENTE_FUNCIONALIDADE` (nova — single-tenant)

Espelha `ClienteFuncionalidade.habilitada = true` do portal para **o cliente dono daquele ambiente**. Uma linha = uma funcionalidade habilitada.

| Coluna | Origem no portal | Exemplo | Notas |
|---|---|---|---|
| `CD_FUNCIONALIDADE` | `Funcionalidade.codigoLegado` | `2` | PK; FK → `TB_FUNCIONALIDADE` |

**DDL (Oracle):**

```sql
CREATE TABLE TB_CLIENTE_FUNCIONALIDADE (
  CD_FUNCIONALIDADE  NUMBER  NOT NULL,
  CONSTRAINT PK_CLIENTE_FUNCIONALIDADE
    PRIMARY KEY (CD_FUNCIONALIDADE),
  CONSTRAINT FK_CLIENTE_FUNC_FUNCIONALIDADE
    FOREIGN KEY (CD_FUNCIONALIDADE) REFERENCES TB_FUNCIONALIDADE (CD_FUNCIONALIDADE)
);
```

- Script de criação no pacote (primeira entrega ou quando ainda não existir): `funcionalidades/00_ddl_tb_cliente_funcionalidade.sql` — idempotente (`EXECUTE IMMEDIATE` ou checagem em `USER_TABLES`).
- Domínios “possuídos” pelo cliente continuam **derivados** no portal (`05` §2); no banco destino, basta a lista de `CD_FUNCIONALIDADE` habilitadas.
- **Não** incluir `NM_SIGLA_CLIENTE` — o ambiente já **é** o cliente.

### 17.2 Script tipo A — Catálogo mestre (domínio + funcionalidades)

Idempotente via `MERGE`. Um bloco por domínio; funcionalidades agrupadas abaixo do domínio.

**Exemplo oficial (Domínio 1 — Via):**

```sql
/* =========================
   DOMÍNIO 1 — Via (via)
   ========================= */
MERGE INTO TB_DOMINIO_FUNCIONAL P USING
  (SELECT  1 CD_DOMINIO_FUNCIONAL, 'Via' NM_DOMINIO_FUNCIONAL, 'via'  NM_MODELO_DOMINIO FROM DUAL) A
  ON (A.CD_DOMINIO_FUNCIONAL = P.CD_DOMINIO_FUNCIONAL)
WHEN MATCHED THEN
  UPDATE SET
    P.NM_DOMINIO_FUNCIONAL = A.NM_DOMINIO_FUNCIONAL,
    P.NM_MODELO_DOMINIO    = A.NM_MODELO_DOMINIO
WHEN NOT MATCHED THEN
  INSERT (P.CD_DOMINIO_FUNCIONAL, P.NM_DOMINIO_FUNCIONAL, P.NM_MODELO_DOMINIO)
  VALUES (A.CD_DOMINIO_FUNCIONAL, A.NM_DOMINIO_FUNCIONAL, A.NM_MODELO_DOMINIO);

/* Funcionalidades do Domínio 1 */
MERGE INTO TB_FUNCIONALIDADE P USING
  (SELECT  2 CD_FUNCIONALIDADE, 'Pesquisar' NM_FUNCIONALIDADE, 'Pesquisar' DS_FUNCIONALIDADE,
          'P' CD_OPERACAO,  1 CD_DOMINIO_FUNCIONAL FROM DUAL) A
  ON (A.CD_FUNCIONALIDADE = P.CD_FUNCIONALIDADE)
WHEN MATCHED THEN
  UPDATE SET
    P.NM_FUNCIONALIDADE     = A.NM_FUNCIONALIDADE,
    P.DS_FUNCIONALIDADE     = A.DS_FUNCIONALIDADE,
    P.CD_OPERACAO           = A.CD_OPERACAO,
    P.CD_DOMINIO_FUNCIONAL  = A.CD_DOMINIO_FUNCIONAL
WHEN NOT MATCHED THEN
  INSERT (P.CD_FUNCIONALIDADE, P.NM_FUNCIONALIDADE, P.DS_FUNCIONALIDADE, P.CD_OPERACAO, P.CD_DOMINIO_FUNCIONAL)
  VALUES (A.CD_FUNCIONALIDADE, A.NM_FUNCIONALIDADE, A.DS_FUNCIONALIDADE, A.CD_OPERACAO, A.CD_DOMINIO_FUNCIONAL);
```

**Regras de geração (orchestrator ou export manual):**

- Domínios ativos primeiro (ordenados por `ordem`, depois `codigoLegado`).
- Funcionalidades ativas de cada domínio em seguida.
- `WHEN MATCHED THEN UPDATE` em funcionalidades inclui todos os campos mutáveis (não só INSERT).
- Comentário de cabeçalho: `DOMÍNIO {codigoLegado} — {nome} ({codigo})`.
- Arquivo sugerido no pacote: `funcionalidades/01_catalogo_dominio_funcional.sql`.

### 17.3 Script tipo B — Habilitação (matriz `05`)

Gerado na **etapa 6** da entrega (`21`) a partir de `ClienteFuncionalidade` (`habilitada = true`).

- Inclui **somente** funcionalidades habilitadas para o cliente da entrega.
- O script roda no **banco dedicado** daquele cliente/ambiente — substitui o conteúdo inteiro da tabela.
- Arquivo sugerido: `funcionalidades/02_cliente_{sigla}_habilitadas.sql` (sigla só para rastreio no pacote; **não** vai para o SQL).
- Estratégia: **`DELETE` + `INSERT`** (estado final idempotente).

**Exemplo oficial (cliente ACME no portal → PRD ACME):**

```sql
/* Funcionalidades habilitadas — ambiente dedicado ACME */
DELETE FROM TB_CLIENTE_FUNCIONALIDADE;

INSERT INTO TB_CLIENTE_FUNCIONALIDADE (CD_FUNCIONALIDADE) VALUES (2);
-- ... uma linha por funcionalidade habilitada na matriz
```

#### 17.3.1 Alternativa MERGE (linha a linha)

Preferir **DELETE + INSERT** (§17.3). Se usar MERGE, ainda é necessário `DELETE` das funcionalidades removidas da matriz:

```sql
MERGE INTO TB_CLIENTE_FUNCIONALIDADE P USING
  (SELECT 2 CD_FUNCIONALIDADE FROM DUAL) A
  ON (A.CD_FUNCIONALIDADE = P.CD_FUNCIONALIDADE)
WHEN NOT MATCHED THEN
  INSERT (CD_FUNCIONALIDADE) VALUES (A.CD_FUNCIONALIDADE);
```

### 17.4 Layout do pacote (módulo `FUNCIONALIDADES`)

```text
pacote/
├── funcionalidades/
│   ├── 00_ddl_tb_cliente_funcionalidade.sql  ← DDL (se tabela ainda não existir)
│   ├── 01_catalogo_dominio_funcional.sql     ← tipo A (MERGE catálogo)
│   ├── 02_cliente_{sigla}_habilitadas.sql    ← tipo B (subset cliente)
│   └── manifest-funcionalidades.json         ← auditoria / reentrega
├── regras/
│   └── ...                                  ← módulo REGRAS (spec 21 §6)
└── manifest.json
```

### 17.5 `manifest-funcionalidades.json` (gerado)

```json
{
  "clienteSigla": "ACME",
  "produtoSigla": "NEXUSLD",
  "dominiosPossuidos": ["via", "usuarios"],
  "funcionalidadesHabilitadas": [
    { "cdFuncionalidade": 2, "cdDominioFuncional": 1, "codigoOperacao": "P", "codigoComposto": "via.pesquisar" }
  ]
}
```

### 17.6 Ordem de execução

1. `00_ddl_tb_cliente_funcionalidade.sql` — cria `TB_CLIENTE_FUNCIONALIDADE` se necessário (nova tabela).
2. `01_catalogo_dominio_funcional.sql` — garante domínios e funcionalidades no banco destino.
3. `02_cliente_*_habilitadas.sql` — aplica subset habilitado na matriz.
4. Scripts `regras/` — permissões/grupos (módulo `REGRAS`).

### 17.7 Convenções

| Regra | Detalhe |
|---|---|
| Ambiente = cliente | Um banco por cliente/ambiente; `TB_CLIENTE_FUNCIONALIDADE` sem coluna tenant |
| IDs legados | `codigoLegado` numérico estável — **nunca reutilizar** após publicado em produção |
| `CD_OPERACAO` | Curto, único no contexto do domínio (ex.: `P`, `I`, `A`, `E`) |
| Slug vs legado | `codigo` (portal) = `NM_MODELO_DOMINIO`; não confundir com `CD_*` |
| Import §7 | JSON exportável espelha campos acima para carga inicial ou diff entre ambientes |

### 17.8 Pendências

- [ ] Mapeamento módulo **REGRAS** ↔ tabelas de permissão/grupo no banco cliente.
- [ ] Variante **SQL Server** dos templates (DDL + MERGE).

