# 36 — Deploy e Operação

Spec de aspectos operacionais: deploy, configuração, backup, retenção, disaster recovery.

---

## 1. Modelo de deploy

### MVP — single instance
- 1 instância do Spring Boot rodando em VM/container.
- 1 PostgreSQL dedicado (ou compartilhado com release-orchestrator).
- Storage local em diretórios `releaseorchestrator.artefatos.dir` e `orchestrator.pacotes.dir`.

### Pós-MVP — escala horizontal
- Múltiplas instâncias atrás de load balancer.
- Storage compartilhado: NFS, S3 ou object storage.
- DB com replica de leitura.
- Cache distribuído (Redis) se houver state em memória relevante.

---

## 2. Configuração

### `application.yml` (prod)

```yaml
spring:
  application:
    name: softon-portal-api
  datasource:
    url: ${DB_URL}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate.jdbc.batch_size: 25
  flyway:
    enabled: true
    locations: classpath:db/migration

server:
  port: 8080
  forward-headers-strategy: framework

releaseorchestrator:
  artefatos:
    dir: ${RELEASEFLOW_ARTEFATOS_DIR:/var/lib/softon/artefatos}
    tamanho-maximo-mb: 500
  pdf:
    snapshot-dir: ${RELEASEFLOW_PDFS_DIR:/var/lib/softon/pdfs}

orchestrator:
  pacotes:
    dir: ${ORCHESTRATOR_PACOTES_DIR:/var/lib/softon/pacotes}
  async:
    core-pool-size: ${ORCH_ASYNC_CORE:2}
    max-pool-size: ${ORCH_ASYNC_MAX:4}
    queue-capacity: ${ORCH_ASYNC_QUEUE:25}
  publicacao:
    timeout-segundos: 600
    retry-tentativas: 3

management:
  endpoints:
    web:
      exposure:
        include: health, info, prometheus, metrics
  endpoint:
    health:
      probes:
        enabled: true
      show-details: when_authorized

logging:
  level:
    root: INFO
    br.com.softon: INFO
    org.springframework.security: WARN
```

### Secrets

| Variável | Conteúdo |
|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Credenciais Postgres |
| `JWT_SECRET` | Chave de assinatura JWT |
| `RELEASEFLOW_GITHUB_TOKEN` | (Fase 2) Token GitHub |
| `RELEASEFLOW_JENKINS_USER`, `_TOKEN` | (Fase 2) Credencial Jenkins |
| `JASYPT_ENCRYPTOR_PASSWORD` | (Fase 3) Chave de cifragem de credenciais de cliente |

**Nunca** versionar secrets. Usar:
- `.env` local (gitignored).
- Variáveis de ambiente em CI/prod.
- Vault em prod robusto.

---

## 3. Build e empacotamento

### Maven
```bash
./mvnw -pl application -am clean package -DskipTests=false
```

Gera `application/target/softon-portal-api-{version}.jar` (fat jar runnable).

### Dockerfile sugerido

```dockerfile
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S softon && adduser -S softon -G softon

COPY --chown=softon:softon application/target/*.jar app.jar

USER softon
EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "/app/app.jar"]
```

### Tags Docker
- `softon/portal-api:1.0.0` (versão).
- `softon/portal-api:latest` (latest released).
- `softon/portal-api:sha-abc1234` (commit).

---

## 4. CI/CD

### Pipeline sugerido

```text
push → trigger pipeline
  1. Checkout
  2. Setup Java 21
  3. mvn verify              ← unit + integration tests
  4. Build Docker image
  5. Push image (registry)
  6. Deploy staging          ← automático
  7. E2E tests em staging
  8. Tag release             ← manual
  9. Deploy prod             ← manual após aprovação
```

### Ferramentas
- GitHub Actions / GitLab CI / Jenkins (interno?).
- Registry: Docker Hub privado ou Harbor.
- Deploy: Kubernetes, Docker Compose, ou ansible.

### Gates de qualidade
- Cobertura ≥ 70% no service layer (falha senão).
- Sem vulnerabilidades críticas (`trivy`, `snyk`).
- Build < 10 min.
- Testes < 15 min total.

---

## 5. Kubernetes (se aplicável)

### Recursos
```yaml
resources:
  requests:
    memory: "1Gi"
    cpu: "500m"
  limits:
    memory: "4Gi"
    cpu: "2000m"
```

### Probes
```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 30

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
```

### Volumes
- PV/PVC para `artefatos/`, `pacotes/`, `pdfs/`.
- Backup do volume via snapshot.

### ConfigMap
- `application.yml` modificações de ambiente.

### Secrets
- DB credentials.
- JWT secret.
- Jasypt encryptor.

---

## 6. Storage strategy

### MVP
- Filesystem local. Diretórios separados por tipo:
  ```
  /var/lib/softon/
  ├── artefatos/     (release-orchestrator)
  ├── pdfs/          (release-orchestrator snapshots)
  └── pacotes/       (orchestrator)
  ```
- Permissão R/W para o user que roda o Spring Boot.

### Evolução: NFS (multi-instância simples)
- Múltiplas instâncias montam o mesmo NFS.
- Cuidado com locking — usar nome de arquivo único (SHA-based).

### Evolução: Object Storage (S3-compatible)
- Abstração via `ArtefatoStorage` / `PacoteStorage`.
- Permite trocar FS para S3 sem mexer no código de domínio.
- Vantagens: backup nativo, replicação, escala infinita.

### Permissões
- Apenas o processo Java tem acesso.
- Web server (Nginx) NÃO serve arquivos diretamente — sempre via endpoint Java (autenticação + audit).

---

## 7. Backup

### O que fazer backup
1. **DB PostgreSQL**: dump completo diário + WAL contínuo.
2. **Storage de artefatos**: rsync incremental para destino remoto.
3. **Storage de pacotes**: idem.
4. **Configuração**: versionada em git (sem secrets!).

### Frequência
- DB: diário (3 retenção: diário 7d, semanal 4w, mensal 12m).
- Storage: diário incremental + semanal full.

### Onde
- Off-site (S3/Glacier, ou storage de outro provedor).
- Encriptado em repouso.

### Restore drill
- Praticar trimestralmente: pegar backup e restaurar em ambiente clean.
- Documentar RPO/RTO atingido.

---

## 8. Política de retenção

| Recurso | Retenção MVP | Retenção evoluída |
|---|---|---|
| Artefatos uploadados | Permanente | Permanente |
| Pacotes gerados | Permanente | 1 ano hot, depois cold |
| PDFs snapshot | Permanente | Permanente |
| Logs aplicação | 30 dias | 30 dias hot, 1 ano cold |
| Métricas Prometheus | 30 dias | 90 dias |
| Backups DB | 12 meses | 12 meses |
| Auditoria (`orchestrator_auditoria`) | Permanente | Permanente |
| Entregas canceladas | Permanente | Mover para cold após 1 ano |

---

## 9. Migrations em prod

### Padrão
- Flyway aplica automaticamente no startup.
- Migrações **forward-only**. Sem rollback automático.
- Para rollback: criar nova migration que reverte.

### Migrations longas
- Não rodar `ALTER TABLE` em tabelas grandes durante peak.
- Para mudanças pesadas: rodar manualmente em janela de manutenção.

### Validações pré-deploy
- `flyway info` em staging antes de prod.
- Aplicar em staging primeiro, validar com smoke tests.

---

## 10. Janelas de manutenção

### Quando precisa
- Migration grande (tabela com >1M registros).
- Mudança incompatível de versão DB.
- Mudança de schema de storage.

### Protocolo
1. Comunicar 7 dias antes.
2. Programar fora de horário de pico.
3. Status page atualizada.
4. Rollback plan documentado.
5. Smoke test pós-manutenção.

---

## 11. Monitoramento operacional

Já coberto em `34-observabilidade.md`. Recapitulando o **mínimo operacional**:

| Métrica | Alerta |
|---|---|
| Pod / processo down | Crítico |
| DB down | Crítico |
| Storage < 10% livre | Crítico |
| Storage < 20% livre | Warning |
| 5xx > 1% em 5 min | Crítico |
| 5xx > 0.5% em 15 min | Warning |
| Geração média > 2x baseline | Warning |

---

## 12. Runbooks (sugerido criar)

### Como restaurar DB
1. Parar API.
2. Drop DB atual.
3. Restore último dump + replay WAL até momento desejado.
4. Subir API.
5. Validar healthcheck.

### Como adicionar instância
1. Provisionar VM/pod.
2. Configurar volumes compartilhados.
3. Aplicar `application.yml` com variáveis.
4. Subir.
5. Health check.
6. Adicionar ao LB.

### Como remover artefato de produção
1. Justificar (LGPD, vazamento, erro).
2. Aprovar via 2 pessoas.
3. Job manual: soft-delete no DB.
4. Job de limpeza eventualmente remove do FS.
5. Documentar em auditoria externa.

---

## 13. Custos operacionais

### MVP estimado
- 1 VM (4 vCPU, 16GB RAM): ~$60/mês.
- DB PostgreSQL (4 vCPU, 16GB): ~$150/mês.
- Storage 500GB SSD: ~$50/mês.
- Backup off-site: ~$20/mês.
- **Total**: ~$280/mês.

### Sinais de necessidade de escalar
- CPU > 70% sustentado.
- Pool de threads de async saturado.
- p95 de geração > 5x baseline.
- DB latency > 100ms p95.

---

## 14. Disaster Recovery (DR)

### Cenários
| Cenário | RPO | RTO |
|---|---|---|
| Falha do pod | 0 | 1 min (rolling) |
| Falha do nó | 0 | 5 min (k8s reschedule) |
| Falha do DC primário | 1h (replicação) | 4h (failover) |
| Corrupção de dados | 24h (backup) | 8h (restore) |

### Plano DR (resumido)
1. Identificar nível do desastre.
2. Comunicar stakeholders.
3. Failover para DR site (se aplicável).
4. Restore de backup se necessário.
5. Smoke test.
6. Comunicar restabelecimento.
7. Post-mortem.

---

## 15. Compliance e LGPD

### Dados sensíveis no orchestrator
- Cliente: razão social, CNPJ, endereço, e-mails de contato.
- Credenciais de entrega.

### Princípios
- Minimização: só coletar o necessário.
- Retenção: apagar dados de cliente encerrado após período legal.
- Acesso: log de quem viu o quê.
- Portabilidade: exportar dados de cliente a pedido.
- Direito ao esquecimento: anonimização (hash dos campos pessoais), mantém auditoria com pseudônimo.

### Audit
- `orchestrator_auditoria` mantém registro permanente.
- Acessos a dados sensíveis registrados separadamente.

---

## 16. Cross-reference

- [`33-decisoes-tecnicas.md`](33-decisoes-tecnicas.md) — Stack técnica.
- [`34-observabilidade.md`](34-observabilidade.md) — Monitoramento.
- [`35-testes-qa.md`](35-testes-qa.md) — Validação contínua.
- [`../ROADMAP.md`](../ROADMAP.md) — Fases de evolução.
- [`99-melhorias-sugeridas.md`](99-melhorias-sugeridas.md) — Seção H.
