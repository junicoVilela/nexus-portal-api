# 21 — Geração de Pacote

> **MVP**: etapas 3–5 (download de GitHub e diff de scripts) são substituídas no MVP por **"copiar artefatos uploadados na release"** (ver `14-release-orchestrator-detalhe.md`). Etapas 6 (KETTLE) e 4 (BANCO) também leem dos uploads, não do Git. Etapa 10 (publicação) salva apenas em **pasta local fixa do servidor** (configurada em `28-configuracoes.md`) no MVP — publicação no destino do cliente (FTP/SFTP/bucket) é **pós-MVP**. Etapa 11 (atualização de versão atual do cliente) é MVP e funciona com a versão registrada na release.

## 1. Papel da tela

**Acompanhar a montagem técnica do pacote final e o resultado**. Mostra progresso em tempo real, logs por etapa e ações corretivas.

Acessível em `/orchestrator/entregas/:id/geracao` ou diretamente após disparar a geração no wizard.

---

## 2. Pré-condições

- Seleção de módulos confirmada (`19`).
- Ranges revisados (`20` — pós-MVP).
- Cliente tem configuração de destino válida (`07`).
- Artefatos uploadados nos módulos obrigatórios (MVP — via `14`).

---

## 3. Layout

```text
┌────────────────────────────────────────────────────────────────────────┐
│ Geração de Pacote — Entrega #ENTR-2026-0142                            │
│ ACME LTDA • DTEC-LD v1.5.0 • Iniciada em 31/05 14:32                   │
├────────────────────────────────────────────────────────────────────────┤
│ Status: 🔵 PROCESSANDO • 6/11 etapas concluídas                        │
│ ━━━━━━━━━━━━━━━━░░░░░░░░░░░░░░░░░░░░  54%                              │
├────────────────────────────────────────────────────────────────────────┤
│ ✅ 1. Validar              concluído (0.3s)                             │
│ ✅ 2. Reservar ID          concluído (0.1s)                             │
│ ✅ 3. Coletar artefatos    concluído (12.3s) • 6 arquivos              │
│ ✅ 4. Coletar SQL          concluído (4.1s) • DDL 12 / DML 8           │
│ ✅ 5. Coletar Kettle       skipped (módulo não selecionado)             │
│ ✅ 6. Gerar funcionalidades concluído (0.8s) • 23 scripts gerados      │
│ 🔵 7. Renderizar PDF       executando... 35%                            │
│ ⚪ 8. Gerar manifest       pendente                                     │
│ ⚪ 9. Calcular checksums   pendente                                     │
│ ⚪ 10. Publicar destino     pendente                                    │
│ ⚪ 11. Atualizar histórico  pendente                                    │
├────────────────────────────────────────────────────────────────────────┤
│ Log:                                                                   │
│ 14:32:01 INFO Validação OK                                             │
│ 14:32:02 INFO Coletando 6 artefatos da release v1.5.0                  │
│ 14:32:14 INFO Coletados: dtec-web.war (47MB), DDL_001..DDL_012 (...)   │
│ 14:32:18 INFO Gerando manifest...                                      │
│ 14:32:22 INFO Renderizando PDF release-notes.pdf                       │
│                                                                        │
│              [Cancelar geração]                                         │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Etapas da geração

Executadas em sequência, com log e progresso individuais.

### Etapa 1: Validar
- Verifica seleção, versões, contratação e config de destino.
- Re-valida que artefatos obrigatórios existem.
- Falha aqui = erro de pré-condição (não consome recursos).

### Etapa 2: Reservar ID
- Cria registro `Entrega` com status `EM_GERACAO`.
- ID sequencial (`#ENTR-2026-0142`) + UUID interno.
- Reserva diretório de trabalho.

### Etapa 3: Coletar artefatos (WEB/BATCH)
- **MVP**: copia artefatos uploadados na release (de `ArtefatoReleaseModulo`).
- **Pós-MVP**: baixa asset da `TO_TAG` no GitHub Releases.

### Etapa 4: Coletar SQL (BANCO)
- **MVP**: descompacta `.zip` uploadado ou copia `.sql` individuais.
- **Pós-MVP**: diff de scripts entre `FROM_TAG` e `TO_TAG`.
- Ordena DDL → DML.
- Gera `DDL.sql` e `DML.sql` unificados por dialeto.

### Etapa 5: Coletar Kettle
- **MVP**: descompacta `.zip` uploadado.
- **Pós-MVP**: transformações/jobs modificados + dependências.

### Etapa 6: Gerar scripts de configuração (FUNCIONALIDADES/REGRAS)
- Lê `ClienteFuncionalidade` atual.
- Renderiza template de script com dados do cliente.
- Gera arquivos `funcionalidades_*.sql` e `regras_*.sql`.

### Etapa 7: Renderizar release notes (PDF)
- Usa o mesmo renderer Markdown → PDF (ver [`25-documento-release-md-pdf.md`](25-documento-release-md-pdf.md)).
- Visibilidade = "Cliente" por default.
- Gera `release-notes.pdf`.

### Etapa 8: Gerar manifest
- Cria `manifest.json` (ver §6) com metadados.

### Etapa 9: Calcular checksums
- SHA-256 de todos os arquivos do pacote.
- Gera `SHA256SUMS.txt`.

### Etapa 10: Publicar
- **MVP**: copia ZIP para `orchestrator.pacotes.dir` local.
- **Pós-MVP**: envia para destino do cliente (FTP/SFTP/Bucket).

### Etapa 11: Atualizar histórico
- Marca entrega como `CONCLUIDA`.
- Atualiza `ClienteProdutoModulo.versaoAtual` para cada módulo entregue.
- Registra histórico em `orchestrator_auditoria`.

### Mapa etapa → origem do artefato

| Etapa | Tipo módulo | MVP (portal) | Fase 2 (repos) | Spec repo |
|---|---|---|---|---|
| 3 | WEB, BATCH | Upload na release (`14`) | Asset GitHub Release da `TO_TAG` | `39` §2.1–2.2 |
| 4 | BANCO | Upload `.sql`/`.zip` (`14`) | Delta DDL/DML entre tags | `39` §2.3, `40` §6 |
| 5 | KETTLE | Upload `.zip` (`14`) | ZIP delta no GitHub Release | `39` §2.6 |
| 6 | FUNCIONALIDADES, REGRAS | Gerado do cliente (`05`) | Idem (sem asset no repo) | `39` §2.4–2.5 |
| 7 | — | Renderer PDF (`25`) | Idem | `25` |
| 10 | — | Pasta local (`28`) | FTP/SFTP/Bucket (`07`) | Fase 3 |

Ver checklist completo em [`39-entregaveis-cicd-repositorios.md`](39-entregaveis-cicd-repositorios.md).

---

## 5. Estrutura final do pacote

```text
{cliente-sigla}_{produto-sigla}_{versao}_{timestamp}/
├── web/
│   └── *.war
├── batch/
│   └── *.jar
├── banco/
│   ├── oracle/
│   │   ├── DDL.sql
│   │   └── DML.sql
│   └── sqlserver/
│       ├── DDL.sql
│       └── DML.sql
├── kettle/
│   └── *.ktr, *.kjb
├── funcionalidades/
│   └── *.sql
├── regras/
│   └── *.sql
├── docs/
│   └── release-notes.pdf
├── manifest.json
└── SHA256SUMS.txt
```

Resultado final: ZIP único com a estrutura acima.

Path final no servidor:
```
{orchestrator.pacotes.dir}/{entregaId}/{nome-do-pacote}.zip
```

---

## 6. manifest.json (estrutura)

```json
{
  "entregaId": "uuid",
  "entregaCodigo": "ENTR-2026-0142",
  "cliente": {
    "id": "uuid",
    "sigla": "ACME",
    "nome": "ACME LTDA",
    "ambiente": "PROD"
  },
  "produto": {
    "id": "uuid",
    "sigla": "DTECLD",
    "nome": "DTEC-LD"
  },
  "releaseId": "uuid",
  "versao": "1.5.0",
  "tipoRelease": "MINOR",
  "geradoEm": "2026-05-31T14:32:00-03:00",
  "geradoPor": "joao.silva",
  "modulos": [
    {
      "codigo": "dtec-web",
      "nome": "DTEC Web",
      "tipo": "WEB",
      "fromTag": "v1.4.0",
      "toTag": "v1.5.0",
      "modo": "AUTOMATICO",
      "artefatos": [
        { "caminho": "web/dtec-web-1.5.0.war", "sha256": "abc...", "tamanhoBytes": 49283746 }
      ]
    },
    {
      "codigo": "dtec-db-ddl",
      "tipo": "BANCO",
      "modo": "AUTOMATICO",
      "artefatos": [
        { "caminho": "banco/oracle/DDL.sql", "sha256": "def...", "tamanhoBytes": 12345 }
      ]
    }
  ],
  "destino": { "tipo": "PASTA", "caminho": "/var/lib/softon/entregas/cliente-acme" },
  "checksumManifest": "ghi..."
}
```

---

## 7. Status visíveis na tela

| Status | UI |
|---|---|
| `PENDENTE` | Aguardando worker; spinner + "Em fila" |
| `PROCESSANDO` | Barra de progresso + etapa atual + log |
| `CONCLUIDO` | ✅ Verde + atalho para detalhe |
| `ERRO` | 🔴 + etapa que falhou + mensagem + ação corretiva |
| `CANCELADO` | ⚫ + motivo |

### Polling
- Frontend faz poll a cada 2-3s no endpoint `/status`.
- Alternativa: WebSocket/SSE para empurrar updates.
- Após CONCLUIDO ou ERRO: para polling.

---

## 8. Ações

### Durante PROCESSANDO
- **Cancelar geração** — para no próximo checkpoint.
- **Ver log completo** — modal com todos os logs.

### Após CONCLUIDO
- **Ver detalhes da entrega** → `22-detalhes-entrega.md`.
- **Download pacote** → ZIP final.
- **Publicar (pós-MVP)** se ainda não publicado.

### Após ERRO
- **Repetir etapa que falhou** — sem refazer as anteriores.
- **Refazer pacote inteiro** — reinicia tudo.
- **Ver detalhes do erro** — log da etapa.
- **Marcar como ignorado** (apenas ADMIN, audit).

---

## 9. Regras

### 9.1 Assíncrono
- Geração roda em thread pool (`ThreadPoolTaskExecutor`, ver `33`).
- Usuário pode sair da tela e voltar.

### 9.2 Logs
- Cada etapa registra log estruturado (timestamp, nível, mensagem).
- Logs persistidos para auditoria.

### 9.3 Falhas
- Falha em qualquer etapa marca entrega como `ERRO`.
- Mantém artefatos parciais para análise.
- Não atualiza `versaoAtual` do cliente.

### 9.4 Reutilização
- Pacote final fica armazenado para reentrega (ver `22`).
- Reentrega sem refazer geração.

### 9.5 Atualização do cliente
- `ClienteProdutoModulo.versaoAtual` só atualizado ao final de publicação bem-sucedida.

### 9.6 Cancelamento
- Cancela no próximo checkpoint (não interrompe meio de download).
- Status → `CANCELADO`.
- Limpeza de arquivos parciais.

### 9.7 Timeout
- Limite global: 30 min por geração (configurável).
- Excede → status `ERRO` + log.

---

## 10. Contratos de API

### Disparar geração

```
POST /api/v1/orchestrator/entregas/{id}/gerar
```

Response: status inicial.

### Status atual

```
GET /api/v1/orchestrator/entregas/{id}/geracao/status
```

```json
{
  "entregaId": "uuid",
  "status": "PROCESSANDO",
  "etapaAtual": 7,
  "etapaAtualNome": "Renderizar PDF",
  "progressoEtapa": 35,
  "progressoTotal": 54,
  "etapas": [
    { "ordem": 1, "nome": "Validar", "status": "CONCLUIDO", "duracaoMs": 300 },
    { "ordem": 2, "nome": "Reservar ID", "status": "CONCLUIDO", "duracaoMs": 100 },
    { "ordem": 3, "nome": "Coletar artefatos", "status": "CONCLUIDO", "duracaoMs": 12300, "info": "6 arquivos" },
    { "ordem": 4, "nome": "Coletar SQL", "status": "CONCLUIDO", "duracaoMs": 4100, "info": "DDL 12 / DML 8" },
    { "ordem": 5, "nome": "Coletar Kettle", "status": "SKIPPED" },
    { "ordem": 6, "nome": "Gerar funcionalidades", "status": "CONCLUIDO", "duracaoMs": 800, "info": "23 scripts" },
    { "ordem": 7, "nome": "Renderizar PDF", "status": "PROCESSANDO", "progressoEtapa": 35 },
    { "ordem": 8, "nome": "Gerar manifest", "status": "PENDENTE" }
  ],
  "logs": [...]
}
```

### Cancelar

```
POST /api/v1/orchestrator/entregas/{id}/geracao/cancelar
```

### Repetir etapa

```
POST /api/v1/orchestrator/entregas/{id}/geracao/repetir-etapa/{ordem}
```

### Refazer

```
POST /api/v1/orchestrator/entregas/{id}/geracao/refazer
```

---

## 11. DTOs

```java
public record StatusGeracaoResponse(
    UUID entregaId,
    GeracaoStatus status,
    int etapaAtual,
    String etapaAtualNome,
    int progressoEtapa,
    int progressoTotal,
    List<EtapaInfo> etapas,
    List<LogEntry> logs
) {
    public record EtapaInfo(
        int ordem, String nome, StatusEtapa status,
        Long duracaoMs, String info, String erro
    ) {}

    public record LogEntry(OffsetDateTime timestamp, String nivel, String mensagem) {}
}

public enum StatusEtapa { PENDENTE, PROCESSANDO, CONCLUIDO, SKIPPED, ERRO }
```

---

## 12. Performance

### Tempo médio (estimativa)
- Release pequena: 30s-1min.
- Release média: 1-3min.
- Release grande: 5-15min.

### Otimizações
- Paralelização entre módulos independentes.
- Stream de bytes em vez de carregar em memória.
- Cache de delta calculado.

---

## 13. Estados e edge cases

### Geração em fila
- Pool saturado → entrega fica `PENDENTE` até worker liberar.
- Indicador "Em fila — 3 entregas à frente".

### Falha intermitente (e.g. GitHub down)
- Retry automático até N tentativas.
- Backoff exponencial.
- Falha definitiva → status ERRO.

### Disco cheio
- Falha imediata com erro descritivo.
- Sugestão: "Limpar pacotes antigos" ou "Expandir storage".

### Concorrência
- 2 gerações para mesmo cliente: 2ª fica em fila (lock por cliente).

---

## 14. Acessibilidade

- Progress bar com `role="progressbar"` + `aria-valuenow`.
- Logs com `aria-live="polite"`.
- Botão de cancelar com confirm.

---

## 15. Auditoria

| Ação | Detalhes |
|---|---|
| `ENTREGA_GERACAO_INICIADA` | configurações |
| `ENTREGA_GERACAO_CONCLUIDA` | duração, módulos, tamanho |
| `ENTREGA_GERACAO_FALHOU` | etapa, erro |
| `ENTREGA_GERACAO_CANCELADA` | quem, motivo |
| `ENTREGA_ETAPA_REPETIDA` | etapa, motivo |

---

## 16. Cross-reference

- [`18-nova-entrega-assistente.md`](18-nova-entrega-assistente.md) — Wizard.
- [`19-selecao-modulos.md`](19-selecao-modulos.md) — Seleção.
- [`20-range-manual-delta.md`](20-range-manual-delta.md) — Delta detail.
- [`22-detalhes-entrega.md`](22-detalhes-entrega.md) — Tela seguinte.
- [`25-documento-release-md-pdf.md`](25-documento-release-md-pdf.md) — Renderer PDF.
- [`33-decisoes-tecnicas.md`](33-decisoes-tecnicas.md) — Stack async/storage.
- [`34-observabilidade.md`](34-observabilidade.md) — Tracing.
- [`99-padroes-tela.md`](99-padroes-tela.md) — Padrões.
