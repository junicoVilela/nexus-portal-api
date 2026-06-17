# 29 — Backlog por Épico

> Épicos relacionados ao Release Orchestrator existente (criação de release, itens, revisão, publicação) **não estão aqui** — já estão implementados no backend `release-orchestrator`. Estão listados em [`00-visao-geral-fluxo-integrado.md`](00-visao-geral-fluxo-integrado.md) como "o que já existe".

> **Escopo MVP**: itens marcados `(pós-MVP)` dependem da integração GitHub/Jenkins ou de publicação remota e ficam para ciclo futuro. Todos os demais são MVP. Ver tabela em [`00`](00-visao-geral-fluxo-integrado.md#6-escopo-mvp-vs-pós-mvp).

> **Roadmap completo** com sequenciamento e prioridades: [`../ROADMAP.md`](../ROADMAP.md).

---

## Épico 0 — Pré-requisitos no Release Orchestrator (Fase 0)

Necessário **antes** do orchestrator entrar.

| História | Status | Esforço |
|---|---|---|
| Criar catálogo cadastrável `ModuloProduto` por produto | Planejado | M |
| Vincular release a versão por módulo (`ReleaseModuloVersao`) | Planejado | M |
| Aba de upload de artefatos por módulo na release (`ArtefatoReleaseModulo`) | Planejado | L |
| Renderer Markdown → PDF da release | Planejado | L |
| Role `VIEWER` + permissões granulares | Planejado | M |
| Naming cleanup (`ProdutoRh` → `Produto`) | Sugerido | M |
| *(pós-MVP)* Integração GitHub/Jenkins ao Produto | Backlog | XL |

Detalhe técnico em [`00-visao-geral-fluxo-integrado.md`](00-visao-geral-fluxo-integrado.md) (§4 Fase 0) e [`10-produtos-modulos-artefatos.md`](10-produtos-modulos-artefatos.md).

---

## Épico 1 — Cadastros base

User stories:

| História | Tela | Prio |
|---|---|---|
| Como administrador, quero cadastrar clientes para controlar quem pode receber entregas | `03` | P0 |
| Como administrador, quero configurar contatos técnicos e comerciais do cliente | `03` | P0 |
| Como administrador, quero configurar destino de entrega do cliente (PASTA local no MVP) | `07` | P0 |
| Como administrador, quero configurar destinos remotos FTP/SFTP/BUCKET para o cliente *(pós-MVP)* | `07` | P2 |
| Como gerente, quero associar produtos contratados ao cliente para restringir módulos enviados | `06` | P0 |
| Como gerente, quero ativar/desativar módulos contratados por cliente | `06` | P0 |
| Como administrador, quero pausar/encerrar cliente sem perder histórico | `04` | P1 |
| Como administrador, quero ver alertas operacionais por cliente | `04` | P1 |

---

## Épico 2 — Domínios e funcionalidades

| História | Tela | Prio |
|---|---|---|
| Como administrador, quero cadastrar domínios funcionais por produto | `10`/catálogo | P0 |
| Como administrador, quero cadastrar funcionalidades dentro de cada domínio | `10`/catálogo | P0 |
| Como administrador, quero habilitar/desabilitar funcionalidades por cliente | `05` | P0 |
| Como administrador, quero copiar configuração de um cliente para outro | `05` | P1 |
| Como administrador, quero aplicar template padrão de funcionalidades | `05` | P2 |
| Como sistema, quero usar funcionalidades habilitadas para gerar scripts `FUNCIONALIDADES`/`REGRAS` | `21` | P0 |

---

## Épico 3 — Planejamento de entregas

| História | Tela | Prio |
|---|---|---|
| Como gerente, quero registrar próximas entregas com data e prioridade | `17` | P0 |
| Como gerente, quero visualizar agenda das próximas entregas e identificar atrasos | `16` | P0 |
| Como gerente, quero replanejar com justificativa | `17` | P0 |
| Como analista, quero converter próxima entrega em entrega real (wizard pré-preenchido) | `18` | P0 |
| Como gerente, quero ver indicadores na agenda (esta semana, atrasadas) | `16` | P1 |
| Como gerente, quero filtros avançados na agenda | `16` | P1 |
| Como gerente, quero exportar agenda em CSV | `16` | P2 |

---

## Épico 4 — Orquestração de entrega

| História | Tela | Prio |
|---|---|---|
| Como analista, quero selecionar cliente, produto e release no wizard | `18` | P0 |
| Como analista, quero escolher quais módulos enviar nesta entrega | `19` | P0 |
| Como analista, quero anexar manualmente artefatos da release (WAR, JAR, SQL, KETTLE) | `14` | P0 |
| Como sistema, quero ler artefatos uploadados da release ao montar o pacote | `21` | P0 |
| Como sistema, quero gerar scripts de `FUNCIONALIDADES`/`REGRAS` a partir da config do cliente | `21` | P0 |
| Como DevOps, quero acompanhar geração do pacote por etapa, com log | `21` | P0 |
| Como sistema, quero calcular delta automaticamente baseado na última versão entregue *(pós-MVP)* | `19`/`20` | P2 |
| Como analista, quero sobrescrever range FROM/TO manualmente com justificativa *(pós-MVP)* | `20` | P2 |
| Como sistema, quero baixar assets WEB/BATCH do GitHub Releases pela TO_TAG *(pós-MVP)* | `21` | P2 |
| Como sistema, quero coletar e unificar scripts DDL/DML do BANCO entre FROM_TAG e TO_TAG *(pós-MVP)* | `21` | P2 |
| Como sistema, quero coletar arquivos KETTLE modificados entre FROM_TAG e TO_TAG *(pós-MVP)* | `21` | P2 |
| Como sistema, quero cancelar geração em curso a pedido do operador | `21` | P0 |
| Como sistema, quero retentar etapa que falhou sem refazer as anteriores | `21`/`27` | P1 |

---

## Épico 5 — Documentos e publicação

| História | Tela | Prio |
|---|---|---|
| Como analista, quero editar documento de release em Markdown e pré-visualizar como PDF | `25` | P0 |
| Como sistema, quero gerar `release-notes.pdf` usando o renderer do Release Orchestrator | `21`/`25` | P0 |
| Como sistema, quero gerar `manifest.json` e `SHA256SUMS.txt` do pacote | `21` | P0 |
| Como sistema, quero salvar pacote em pasta local fixa do servidor | `21` | P0 |
| Como sistema, quero atualizar versão atual no cliente por módulo após geração bem-sucedida | `21` | P0 |
| Como gerente, quero reentregar pacote (regerar/recopiar do local) | `22` | P0 |
| Como sistema, quero publicar pacote no destino configurado do cliente *(pós-MVP)* | `21` | P2 |
| Como administrador, quero gerenciar templates de documento por produto | `24` | P1 |
| Como administrador, quero versionar templates | `24` | P2 |

---

## Épico 6 — Auditoria, histórico e suporte

| História | Tela | Prio |
|---|---|---|
| Como auditor, quero consultar histórico completo de entregas por cliente, produto e período | `23` | P0 |
| Como suporte, quero busca rápida de entrega por código | `23`/`27` | P0 |
| Como suporte, quero ver logs técnicos das etapas de geração | `27` | P1 |
| Como suporte, quero ver fila de execução e falhas atuais | `27` | P1 |
| Como DevOps, quero reprocessar etapas que falharam com auditoria | `27` | P1 |
| Como DevOps, quero regerar documento ou repetir geração sem reanexar artefatos | `27` | P1 |
| Como auditor, quero ver auditoria completa do orchestrator | `27` | P1 |
| Como compliance, quero ver quem acessou dados sensíveis | `27`/`34` | P2 |

---

## Épico 7 — Relatórios

| História | Tela | Prio |
|---|---|---|
| Como gerente, quero relatório de entregas por cliente e por produto | `26` | P1 |
| Como gerente, quero relatório de falhas e reentregas no período | `26` | P1 |
| Como auditor, quero exportar histórico em PDF/Excel/CSV | `26` | P1 |
| Como gerente, quero relatório de funcionalidades habilitadas por cliente | `26` | P2 |
| Como gerente, quero relatórios agendados por e-mail | `26` | P2 |
| Como gerente, quero KPIs operacionais no dashboard | `01` | P0 |

---

## Épico 8 — Plataforma e infra

Cross-cutting que sustenta tudo acima.

| História | Spec | Prio |
|---|---|---|
| Como DevOps, quero healthchecks específicos do orchestrator | `34` | P0 |
| Como DevOps, quero métricas Prometheus expostas | `34` | P1 |
| Como DevOps, quero logs estruturados JSON | `34` | P1 |
| Como DevOps, quero tracing OTLP do fluxo de geração | `34` | P2 |
| Como tester, quero cobertura de testes mínima 70% | `35` | P0 |
| Como tester, quero testes E2E do fluxo principal | `35` | P1 |
| Como DevOps, quero deploy automatizado via CI | `36` | P0 |
| Como DevOps, quero backup automatizado | `36` | P0 |
| Como DevOps, quero OpenAPI publicado | `37` | P1 |
| Como dev, quero glossário centralizado de termos | `38` | P2 |

---

## Estimativas (high-level)

| Épico | Esforço total |
|---|---|
| 0 — Pré-req | 4-6 sprints |
| 1 — Cadastros | 3-4 sprints |
| 2 — Domínios | 2-3 sprints |
| 3 — Planejamento | 2-3 sprints |
| 4 — Orquestração | 4-6 sprints |
| 5 — Documentos | 2-3 sprints |
| 6 — Auditoria | 2-3 sprints |
| 7 — Relatórios | 2-3 sprints |
| 8 — Plataforma | contínuo |

**MVP total**: ~12-18 sprints (3-5 meses).

---

## Priorização (MoSCoW)

### MUST (P0)
- Épico 0 (todos exceto integrações).
- Épicos 1, 3, 4, 5 (sem pós-MVP).
- Histórico básico (Épico 6).

### SHOULD (P1)
- Auditoria e suporte avançado.
- Relatórios básicos.
- Indicadores no dashboard.

### COULD (P2)
- Integrações GitHub/Jenkins.
- Publicação remota.
- Relatórios avançados.
- Tracing.

### WON'T (por enquanto)
- Multi-tenancy.
- Cobrança/faturamento.
- Mobile app.
- i18n.

---

## Cross-reference

- [`00-visao-geral-fluxo-integrado.md`](00-visao-geral-fluxo-integrado.md) — Contexto.
- [`../ROADMAP.md`](../ROADMAP.md) — Roadmap consolidado.
- [`99-melhorias-sugeridas.md`](99-melhorias-sugeridas.md) — Melhorias.
- [`35-testes-qa.md`](35-testes-qa.md) — Estratégia de testes.
