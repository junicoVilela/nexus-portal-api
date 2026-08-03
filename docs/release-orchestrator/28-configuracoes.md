# 28 — Configurações Gerais

> **MVP**: as seções **GitHub**, **Jenkins** e **FTP/Destinos** abaixo são **pós-MVP**. No MVP, apenas **Geral**, **Notificações**, **Segurança** e **Parâmetros** são implementadas. A pasta local onde o Orchestrator salva os pacotes gerados é configurada em **Parâmetros** (ex.: chave `orchestrator.pacotes.dir`).

## 1. Papel da tela

**Parâmetros globais e integrações do sistema**. Configurações que afetam todo o orchestrator, independentes de cliente individual.

Acessível em `/orchestrator/configuracoes`. Apenas ADMIN.

---

## 2. Objetivos

| Objetivo | Como atende |
|---|---|
| Configurar parâmetros globais | Aba "Parâmetros" |
| Gerenciar credenciais globais | Aba "Credenciais" (pós-MVP) |
| Configurar notificações | Aba "Notificações" |
| Ajustar segurança | Aba "Segurança" |
| Testar integrações | Botão em cada seção |

---

## 3. Layout

```text
┌────────────────────────────────────────────────────────────────────────┐
│ Configurações Gerais                                                   │
├────────────────────────────────────────────────────────────────────────┤
│ [Geral] [Parâmetros] [Notificações] [Segurança] [GitHub] [Jenkins]     │
│         [FTP/Destinos] [Auditoria]                                     │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│ Aba: Parâmetros                                                        │
│                                                                        │
│ ┌─ Storage de pacotes ─────────────────────────────────────────────┐ │
│ │ Diretório de pacotes (orchestrator.pacotes.dir)*                  │ │
│ │ [/var/lib/nexus/pacotes              ]                           │ │
│ │ Espaço livre: 250GB / 500GB usados                                │ │
│ │ [Testar acesso]                                                   │ │
│ └───────────────────────────────────────────────────────────────────┘ │
│                                                                        │
│ ┌─ Limites ─────────────────────────────────────────────────────────┐ │
│ │ Tamanho máximo upload (MB)*           [500]                       │ │
│ │ Tempo máximo de geração (min)*        [30]                        │ │
│ │ Concorrência de geração (workers)*    [4]                         │ │
│ │ Fila máxima*                          [25]                        │ │
│ └───────────────────────────────────────────────────────────────────┘ │
│                                                                        │
│ ┌─ Retenção ────────────────────────────────────────────────────────┐ │
│ │ Pacotes ativos: indeterminado                                     │ │
│ │ Pacotes arquivados após: [365] dias                               │ │
│ │ Logs detalhados retidos por: [90] dias                            │ │
│ └───────────────────────────────────────────────────────────────────┘ │
│                                                                        │
│              [Reverter]   [Salvar configurações]                       │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Abas

### 4.1 Geral
- Nome da instância.
- URL pública.
- Timezone default.
- Logo institucional.

### 4.2 Parâmetros (MVP)

| Parâmetro | Tipo | Default | Descrição |
|---|---|---|---|
| `orchestrator.pacotes.dir` | path | `/var/lib/nexus/pacotes` | Onde pacotes gerados ficam |
| `releaseorchestrator.artefatos.dir` | path | `/var/lib/nexus/artefatos` | Onde artefatos uploadados ficam |
| `releaseorchestrator.pdf.dir` | path | `/var/lib/nexus/pdfs` | Snapshots de PDF |
| `releaseorchestrator.artefatos.tamanho-maximo-mb` | int | 500 | Limite de upload |
| `orchestrator.async.core-pool-size` | int | 2 | Pool de geração |
| `orchestrator.async.max-pool-size` | int | 4 | Pool máximo |
| `orchestrator.async.queue-capacity` | int | 25 | Fila |
| `orchestrator.geracao.timeout-minutos` | int | 30 | Timeout de geração |
| `orchestrator.pacotes.retencao-dias` | int | 365 | Retenção antes de arquivar |
| `orchestrator.logs.retencao-dias` | int | 90 | Logs detalhados |

### 4.3 Notificações (MVP)
- Servidor SMTP.
- Remetente padrão.
- Templates de e-mail (sucesso, falha, alertas).
- Canais alternativos (Slack webhook, MS Teams).

### 4.4 Segurança (MVP)
- Política de senha.
- Tempo de expiração de sessão.
- 2FA (pós-MVP).
- Lista de IPs permitidos (opcional).
- Rate limiting global.

### 4.5 GitHub (Pós-MVP)
- URL base do GitHub Enterprise (se aplicável).
- Token global (fallback para produtos sem token específico).
- Webhook URL para receber eventos.

### 4.6 Jenkins (Pós-MVP)
- URL base.
- Credencial global (user + token).
- Webhook URL.

### 4.7 FTP / Destinos (Pós-MVP)
- Hosts permitidos.
- Restrição de IP de origem.
- Configurações TLS.

### 4.8 Auditoria
- Configurar nível de detalhe.
- Retenção de auditoria.
- Webhooks para SIEM externo.

---

## 5. Regras

### 5.1 Validações
- Paths devem ser absolutos.
- Pools devem ter `max >= core`.
- Queue devem ser > 0.
- Timeouts devem estar em ranges razoáveis.

### 5.2 Aplicação
- Algumas mudanças exigem **restart** do serviço (mostradas com badge).
- Outras aplicam imediatamente.

### 5.3 Teste antes de salvar
- Sempre obrigatório em integrações.
- Botão "Testar acesso" / "Testar conexão" em cada seção sensível.

### 5.4 Auditoria reforçada
- Toda mudança gera `orchestrator_auditoria` com severidade ALTA.
- Mudança de credenciais marcadas como CRÍTICA.

---

## 6. Contratos de API

### Buscar todas as configurações

```
GET /api/v1/orchestrator/configuracoes
```

Retorna agrupado por aba. Credenciais mascaradas.

### Salvar aba

```
PUT /api/v1/orchestrator/configuracoes/{aba}
```

### Testar storage

```
POST /api/v1/orchestrator/configuracoes/parametros/testar-storage
```

Response: ok/erro + espaço livre.

### Testar SMTP

```
POST /api/v1/orchestrator/configuracoes/notificacoes/testar-smtp
```

Envia e-mail de teste.

### Testar GitHub/Jenkins global

```
POST /api/v1/orchestrator/configuracoes/github/testar
POST /api/v1/orchestrator/configuracoes/jenkins/testar
```

---

## 7. DTOs

```java
public record ConfiguracoesResponse(
    Geral geral,
    Parametros parametros,
    Notificacoes notificacoes,
    Seguranca seguranca,
    GitHubGlobal github,
    JenkinsGlobal jenkins,
    Auditoria auditoria
) {
    public record Geral(String nomeInstancia, String urlPublica, String timezone, String logoUrl) {}
    public record Parametros(/* todas as chaves acima */) {}
    public record Notificacoes(/* SMTP, templates */) {}
    public record Seguranca(/* policies */) {}
    public record GitHubGlobal(String baseUrl, String tokenMascarado, String webhookUrl) {}
    public record JenkinsGlobal(String baseUrl, String userMascarado, String tokenMascarado) {}
    public record Auditoria(int retencaoDias, String webhookSiem) {}
}
```

---

## 8. Performance

- Configuração lida em memória (cache `@Cacheable`).
- Invalidação on save.
- Read-heavy: cache pesado faz sentido.

---

## 9. Segurança

### Princípios
- Apenas ADMIN acessa.
- Credenciais nunca expostas.
- Toda mudança auditada.
- Backups da configuração (export periódico).

### Migração
- Configuração crítica em variáveis de ambiente em prod.
- Tela apenas para overrides operacionais.

---

## 10. Estados e edge cases

### Path inválido
- Erro inline com sugestão.

### Restart necessário
- Badge: "Aplicar requer restart do serviço."
- Botão "Salvar e marcar para restart" gera arquivo de sinalização.

### Teste falhou
- Mensagem detalhada com causa.
- Salvamento bloqueado.

---

## 11. Acessibilidade

- Inputs labeled.
- Tooltips em parâmetros técnicos.
- Confirm em mudanças críticas.

---

## 12. Auditoria

| Ação | Detalhes |
|---|---|
| `CONFIGURACAO_ALTERADA` | aba, chaves alteradas (sem valores sensíveis) |
| `CONFIGURACAO_CREDENCIAL_ALTERADA` | apenas indicador |
| `CONFIGURACAO_TESTE_EXECUTADO` | resultado |

---

## 13. Cross-reference

- [`33-decisoes-tecnicas.md`](33-decisoes-tecnicas.md) — Stack.
- [`36-deploy-operacao.md`](36-deploy-operacao.md) — Configuração em prod.
- [`34-observabilidade.md`](34-observabilidade.md) — Alertas.
- [`27-suporte-operacional.md`](27-suporte-operacional.md) — Saúde.
- [`07-cliente-configuracoes-entrega.md`](07-cliente-configuracoes-entrega.md) — Config por cliente.
- [`99-padroes-tela.md`](99-padroes-tela.md) — Padrões.
