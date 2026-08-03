# 01 — GitHub, Jenkins e delta por cliente

Índice narrativo de **onde** entram GitHub e Jenkins e **onde** o portal calcula o que falta enviar para cada cliente. Não duplica as specs — aponta para elas.

> Última revisão: 2026-06-16.

---

## Três camadas (não confundir)

| Camada | Onde vive | Responsabilidade |
|---|---|---|
| **Repositórios GitHub** | `nexus/nexus-ld`, `nexus-ld-db`, … | Código, SQL, Kettle, `Jenkinsfile` |
| **Jenkins** | Infra interna | Build ao criar tag → publica **assets no GitHub Release** |
| **Portal Orchestrator** | `release-orchestrator` (API) | Sabe versão **por cliente**; monta **pacote personalizado** |

O Jenkins **não** monta pacote por cliente. Ele publica artefatos **genéricos da versão**. O delta **por cliente** é calculado no **backend do portal** na geração da entrega.

---

## Fluxo Fase 2 (automação)

```text
1. Release PUBLICADA no portal (changelog, itens)
2. git tag v1.5.0 && git push  (repo do produto)
3. Jenkins build-on-tag (Jenkinsfile — spec 39)
4. Assets no GitHub Release v1.5.0 (.war, DDL.sql, kettle-delta.zip, …)
5. Operador: assistente nova entrega → cliente ACME
6. Portal: FROM_TAG = última entrega ACME (v1.4.0)
           TO_TAG   = v1.5.0
7. Geração (spec 21): download GitHub + diff Git + func/regras do cliente → ZIP ACME
```

Guia operacional de tags: [`../release-orchestrator/40-guia-versao-tag.md`](../release-orchestrator/40-guia-versao-tag.md).

---

## MVP vs Fase 2

| Etapa | MVP (Fase 0–1) | Fase 2 (GitHub/Jenkins) |
|---|---|---|
| Artefatos WEB/BATCH | Upload manual na release (`14`) | Download asset da `TO_TAG` no GitHub Release |
| Scripts BANCO | Upload `.zip`/`.sql` manual | Diff commits `FROM_TAG..TO_TAG` |
| Kettle | Upload manual | Diff `.ktr`/`.kjb` entre tags |
| Cadastro produto | Nome, sigla, cor | + `repositorioGithub`, `jenkinsUrl`, `jenkinsJob` (`09`) |
| Delta automático | Operador escolhe o que subiu | `FROM` = última entrega; `TO` = release alvo (`19`, `20`) |

Roadmap: [`../ROADMAP.md`](../ROADMAP.md) § FASE 2 (F2.0–F2.10).

---

## O que entra no pacote de **cada** cliente (modo automático)

Referência: [`../release-orchestrator/19-selecao-modulos.md`](../release-orchestrator/19-selecao-modulos.md) §5.1.

| Tipo módulo | Comportamento |
|---|---|
| **WEB / BATCH** | Asset **completo** da `TO_TAG` (sem delta de conteúdo) |
| **BANCO** | Scripts SQL **adicionados/alterados** entre `FROM_TAG` e `TO_TAG` → `DDL.sql` + `DML.sql` |
| **KETTLE** | Jobs **modificados** entre tags (+ dependências) |
| **FUNCIONALIDADES / REGRAS** | Gerados no portal a partir de `ClienteFuncionalidade` (não vêm do Git) |

Pré-visualização antes de gerar: [`../release-orchestrator/20-range-manual-delta.md`](../release-orchestrator/20-range-manual-delta.md).

---

## Telas do portal envolvidas

| Momento | Tela (rota prevista) | Spec |
|---|---|---|
| Configurar repo e Jenkins | `/orchestrator/produtos/:id/editar` | `09` |
| Catálogo de módulos + pasta `db/` | `/orchestrator/produtos/:id/modulos` | `10` |
| Upload MVP / sync GitHub | `/release-orchestrator/releases/:id` (aba Artefatos) | `14` |
| Build em andamento (F2.10) | Detalhe release | ROADMAP F2.10 |
| Selecionar módulos + detectar mudança | Wizard passo 4 | `19` |
| Ajustar FROM/TO + preview delta | `/orchestrator/entregas/:id/delta` | `20` |
| Executar geração | `/orchestrator/entregas/:id/geracao` | `21` |
| Resultado + download | `/orchestrator/entregas/:id` | `22` |

Frontend implementado hoje: apenas gestão de **releases** (`nexus-portal-web/docs/release-orchestrator/00`–`10`). Entregas: 📋.

---

## Etapas do job de geração (backend)

Referência: [`../release-orchestrator/21-geracao-pacote.md`](../release-orchestrator/21-geracao-pacote.md) §4.

| Etapa | Origem MVP | Origem Fase 2 |
|---|---|---|
| 3 — WEB/BATCH | Upload release | GitHub Release `TO_TAG` |
| 4 — BANCO | Upload zip/sql | Git diff `FROM..TO` |
| 5 — KETTLE | Upload zip | Git diff `FROM..TO` |
| 6 — FUNC/REGRAS | Matriz cliente | Idem |
| 7 — PDF | Renderer release | Idem |

API de cálculo de delta: `POST /api/v1/orchestrator/entregas/{id}/delta/calcular` — spec `20` §8.

---

## Trabalho fora do portal (repos)

Checklist Jenkinsfile, assets, pastas `db/`, `kettle/`: [`../release-orchestrator/39-entregaveis-cicd-repositorios.md`](../release-orchestrator/39-entregaveis-cicd-repositorios.md).

---

## DocFlow

DocFlow **não** usa Jenkins/GitHub para delta técnico. Publica manual HTML/PDF por cliente (`/doc-flow/publicacoes`), incluindo só páginas `PUBLICADO` vinculadas — analogia editorial, não diff Git.

Jornada integrada: [`00-cenario-feliz-acme.md`](00-cenario-feliz-acme.md).
