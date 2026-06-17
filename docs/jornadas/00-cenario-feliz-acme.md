# 00 — Cenário feliz: ACME recebe DTEC-LD v1.5.0

Jornada **integrada** cobrindo Release Orchestrator (pacote técnico) e DocFlow (manual). Personas, sequência de telas e entregáveis.

> Estados: ✅ implementado · 📋 especificado · 🔧 fora do portal (repos/Jenkins)

---

## Personas

| Persona | Papel nesta jornada |
|---|---|
| **João** | Engenheiro de release — registra changelog e dispara build |
| **Ana** | Revisora — aprova e publica release |
| **Carlos** | Gestor de operações — planeja entrega na agenda |
| **Pedro** | Operador — gera pacote por cliente |
| **Lucia** | Editor de documentação — atualiza manual e publica pacote DocFlow |

---

## Visão em uma linha

```text
Release PUBLICADA → (opcional) assets no GitHub → entrega técnica ACME → manual DocFlow alinhado
```

---

## Fase 0 — Preparação (uma vez)

| # | Quem | Onde | Ação | Estado |
|---|---|---|---|---|
| 0.1 | Admin | `/seguranca` | Usuários, grupos, permissões | ✅ UI mock; 📋 backend real |
| 0.2 | Admin | `/release-orchestrator/produtos` | Cadastra produto **DTEC-LD** (`DTECLD`) | ✅ |
| 0.3 | Admin | `/orchestrator/produtos/:id/modulos` | Catálogo WEB, BATCH, BANCO, KETTLE, FUNC, REGRAS | 📋 spec `10` |
| 0.4 | Admin | `/orchestrator/clientes/novo` | Cadastra **ACME** (CNPJ, ambiente, banco) | 📋 spec `03` |
| 0.5 | Admin | Cliente ACME → abas | Produtos contratados (v1.4.0 instalada), funcionalidades, config entrega | 📋 `05`–`07` |
| 0.6 | Lucia | `/doc-flow` | Projeto DTEC-LD, módulos, **cliente DocFlow** ACME + vínculos | ✅ |
| 0.7 | Admin | `/doc-flow/configuracoes` | Logo da empresa nos manuais | ✅ UI; 📋 integração API empresa |

> **Nota:** cliente **Orchestrator** (operacional) e cliente **DocFlow** (manual) são conceitos distintos hoje; unificação futura.

---

## Fase 1 — Release do produto

| # | Quem | Tela | Passos | Estado |
|---|---|---|---|---|
| 1.1 | João | `/release-orchestrator/builder` | Cria release `1.5.0`, adiciona itens por categoria, envia para `EM_REVISAO` | ✅ |
| 1.2 | João | `/releases/:id` | (Fase 0) Vincula versão por módulo; upload artefatos | 📋 aba `14` |
| 1.3 | João | 🔧 Repo + Jenkins | Tag `v1.5.0` → build → assets no GitHub Release | 🔧 spec `39`, `40` |
| 1.4 | Lucia | `/doc-flow/paginas` | Atualiza páginas afetadas; workflow até `PUBLICADO` | ✅ |
| 1.5 | Ana | `/releases/:id/revisao` | Valida pendências/alertas → **Publicar** → `PUBLICADA` | ✅ |

**Fluxo de status da release:** `RASCUNHO` → `EM_DESENVOLVIMENTO` → `EM_REVISAO` → `APROVADA` → `PUBLICADA`

---

## Fase 2 — Planejamento da entrega (Orchestrator)

| # | Quem | Tela | Passos | Estado |
|---|---|---|---|---|
| 2.1 | Carlos | `/orchestrator/entregas/agenda` | Planeja entrega ACME · DTEC-LD · v1.5.0 · data janela | 📋 spec `16` |
| 2.2 | Carlos | `/orchestrator/clientes/:id` | Confere última entrega v1.4.0, aprova próxima entrega | 📋 spec `04` |

---

## Fase 3 — Entrega técnica ao cliente

| # | Quem | Tela | Passos | Estado |
|---|---|---|---|---|
| 3.1 | Pedro | `/orchestrator/entregas/nova` | Wizard: ACME → DTEC-LD PROD → release 1.5.0 | 📋 spec `18` |
| 3.2 | Pedro | Passo 4 — módulos | Sistema compara v1.4.0 (cliente) vs v1.5.0 (alvo); marca módulos com mudança | 📋 spec `19` |
| 3.3 | Pedro | `/orchestrator/entregas/:id/delta` | (Opcional) Ajusta `FROM_TAG`/`TO_TAG`, pré-visualiza SQL/Kettle | 📋 spec `20` |
| 3.4 | Pedro | Passo 5 — revisão | Confirma resumo → **Gerar pacote** | 📋 spec `18` |
| 3.5 | Sistema | `/orchestrator/entregas/:id/geracao` | Job: artefatos + delta + func/regras + PDF + ZIP + checksums | 📋 spec `21` |
| 3.6 | Pedro | `/orchestrator/entregas/:id` | Download pacote; publica no destino ACME; versão instalada → 1.5.0 | 📋 spec `22` |

**MVP (Fase 1):** artefatos vêm de **upload manual** na release, não do GitHub.  
**Fase 2:** download e diff via GitHub — ver [`01-github-jenkins-delta.md`](01-github-jenkins-delta.md).

---

## Fase 4 — Manual DocFlow

| # | Quem | Tela | Passos | Estado |
|---|---|---|---|---|
| 4.1 | Lucia | `/doc-flow/publicacoes/novo` | Cliente ACME → preview páginas elegíveis → versão `2026.06.1` → gerar | ✅ |
| 4.2 | Lucia | `/doc-flow/publicacoes` | Acompanha `GERANDO` (polling) → `SUCESSO` | ✅ |
| 4.3 | Lucia | Lista / detalhe | Download ZIP/PDF; copia link público temporário | ✅ parcial (detalhe sem abas) |

**Fluxo editorial da página (pré-requisito):** `RASCUNHO` → `EM_REVISAO` → `APROVADO` → `PUBLICADO`

---

## Fase 5 — Pós-entrega

| # | Quem | Tela | Ação | Estado |
|---|---|---|---|---|
| 5.1 | Suporte | `/orchestrator/suporte` | “O que ACME recebeu?” | 📋 spec `27` |
| 5.2 | Suporte | `/doc-flow/busca` | Localiza página no manual publicado | ✅ |
| 5.3 | Pedro | Detalhe entrega | **Reentregar** se pacote corrompido | 📋 spec `22` |

---

## Diagrama simplificado

```text
[João] Builder/Detalhe release ──► [Ana] Revisão PUBLICADA
         │                                    │
         │ 🔧 tag v1.5.0 → Jenkins → GitHub   │
         ▼                                    ▼
[Carlos] Agenda ──► [Pedro] Assistente ──► Geração pacote ACME
         │
[Lucia] Páginas PUBLICADO ──► Publicação DocFlow ACME
```

---

## Jornadas alternativas (resumo)

| Cenário | Orchestrator | DocFlow |
|---|---|---|
| **Só correção de manual** | — | Editar página → publicar pacote patch |
| **Hotfix urgente** | Builder HOTFIX → revisão expressa → entrega | Páginas afetadas + publicação |
| **Cliente novo (primeira entrega)** | Range “full” ou baseline → assistente | Copiar vínculos de cliente template |
| **Sem GitHub (MVP)** | Upload manual na release (`14`) | Inalterado |

---

## Lacunas conhecidas (integração)

Itens que a jornada assume mas ainda precisam de código:

- Paths API DocFlow: front `/api/doc-flow/*` vs back `/api/v1/docflow/*` (proxy/services).
- Auth: front mock (`seguranca`) vs JWT real (`/api/v1/auth/login`).
- Telas `/orchestrator/*` (entregas) ainda não existem no frontend.
- `ConfiguracaoService` logo empresa: mock localStorage vs `EmpresaController`.

Ver [`../doc-flow/README.md`](../doc-flow/README.md) § Integração frontend.

---

## Referências

- Fluxo técnico detalhado: [`../release-orchestrator/00-visao-geral-fluxo-integrado.md`](../release-orchestrator/00-visao-geral-fluxo-integrado.md) §7
- Assistente: [`../release-orchestrator/18-nova-entrega-assistente.md`](../release-orchestrator/18-nova-entrega-assistente.md)
- GitHub/Jenkins: [`01-github-jenkins-delta.md`](01-github-jenkins-delta.md)
