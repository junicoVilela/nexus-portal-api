# 38 — Glossário

Termos do domínio do **Release Orchestrator + Orchestrator**. Use este documento para alinhar vocabulário entre time, specs e código.

---

## Domínios de negócio

### **Cliente**
Pessoa jurídica que recebe entregas (pacotes) dos produtos Softon. Tem CNPJ, sigla, contatos, configuração de entrega.

### **Contato (do cliente)**
Pessoa física associada a um cliente. Recebe notificações por e-mail/papel quando uma entrega acontece.

### **Configuração de Entrega**
Como o pacote é entregue ao cliente: destino (PASTA local, FTP, SFTP, Bucket), caminho, credenciais, janela permitida, aprovação obrigatória.

### **Domínio (de funcionalidade)**
Área funcional do sistema (Usuários, Relatórios, Integrações). Catálogo por **produto**.

### **Funcionalidade**
Ação específica dentro de um domínio (Inserir, Bloquear, Resetar Senha). Catálogo por **produto** sob um domínio.

### **ClienteFuncionalidade**
Matriz que indica quais funcionalidades um cliente específico tem habilitadas. Alimenta os módulos `FUNCIONALIDADES` e `REGRAS` no pacote.

---

## Produto e módulos

### **Produto** (`ProdutoRh` — a renomear)
Sistema gerenciado pela Softon (ex.: DTEC-LD, FOLHA-WEB). Tem nome, sigla, cor, módulos, releases.

### **ModuloProduto**
Sub-componente do produto: WEB, BATCH, BANCO, KETTLE, FUNCIONALIDADES ou REGRAS. Catálogo por produto.

### **Tipo de Módulo**
Enum dos 6 tipos suportados:
- **WEB**: artefato `.war` ou `.jar` da aplicação web.
- **BATCH**: artefato `.jar` de processamento batch.
- **BANCO**: scripts SQL (DDL/DML) — gera delta.
- **KETTLE**: jobs Pentaho ETL — gera delta.
- **FUNCIONALIDADES**: gerado da config do cliente.
- **REGRAS**: gerado da config do cliente.

### **ClienteProduto**
Vínculo entre cliente e produto contratado. Configura ambiente e ativação.

### **ClienteProdutoModulo**
Para cada módulo contratado por um cliente, qual a versão atual instalada. Atualizado após cada entrega bem-sucedida.

---

## Release Orchestrator

### **Release**
Versão entregável de um produto (ex.: v1.5.0). Tem ciclo de vida (RASCUNHO → PUBLICADA) e contém itens.

### **Versão semântica**
Formato `major.minor.patch[-pré-release]` (ex.: `1.5.0`, `2.0.0-beta`).

### **Tipo de Release**
MAJOR / MINOR / PATCH / HOTFIX / FEATURE. Não calculado automaticamente — escolhido pelo autor.

### **Status da Release**
- **RASCUNHO**: criação inicial.
- **EM_DESENVOLVIMENTO**: trabalho em curso.
- **EM_REVISAO**: pronta para revisão.
- **APROVADA**: revisão concluída, aguardando publicação.
- **PUBLICADA**: entregue/encerrada. Terminal.
- **CANCELADA**: descartada. Terminal.

### **Item da Release** (`ReleaseItem`)
Unidade de conteúdo: uma funcionalidade, correção, melhoria. Tem categoria, visibilidade, ordem, ticket.

### **Categoria do Item**
NOVIDADE, MELHORIA, CORRECAO, SEGURANCA, PERFORMANCE, DOCUMENTACAO, AJUSTE_TECNICO, IMPACTO_OPERACIONAL, IMPORTANTE.

### **Visibilidade do Item**
- **TODOS**: visível ao cliente final (no PDF do cliente).
- **SUPORTE**: visível ao suporte interno.
- **TECNICO**: visível apenas internamente (equipe técnica).

### **Histórico** (`ReleaseHistorico`)
Audit trail append-only de toda ação na release. Cada entrada tem ação, descrição, status antes/depois, usuário, data.

### **Template** (`ReleaseTemplate`)
Estrutura reutilizável (Markdown) usada como ponto de partida ao criar uma release.

### **ReleaseModuloVersao**
Vínculo entre release e a versão específica de cada módulo. No MVP é rótulo livre; no pós-MVP é tag GitHub.

### **Artefato** (`ArtefatoReleaseModulo`)
Arquivo entregável uploadado para um módulo da release. Tem SHA-256 para integridade.

---

## Orchestrator

### **Próxima Entrega** (`ProximaEntrega`)
Item da agenda: planejamento de uma entrega futura. Tem data planejada, cliente, produto, release alvo.

### **Status da Próxima Entrega**
- **PLANEJADA**: criada, ainda não aprovada.
- **APROVADA**: pronta para gerar.
- **EM_GERACAO**: virou Entrega, geração em curso.
- **CONCLUIDA**: virou Entrega concluída.
- **CANCELADA**: descartada.

### **Entrega** (`Entrega`)
Execução real de uma entrega para um cliente. Cria um Pacote a partir das versões alvo dos módulos e do delta.

### **Status de Geração** (`GeracaoStatus`)
- **PENDENTE**: criada, aguardando worker.
- **PROCESSANDO**: geração em andamento.
- **CONCLUIDO**: pacote pronto.
- **ERRO**: falhou durante geração.
- **CANCELADO**: interrompido manualmente.

### **Delta**
Conjunto de scripts/artefatos entre a versão atual no cliente e a versão alvo da release. Calculado por strategy específica do tipo de módulo.

### **Pacote**
ZIP final montado contendo: artefatos por módulo, scripts de delta, manifest, checksums, release-notes.pdf.

### **Manifest**
Arquivo JSON dentro do pacote com a lista completa do que foi entregue: módulos, versões, arquivos, SHAs.

### **Checksums**
Arquivo `.sha256` listando SHA-256 de cada arquivo do pacote. Cliente pode verificar integridade.

### **Publicação**
Ato de enviar o pacote pronto para o destino configurado do cliente (pasta local no MVP).

### **Reentrega**
Nova `Entrega` apontando para entrega anterior (`entregaOriginalId`). Pode reutilizar pacote ou regerar.

---

## CI/CD e versionamento

### **Tag (Git)**
Referência imutável a um commit (ex.: `v1.5.0`). Dispara Jenkins e identifica assets no GitHub Release. Ver [`40-guia-versao-tag.md`](40-guia-versao-tag.md).

### **Asset (GitHub Release)**
Arquivo binário anexado a uma release GitHub (`.war`, `.jar`, `.sql`, `.zip`). Baixado pelo orchestrator na Fase 2.

### **Build-on-tag**
Padrão Jenkins: pipeline executado ao criar tag `v*.*.*` no repositório. Ver [`39-entregaveis-cicd-repositorios.md`](39-entregaveis-cicd-repositorios.md).

### **FROM_TAG / TO_TAG**
Intervalo de versões para cálculo de delta (banco, kettle). `FROM` = última entregue ao cliente; `TO` = release-alvo. Ver [`20-range-manual-delta.md`](20-range-manual-delta.md).

### **Entregável de repositório**
Artefato ou pipeline que a equipe de produto mantém **fora do portal** (Jenkinsfile, scripts, publicação no GitHub). Distinto de funcionalidade implementada no `softon-portal-api`.

---

## Auditoria

### **Auditoria**
Registro de quem fez o quê, quando. Diferente de log de aplicação (volátil) — persistente no banco.

### **Append-only**
Modelo onde registros nunca são editados ou removidos individualmente. Histórico é imutável.

### **Snapshot**
Cópia imutável de um estado. Ex.: `ReleasePdfSnapshot` (PDF gerado de uma versão publicada).

---

## Segurança

### **Role**
Papel do usuário no sistema. Atualmente: ADMIN, EDITOR. Pós-Fase 0: ADMIN, EDITOR, VIEWER.

### **Permissão (granular)**
Capacidade nomeada (`release.criar`, `cliente.editar`). Mapeada de Role → Set<Permissão>.

### **JWT**
JSON Web Token usado para autenticação. Validado em cada requisição pelo `JwtAuthFilter`.

### **Credencial cifrada**
Credenciais sensíveis (token GitHub, senha SFTP) armazenadas com `Jasypt`. Nunca expostas após salvas.

---

## Operação

### **Storage**
Diretório no filesystem onde artefatos e pacotes são armazenados. Por tipo: `artefatos/`, `pdfs/`, `pacotes/`.

### **SHA-256**
Hash criptográfico de 256 bits usado para identificar arquivos univocamente. Detecta corrupção e duplicidade.

### **Idempotência**
Garantia que repetir uma operação produz o mesmo resultado. Importante em mutações com retry.

### **Healthcheck**
Endpoint que reporta saúde do serviço (`/actuator/health`). Kubernetes usa para liveness/readiness probes.

### **CorrelationId**
UUID gerado por requisição que aparece em logs, métricas e traces para casar tudo da mesma operação.

---

## Tecnologia (siglas comuns)

### **MVP**
Minimum Viable Product. Primeiro ciclo de entrega.

### **CRUD**
Create, Read, Update, Delete. Operações básicas em entidade.

### **DTO**
Data Transfer Object. Objeto usado para transferir dados entre camadas (especialmente em request/response da API).

### **FSM**
Finite State Machine. Modelo de estado com transições controladas (ex.: ciclo de vida da release).

### **OTLP**
OpenTelemetry Protocol. Padrão para exportar traces.

### **RBAC**
Role-Based Access Control. Modelo de autorização baseado em papéis.

### **N+1**
Anti-padrão de query: 1 query principal + N queries por linha. Resolvido com `@EntityGraph` ou JOIN explícito.

### **TTL**
Time To Live. Período de validade de cache, token ou registro.

### **RPO/RTO**
Recovery Point Objective / Recovery Time Objective. Métricas de disaster recovery.

---

## Convenções no projeto

### **Sufixos em arquivos**

| Sufixo Java | Significado |
|---|---|
| `*Controller` | Controller REST |
| `*Service` | Camada de regras de negócio |
| `*Repository` | Acesso a dados |
| `*Request` | DTO de request |
| `*Response` | DTO de response |
| `*Exception` | Exceção custom |

| Sufixo TS | Significado |
|---|---|
| `*.component.ts` | Componente Angular |
| `*.service.ts` | Service Angular |
| `*.model.ts` | Interface TS de modelo |
| `*.routes.ts` | Definição de rotas |
| `*.guard.ts` | Guard de rota |
| `*.interceptor.ts` | Interceptor HTTP |
| `*.pipe.ts` | Pipe Angular |
| `*.validator.ts` | Validator de form |

### **Prefixos a remover (cf. naming-suggestions)**

| Antes | Depois |
|---|---|
| `ProdutoRh*` | `Produto*` |
| `Rh*` (frontend components) | sem prefixo |
| `Hub*` (frontend) | `Flow*` ou sem prefixo |
| `RELEASE_ORCHESTRATOR_ROUTES` | `RELEASE_ORCHESTRATOR_ROUTES` |

---

## Cross-reference

- [`../naming-suggestions.md`](../naming-suggestions.md) — Propostas de renomeação.
- [`39-entregaveis-cicd-repositorios.md`](39-entregaveis-cicd-repositorios.md) — Entregáveis nos repos.
- [`40-guia-versao-tag.md`](40-guia-versao-tag.md) — Versionamento e tags.
- [`32-modelo-dados-sugerido.md`](32-modelo-dados-sugerido.md) — Modelo de dados.
- [`33-decisoes-tecnicas.md`](33-decisoes-tecnicas.md) — Stack.
