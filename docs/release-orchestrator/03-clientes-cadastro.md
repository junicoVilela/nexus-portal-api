# 03 — Clientes — Cadastro

## 1. Papel da tela

Tela de **cadastro e edição de cliente** com todas as informações necessárias para iniciar entregas. Atende tanto criação inicial quanto edição posterior.

Acessível em:
- `/orchestrator/clientes/novo` (criação).
- `/orchestrator/clientes/:id/editar` (edição).

---

## 2. Objetivos

| Objetivo | Como atende |
|---|---|
| Registrar dados cadastrais | Seção "Dados gerais" |
| Registrar contatos técnicos | Seção "Contatos" |
| Definir ambiente e banco | Seção "Ambiente" |
| Definir destino de entrega (high-level) | Seção "Entrega" (detalhe completo em `07-cliente-configuracoes-entrega.md`) |
| Associar produtos contratados | Seção "Produtos" (detalhe em `06-cliente-produtos-contratados.md`) |
| Criar base para domínios e funcionalidades | Não nesta tela — vai para `05-cliente-dominios-funcionalidades.md` após salvar |

---

## 3. Layout

```text
┌────────────────────────────────────────────────────────────────────────┐
│ < Voltar           Novo Cliente                          [Salvar]      │
├────────────────────────────────────────────────────────────────────────┤
│ ┌─ Dados Gerais ──────────────────────────────────────────────────┐  │
│ │ Nome*                Razão Social*           CNPJ*                │  │
│ │ [_______________]    [_____________]         [__.___.___/____-__] │  │
│ │                                                                   │  │
│ │ Sigla*               Status*                 Responsável comerc.  │  │
│ │ [______]             [Ativo ▼]               [Select usuário ▼]   │  │
│ └──────────────────────────────────────────────────────────────────┘  │
│                                                                        │
│ ┌─ Contatos ──────────────────────────────────────────────────────┐  │
│ │ + Adicionar contato                                              │  │
│ │ ┌─ Contato 1 ────────────────────────────────────────┐ [Remover] │  │
│ │ │ Nome*       Papel*         Email*       Telefone   │           │  │
│ │ │ [_______]   [Técnico ▼]   [_______]    [_______]   │           │  │
│ │ └────────────────────────────────────────────────────┘           │  │
│ │                                                                   │  │
│ │ E-mail de notificações* (separar por vírgula)                    │  │
│ │ [_____________________________________________________________]  │  │
│ └──────────────────────────────────────────────────────────────────┘  │
│                                                                        │
│ ┌─ Ambiente ──────────────────────────────────────────────────────┐  │
│ │ Ambiente padrão*    Tipo de banco*       Codificação             │  │
│ │ [PROD ▼]            [Oracle ▼]           [UTF-8 ▼]               │  │
│ │                                                                   │  │
│ │ Fuso horário                                                     │  │
│ │ [America/Sao_Paulo ▼]                                            │  │
│ └──────────────────────────────────────────────────────────────────┘  │
│                                                                        │
│ ┌─ Entrega ───────────────────────────────────────────────────────┐  │
│ │ Tipo de destino*                                                 │  │
│ │ [Pasta local ▼]                                                  │  │
│ │ Caminho base*                                                    │  │
│ │ [/var/lib/nexus/entregas/cliente-acme]                          │  │
│ │                                                                   │  │
│ │ ℹ️  Configurações avançadas após salvar.                          │  │
│ └──────────────────────────────────────────────────────────────────┘  │
│                                                                        │
│ ┌─ Produtos Contratados ──────────────────────────────────────────┐  │
│ │ Selecione os produtos que este cliente possui:                   │  │
│ │ [✅] NEXUS-LD   Ambiente: [PROD]                                  │  │
│ │ [ ] FOLHA-WEB                                                    │  │
│ │ [✅] CONTAS    Ambiente: [HOM]                                   │  │
│ │                                                                   │  │
│ │ ℹ️  Módulos por produto serão configurados depois.                │  │
│ └──────────────────────────────────────────────────────────────────┘  │
│                                                                        │
│              [Cancelar]   [Salvar e continuar]   [Salvar]              │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Seções e campos

### 4.1 Dados Gerais

| Campo | Tipo | Obrigatório | Validação |
|---|---|---|---|
| Nome | text | ✅ | max 200, trim |
| Razão Social | text | ✅ | max 200 |
| CNPJ | text masked | ✅ | regex CNPJ válido, dígito verificador |
| Sigla | text | ✅ | max 20, [A-Z0-9-]+, UNIQUE (case-insensitive) |
| Status | select | ✅ | enum StatusCliente |
| Responsável comercial | select usuário | ❌ | FK a usuário (futuro) |

**CNPJ:**
- Mask `__.___.___/____-__` no frontend.
- Backend armazena só dígitos (`12345678000199`).
- Validação: 14 dígitos + dígito verificador.
- Unicidade global.

**Sigla:**
- Normalizada para uppercase.
- Slug-friendly (sem espaços, acentos).
- Aparece em UI e em logs ("entrega para ACME").

**Status:**
- Default: `EM_ANALISE` em criação.
- Após validação de cadastro completo: muda para `ATIVO`.
- Transições controladas (ver `02-clientes-lista.md` §7).

### 4.2 Contatos

Lista de `ClienteContato` (1..N). Pelo menos 1 obrigatório.

| Campo | Tipo | Obrigatório | Validação |
|---|---|---|---|
| Nome | text | ✅ | max 120 |
| Papel | select | ✅ | TÉCNICO, COMERCIAL, FINANCEIRO, OUTRO |
| Email | email | ✅ | formato e-mail, max 200 |
| Telefone | text | ❌ | máx 30 |

**E-mail de notificações** (campo separado no Cliente):
- Lista de e-mails separados por vírgula.
- Para onde vão notificações automáticas (entrega concluída, falha, etc.).
- Pode ser diferente dos contatos individuais (ex.: lista de distribuição).

### 4.3 Ambiente

| Campo | Tipo | Obrigatório | Validação |
|---|---|---|---|
| Ambiente padrão | select | ✅ | PROD, HOM, DEV, QA (configurável) |
| Tipo de banco | select | ✅ | Oracle, PostgreSQL, SQL Server, MySQL |
| Codificação | select | ❌ | UTF-8 (default), Latin-1, etc. |
| Fuso horário | select | ❌ | TZ database, default America/Sao_Paulo |

### 4.4 Entrega (resumida)

Apenas o suficiente para criar o cliente:

| Campo | Tipo | Obrigatório | Validação |
|---|---|---|---|
| Tipo de destino | select | ✅ | PASTA (MVP), FTP, SFTP, BUCKET (pós-MVP) |
| Caminho base | text | ✅ se PASTA | path válido |

Outros campos (janelas, credenciais, aprovação) ficam em `07-cliente-configuracoes-entrega.md`. Tela mostra mensagem "Configurações avançadas após salvar".

### 4.5 Produtos Contratados (high-level)

Lista de produtos cadastrados com checkbox. Cada selecionado pede:
- Ambiente onde está instalado (default = ambientePadrao do cliente).

Módulos por produto (com versão atual) ficam em `06-cliente-produtos-contratados.md` — disponível após salvar.

---

## 5. Botões

### Cancelar
- Volta para `02-clientes-lista.md`.
- Se form sujo: modal "Descartar mudanças?".

### Salvar
- Persiste cliente + contatos + produtos.
- Se sucesso: redireciona para `04-cliente-visao-geral.md` (modo edit redireciona para de onde veio).
- Toast "Cliente salvo".

### Salvar e continuar
- Persiste e fica na mesma tela.
- Útil para edição rápida.
- Útil em criação para depois ir configurar domínios/produtos sem voltar.

---

## 6. Regras de negócio

### 6.1 Criação

- **CNPJ obrigatório e único globalmente**.
- **Sigla obrigatória e única** (case-insensitive).
- **Pelo menos 1 contato obrigatório**.
- **E-mail de notificações obrigatório** (se Status = ATIVO).
- **Pelo menos 1 produto contratado se Status = ATIVO**.
- **Responsável técnico identificado**: pelo menos 1 contato com papel `TÉCNICO`.

Cliente pode ser salvo como **EM_ANALISE** sem produtos contratados (cadastro incompleto). Não pode receber entregas até virar ATIVO.

### 6.2 Edição

- CNPJ é **imutável após criação** (regra de negócio: cliente é identidade jurídica fixa).
- Sigla é editável mas com aviso: "alteração de sigla afeta histórico visual".
- Status muda via fluxo separado (botão "Pausar"/"Reativar" na visão geral).

### 6.3 Validações de transição de status

| De | Para | Permitido |
|---|---|---|
| EM_ANALISE | ATIVO | ✅ (após validação completa) |
| EM_ANALISE | ENCERRADO | ✅ (descarte) |
| ATIVO | PAUSADO | ✅ |
| ATIVO | ENCERRADO | ✅ (com confirm dupla) |
| PAUSADO | ATIVO | ✅ |
| PAUSADO | ENCERRADO | ✅ |
| ENCERRADO | qualquer | ❌ (terminal) |

### 6.4 Validações cross-section

- Se `Status = ATIVO`: exige completar todas as seções.
- Se `Status = EM_ANALISE`: permite salvar parcial.
- Mostrar lista de pendências no topo se ATIVO e algo faltando.

---

## 7. Contratos de API

### Criar cliente

```
POST /api/v1/orchestrator/clientes
```

```json
{
  "nome": "ACME LTDA",
  "razaoSocial": "ACME Comércio Ltda",
  "cnpj": "12345678000199",
  "sigla": "ACME",
  "status": "EM_ANALISE",
  "responsavelComercialId": "uuid",
  "ambientePadrao": "PROD",
  "tipoBanco": "Oracle",
  "codificacao": "UTF-8",
  "fusoHorario": "America/Sao_Paulo",
  "emailNotificacoes": "alertas@acme.com,operacoes@acme.com",
  "contatos": [
    {
      "nome": "João Silva",
      "papel": "TECNICO",
      "email": "joao@acme.com",
      "telefone": "+55 11 99999-0000"
    }
  ],
  "configuracaoEntrega": {
    "tipoDestino": "PASTA",
    "caminhoBase": "/var/lib/nexus/entregas/cliente-acme"
  },
  "produtosContratados": [
    { "produtoId": "uuid-nexusld", "ambiente": "PROD" }
  ]
}
```

**Response 201**: `ClienteResponse` completo.

**Errors:**
| Code | Causa |
|---|---|
| 400 | Validação Bean Validation falhou |
| 409 | `CLIENTE_CNPJ_DUPLICADO` |
| 409 | `CLIENTE_SIGLA_DUPLICADA` |
| 422 | `CLIENTE_VALIDACAO_ATIVO_INCOMPLETA` (tentou ATIVO sem dados completos) |

### Atualizar cliente

```
PUT /api/v1/orchestrator/clientes/{id}
```

Body igual ao criar (sem `cnpj` — ignorado).

### Buscar cliente

```
GET /api/v1/orchestrator/clientes/{id}
```

Response: `ClienteResponse` completo, com contatos, config entrega resumida, produtos contratados.

### Validar campos (ajax)

```
HEAD /api/v1/orchestrator/clientes?cnpj=12345678000199
HEAD /api/v1/orchestrator/clientes?sigla=ACME
```

Retorna `200` (existe) ou `404` (livre). Usado para validação assíncrona no form.

---

## 8. DTOs

```java
public record ClienteRequest(
    @NotBlank @Size(max = 200) String nome,
    @NotBlank @Size(max = 200) String razaoSocial,
    @NotBlank @CnpjValido String cnpj,
    @NotBlank @Pattern(regexp = "^[A-Z0-9-]+$") @Size(max = 20) String sigla,
    @NotNull StatusCliente status,
    UUID responsavelComercialId,
    @NotBlank @Size(max = 20) String ambientePadrao,
    @NotBlank @Size(max = 60) String tipoBanco,
    @Size(max = 20) String codificacao,
    @Size(max = 60) String fusoHorario,
    @Email @Size(max = 500) String emailNotificacoes,
    @NotEmpty @Valid List<ContatoRequest> contatos,
    @Valid ConfiguracaoEntregaRequest configuracaoEntrega,
    @Valid List<ProdutoContratadoRequest> produtosContratados
) {
    public record ContatoRequest(
        @NotBlank @Size(max = 120) String nome,
        @NotNull PapelContato papel,
        @NotBlank @Email String email,
        @Size(max = 30) String telefone
    ) {}

    public record ConfiguracaoEntregaRequest(
        @NotNull TipoDestino tipoDestino,
        @NotBlank @Size(max = 500) String caminhoBase
    ) {}

    public record ProdutoContratadoRequest(
        @NotNull UUID produtoId,
        @NotBlank String ambiente
    ) {}
}

public record ClienteResponse(
    UUID id,
    String nome, String razaoSocial, String cnpj, String cnpjFormatado, String sigla,
    StatusCliente status,
    UUID responsavelComercialId,
    String ambientePadrao, String tipoBanco, String codificacao, String fusoHorario,
    String emailNotificacoes,
    List<ContatoResponse> contatos,
    ConfiguracaoEntregaResumida configuracaoEntrega,
    List<ProdutoContratadoResumido> produtosContratados,
    OffsetDateTime createdAt, OffsetDateTime updatedAt,
    String createdBy, String updatedBy
) {}
```

---

## 9. Errors no frontend

| Backend code | Frontend faz |
|---|---|
| `CLIENTE_CNPJ_DUPLICADO` | Erro inline no campo CNPJ. Tooltip: "Cliente com este CNPJ já cadastrado: ACME LTDA." |
| `CLIENTE_SIGLA_DUPLICADA` | Erro inline no campo sigla. |
| `CLIENTE_VALIDACAO_ATIVO_INCOMPLETA` | Mostra lista de pendências no topo + scroll para primeira pendência. |
| `REQUEST_INVALIDO` | Mapeia `fields[]` para erros inline. |

---

## 10. Auditoria

| Ação | Registrado |
|---|---|
| Criar cliente | `CLIENTE_CRIADO` com snapshot inicial |
| Editar dados gerais | `CLIENTE_EDITADO` com diff dos campos alterados |
| Editar contatos | `CONTATO_ADICIONADO` / `CONTATO_REMOVIDO` / `CONTATO_EDITADO` |
| Alterar produtos contratados | `PRODUTO_CONTRATADO_ALTERADO` |
| Alterar config entrega | `CONFIG_ENTREGA_ALTERADA` |

Diff resumido em `detalhes` (JSONB). Sem dados pessoais completos.

---

## 11. Performance

### Tamanhos
- Cliente típico: pequeno (~1KB JSON).
- Com 5 contatos + 5 produtos: ~3KB.

### Otimizações
- Validação de unicidade assíncrona (debounce no frontend).
- Salvar com `@Transactional` para garantir atomicidade (cliente + contatos + produtos).

---

## 12. Estados e edge cases

### Form vazio (criação)
- Status default = `EM_ANALISE`.
- Salvamento parcial permitido se EM_ANALISE.

### Edição
- Carrega todos os dados.
- Form sujo apenas se houve mudança real.
- Discard guard ao tentar sair.

### Validação tardia (servidor)
- Frontend mostra erros inline próximos aos campos.
- Se erro não casa campo: toast genérico + correlation ID.

### Auto-save (sugestão futura)
- Para forms longos, salvar rascunho a cada 10s.

---

## 13. Acessibilidade

- Labels associados a cada input.
- Mensagens de erro com `aria-live="polite"`.
- Foco no primeiro campo inválido após submit.
- Navegação por Tab seguindo ordem lógica.
- Confirm modal trapável por foco.

---

## 14. Cross-reference

- [`02-clientes-lista.md`](02-clientes-lista.md) — Listagem.
- [`04-cliente-visao-geral.md`](04-cliente-visao-geral.md) — Detalhe pós-cadastro.
- [`05-cliente-dominios-funcionalidades.md`](05-cliente-dominios-funcionalidades.md) — Próximo passo após cadastro.
- [`06-cliente-produtos-contratados.md`](06-cliente-produtos-contratados.md) — Módulos por produto.
- [`07-cliente-configuracoes-entrega.md`](07-cliente-configuracoes-entrega.md) — Config completa.
- [`32-modelo-dados-sugerido.md`](32-modelo-dados-sugerido.md) — Schema.
- [`99-padroes-tela.md`](99-padroes-tela.md) — Padrões de form.
