# 34 — Observabilidade do Orchestrator

Spec dedicada de observabilidade. Complementa `release-orchestrator/15-observabilidade.md` com particularidades do orchestrator (operações longas, integrações, geração de pacote).

---

## 1. Por que observar é crítico aqui

O orchestrator tem características que tornam observabilidade essencial:
- **Operações longas** (geração de pacote pode levar minutos).
- **Múltiplos passos** (delta → download → empacotar → publicar).
- **Integrações externas** (GitHub, Jenkins, SFTP) — pontos de falha extra.
- **Estado distribuído** (DB + filesystem + remotos).
- **Auditoria obrigatória** (cliente quer saber o que recebeu).

---

## 2. Logs

### Estrutura JSON em prod
```json
{
  "timestamp": "2026-05-31T14:30:00Z",
  "level": "INFO",
  "logger": "com.nexus.portal.orchestrator.service.EntregaService",
  "message": "Geração de entrega concluída",
  "correlationId": "abc-...",
  "userId": "joao.silva",
  "entregaId": "uuid",
  "clienteId": "uuid",
  "produtoId": "uuid",
  "duracaoMs": 45230,
  "tamanhoBytes": 89456789,
  "modulosIncluidos": 6
}
```

### MDC obrigatório
| Chave | Quando popular |
|---|---|
| `correlationId` | Filtro de entrada (geral) |
| `userId` | Filtro auth |
| `clienteId` | Em qualquer operação com escopo de cliente |
| `entregaId` | Durante o ciclo de uma entrega |
| `produtoId` | Operações de produto |

### Eventos importantes (INFO)
- Cliente criado / editado / configuração atualizada.
- Próxima entrega planejada / aprovada / cancelada.
- Geração de entrega iniciada / concluída / falhada / cancelada.
- Publicação iniciada / concluída.
- Reentrega disparada.

### Eventos de alerta (WARN)
- Validação de configuração de entrega falhou.
- Download de asset GitHub falhou (mas teve retry).
- Conexão SFTP recusada.
- Cancelamento manual de geração em curso.

### Erros (ERROR)
- Falha de geração após retries.
- Storage indisponível.
- DB indisponível.
- Falha catastrófica em integração externa.

---

## 3. Métricas (Micrometer)

### Contadores
```text
orchestrator_clientes_total                         (gauge)
orchestrator_clientes_ativos                        (gauge)
orchestrator_proximas_entregas_planejadas{periodo}  (gauge) — "esta_semana", "este_mes"
orchestrator_entregas_total{outcome}                (contador) — "concluida", "cancelada", "falhada"
orchestrator_reentregas_total                       (contador)
orchestrator_publicacoes_total{destino, outcome}    (contador) — destino="PASTA","FTP","SFTP","BUCKET"
```

### Histogramas
```text
orchestrator_entrega_geracao_duracao_seconds{tipo_pacote}
orchestrator_entrega_tamanho_bytes{tipo_pacote}
orchestrator_modulos_por_entrega
orchestrator_delta_calculo_duracao_seconds{tipo_modulo}
orchestrator_download_artefato_duracao_seconds
orchestrator_publicacao_duracao_seconds{destino}
```

### Gauges (estado atual)
```text
orchestrator_geracoes_em_curso              ← gauge (PROCESSANDO)
orchestrator_entregas_pendentes_aprovacao   ← gauge
orchestrator_credenciais_expirando_30d      ← gauge (Fase 2/3)
```

### Cardinalidade — cuidado
- **OK como label**: `outcome`, `tipo_modulo`, `destino`, `periodo`.
- **NÃO usar como label**: `clienteId`, `entregaId`, `usuarioId` (cardinal infinito).
- Esses ficam em logs/traces.

---

## 4. Tracing (OpenTelemetry)

### Spans macro (do ponto de vista do operador)

```text
POST /api/v1/orchestrator/entregas
└── entrega.criar (orchestrator)
    ├── proxima-entrega.aprovar
    ├── delta.calcular
    │   ├── modulo.delta (×N módulos)
    │   └── delta.consolidar
    ├── artefatos.coletar
    │   ├── artefato.download (×N artefatos)
    │   └── artefato.verificar-integridade
    ├── pacote.montar
    │   ├── pacote.adicionar-modulo (×N)
    │   ├── pacote.gerar-manifest
    │   └── pacote.gerar-checksums
    ├── pdf.gerar
    │   └── thymeleaf.render
    │       └── openhtmltopdf.convert
    └── publicacao.executar
        └── publisher.{pasta|ftp|sftp|bucket}
```

### Atributos por span

| Span | Atributos |
|---|---|
| `entrega.criar` | `cliente.id`, `produto.id`, `entrega.id` |
| `delta.calcular` | `modulos.count`, `delta.scripts.count` |
| `artefato.download` | `artefato.tamanho_bytes`, `github.repo` (se aplicável) |
| `pacote.montar` | `pacote.tamanho_bytes`, `modulos.count` |
| `pdf.gerar` | `pdf.paginas`, `pdf.tamanho_bytes` |
| `publicacao.executar` | `publicacao.destino`, `publicacao.host` (sem credencial!) |

### Anotação Spring

```java
@Observed(name = "entrega.criar",
          contextualName = "criar entrega",
          lowCardinalityKeyValues = { "endpoint", "POST /entregas" })
public Entrega criar(NovaEntregaRequest req) { ... }
```

---

## 5. Healthchecks

### Indicadores custom

```java
@Component
public class PacoteStorageHealthIndicator implements HealthIndicator {

    private final OrchestratorProperties props;

    @Override
    public Health health() {
        Path dir = props.pacotesDir();
        if (!Files.isDirectory(dir) || !Files.isWritable(dir)) {
            return Health.down()
                    .withDetail("dir", dir.toString())
                    .withDetail("motivo", "Diretório inacessível")
                    .build();
        }
        long livre = dir.toFile().getFreeSpace();
        if (livre < 5L * 1024 * 1024 * 1024) {  // < 5GB
            return Health.status("DEGRADED")
                    .withDetail("livre_bytes", livre)
                    .build();
        }
        return Health.up().withDetail("livre_bytes", livre).build();
    }
}
```

### Outros healthchecks

| Indicador | Verifica |
|---|---|
| `PacoteStorageHealthIndicator` | Disco escrevível e com espaço |
| `OrchestratorAsyncHealthIndicator` | Thread pool de geração não saturado |
| `SftpHealthIndicator` (Fase 3) | Conectividade SFTP de cada cliente configurado |
| `JenkinsHealthIndicator` (Fase 2) | Jenkins responde |
| `GitHubHealthIndicator` (Fase 2) | GitHub respondendo + rate limit OK |

---

## 6. Alertas

### Críticos (PagerDuty)
- Pod down > 1 min.
- DB down.
- Storage de pacotes com < 5% livre.
- Taxa de falha de geração > 20% em 10 min.

### Warning (Slack)
- Pacote em geração > 10 min (limite suspeito).
- > 5 entregas falhadas em 1 hora.
- Tempo médio de geração sobe > 100% vs baseline.
- Credencial cliente expirando em < 30 dias.
- Próxima entrega planejada com data passada.

### Insights (e-mail diário)
- Resumo de entregas do dia.
- Próximas 7 dias.
- Top 5 clientes com mais entregas no mês.
- Tempo médio das entregas.

---

## 7. Dashboards (Grafana sugerido)

### Painel "Orchestrator — Operacional"
- **KPIs**: entregas hoje, em curso, pendentes de aprovação.
- **Linha**: entregas/dia (últimos 30 dias).
- **Heatmap**: distribuição de entregas por dia da semana × hora.
- **Top 5**: clientes com mais entregas.
- **Erros 5xx**: por endpoint.

### Painel "Orchestrator — Detalhe"
- Latência p50/p95/p99 por endpoint.
- Tempo de geração de pacote (p95).
- Tempo de publicação por destino.
- Bytes uploadados/baixados por integração.
- Status dos healthchecks.

### Painel "Orchestrator — Negócio"
- Entregas por cliente (barra).
- Entregas por produto (barra).
- Tempo médio "planejada → publicada".
- % entregas sem incidentes (reentrega).

---

## 8. Distributed tracing entre release-orchestrator ↔ orchestrator

Quando orchestrator chama services públicos do release-orchestrator:
- Propagação automática de `traceparent` via `Micrometer Tracing`.
- Span do orchestrator pai → span do release-orchestrator filho no mesmo trace.

Resultado: tela única no Tempo/Jaeger mostra fluxo completo.

---

## 9. Logging de ações sensíveis (Audit)

### Padrão
Toda ação de geração/publicação que envolva credenciais ou dados de cliente:
1. **Registro em `OrchestratorAuditoria`** (DB, persistente).
2. **Log estruturado** (INFO) com `audit=true` para Elastic.
3. **Métrica** correspondente.

### Schema
```sql
CREATE TABLE orchestrator_auditoria (
  id              UUID PRIMARY KEY,
  acao            VARCHAR(60) NOT NULL,   -- ENTREGA_GERADA, CLIENTE_CRIADO, CREDENCIAL_ALTERADA, ...
  entidade_tipo   VARCHAR(40),            -- CLIENTE, ENTREGA, PRODUTO, ...
  entidade_id     UUID,
  usuario         VARCHAR(120) NOT NULL,
  ip              VARCHAR(45),
  user_agent      VARCHAR(300),
  detalhes        JSONB,                  -- payload contextual
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

---

## 10. Implementação faseada

| Fase | Foco | Esforço |
|---|---|---|
| **F4.1** | Logs estruturados + MDC | M |
| **F4.2** | Métricas básicas (entregas, geração) | M |
| **F4.3** | Tracing OTLP | L |
| **F4.4** | Healthchecks de storage e thread pool | S |
| **F4.6** | Auditoria expandida | M |
| **F4.10** | Dashboards Grafana | M |

---

## 11. Stack de observabilidade sugerida

| Componente | Stack | Notas |
|---|---|---|
| Logs | Logback + Logstash encoder | JSON em prod |
| Métricas | Micrometer + Prometheus | `/actuator/prometheus` |
| Tracing | Micrometer Tracing + OTLP exporter | Exporta para Tempo/Jaeger |
| APM | OpenTelemetry Collector | Sidecar agregador |
| Dashboards | Grafana | Painéis versionados em git |
| Alertas | Grafana Alerting / AlertManager | Canais: PagerDuty + Slack |

---

## 12. Cross-reference

- Bases gerais de observabilidade (logs, métricas, tracing) — consolidadas neste documento.
- [`33-decisoes-tecnicas.md`](33-decisoes-tecnicas.md) — Stack do orchestrator.
- [`99-melhorias-sugeridas.md`](99-melhorias-sugeridas.md) — Seção F.
