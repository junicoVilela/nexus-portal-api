# 07 — Cliente — Configurações de Entrega

> **MVP**: apenas destino `PASTA` local está no escopo do primeiro ciclo. `FTP`, `SFTP` e `BUCKET` são **pós-MVP** (dependem de gestão de credenciais cifradas).

## 1. Papel da tela

Define **onde e como** os pacotes gerados são publicados para o cliente. Inclui destino, credenciais (mascaradas após salvar), janela permitida e fluxo de aprovação.

Acessível em:
- `/orchestrator/clientes/:id/configuracao-entrega`.
- Aba "Configurações" em `04-cliente-visao-geral.md`.

---

## 2. Objetivos

| Objetivo | Como atende |
|---|---|
| Configurar local de publicação | Seção "Destino" |
| Definir protocolo e credenciais | Seções específicas por tipo (pós-MVP) |
| Definir janelas de implantação | Seção "Janela" |
| Registrar regras específicas | Seção "Aprovação e notificações" |
| Testar conexão | Botão "Testar conexão" (pós-MVP) |
| Auditar mudanças | Todas as alterações registradas |

---

## 3. Layout

```text
┌────────────────────────────────────────────────────────────────────────┐
│ Configurações de Entrega                                               │
├────────────────────────────────────────────────────────────────────────┤
│ ┌─ Destino ───────────────────────────────────────────────────────┐  │
│ │ Tipo de destino*                                                 │  │
│ │ ◉ Pasta local (MVP)                                              │  │
│ │ ○ FTP        (Pós-MVP — em breve)                                │  │
│ │ ○ SFTP       (Pós-MVP — em breve)                                │  │
│ │ ○ Bucket S3  (Pós-MVP — em breve)                                │  │
│ └──────────────────────────────────────────────────────────────────┘  │
│                                                                        │
│ ┌─ Pasta local ───────────────────────────────────────────────────┐  │
│ │ Caminho base*                                                    │  │
│ │ [/var/lib/softon/entregas/cliente-acme              ]            │  │
│ │ ℹ️  Pasta deve existir e ter permissão de escrita.                │  │
│ │ [Testar acesso]                                                  │  │
│ └──────────────────────────────────────────────────────────────────┘  │
│                                                                        │
│ ┌─ Credenciais (Pós-MVP) ─────────────────────────────────────────┐  │
│ │ Não aplicável para destino PASTA local.                          │  │
│ └──────────────────────────────────────────────────────────────────┘  │
│                                                                        │
│ ┌─ Janela permitida ──────────────────────────────────────────────┐  │
│ │ ☑ Restringir entrega a janela específica                         │  │
│ │ Dias da semana: [Seg] [Ter] [Qua] [Qui] [Sex] [Sab] [Dom]        │  │
│ │ Horário: das [22:00] às [06:00]                                  │  │
│ │ Fuso horário: [America/Sao_Paulo ▼]                              │  │
│ │ ℹ️  Entregas fora da janela são bloqueadas (ou alertadas).        │  │
│ └──────────────────────────────────────────────────────────────────┘  │
│                                                                        │
│ ┌─ Aprovação ─────────────────────────────────────────────────────┐  │
│ │ ☑ Exigir aprovação de operador antes de publicar                 │  │
│ │ Aprovadores permitidos: [Apenas ADMIN ▼]                         │  │
│ └──────────────────────────────────────────────────────────────────┘  │
│                                                                        │
│ ┌─ Notificações ──────────────────────────────────────────────────┐  │
│ │ E-mails de notificação (separar por vírgula)                     │  │
│ │ [alertas@acme.com, operacoes@acme.com               ]            │  │
│ │ Eventos:                                                         │  │
│ │ ☑ Início da geração   ☑ Geração concluída   ☑ Falha              │  │
│ │ ☐ Publicação concluída                                           │  │
│ └──────────────────────────────────────────────────────────────────┘  │
│                                                                        │
│                          [Reverter]   [Salvar configurações]            │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Seções e campos

### 4.1 Destino

| Tipo | Disponibilidade | Campos adicionais |
|---|---|---|
| **PASTA** (local) | ✅ MVP | Caminho base |
| **FTP** | 🔒 Pós-MVP | Host, porta, usuário, senha, modo (ativo/passivo) |
| **SFTP** | 🔒 Pós-MVP | Host, porta, usuário, autenticação (senha ou chave SSH) |
| **BUCKET** | 🔒 Pós-MVP | Provedor (S3/MinIO/Azure Blob), bucket, region, access key, secret key |

### 4.2 PASTA local (MVP)

| Campo | Tipo | Obrigatório | Validação |
|---|---|---|---|
| Caminho base | text | ✅ | path válido, normalizado (sem `..`), absoluto |

**Validação:**
- Backend valida que path é absoluto, não tem `..`, não escape para fora de `/var/lib/softon/`.
- "Testar acesso" verifica se path existe e é escrevível.

### 4.3 FTP / SFTP (pós-MVP)

| Campo | Tipo |
|---|---|
| Host | text |
| Porta | number |
| Usuário | text |
| Senha / Chave | password / file upload |
| Caminho base | text |
| Modo (FTP) | active/passive |

**Mascaramento**: após salvar, senha aparece como `••••••` com botão "Alterar".

### 4.4 BUCKET (pós-MVP)

| Campo | Tipo |
|---|---|
| Provedor | select (S3, MinIO, Azure Blob) |
| Endpoint | text (URL) |
| Bucket | text |
| Region | text |
| Access Key | text |
| Secret Key | password |
| Prefixo | text |

### 4.5 Janela permitida

| Campo | Tipo | Obrigatório |
|---|---|---|
| Restringir janela | checkbox | ❌ |
| Dias da semana | multi-toggle | ✅ se restringir |
| Horário início/fim | time | ✅ se restringir |
| Fuso horário | timezone | ✅ se restringir |
| Comportamento fora da janela | radio | ✅ |

**Comportamento fora da janela:**
- **Bloquear**: rejeitar entrega.
- **Alertar**: permitir mas registrar alerta.

### 4.6 Aprovação

| Campo | Tipo | Default |
|---|---|---|
| Exigir aprovação | checkbox | false |
| Aprovadores permitidos | select | ADMIN |

**Quando ativada:**
- Entrega gerada fica em status `AGUARDANDO_APROVACAO`.
- Aprovador autorizado pode liberar publicação.

### 4.7 Notificações

| Campo | Tipo |
|---|---|
| E-mails | text (CSV) |
| Eventos a notificar | checkboxes |

Eventos:
- Início da geração.
- Geração concluída.
- Falha.
- Publicação concluída.
- Publicação falhada.

---

## 5. Botões

### Reverter
- Restaura valores do servidor.
- Confirm se form sujo.

### Salvar configurações
- Salva e mantém na mesma tela.
- Toast "Configurações salvas".
- Próxima entrega usará a nova config.

### Testar acesso / Testar conexão
- Apenas valida sem salvar.
- Mostra resultado: OK ou erro detalhado.

---

## 6. Regras de negócio

### 6.1 Caminho válido (PASTA)
- Path absoluto.
- Sem `..`.
- Dentro de área permitida (whitelist via property).
- Backend verifica existência e escrita.

### 6.2 Mascaramento de credenciais
- Senhas, secret keys, chaves privadas **nunca** retornam no GET.
- Aparecem como `••••••` com botão "Alterar".
- "Alterar" abre input vazio — só sobrescreve se digitado.

### 6.3 Cifragem em repouso
- Credenciais armazenadas com **Jasypt** (`@Convert` JPA).
- Master key em variável de ambiente (`JASYPT_ENCRYPTOR_PASSWORD`).
- Pós-MVP: migrar para Vault.

### 6.4 Auditoria reforçada
- Toda mudança de credencial → entrada em `orchestrator_auditoria` com `severidade=ALTA`.
- Conteúdo da credencial **nunca** vai para log.
- Apenas "credencial X foi alterada por Y".

### 6.5 Teste de conexão
- Endpoint dedicado, executa connect sem salvar.
- Retorna sucesso ou erro descritivo (sem expor stack).
- Rate limit: máx 10/min por usuário.

### 6.6 Janela e UTC
- Janela definida no fuso do cliente.
- Backend converte para comparação.
- Mudança de DST tratada via biblioteca (java.time.zone).

---

## 7. Contratos de API

### Buscar configuração

```
GET /api/v1/orchestrator/clientes/{id}/configuracao-entrega
```

Response:
```json
{
  "tipoDestino": "PASTA",
  "caminhoBase": "/var/lib/softon/entregas/cliente-acme",
  "credenciaisResumo": "Sem credenciais (PASTA)",
  "janela": {
    "restringida": true,
    "diasSemana": ["SEG","TER","QUA","QUI","SEX"],
    "horarioInicio": "22:00",
    "horarioFim": "06:00",
    "fusoHorario": "America/Sao_Paulo",
    "comportamentoForaDaJanela": "BLOQUEAR"
  },
  "aprovacao": {
    "exigida": true,
    "aprovadoresPermitidos": ["ADMIN"]
  },
  "notificacoes": {
    "emails": ["alertas@acme.com","operacoes@acme.com"],
    "eventos": ["INICIO","CONCLUSAO","FALHA"]
  },
  "atualizadoEm": "2026-05-31T14:00:00Z",
  "atualizadoPor": "joao.silva"
}
```

> Credenciais **nunca** aparecem em GET.

### Atualizar

```
PUT /api/v1/orchestrator/clientes/{id}/configuracao-entrega
```

Body completo (com campos `password`/`secretKey` opcionais — só atualiza se enviados).

### Testar conexão / acesso

```
POST /api/v1/orchestrator/clientes/{id}/configuracao-entrega/testar
```

Body opcional com credenciais provisórias (testar sem salvar).

Response:
```json
{ "ok": true, "detalhe": "Conexão estabelecida. Pasta gravável. Espaço livre 250GB." }
```

ou:
```json
{ "ok": false, "codigo": "FTP_AUTENTICACAO_FALHOU", "detalhe": "Usuário ou senha inválidos." }
```

---

## 8. DTOs

```java
public record ConfiguracaoEntregaResponse(
    TipoDestino tipoDestino,
    String caminhoBase,
    String credenciaisResumo,        // sempre mascarado
    JanelaPermitida janela,
    Aprovacao aprovacao,
    Notificacoes notificacoes,
    OffsetDateTime atualizadoEm,
    String atualizadoPor
) {
    public record JanelaPermitida(
        boolean restringida,
        List<DayOfWeek> diasSemana,
        LocalTime horarioInicio,
        LocalTime horarioFim,
        String fusoHorario,
        ComportamentoForaDaJanela comportamentoForaDaJanela
    ) {}

    public record Aprovacao(boolean exigida, List<String> aprovadoresPermitidos) {}

    public record Notificacoes(List<String> emails, Set<EventoEntrega> eventos) {}
}

public record AtualizarConfiguracaoEntregaRequest(
    @NotNull TipoDestino tipoDestino,
    @NotBlank @Size(max = 500) String caminhoBase,
    String host,             // pós-MVP
    @Min(1) @Max(65535) Integer porta,
    String usuario,
    String senha,            // null = manter; preenchido = atualizar
    String chavePrivada,
    String bucket,
    String region,
    String accessKey,
    String secretKey,        // null = manter
    @Valid JanelaPermitida janela,
    @Valid Aprovacao aprovacao,
    @Valid Notificacoes notificacoes
) {}
```

---

## 9. Performance

### Tamanhos
- Configuração é pequena (<2KB).
- Cache local OK por sessão.

### Cifragem
- Encrypt/decrypt em rota crítica é overhead. Mensurar p95.

---

## 10. Segurança

### Princípios
- **Nunca expor credencial em resposta** (mascarar via `credenciaisResumo`).
- **Nunca logar credencial**.
- **Auditar toda alteração** com severidade alta.
- **Limitar acesso ao endpoint** (apenas ADMIN).
- **Rate limit** em endpoint de teste.

### Defesa em profundidade
- TLS sempre (API HTTPS).
- Vault para master key (pós-MVP).
- Sem export de credenciais no backup default.

### Headers de segurança
- `Cache-Control: no-store` em endpoints sensíveis.
- `X-Content-Type-Options: nosniff`.

---

## 11. Estados e edge cases

### Caminho de pasta inválido
- Erro inline + sugestão de path correto.

### Pasta sem permissão de escrita
- Erro: "Pasta sem permissão de escrita. Verifique com o time de operação."

### Teste de conexão falha
- Erro com causa específica.
- Botão "Tentar novamente" + dicas.

### Mudança de tipo de destino
- Confirm: "Mudar de PASTA para SFTP descarta as credenciais atuais. Continuar?"

### Credencial expirando (pós-MVP)
- Banner amarelo: "Credencial SFTP expira em 15 dias."

---

## 12. Acessibilidade

- Radio group de tipos de destino com `<fieldset>` + `<legend>`.
- Campos sensíveis (senha) com `type="password"`.
- Botões de ação com labels descritivas.
- Avisos de "pós-MVP" com `aria-disabled`.

---

## 13. Auditoria

| Ação | Detalhes |
|---|---|
| `CONFIG_ENTREGA_ALTERADA` | Lista dos campos alterados (sem valores sensíveis) |
| `CONFIG_ENTREGA_CREDENCIAL_ALTERADA` | Apenas indicador (nunca o valor) |
| `CONFIG_ENTREGA_TESTE_EXECUTADO` | Resultado (ok/falha) |
| `CONFIG_ENTREGA_TIPO_DESTINO_MUDADO` | De/Para |

---

## 14. Cross-reference

- [`04-cliente-visao-geral.md`](04-cliente-visao-geral.md) — Tela mãe.
- [`21-geracao-pacote.md`](21-geracao-pacote.md) — Como config é usada na geração.
- [`33-decisoes-tecnicas.md`](33-decisoes-tecnicas.md) — Stack de cifragem.
- [`36-deploy-operacao.md`](36-deploy-operacao.md) — Vault e secrets em prod.
- [`99-padroes-tela.md`](99-padroes-tela.md) — Padrões.
