# 02 — Clientes — Listagem

## 1. Papel da tela

Tela central de **consulta e administração de clientes** cadastrados no orchestrator. Ponto de entrada para todos os fluxos relacionados a um cliente específico (cadastro, configurações, histórico).

Acessível em `/orchestrator/clientes`. Atalho a partir do dashboard (`01-dashboard.md`).

---

## 2. Objetivos

| Objetivo | Como atende |
|---|---|
| Encontrar cliente rapidamente | Busca livre + filtros |
| Avaliar status operacional | Coluna de status + alertas visuais |
| Navegar para detalhes | Click na linha vai para `04-cliente-visao-geral.md` |
| Criar novo cliente | Botão "+ Novo cliente" |
| Acessar configurações | Ações inline ou contexto |
| Triagem (qual cliente precisa atenção?) | Indicadores de entrega atrasada, problemas |
| Comparar funcionalidades entre clientes | Atalho **Resumo funcional** → `05` §8 (filtro por produto) |

---

## 3. Layout

```text
┌────────────────────────────────────────────────────────────────────────┐
│ Clientes                                              [+ Novo cliente] │
├────────────────────────────────────────────────────────────────────────┤
│ 🔍 [Busca por nome, CNPJ, sigla...           ]                         │
│                                                                        │
│ Status: [Todos ▼]  Ambiente: [Todos ▼]  Banco: [Todos ▼]              │
│ Produto: [Todos ▼]                          [Limpar tudo]              │
├────────────────────────────────────────────────────────────────────────┤
│ ┌──────────────────────────────────────────────────────────────────┐  │
│ │ Cliente             │ CNPJ      │ Ambiente │ Produtos │ Status   │  │
│ ├──────────────────────────────────────────────────────────────────┤  │
│ │ 🟢 ACME LTDA        │ 12.345... │ Prod     │ 3        │ ATIVO    │  │
│ │ 🟡 Beta S.A. ⚠️     │ 98.765... │ Prod     │ 2        │ ATIVO    │  │
│ │ ⚪ Gamma Inc.       │ 11.111... │ Hom      │ 1        │ PAUSADO  │  │
│ │ ⚫ Delta Co.        │ 22.222... │ Prod     │ 0        │ ENCERRADO│  │
│ └──────────────────────────────────────────────────────────────────┘  │
│                                                                        │
│ Mostrando 1-15 de 47   [< 1 2 3 4 >]                                  │
└────────────────────────────────────────────────────────────────────────┘
```

Legenda dos ícones de status:
- 🟢 ATIVO sem alertas
- 🟡 ATIVO com alerta (entrega atrasada, credencial expirando, etc.)
- ⚪ PAUSADO
- ⚫ ENCERRADO

---

## 4. Filtros

### Busca livre
- Campo único.
- Pesquisa em: `nome`, `razaoSocial`, `cnpj`, `sigla`, `emailNotificacoes`.
- Match: `ILIKE %termo%` (case-insensitive).
- Debounce 400ms.
- Reset paginação ao mudar.

### Status
Multi-select. Opções:
- ATIVO (default selecionado)
- PAUSADO
- EM_ANALISE
- ENCERRADO

### Ambiente
Multi-select. Lista os ambientes únicos cadastrados nos clientes (PROD, HOM, DEV, etc.).

### Tipo de Banco
Multi-select. Lista distinct de `tipoBanco`: Oracle, PostgreSQL, SQL Server, MySQL.

### Produto
Multi-select. Lista produtos cadastrados. Filtro mostra clientes que contrataram pelo menos um dos produtos selecionados.

### Filtros avançados (collapse)
- **Possui entregas atrasadas**: checkbox.
- **Sem entrega há mais de N dias**: input numérico.
- **Cadastro entre datas**: date range.

### Persistência
- Filtros vão para querystring: `?status=ATIVO&produto=DTECLD&q=acme`.
- Resetam paginação para página 1.
- Botão "Limpar tudo" só aparece com ≥ 2 filtros aplicados.

---

## 5. Tabela

### Colunas

| Coluna | Conteúdo | Sortable | Width |
|---|---|---|---|
| (status visual) | Ícone de status | — | 40px |
| Cliente | Sigla + nome | ✅ (nome) | flex |
| CNPJ | Formatado XX.XXX.XXX/XXXX-XX | ✅ | 180px |
| Ambiente | PROD/HOM/DEV | — | 100px |
| Produtos | Contagem clicável | ✅ | 100px |
| Última entrega | Data relativa | ✅ | 120px |
| Status | Badge colorido | ✅ | 100px |
| Ações | Menu kebab | — | 60px |

### Sort default
`nome ASC`.

### Click na linha
- Click → vai para `04-cliente-visao-geral.md` (`/orchestrator/clientes/:id`).
- Click no número de produtos → filtra produtos do cliente.
- Click no badge de status → filtra listagem por esse status.

### Indicadores visuais por linha
- **Borda esquerda colorida** por status (verde, amarelo, cinza, preto).
- **Ícone de alerta ⚠️** na coluna nome se há alertas (próxima entrega atrasada, credencial expirando).
- **Opacidade reduzida** em clientes inativos (PAUSADO/ENCERRADO).

---

## 6. Ações

### Toolbar (topo da tela)
| Ação | Visível para | Comportamento |
|---|---|---|
| 🔍 Busca | Todos | Filtra inline |
| + Novo cliente | ADMIN, EDITOR | Vai para `03-clientes-cadastro.md` |
| Resumo funcional | ADMIN, EDITOR, LEITOR | Vai para `05` §8 — domínios/func. por cliente e produto |
| ⚙️ Exportar | ADMIN, EDITOR | Download CSV com filtros aplicados |

### Inline (por linha — menu kebab "⋮")
| Ação | Visível para | Comportamento |
|---|---|---|
| Visualizar | Todos | Vai para `04-cliente-visao-geral.md` |
| Editar | ADMIN, EDITOR | Vai para `03-clientes-cadastro.md` modo edit |
| Configurações de entrega | ADMIN | Vai para `07-cliente-configuracoes-entrega.md` |
| Domínios e funcionalidades | ADMIN, EDITOR | Vai para `05-cliente-dominios-funcionalidades.md` |
| Produtos contratados | ADMIN, EDITOR | Vai para `06-cliente-produtos-contratados.md` |
| Nova entrega para este cliente | ADMIN, EDITOR | Atalho para `18-nova-entrega-assistente.md` pré-preenchido |
| — | — | — |
| Pausar / Reativar | ADMIN | Toggle status (com confirm) |
| Histórico de entregas | Todos | Vai para `23-historico-entregas.md` filtrado |

### Permissões — resumo

| Ação | ADMIN | EDITOR | LEITOR |
|---|---|---|---|
| Listar e ver | ✅ | ✅ | ✅ |
| Criar | ✅ | ✅ | ❌ |
| Editar | ✅ | ✅ | ❌ |
| Configurar entrega | ✅ | ❌ | ❌ |
| Pausar / Reativar | ✅ | ❌ | ❌ |
| Encerrar | ✅ | ❌ | ❌ |
| Exportar | ✅ | ✅ | ❌ |

---

## 7. Regras de negócio

### Visibilidade
- Cliente **ATIVO** aparece por padrão.
- Cliente **PAUSADO/ENCERRADO** só aparece se filtro inclui.
- Cliente **EM_ANALISE** aparece com badge especial (cadastro recente, não validado ainda).

### Alertas visuais (⚠️)
São agregados de várias condições:
- Próxima entrega com `dataPlanejada` no passado e ainda `PLANEJADA/APROVADA`.
- Credencial GitHub/Jenkins/SFTP expirando em < 30 dias (pós-MVP).
- Última entrega com `GeracaoStatus.ERRO` não tratada.
- Cliente sem produto contratado (configuração incompleta).
- Cliente sem responsável técnico.

### Status do cliente
- **ATIVO**: pode receber novas entregas.
- **PAUSADO**: temporariamente sem entregas (cliente pediu pausa, problema operacional). Mantém histórico, pode ser reativado.
- **EM_ANALISE**: recém-cadastrado, ainda validando. Não pode receber entregas até virar ATIVO.
- **ENCERRADO**: cliente saiu definitivamente. Histórico mantido, mas oculto por default.

### Inativação
- Cliente inativo (PAUSADO/ENCERRADO) **aparece na consulta** (se filtro inclui) mas **não pode receber nova entrega**.
- Botões/ações de "Nova entrega" ficam desabilitados com tooltip.

### Exclusão
- **Não há exclusão física** de cliente. Apenas status `ENCERRADO`.
- Motivo: preservar histórico, auditoria, LGPD.

---

## 8. Contratos de API

### Listar clientes

```
GET /api/v1/orchestrator/clientes
```

#### Query params

| Param | Tipo | Default | Descrição |
|---|---|---|---|
| q | String | — | Busca livre |
| status | List<StatusCliente> | [ATIVO] | Filtro de status |
| ambiente | List<String> | — | Ambientes |
| tipoBanco | List<String> | — | Tipos de banco |
| produtoId | List<UUID> | — | Filtro por produto contratado |
| temAlertas | Boolean | — | Apenas com alertas |
| semEntregaHaDias | Integer | — | Sem entrega nos últimos N dias |
| page | Integer | 1 | Página (1-based) |
| size | Integer | 15 | Itens por página |
| sort | String | nome | Campo de ordenação |
| direction | SortDirection | ASC | ASC ou DESC |

#### Response 200

```json
{
  "items": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "nome": "ACME LTDA",
      "razaoSocial": "ACME Comércio Ltda",
      "cnpj": "12345678000199",
      "cnpjFormatado": "12.345.678/0001-99",
      "sigla": "ACME",
      "ambientePadrao": "PROD",
      "tipoBanco": "Oracle",
      "status": "ATIVO",
      "totalProdutos": 3,
      "ultimaEntrega": {
        "id": "uuid",
        "dataPublicacao": "2026-05-15",
        "produtoSigla": "DTECLD",
        "versao": "1.4.0"
      },
      "proximaEntrega": {
        "id": "uuid",
        "dataPlanejada": "2026-06-01",
        "status": "APROVADA"
      },
      "alertas": [
        { "tipo": "ENTREGA_ATRASADA", "severidade": "WARN", "mensagem": "Próxima entrega planejada para 2026-05-20" }
      ],
      "createdAt": "2025-01-15T10:00:00Z",
      "updatedAt": "2026-05-15T16:30:00Z"
    }
  ],
  "page": 1,
  "size": 15,
  "totalElements": 47,
  "totalPages": 4
}
```

#### Errors

| Código | Causa |
|---|---|
| 400 | Filtro inválido (status desconhecido, etc.) |
| 401 | Sem autenticação |
| 403 | Sem role para listar |

### Toggle status

```
PATCH /api/v1/orchestrator/clientes/{id}/status
```

```json
{ "status": "PAUSADO", "motivo": "Cliente solicitou pausa para revisão de contrato" }
```

Registra `orchestrator_auditoria` (`acao=CLIENTE_STATUS_ALTERADO`, `detalhes.motivo`).

### Exportar CSV

```
GET /api/v1/orchestrator/clientes/export?formato=CSV
```

Aceita os mesmos filtros da listagem. Retorna stream `text/csv` com `Content-Disposition: attachment`.

---

## 9. DTOs

```java
public record ClienteListaResponse(
    UUID id,
    String nome,
    String razaoSocial,
    String cnpj,
    String cnpjFormatado,
    String sigla,
    String ambientePadrao,
    String tipoBanco,
    StatusCliente status,
    int totalProdutos,
    UltimaEntregaInfo ultimaEntrega,
    ProximaEntregaInfo proximaEntrega,
    List<AlertaCliente> alertas,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
    public record UltimaEntregaInfo(UUID id, LocalDate dataPublicacao, String produtoSigla, String versao) {}
    public record ProximaEntregaInfo(UUID id, LocalDate dataPlanejada, ProximaEntregaStatus status) {}
    public record AlertaCliente(TipoAlerta tipo, Severidade severidade, String mensagem) {}
}

public record AlterarStatusClienteRequest(
    @NotNull StatusCliente status,
    @Size(max = 500) String motivo
) {}
```

---

## 10. Performance

### Volumes esperados
- 50-500 clientes ativos.
- 5-20 entregas/mês por cliente.

### Otimizações
- `@EntityGraph` para evitar N+1 ao trazer última/próxima entrega.
- Cache de agregados (`totalProdutos`) com TTL 5 min.
- Alertas pré-computados via materialized view ou job.

### Queries críticas
```sql
-- Listagem agregada (rascunho)
SELECT c.*,
       (SELECT COUNT(*) FROM cliente_produto cp WHERE cp.cliente_id = c.id AND cp.ativo) AS total_produtos,
       (SELECT row_to_json(e) FROM (
           SELECT id, data_publicacao, produto_sigla, versao
           FROM entrega
           WHERE cliente_id = c.id AND status_geracao = 'CONCLUIDO'
           ORDER BY data_publicacao DESC LIMIT 1
       ) e) AS ultima_entrega
FROM cliente c
WHERE c.status IN (?, ?)
ORDER BY c.nome
LIMIT 15;
```

### Índices recomendados
- `cliente(status, nome)` — listagem comum.
- `cliente(cnpj)` — busca por CNPJ.
- `cliente(sigla)` — UNIQUE.

---

## 11. Erros, estados e edge cases

### Sem clientes ainda
- Empty state: "Nenhum cliente cadastrado ainda."
- CTA: "Cadastrar primeiro cliente".

### Filtro sem resultado
- "Nenhum cliente corresponde aos filtros."
- CTA: "Limpar filtros".

### Erro de carregamento
- Banner vermelho + retry.
- Mantém filtros aplicados.

### Sem permissão (403)
- Listagem invisível, mensagem "Sem permissão para listar clientes."

### Cliente com dados parciais
- Se faltar CNPJ ou ambiente (raro): exibe "—" com tooltip "Dados incompletos".

---

## 12. Acessibilidade

- Tabela com `<th scope="col">`.
- Sort buttons com `aria-sort`.
- Click row → keyboard equivalente (Enter na linha focada).
- Ícone de alerta com `aria-label="Cliente com alertas"`.
- Filtros com `<label>` associada.

---

## 13. Auditoria

Ações que vão para `orchestrator_auditoria`:
- `CLIENTE_LISTADO` (opcional, só se for crítico saber quem viu o quê).
- `CLIENTE_STATUS_ALTERADO`.
- `CLIENTE_EXPORTADO`.

Ações sem auditoria (consulta normal):
- Filtros, paginação, sorting.

---

## 14. Cross-reference

- [`03-clientes-cadastro.md`](03-clientes-cadastro.md) — Cadastro completo.
- [`04-cliente-visao-geral.md`](04-cliente-visao-geral.md) — Tela de detalhe.
- [`23-historico-entregas.md`](23-historico-entregas.md) — Histórico filtrado.
- [`18-nova-entrega-assistente.md`](18-nova-entrega-assistente.md) — Início de nova entrega.
- [`32-modelo-dados-sugerido.md`](32-modelo-dados-sugerido.md) — Schema da entidade Cliente.
- [`99-padroes-tela.md`](99-padroes-tela.md) — Padrões aplicáveis.
