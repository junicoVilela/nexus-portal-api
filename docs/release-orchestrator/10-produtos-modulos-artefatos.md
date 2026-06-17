# 10 — Produtos — Módulos

> **MVP**: todos os artefatos chegam via **upload manual na release** (ver `14-release-orchestrator-detalhe.md` e as seções abaixo). As colunas "Origem do artefato" e "Mecanismo de empacotamento" descritas abaixo refletem o desenho **pós-MVP** (com GitHub/Jenkins). No MVP, o operador é responsável por subir o artefato correspondente ao tipo de cada módulo, e a "config específica por tipo" (diretório do repo, padrão de tag, etc.) só é exigida em pós-MVP.

## 1. Papel da tela

**Cadastro livre dos módulos que compõem um produto**. Define o que pode ser empacotado e como o delta é calculado durante a geração da entrega.

Acessível em `/orchestrator/produtos/:produtoId/modulos`.

---

## 2. Conceito

- Cada produto tem N módulos cadastráveis.
- Cada módulo tem um **tipo** (categoria fixa do sistema) que determina como o pacote é montado.
- O cliente contrata módulos individualmente (ver `06-cliente-produtos-contratados.md`).

```text
Produto DTEC-LD
├─ Módulo dtec-web         (WEB)
├─ Módulo dtec-batch       (BATCH)
├─ Módulo dtec-db-ddl      (BANCO)
├─ Módulo dtec-db-dml      (BANCO)
├─ Módulo dtec-etl         (KETTLE)
├─ Módulo dtec-funcs       (FUNCIONALIDADES)
└─ Módulo dtec-regras      (REGRAS)
```

---

## 3. Tipos de módulo (fixos)

| Tipo | Origem do artefato | Mecanismo de empacotamento |
|---|---|---|
| `WEB` | GitHub Release asset (`.war`) — MVP: upload manual | Baixa o asset da `TO_TAG`. Substitui inteiro (sem delta). |
| `BATCH` | GitHub Release asset (`.jar`) — MVP: upload manual | Baixa o asset da `TO_TAG`. Substitui inteiro. |
| `BANCO` | Diretório de scripts SQL no repositório — MVP: upload `.zip`/`.sql` | Coleta SQL entre `FROM_TAG` e `TO_TAG`, ordena DDL → DML, gera `DDL.sql` e `DML.sql` unificados. **Gera delta.** |
| `KETTLE` | Diretório `.ktr`/`.kjb` no repositório — MVP: upload `.zip` | Coleta arquivos modificados entre `FROM_TAG` e `TO_TAG`. **Gera delta.** |
| `FUNCIONALIDADES` | Configuração do cliente (`05-cliente-dominios-funcionalidades.md`) | Gera scripts a partir das funcionalidades habilitadas. Sempre re-gera. |
| `REGRAS` | Configuração do cliente | Gera scripts de regras (matriz de permissões/grupos). |

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
│ < DTEC-LD       Módulos                                [+ Novo módulo] │
├────────────────────────────────────────────────────────────────────────┤
│ ☰ (drag para reordenar)                                                │
│ ┌──────────────────────────────────────────────────────────────────┐  │
│ │ Ord │ Status │ Código        │ Nome             │ Tipo  │ Δ │ ⚙️ │  │
│ ├──────────────────────────────────────────────────────────────────┤  │
│ │ 1   │ ✅     │ dtec-db-ddl   │ Banco DDL        │ BANCO │✓ │ ⋮  │  │
│ │ 2   │ ✅     │ dtec-db-dml   │ Banco DML        │ BANCO │✓ │ ⋮  │  │
│ │ 3   │ ✅     │ dtec-funcs    │ Funcionalidades  │ FUNC  │  │ ⋮  │  │
│ │ 4   │ ✅     │ dtec-regras   │ Regras           │ REGRAS│  │ ⋮  │  │
│ │ 5   │ ✅     │ dtec-web      │ Portal Web       │ WEB   │  │ ⋮  │  │
│ │ 6   │ ✅     │ dtec-batch    │ Processador      │ BATCH │  │ ⋮  │  │
│ │ 7   │ ❌     │ dtec-etl      │ ETL Kettle       │ KETTLE│✓ │ ⋮  │  │
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
| Código | Slug único no produto (ex.: `dtec-web`) |
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

#### WEB / BATCH (pós-MVP)
```json
{
  "padraoAsset": "*.war",
  "jenkinsJob": "dtec-web-build",
  "destinoPacote": "web/"
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
- Upload de artefatos na release — ver [`14-release-orchestrator-detalhe.md`](14-release-orchestrator-detalhe.md).
- [`99-padroes-tela.md`](99-padroes-tela.md) — Padrões.
