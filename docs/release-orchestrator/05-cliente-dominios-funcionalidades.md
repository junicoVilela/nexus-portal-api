# 05 — Cliente — Domínios e Funcionalidades

## 1. Papel da tela

**Matriz de configuração funcional do cliente**. Define quais ações dentro de cada área do sistema estão habilitadas para o cliente. Alimenta os módulos `FUNCIONALIDADES` e `REGRAS` no momento da geração do pacote.

Acessível em:
- `/orchestrator/clientes/:id/dominios-funcionalidades` (tela dedicada).
- Aba "Domínios" em `04-cliente-visao-geral.md`.

---

## 2. Conceito

### Domínio
Área funcional do sistema (definida no catálogo do **produto**).

Exemplos:
- Usuários
- Grupos de Acesso
- Clientes
- Produtos
- Relatórios
- Integrações
- Parâmetros

### Funcionalidade
Ação dentro do domínio (catálogo do produto).

Exemplos:
- Inserir, Atualizar, Bloquear, Desbloquear, Deletar
- Visualizar, Exportar, Importar
- Resetar Senha, Associar Perfil
- Aprovar, Rejeitar

### ClienteFuncionalidade
Vínculo entre cliente e funcionalidade que indica se está **habilitada** para esse cliente.

### O que significa “cliente possui domínio / funcionalidade”

| Termo | Significado | Persistência |
|---|---|---|
| **Possui funcionalidade** | `ClienteFuncionalidade.habilitada = true` | Linha explícita na matriz |
| **Possui domínio** | Pelo menos **uma** funcionalidade **habilitada** dentro desse domínio | **Derivado** (não exige tabela `ClienteDominio`) |
| **Não possui domínio** | Nenhuma funcionalidade habilitada naquele domínio | Domínio continua no catálogo do produto; some do pacote e dos resumos “possuídos” |

Cada cliente enxerga **todo o catálogo do produto** na tela de edição, mas **só leva no pacote** o que estiver habilitado. Dois clientes do mesmo produto podem ter conjuntos **totalmente diferentes**.

### Exemplo — mesmo produto, matrizes distintas

Catálogo do produto **NEXUS-LD** (`11`):

```text
Domínio A → funcionalidades 1, 2, 4
Domínio B → funcionalidades 1, 2, 3
Domínio C → funcionalidades 2, 3
Domínio T → funcionalidades 2, 3, 5
```

| Cliente | Domínios possuídos | Funcionalidades habilitadas | Observação |
|---|---|---|---|
| **Cliente X** | **A, B, C** | **1, 2** | Em A: só 1 e 2. Em B: 1 e 2 (não 3). Em C: 2. Domínio T: nenhuma → **não possui T** |
| **Cliente Y** | **T, B, C** | **2, 3** | Em T: 2 e 3. Em B/C: 2 e/ou 3 conforme toggles. Domínio A: nenhuma → **não possui A** |

Funcionalidade **2** pode estar habilitada nos dois clientes; **1** só no X; **3** só no Y. Domínios **B** e **C** podem ser compartilhados, mas com funcionalidades diferentes dentro deles.

```text
Catálogo (produto NEXUS-LD) — vocabulário completo
  A.{1,2,4}  B.{1,2,3}  C.{2,3}  T.{2,3,5}

Cliente X — subconjunto contratado
  A → {1✅, 2✅, 4❌}
  B → {1✅, 2✅, 3❌}
  C → {2✅, 3❌}
  T → (nenhuma habilitada)

Cliente Y — subconjunto contratado
  T → {2✅, 3✅, 5❌}
  B → {1❌, 2✅, 3✅}
  C → {2✅, 3✅}
  A → (nenhuma habilitada)
```

```text
Catálogo (por Produto) — exemplo genérico
  Domínio "Usuários"
   ├─ Funcionalidade "Inserir"
   ├─ Funcionalidade "Bloquear"
   ├─ Funcionalidade "Resetar Senha"
   └─ Funcionalidade "Exportar"

Cliente "ACME":
  ├─ Usuários.Inserir         ✅ habilitada
  ├─ Usuários.Bloquear        ❌ desabilitada
  ├─ Usuários.Resetar Senha   ✅ habilitada
  └─ Usuários.Exportar        ✅ habilitada
```

---

## 3. Layout

```text
┌────────────────────────────────────────────────────────────────────────┐
│ < Voltar para Cliente ACME                                             │
├────────────────────────────────────────────────────────────────────────┤
│ Domínios e Funcionalidades                       Produto: [NEXUS-LD ▼] │
├────────────────────────────────────────────────────────────────────────┤
│ Filtros: [Apenas possuídos] [Apenas desabilitadas] [Habilitadas] [Todas] │
│ 🔍 [Buscar funcionalidade...]                                          │
│ Resumo cliente: domínios A, B, C • func. 1, 2 (de 4 dom. / 12 func.)  │
├────────────────────────────────────────────────────────────────────────┤
│ ┌─ Domínios ──────────────┐ ┌─ Funcionalidades de "Usuários" ──────┐ │
│ │ Resumo: 32/48 habilit.   │ │ ✅ Inserir                            │ │
│ │                          │ │ ✅ Atualizar                          │ │
│ │ Usuários            8/10 │ │ ❌ Bloquear                           │ │
│ │ Grupos de Acesso    5/8  │ │ ❌ Desbloquear                        │ │
│ │ Clientes            6/6  │ │ ✅ Resetar Senha                      │ │
│ │ Produtos            3/6  │ │ ❌ Excluir                            │ │
│ │ Relatórios          4/8  │ │ ✅ Visualizar                         │ │
│ │ Integrações         3/5  │ │ ✅ Exportar                           │ │
│ │ Parâmetros          3/5  │ │ ❌ Importar                           │ │
│ │                          │ │ ✅ Associar Perfil                    │ │
│ └──────────────────────────┘ └───────────────────────────────────────┘ │
│                                                                        │
│ Ações em lote: [Habilitar todas]  [Desabilitar todas]  [Importar...]   │
├────────────────────────────────────────────────────────────────────────┤
│                                       [Reverter]  [Salvar alterações]  │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Seletor de produto

- Cliente pode ter múltiplos produtos contratados → seletor mostra cada um.
- Cada produto tem seu próprio catálogo de domínios/funcionalidades.
- Mudança de produto recarrega a matriz.
- Mudanças não salvas → confirm antes de trocar.

---

## 5. Painel esquerdo: Domínios

### Conteúdo por linha
- Nome do domínio.
- Contador "habilitadas / total" (ex.: `8/10`).
- Badge **Possuído** se `habilitadas ≥ 1`; caso contrário **Não contratado** (visual cinza).
- Barra de progresso visual.

### Filtro "Apenas possuídos"
- Mostra só domínios com ≥1 funcionalidade habilitada para o cliente.
- Útil para revisar o que entra no pacote (`FUNCIONALIDADES` / `REGRAS`).

### Estados visuais
- **Verde** se 100% habilitado.
- **Amarelo** se parcial.
- **Cinza** se 0% habilitado.

### Header do painel
- "Resumo total: X / Y funcionalidades habilitadas" (do produto inteiro).

### Click
- Click no domínio → carrega funcionalidades no painel direito.
- Default: primeiro domínio selecionado.

---

## 6. Painel direito: Funcionalidades

### Por funcionalidade
- Toggle (✅/❌).
- Nome.
- Descrição (tooltip ou expansível).
- Indicador de origem: `MANUAL`, `TEMPLATE`, `HERDADA`.

### Toggle individual
- Click marca local como sujo (não persistido ainda).
- Visual muda imediatamente.

### Ações em lote
- "Habilitar todas no domínio" / "Desabilitar todas" — desabilitar todas faz o cliente **deixar de possuir** aquele domínio.
- "Selecionar template" (aplicar conjunto pré-definido).
- "Copiar de outro cliente" (modal de seleção — ex.: copiar matriz do Cliente Y para novo cliente).

---

## 7. Componentes auxiliares

### Resumo total (toolbar)
- "Há 5 mudanças pendentes." (botão "Ver mudanças")
- Lista das mudanças antes de salvar.

### Filtros
- "Habilitadas" / "Desabilitadas" / "Todas".
- Busca textual filtra por nome de funcionalidade.

### Origem
- **MANUAL**: editada na tela.
- **TEMPLATE**: veio de template aplicado.
- **HERDADA**: default do produto.
- Ícone indica origem (com tooltip).

---

## 8. Regras de negócio

### Persistência
- Salvar é explícito (batch).
- Botão "Reverter" descarta mudanças.
- Sair com mudanças não salvas → confirm.

### Auditoria
- Cada mudança gera entrada em histórico do cliente: ação, funcionalidade, antes/depois, usuário.
- Mudança em massa: 1 entrada agregada + detalhes em `JSON`.

### Funcionalidades críticas
- Funcionalidades marcadas como `critica = true` (no catálogo) exigem **confirmação extra** para desabilitar.
- Ex.: "Excluir Usuário" — desabilitar exige confirm com motivo.

### Herança
- Templates de produto definem default.
- Cliente novo recebe defaults.
- Edições viram `MANUAL` (sobrescrevem).

### Versionamento
- Snapshot da matriz no momento da entrega.
- Histórico permite ver o estado em data passada.

### Subconjunto por cliente (geração de pacote)
- Scripts `FUNCIONALIDADES` incluem **somente** funcionalidades com `habilitada = true`.
- Domínios sem nenhuma funcionalidade habilitada **não geram** bloco no pacote.
- Dois clientes do mesmo produto podem receber pacotes com domínios e funcionalidades distintos (ex.: Cliente X ≠ Cliente Y na tabela §2).

### Visão consolidada (todos os clientes)

Rota complementar (read-only ou com drill-down para matriz):

`/orchestrator/clientes/resumo-funcionalidades?produtoId={produtoId}`

```text
┌────────────────────────────────────────────────────────────────────────┐
│ Resumo funcional — NEXUS-LD                         Produto: [NEXUS-LD ▼] │
├────────────────────────────────────────────────────────────────────────┤
│ Cliente      │ Domínios possuídos │ Funcionalidades (códigos) │ Ações  │
├────────────────────────────────────────────────────────────────────────┤
│ Cliente X    │ A, B, C            │ 1, 2                      │ [Abrir]│
│ Cliente Y    │ T, B, C            │ 2, 3                      │ [Abrir]│
│ ACME LTDA    │ A, B, T            │ 1, 2, 3, 5                │ [Abrir]│
└────────────────────────────────────────────────────────────────────────┘
```

- **Domínios possuídos**: códigos com ≥1 funcionalidade habilitada.
- **Funcionalidades**: códigos numéricos/slug **únicos** habilitados no cliente (podem repetir entre domínios no catálogo; lista exibe código da funcionalidade).
- Coluna clicável → abre matriz do cliente (`05`) já filtrada em "Apenas possuídos".
- Export CSV para auditoria comercial.

```
GET /api/v1/release-orchestrator/clientes/resumo-funcionalidades?produtoId={produtoId}
```

Response (trecho):
```json
{
  "produtoId": "uuid",
  "produtoSigla": "NEXUSLD",
  "clientes": [
    {
      "clienteId": "uuid-x",
      "clienteNome": "Cliente X",
      "dominiosPossuidos": ["a", "b", "c"],
      "funcionalidadesHabilitadas": ["1", "2"]
    },
    {
      "clienteId": "uuid-y",
      "clienteNome": "Cliente Y",
      "dominiosPossuidos": ["t", "b", "c"],
      "funcionalidadesHabilitadas": ["2", "3"]
    }
  ]
}
```

---

## 9. Contratos de API

### Buscar matriz

```
GET /api/v1/orchestrator/clientes/{id}/dominios-funcionalidades?produtoId={produtoId}
```

Response:
```json
{
  "produtoId": "uuid",
  "produtoSigla": "NEXUSLD",
  "resumo": {
    "habilitadas": 32,
    "total": 48,
    "dominiosPossuidos": 5,
    "totalDominios": 7,
    "codigosFuncionalidadesHabilitadas": ["1", "2", "inserir", "exportar"]
  },
  "dominios": [
    {
      "id": "uuid-dominio-usuarios",
      "nome": "Usuários",
      "codigo": "usuarios",
      "possuiDominio": true,
      "habilitadas": 8,
      "total": 10,
      "funcionalidades": [
        {
          "id": "uuid",
          "nome": "Inserir",
          "codigo": "inserir",
          "descricao": "Inserir novo usuário no sistema.",
          "critica": false,
          "habilitada": true,
          "origem": "TEMPLATE"
        },
        {
          "id": "uuid",
          "nome": "Excluir",
          "codigo": "excluir",
          "descricao": "Excluir permanentemente um usuário.",
          "critica": true,
          "habilitada": false,
          "origem": "MANUAL"
        }
      ]
    }
  ]
}
```

### Salvar alterações em lote

```
PUT /api/v1/orchestrator/clientes/{id}/dominios-funcionalidades?produtoId={produtoId}
```

```json
{
  "alteracoes": [
    { "funcionalidadeId": "uuid-1", "habilitada": true },
    { "funcionalidadeId": "uuid-2", "habilitada": false }
  ],
  "motivo": "Cliente solicitou bloqueio da exclusão de usuários."
}
```

Response 200: matriz atualizada.

### Aplicar template (futuro)

```
POST /api/v1/orchestrator/clientes/{id}/dominios-funcionalidades/aplicar-template
```

```json
{ "templateId": "uuid-template", "produtoId": "uuid" }
```

### Copiar de outro cliente

```
POST /api/v1/orchestrator/clientes/{id}/dominios-funcionalidades/copiar
```

```json
{ "clienteOrigemId": "uuid", "produtoId": "uuid", "sobrescrever": true }
```

---

## 10. DTOs

```java
public record MatrizFuncionalidadesResponse(
    UUID produtoId,
    String produtoSigla,
    Resumo resumo,
    List<DominioMatriz> dominios
) {
    public record Resumo(int habilitadas, int total) {}

    public record DominioMatriz(
        UUID id, String nome, String codigo,
        int habilitadas, int total,
        List<FuncionalidadeMatriz> funcionalidades
    ) {}

    public record FuncionalidadeMatriz(
        UUID id, String nome, String codigo, String descricao,
        boolean critica, boolean habilitada, OrigemHabilitacao origem
    ) {}
}

public record AlterarMatrizRequest(
    @NotEmpty @Valid List<Alteracao> alteracoes,
    @Size(max = 500) String motivo
) {
    public record Alteracao(
        @NotNull UUID funcionalidadeId,
        @NotNull Boolean habilitada
    ) {}
}
```

---

## 11. Integração com geração de pacote

Quando uma entrega é gerada, o pacote inclui os módulos `FUNCIONALIDADES` e `REGRAS`:

| Artefato | Conteúdo |
|---|---|
| `funcionalidades/00_ddl_tb_cliente_funcionalidade.sql` | DDL da tabela **nova** `TB_CLIENTE_FUNCIONALIDADE` — ver `11` §17.1 |
| `funcionalidades/01_catalogo_dominio_funcional.sql` | MERGE catálogo (`TB_DOMINIO_FUNCIONAL`, `TB_FUNCIONALIDADE`) — ver `11` §17.2 |
| `funcionalidades/02_cliente_{sigla}_habilitadas.sql` | `DELETE` + `INSERT` em `TB_CLIENTE_FUNCIONALIDADE` (ambiente dedicado; sigla só no nome do arquivo) — ver `11` §17.3 |
| `funcionalidades/manifest-funcionalidades.json` | Snapshot para auditoria/reentrega |
| `regras/*.sql` | Permissões/grupos (módulo `REGRAS`) |

Detalhes em [`21-geracao-pacote.md`](21-geracao-pacote.md) etapa 6 e [`11-produtos-catalogo-funcional.md`](11-produtos-catalogo-funcional.md) §17.

### Snapshot
- Ao gerar entrega: estado atual da matriz é **snapshotado** em `EntregaFuncionalidades` (sugestão de tabela).
- Permite reentregar com a mesma config mesmo se cliente alterar depois.

---

## 12. Performance

### Tamanhos
- Produto típico: 7 domínios × 8 funcionalidades = ~56 itens.
- Render em página única é factível.

### Otimizações
- Carregamento único (toda matriz).
- Sem chamadas individuais por toggle (apenas estado local).
- Save em batch (1 request com várias alterações).

---

## 13. Estados e edge cases

### Sem produto contratado
- Tela mostra: "Cliente não tem produto contratado. [Configurar produtos]".

### Catálogo do produto vazio
- "Produto não tem domínios/funcionalidades cadastradas. [Configurar catálogo do produto]" → link para [`11-produtos-catalogo-funcional.md`](11-produtos-catalogo-funcional.md).

### Mudança em massa
- "Habilitar todas no domínio" → marca local.
- Mostra contagem: "Habilitando 4 funcionalidades adicionais."

### Conflito de concorrência
- Outro usuário salvou enquanto este editava.
- Backend retorna `409 CONFLITO_VERSAO`.
- Frontend: "Outra alteração foi salva. Recarregue para ver."

### Reverter
- Restaura estado inicial (recarrega do servidor).
- Confirm: "Descartar todas as mudanças?".

---

## 14. Acessibilidade

- Toggle com `role="switch"` + `aria-checked`.
- Painéis separados com `aria-labelledby`.
- Atalho `Espaço` para toggle no item focado.
- Lista de mudanças pendentes anunciada via `aria-live`.

---

## 15. Auditoria

| Ação | Detalhes registrados |
|---|---|
| `MATRIZ_FUNCIONALIDADES_ALTERADA` | Lista de alterações, motivo, contagem |
| `MATRIZ_FUNCIONALIDADES_TEMPLATE_APLICADO` | TemplateId, total de mudanças |
| `MATRIZ_FUNCIONALIDADES_COPIADA` | ClienteOrigemId, total de mudanças |

Em `detalhes` (JSON): lista `[{funcionalidadeId, codigo, antes, depois}]`.

---

## 16. Cross-reference

- [`04-cliente-visao-geral.md`](04-cliente-visao-geral.md) — Tela mãe.
- [`06-cliente-produtos-contratados.md`](06-cliente-produtos-contratados.md) — Catálogo de produtos.
- [`11-produtos-catalogo-funcional.md`](11-produtos-catalogo-funcional.md) — Catálogo mestre de domínios/funcionalidades (por produto).
- [`10-produtos-modulos-artefatos.md`](10-produtos-modulos-artefatos.md) — Módulos de empacotamento (WEB/BATCH/…).
- [`21-geracao-pacote.md`](21-geracao-pacote.md) — Como matriz alimenta o pacote.
- [`32-modelo-dados-sugerido.md`](32-modelo-dados-sugerido.md) — Entidades.
- [`99-padroes-tela.md`](99-padroes-tela.md) — Padrões.
