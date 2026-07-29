# Release Orchestrator — Especificação Funcional e Técnica

Documento de entrada das specs do **Release Orchestrator** — releases, entregas a clientes, delta de artefatos e pacotes.

> Última revisão da estrutura: 2026-06-16 (adicionados 39-40 — CI/CD nos repos + guia versão/tag).

## Como ler

1. Comece por [`00-visao-geral-fluxo-integrado.md`](00-visao-geral-fluxo-integrado.md) — visão integrada (releases + entregas).
2. Padrões comuns a todas as telas em [`99-padroes-tela.md`](99-padroes-tela.md) — não repetidos em cada spec.
3. Specs **01-28** são telas/conjuntos funcionais.
4. Specs **29-33** são anexos técnicos (backlog, rotas, handoff, modelo de dados, decisões).
5. Specs **34-38** — observabilidade, testes, deploy, OpenAPI, glossário.
6. Specs **39-40** — entregáveis nos **repositórios de produto** (Jenkinsfile, assets GitHub) e guia operacional de versão/tag.
7. Spec **99-melhorias-sugeridas** é o backlog específico do orchestrator (back + front).

## Escopo MVP

O primeiro ciclo de desenvolvimento entrega o fluxo end-to-end com:
- **Upload manual** de artefatos na release (sem integração GitHub/Jenkins ainda).
- **Pacote local** salvo em pasta fixa do servidor (sem publicação FTP/SFTP/bucket).

GitHub/Jenkins e publicação remota fazem parte do **pós-MVP** — estão no design mas a implementação fica para ciclo futuro. Tabela completa em `00-visao-geral-fluxo-integrado.md`.

## Estrutura

```text
Release Orchestrator (módulo Maven `release-orchestrator`)
  ├─ Gestão de releases (implementado)
  │    produto, releases, itens, histórico, templates
  │    workflow RASCUNHO → PUBLICADA
  │    PDF da release (a implementar — ver 25-documento-release-md-pdf.md)
  └─ Entregas a clientes (a implementar — Fase 1)
       clientes, contatos, config de entrega
       domínios e funcionalidades por cliente
       produtos e módulos contratados por cliente
       próximas entregas (agenda)
       nova entrega (assistente + delta + pacote)
       histórico, reentrega, relatórios, suporte
       PDF da entrega (reusa renderer de releases)
```

## Índice

### Visão geral e padrões
- [00 — Visão geral do fluxo integrado](00-visao-geral-fluxo-integrado.md)
- [99 — Padrões gerais de tela](99-padroes-tela.md)

### Dashboard
- [01 — Dashboard](01-dashboard.md)

### Clientes
- [02 — Listagem](02-clientes-lista.md)
- [03 — Cadastro](03-clientes-cadastro.md)
- [04 — Visão geral do cliente](04-cliente-visao-geral.md)
- [05 — Domínios e funcionalidades](05-cliente-dominios-funcionalidades.md)
- [06 — Produtos contratados](06-cliente-produtos-contratados.md)
- [07 — Configurações de entrega](07-cliente-configuracoes-entrega.md)

### Produtos
- [08 — Listagem](08-produtos-lista.md)
- [09 — Cadastro (com integração GitHub + Jenkins)](09-produtos-cadastro.md)
- [10 — Módulos (catálogo cadastrável por produto)](10-produtos-modulos-artefatos.md)
- [11 — Catálogo de domínios e funcionalidades (por produto)](11-produtos-catalogo-funcional.md)

### Release Orchestrator (apenas o que falta — restante já existe no backend)
- [14 — Detalhe da release (aba PDF)](14-release-orchestrator-detalhe.md)

### Próximas entregas
- [16 — Agenda](16-proximas-entregas-agenda.md)
- [17 — Cadastro](17-proximas-entregas-cadastro.md)

### Nova entrega (orchestrator)
- [18 — Assistente de nova entrega](18-nova-entrega-assistente.md)
- [19 — Seleção de módulos](19-selecao-modulos.md)
- [20 — Range manual / cálculo de delta](20-range-manual-delta.md)
- [21 — Geração de pacote](21-geracao-pacote.md)
- [22 — Detalhes da entrega](22-detalhes-entrega.md)
- [23 — Histórico de entregas](23-historico-entregas.md)

### Documentos
- [24 — Documentos e templates](24-documentos-templates.md)
- [25 — Documento de release (Markdown → PDF)](25-documento-release-md-pdf.md)

### Operação
- [26 — Relatórios](26-relatorios.md)
- [27 — Suporte operacional](27-suporte-operacional.md)
- [28 — Configurações gerais](28-configuracoes.md)

### Anexos técnicos
- [29 — Backlog por épico](29-backlog-por-epico.md)
- [30 — Rotas Angular sugeridas](30-rotas-angular-sugeridas.md)
- [31 — Guia Figma / handoff visual](31-guia-figma-handoff.md)
- [32 — Modelo de dados sugerido](32-modelo-dados-sugerido.md)
- [33 — Decisões técnicas (MVP): stack PDF, storage, async, autorização](33-decisoes-tecnicas.md)

### Específicos técnicos (adicionados 2026-05-31)
- [34 — Observabilidade (logs, métricas, tracing, healthchecks)](34-observabilidade.md)
- [35 — Estratégia de Testes e QA](35-testes-qa.md)
- [36 — Deploy e Operação](36-deploy-operacao.md)
- [37 — Contratos OpenAPI](37-contratos-openapi.md)
- [38 — Glossário](38-glossario.md)

### CI/CD nos repositórios (adicionados 2026-06-16)
- [39 — Entregáveis CI/CD nos repositórios](39-entregaveis-cicd-repositorios.md)
- [40 — Guia versão / tag](40-guia-versao-tag.md)
- [41 — Runbook DTEC Suite V5 com Jenkins local](41-runbook-dtec-suite-v5-jenkins-local.md)

### Backlog
- [99 — Melhorias Sugeridas](99-melhorias-sugeridas.md)

## Histórico desta documentação

- **2026-06-16**:
  - Criados 39 (entregáveis Jenkins/GitHub nos repos de produto) e 40 (guia versão/tag).
  - `00-visao-geral` com mapa história → specs; cross-links em ROADMAP, 09, 21, 38.
- **2026-05-31**:
  - Adicionados 34 (observabilidade), 35 (testes/qa), 36 (deploy), 37 (OpenAPI), 38 (glossário).
  - Criado 99-melhorias-sugeridas.md com backlog específico do orchestrator (back + front).
  - README expandido para refletir nova estrutura.
- **2026-05-24**: Removidos arquivos 11, 12, 13, 15 (telas RF já implementadas). Reduzido 14 a "aba PDF". Reescrito 09, 10, 17, 19, 20, 21, 25, 32 para alinhar com escopo definido (GitHub/Jenkins, módulos cadastráveis, separação de status próxima-entrega vs entrega). Criado 99 (padrões consolidados).

## Documentos relacionados

- [`../ROADMAP.md`](../ROADMAP.md) — Roadmap consolidado (back + front).
- [`../jornadas/README.md`](../jornadas/README.md) — Jornadas de uso (narrativa end-to-end).
- [`../doc-flow/README.md`](../doc-flow/README.md) — DocFlow backend (API).
- `softon-portal-web/docs/release-orchestrator/` — Documentação do frontend (por tela).

> **Nota**: este é o **único** conjunto de specs do backend do Release Orchestrator (releases + entregas no mesmo módulo Maven).
