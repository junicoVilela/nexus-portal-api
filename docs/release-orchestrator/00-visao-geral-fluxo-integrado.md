# 00 — Visão Geral do Fluxo Integrado

Documento de entrada para entender como **Gestão de Releases** (já implementada) e **Entregas a Clientes** (a implementar) coexistem no mesmo **Release Orchestrator**. Antes de mergulhar nas specs de tela, ler este.

---

## 1. Contexto e motivação

A Softon mantém vários produtos internos (DTEC-LD, FOLHA-WEB, etc.) instalados em **múltiplos clientes**. Cada cliente:

- Tem versão própria de cada módulo (banco, web, batch, kettle).
- Tem combinação própria de funcionalidades habilitadas.
- Recebe atualizações em ciclos individuais.

Hoje (pré-entregas):
- O Release Orchestrator registra **o que foi entregue como produto** (versão da release, itens, audit).
- Mas **não orquestra** a entrega aos clientes — quem recebeu, quando, qual versão, qual delta de scripts.
- Esse trabalho é manual: planilhas, e-mails, FTPs.

**A orquestração de entregas resolve isso**: torna a entrega rastreável, repetível e auditável.

---

## 2. Quem usa (personas)

### Engenheiro de release
- Cria releases, marca itens, gerencia ciclo de vida.
- Hoje já usa Release Orchestrator.

### Gestor de produto / suporte
- Decide quando um cliente receberá qual release.
- Hoje gerencia via planilha; passa a usar a agenda do orchestrator.

### Operador / DevOps
- Executa a entrega: gera pacote, publica no destino do cliente.
- Hoje monta pacote manualmente; passa a usar o assistente de nova entrega.

### Suporte ao cliente
- Consulta histórico ("o que foi entregue a esse cliente?").
- Hoje consulta logs/e-mails; passa a usar a tela de suporte operacional (27).

### Auditor / compliance
- Verifica conformidade (quem entregou o quê, quando).
- Hoje sem ferramenta dedicada; passa a usar a tela de auditoria (futura).

---

## 3. O que já existe (gestão de releases)

Implementado no módulo Maven `release-orchestrator` do backend `softon-portal-api`. As specs técnicas correspondentes foram consolidadas neste mesmo conjunto de documentos.

### Entidades implementadas
- `ProdutoRh` — produtos gerenciados.
- `Release` — versão entregável com ciclo `RASCUNHO → EM_DESENVOLVIMENTO → EM_REVISAO → APROVADA → PUBLICADA` (+ `CANCELADA`).
- `ReleaseItem` — itens da release com categoria e visibilidade (`TODOS` / `SUPORTE` / `TECNICO`).
- `ReleaseHistorico` — auditoria append-only.
- `ReleaseTemplate` — templates estruturais.

### Funcionalidades implementadas
- CRUD completo de produto, release, item, template.
- Transições de status com validação (grafo declarado).
- Duplicação de release com cópia dos itens.
- Validação pré-publicação (pendências + alertas).
- Histórico de auditoria para todas as ações.

---

## 4. O que falta na Fase 0 (antes das entregas a clientes)

Pré-requisitos no **mesmo módulo Maven** `release-orchestrator` antes de implementar entregas a clientes:

### 4.1 `ModuloProduto` (catálogo livre de módulos por produto)
- Cadastrável: nome, código, tipo, defaults.
- Tipos fixos: `WEB`, `BATCH`, `BANCO`, `KETTLE`, `FUNCIONALIDADES`, `REGRAS`.
- Detalhe em [`10-produtos-modulos-artefatos.md`](10-produtos-modulos-artefatos.md).

### 4.2 `ReleaseModuloVersao` (vínculo release ↔ versão por módulo)
- Cada release menciona qual versão de cada módulo está incluindo.
- No MVP: rótulo livre (ex.: `1.5.0`). No pós-MVP: tag GitHub real.

### 4.3 `ArtefatoReleaseModulo` (upload manual de artefatos)
- Upload de `.war`, `.jar`, `.sql`, `.ktr` etc.
- SHA-256 calculado pelo backend.
- Imutável após release publicada.
- Detalhe em [`10-produtos-modulos-artefatos.md`](10-produtos-modulos-artefatos.md) e [`14-release-orchestrator-detalhe.md`](14-release-orchestrator-detalhe.md).

### 4.4 Renderer Markdown → PDF
- Stack: Thymeleaf + openhtmltopdf + commonmark.
- Geração on-demand + snapshot na publicação.
- Filtros de visibilidade (CLIENTE/SUPORTE/INTERNO).
- Detalhe em [`25-documento-release-md-pdf.md`](25-documento-release-md-pdf.md).

### 4.5 `ROLE_LEITOR` + permissões granulares
- Detalhe em [`33-decisoes-tecnicas.md`](33-decisoes-tecnicas.md) (autorização).

### 4.6 Naming cleanup (opcional, mas recomendado)
- `ProdutoRh` → `Produto`.
- Frontend: remover prefixos `Rh*` / `Hub*`.
- Spec em [`../naming-suggestions.md`](../naming-suggestions.md).

---

## 5. O que entra na Fase 1 (entregas a clientes)

Extensão do módulo Maven `release-orchestrator` (mesmo artefato, novo domínio). Escopo:

### 5.1 Cadastros base
- **Cliente**: dados básicos + contatos.
- **Configuração de Entrega**: destino (PASTA/FTP/SFTP/BUCKET), credenciais.
- **Domínio + Funcionalidade**: catálogo por produto.
- **ClienteFuncionalidade**: matriz que habilita/desabilita por cliente.
- **ClienteProduto / ClienteProdutoModulo**: produtos contratados + versão atual instalada por módulo.

### 5.2 Planejamento
- **Próxima Entrega**: agenda de entregas futuras. Status: PLANEJADA / APROVADA / EM_GERACAO / CONCLUIDA / CANCELADA.

### 5.3 Execução
- **Assistente de Nova Entrega**: wizard multi-step.
- **Seleção de módulos**: marca quais módulos contratados serão enviados.
- **Cálculo de delta**: scripts/artefatos entre versão atual e target, por strategy de tipo.
- **Geração de pacote**: monta ZIP final, manifest, checksums, release-notes.pdf.
- **Publicação**: copia/envia para o destino configurado.

### 5.4 Pós-entrega
- **Entrega**: registro de execução (com `GeracaoStatus`).
- **Histórico de entregas**: consulta histórica.
- **Reentrega**: nova Entrega com `entregaOriginalId` apontando para original.
- **Relatórios**: por cliente, por produto, por período.
- **Suporte operacional**: "o que esse cliente recebeu?".

---

## 6. Escopo MVP vs pós-MVP

### MVP — primeiro ciclo

| Funcionalidade | Detalhe |
|---|---|
| Upload manual de artefatos | Ver `14-release-orchestrator-detalhe.md` |
| Pacote em pasta local fixa | Configurado por servidor, sem credencial por cliente |
| PDF + manifest + checksums | Stack fechada (Thymeleaf + openhtmltopdf) |
| Histórico + reentrega local | Reutilizar pacote ou regerar |
| Cliente com destino `PASTA` | Único tipo suportado |
| Cadastros base, agenda, assistente | UI completa para o fluxo |

### Pós-MVP — ciclos futuros

| Funcionalidade | Quando |
|---|---|
| GitHub Releases (download de assets) | Fase 2 |
| Pipeline Jenkins (build-on-tag) | Fase 2 |
| Cálculo automático de delta via Git | Fase 2 |
| Publicação FTP / SFTP / Bucket | Fase 3 |
| Credenciais cifradas (Jasypt / Vault) | Fase 3 |
| Tela de credenciais (GitHub, Jenkins, SFTP) | Fase 3 |
| Multi-DC, escala horizontal | Fase 4 |

Esta separação está marcada com avisos no topo das specs individuais afetadas (07, 09, 10, 14, 19, 20, 21, 28, 29, 32). Ver [`../ROADMAP.md`](../ROADMAP.md) para sequenciamento completo.

---

## 6.1 Mapa história → documentação

Legenda: **✅** implementado no portal · **📋** especificado · **🔧** nos repositórios de produto (fora do portal).

| Bloco da história | Onde está documentado | Estado |
|---|---|---|
| Gestão de releases (produto, itens, workflow) | `01`–`08` (parcial), web `02`–`10` | ✅ portal |
| Módulos por produto (WEB/BATCH/BANCO/…) | `10`, Fase 0 em §4 | 📋 portal |
| Upload manual de artefatos | `14`, `21` (MVP) | 📋 portal |
| PDF release (MD → PDF) | `25`, `14` aba PDF | 📋 portal |
| Clientes, domínios, funcionalidades | `02`–`07` | 📋 portal |
| Próximas entregas + assistente | `16`–`18` | 📋 portal |
| Delta + range manual | `19`, `20` | 📋 portal |
| Geração de pacote por cliente | `21`, `22` | 📋 portal |
| Histórico, reentrega, suporte | `23`, `27` | 📋 portal |
| WAR/JAR via GitHub Releases | `09`, `21` §3, `39` | 📋 Fase 2 |
| Jenkins build-on-tag | `09`, `39`, `40` | 🔧 repos + 📋 Fase 2 |
| DDL/DML, Kettle, func/regras nos repos | `39` §2.3–2.6 | 🔧 repos |
| Guia versão/tag por produto | `40` | 🔧 repos (template) |
| FTP / destino remoto | `07`, `21` §10, Fase 3 | 📋 pós-MVP |
| Jenkinsfile + pipelines nos repos | `39` §3 | 🔧 repos |
| Prazos (~2 dias por entregável) | `39` §2, `../ROADMAP.md` F2.0 | 📋 checklist |

> Entregáveis **nos repositórios** (Jenkinsfile, scripts, publicação de assets) não são código do portal — ver [`39-entregaveis-cicd-repositorios.md`](39-entregaveis-cicd-repositorios.md) e [`40-guia-versao-tag.md`](40-guia-versao-tag.md).

---

## 7. Fluxo integrado completo

### Cenário feliz: nova versão entregue a um cliente

```text
[Time de produto]
    1. Produto cadastrado + módulos catalogados
    2. Tag criada no GitHub                        (pós-MVP)
    3. Jenkins faz build → publica assets          (pós-MVP)
    4. (No MVP: upload manual dos assets)

[Time de release]
    5. Release criada no Release Orchestrator
    6. Adiciona itens (novidades, correções)
    7. Vincula versão de cada módulo
    8. (MVP) Faz upload manual dos artefatos
    9. Envia para revisão
    10. Revisor aprova
    11. Publica → status PUBLICADA, gera PDFs snapshot

[Time de release/operação]
    12. Orchestrator: planeja próxima entrega para Cliente X
    13. Operador aprova a próxima entrega
    14. Operador inicia assistente de nova entrega:
        a. Seleção de módulos contratados
        b. Cálculo de delta (versão atual no cliente vs target)
        c. Confirma e dispara geração
    15. Geração assíncrona:
        a. Coleta artefatos da release
        b. Calcula delta por strategy (Banco, Kettle, Web, etc.)
        c. Monta ZIP com módulos, manifest, checksums
        d. Gera release-notes.pdf (cliente)
        e. Salva pacote em /var/lib/softon/pacotes/{entregaId}/
    16. Operador baixa o pacote ou publica no destino
    17. Sistema atualiza ClienteProdutoModulo.versaoAtual
    18. Histórico atualizado, reentrega disponível
```

---

## 8. Responsabilidades por domínio (mesmo módulo Maven)

### Domínio Releases (implementado)

**Conhece:**
- Produtos e módulos do produto.
- Releases (versão por módulo).
- Itens, templates, histórico interno.
- Artefatos por módulo (Fase 0).
- PDF da release (interno/suporte/cliente).

**Não conhece (Fase 1):**
- Quem são os clientes.
- Configurações de entrega.
- Histórico de entregas.

### Domínio Entregas (Fase 1 — a implementar)

**Conhece:**
- Clientes, contatos, configuração de entrega.
- Produtos contratados e módulos contratados por cliente.
- Domínios e funcionalidades por cliente.
- Próximas entregas e agenda.
- Entrega real: seleção de módulos, delta, geração de pacote, publicação.
- Histórico de entregas e reentrega.
- Relatórios e suporte operacional.

**Não conhece:**
- Detalhes internos da release (itens, fluxo de aprovação).
- Como o artefato foi buildado.

**Consome o domínio Releases via services internos:**
- `ProdutoService.buscar(UUID)`
- `ReleaseService.buscar(UUID)`, `listar(filtros)`
- `ReleaseItemService.listar(UUID)`
- `ModuloProdutoService.listar(produtoId)` (Fase 0)
- `ArtefatoService.listar(releaseId)` (Fase 0)
- `ReleasePdfService.gerar(...)` (Fase 0)

---

## 9. Conceito de domínio e funcionalidade

Modelagem para responder: **"o que cada cliente tem habilitado?"**

- **Domínio**: área funcional do sistema. Exemplos: Usuários, Grupos de Acesso, Clientes, Produtos, Relatórios, Integrações, Parâmetros.
- **Funcionalidade**: ação dentro do domínio. Exemplos: Inserir, Atualizar, Bloquear, Desbloquear, Deletar, Visualizar, Exportar, Resetar Senha, Associar Perfil.

**Cadastros:**
- Funcionalidades são cadastradas por **produto** (catálogo padrão).
- Cada **cliente** habilita/desabilita as funcionalidades que possui.

**Uso:**
- Alimenta os módulos `FUNCIONALIDADES` e `REGRAS` no momento da geração do pacote.
- Os scripts/configurações gerados dependem do que está habilitado no cliente.

**Exemplo concreto:**

| Domínio | Funcionalidade | Cliente A | Cliente B |
|---|---|---|---|
| Usuários | Inserir | ✅ | ✅ |
| Usuários | Bloquear | ✅ | ❌ |
| Usuários | Resetar Senha | ✅ | ✅ |
| Relatórios | Exportar PDF | ✅ | ❌ |
| Relatórios | Exportar Excel | ✅ | ✅ |
| Integrações | API REST | ❌ | ✅ |

Spec dedicada em [`05-cliente-dominios-funcionalidades.md`](05-cliente-dominios-funcionalidades.md).

---

## 10. Resultado esperado (perguntas que o sistema responde)

Após implementação:

- ✅ Qual release foi publicada para o produto X?
- ✅ Qual cliente recebeu qual versão de qual módulo?
- ✅ Quais módulos foram enviados em cada entrega?
- ✅ Quais funcionalidades cada cliente possui?
- ✅ Quais scripts (DDL/DML/Kettle/Funcionalidades) entraram em um pacote específico?
- ✅ Onde o pacote foi publicado e quando?
- ✅ É possível reentregar sem rebuild?
- ✅ Qual é o histórico completo de entregas do cliente X?
- ✅ Quantas entregas tivemos no último mês?
- ✅ Qual a próxima entrega planejada?
- ✅ Houve falha de geração? Por quê?

---

## 11. Não-objetivos (out of scope)

Para deixar claro o que o orchestrator **NÃO** faz:

- ❌ Não substitui git/repositório de código.
- ❌ Não gerencia código-fonte ou branches.
- ❌ Não executa testes de regressão dos produtos.
- ❌ Não monitora aplicações em runtime nos clientes (não é APM).
- ❌ Não substitui ITSM (não é ServiceNow).
- ❌ Não faz cobrança / faturamento.
- ❌ Não gerencia SLA contratual com cliente.
- ❌ Não distribui hot-fixes em runtime (cliente aplica o pacote).

---

## 12. Métricas de sucesso (post-launch)

Para avaliar se o orchestrator entregou o valor esperado:

| Métrica | Baseline (hoje) | Meta após orchestrator |
|---|---|---|
| Tempo médio: planejar → publicar entrega | desconhecido (planilha) | < 30 minutos |
| % entregas com erro/rework | desconhecido | < 5% |
| Tempo de resposta a "o que cliente X recebeu?" | minutos a horas | < 30 segundos |
| Reentrega bem-sucedida sem rebuild | manual / sujeito a erro | 100% automatizado |
| Auditoria de quem entregou o quê | parcial | 100% rastreável |

---

## 13. Riscos e mitigações

| Risco | Mitigação |
|---|---|
| Cálculo de delta errado (script faltando) | Tests de strategy + validação de pacote antes de publicar |
| Pacote corrompido em trânsito | SHA-256 + manifest verificável |
| Credencial vazada | Cifragem em repouso (Jasypt) + nunca exibir após salvar |
| Geração trava (deadlock, OOM) | Timeout configurado + cancelamento manual + métricas |
| Storage cheio | Healthcheck + alerta + política de retenção |
| Dependência forte do release-orchestrator | Versionamento explícito da API + contratos OpenAPI |
| Erro de seleção de módulo | Pré-validação + diff visual antes de gerar |
| Cliente reportando "não recebi" | Auditoria + reentrega + log de publicação |

---

## 14. Cronograma estimado (high-level)

| Marco | Duração estimada | Conteúdo |
|---|---|---|
| Fase 0 (pré-orchestrator) | 4-6 semanas | ModuloProduto, Artefato, PDF, naming cleanup |
| Fase 1 — MVP Orchestrator | 8-12 semanas | Cadastros + agenda + assistente + geração local |
| Fase 2 — GitHub/Jenkins | 6-8 semanas | Integrações automáticas |
| Fase 3 — Publicação remota | 4-6 semanas | FTP/SFTP/Bucket + credenciais cifradas |
| Fase 4 — Hardening | contínuo | Observabilidade, performance, escala |

Total MVP (Fase 0 + 1): 3-4 meses. Roadmap completo: [`../ROADMAP.md`](../ROADMAP.md).

---

## 15. Documentos relacionados

- [`99-padroes-tela.md`](99-padroes-tela.md) — Padrões aplicáveis a todas as telas (loading, vazio, erro, etc.).
- [`32-modelo-dados-sugerido.md`](32-modelo-dados-sugerido.md) — Entidades novas em detalhe.
- [`33-decisoes-tecnicas.md`](33-decisoes-tecnicas.md) — Stack PDF, storage, async, autorização.
- [`34-observabilidade.md`](34-observabilidade.md) — Logs, métricas, tracing.
- [`35-testes-qa.md`](35-testes-qa.md) — Estratégia de testes.
- [`36-deploy-operacao.md`](36-deploy-operacao.md) — Deploy, backup, retenção.
- [`37-contratos-openapi.md`](37-contratos-openapi.md) — Contratos de API.
- [`38-glossario.md`](38-glossario.md) — Termos do domínio.
- [`39-entregaveis-cicd-repositorios.md`](39-entregaveis-cicd-repositorios.md) — Jenkinsfile, assets GitHub nos repos.
- [`40-guia-versao-tag.md`](40-guia-versao-tag.md) — Procedimento de versionamento e tags.
- [`99-melhorias-sugeridas.md`](99-melhorias-sugeridas.md) — Backlog de melhorias.
- [`../jornadas/README.md`](../jornadas/README.md) — Jornadas de uso (narrativa end-to-end).
- `softon-portal-web/docs/release-orchestrator/` — Documentação do frontend (por tela).
- [`../ROADMAP.md`](../ROADMAP.md) — Roadmap consolidado.
