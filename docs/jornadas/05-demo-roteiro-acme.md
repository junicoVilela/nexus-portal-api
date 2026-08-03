# 05 — Roteiro de demo: ACME recebe NEXUS-LD v1.5.0

Roteiro operacional de **15–30 minutos** para mostrar a jornada feliz do
[`00-cenario-feliz-acme.md`](00-cenario-feliz-acme.md) usando o que está
implementado hoje (Fase 1 / MVP manual, sem GitHub).

Lacunas reais conhecidas nessa demo estão em
[`06-gaps-jornada-acme.md`](06-gaps-jornada-acme.md). Aqui descrevemos o
caminho **que funciona** — onde houver desvio do `00`, indicamos com 🟡.

---

## Quem usa, em ordem

1. **João** — engenheiro de release (registra changelog, sobe artefatos)
2. **Ana** — revisora (publica a release)
3. **Carlos** — gestor de operações (planeja a entrega na agenda)
4. **Pedro** — operador (gera o pacote pela ACME)
5. **Lucia** — editora (publica o manual DocFlow)

Para a demo curta, qualquer pessoa logada como ADMIN faz o papel dos
cinco — o foco é mostrar a sequência de telas.

---

## Pré-requisitos (1×, antes da demo)

| # | Ação | Onde | Como |
|---|---|---|---|
| 0.1 | Backend up | `nexus-portal-api` | `./mvnw spring-boot:run -pl application` (profile `dev`) |
| 0.2 | Frontend up | `nexus-portal-web/frontend` | `npx ng serve` (proxy `proxy.conf.json` cuida do roteamento) |
| 0.3 | Login | `/login` | usuário ADMIN do seed |
| 0.4 | Produto NEXUS-LD cadastrado | `/release-orchestrator/produtos` | Sigla `NEXUSLD`, cor azul. Cadastra 1× se não existir. |
| 0.5 | Módulos catálogo cadastrados | `/release-orchestrator/produtos/:id/modulos` | `WEB`, `BATCH`, `BANCO`, `KETTLE` (1× cada). FUNC/REGRAS opcionais. |
| 0.6 | Cliente ACME ativo | `/release-orchestrator/clientes/novo` | Sigla `ACME`, CNPJ `12345678000199`, ambiente `PROD`, banco `ORACLE`, 1 contato técnico. |

Tempo de prep: ~5 min se já estiver familiarizado com as telas.

> 💡 Para zerar entre demos, basta dropar/recriar o database — Flyway
> roda as migrations e o seed dev refaz os básicos.

---

## Demo (~20 minutos)

### Cena 1 · João monta a release (5 min)

| Passo | Ação | Tela |
|---|---|---|
| 1.1 | Abrir builder | `/release-orchestrator/builder` |
| 1.2 | Selecionar produto `NEXUS-LD` |  |
| 1.3 | Preencher versão `1.5.0`, título "Release de mai/26", tipo MINOR | |
| 1.4 | Adicionar 3-4 itens (FEATURE, FIX, NOTA) e Salvar como `RASCUNHO` → Avançar para `EM_DESENVOLVIMENTO` | |
| 1.5 | Abrir detalhe da release recém-criada | `/release-orchestrator/releases/:id` |
| 1.6 | Na aba **Artefatos**, fazer upload de 1 `.war` no módulo WEB e 1 `.sql` no módulo BANCO | aba `Artefatos` |
| 1.7 | Voltar topo, clicar **Enviar para revisão** → status `EM_REVISAO` | |

**Resultado esperado:** release `EM_REVISAO` com 2 artefatos vinculados.

### Cena 2 · Ana publica a release (2 min)

| Passo | Ação | Tela |
|---|---|---|
| 2.1 | Ir para revisão | `/release-orchestrator/releases/:id/revisao` |
| 2.2 | Conferir pendências → clicar **Aprovar** → `APROVADA` | |
| 2.3 | Voltar pro detalhe e **Publicar** → `PUBLICADA` | |

**Resultado esperado:** release `PUBLICADA` aparecendo no dashboard.

### Cena 3 · Carlos planeja a entrega (3 min)

| Passo | Ação | Tela |
|---|---|---|
| 3.1 | Abrir agenda | `/release-orchestrator/proximas-entregas` |
| 3.2 | Clicar **Planejar entrega** → wizard de cadastro | |
| 3.3 | Selecionar ACME, NEXUS-LD, ambiente `PROD`, release `1.5.0`, data prevista (sábado próximo), prioridade `ALTA` | |
| 3.4 | Salvar → status `PLANEJADA` | volta para `/proximas-entregas` |
| 3.5 | Clicar **Agendada** na linha → muda para `AGENDADA` | |

**Resultado esperado:** linha na agenda com status `AGENDADA` e botão `Gerar`.

### Cena 4 · Pedro gera o pacote (7 min) — **cena principal**

| Passo | Ação | Tela |
|---|---|---|
| 4.1 | Na linha da próxima entrega `AGENDADA`, clicar **Gerar** | navega para `/entregas/nova?proximaEntregaId=…` |
| 4.2 | Wizard abre **já no passo 4** com cliente/produto/release/ambiente pré-preenchidos da agenda | |
| 4.3 | Conferir módulos selecionados (WEB e BANCO marcados; FUNC/REGRAS desmarcados se vazios) | passo 4 |
| 4.4 | Avançar para Revisão → conferir resumo + delta calculado (artefatos, tamanho) | passo 5 |
| 4.5 | Clicar **Gerar pacote** | redireciona para `/entregas/:id` |
| 4.6 | Tela do detalhe mostra `EM_GERACAO` com indicador "acompanhando…" e polling a cada 5 s | |
| 4.7 | Em alguns segundos status muda para `CONCLUIDA` | toast confirma |
| 4.8 | Clicar **Documento (PDF)** → faz download do release-notes do cliente | |

**Resultado esperado:** entrega `CONCLUIDA` com SHA-256, tamanho do pacote e PDF baixado.

🟡 *O caminho físico do ZIP aparece como `arquivoPacoteCaminho`; download
direto do ZIP via UI não está exposto — para a demo, copiar o caminho e
abrir no terminal do servidor.*

### Cena 5 · Lucia publica o manual DocFlow (3 min)

| Passo | Ação | Tela |
|---|---|---|
| 5.1 | Trocar de módulo para DocFlow | `/doc-flow` |
| 5.2 | Garantir que ACME existe como cliente DocFlow e tem páginas `PUBLICADO` | `/doc-flow/clientes`, `/doc-flow/paginas` |
| 5.3 | Abrir nova publicação | `/doc-flow/publicacoes/novo` |
| 5.4 | Selecionar ACME → preview das páginas elegíveis → versão `2026.06.1` | |
| 5.5 | Gerar → acompanhar `GERANDO` → `SUCESSO` | `/doc-flow/publicacoes` |
| 5.6 | Abrir detalhe → download do ZIP/PDF | |

**Resultado esperado:** publicação DocFlow `2026.06.1` `SUCESSO`.

### Cena 6 (opcional) · Suporte consulta o histórico (1 min)

| Passo | Ação | Tela |
|---|---|---|
| 6.1 | Voltar para Release Orchestrator → histórico de entregas | `/release-orchestrator/entregas` |
| 6.2 | Filtrar por cliente ACME → vê a entrega `CONCLUIDA` recente | |
| 6.3 | Clicar na linha → detalhe da entrega com SHA-256 + artefatos | |

🟡 *Tela dedicada de "Suporte" (spec `27`) não existe; usar o histórico
filtrado é o substituto MVP.*

---

## Roteiro express (5 min)

Se faltar tempo, faça **apenas** as cenas 3 → 4 → 6, partindo de uma
release já `PUBLICADA` no seed. Cena 4 é o ponto alto.

---

## DoD da Fase 1 atendido pela demo

| Critério ROADMAP | Cena |
|---|---|
| Cadastrar cliente | (pré-requisito 0.6) |
| Planejar entrega | Cena 3 |
| Gerar pacote MVP (upload manual) | Cenas 1.6 + 4 |
| Histórico de entregas | Cena 6 |
| Reentregar | Botão **Reentregar** no detalhe da entrega (Cena 4.8) |

---

## Troubleshooting rápido

| Sintoma | Causa provável | Correção |
|---|---|---|
| `EM_GERACAO` nunca avança | `@Async` não habilitado ou falta diretório de saída | Conferir log; criar `/var/lib/nexus/entregas` (ou path do `application-dev.yml`) |
| 404 ao listar clientes | Backend não subiu o módulo release-orchestrator | `./mvnw spring-boot:run -pl application` em dev |
| Wizard mostra "nenhuma release disponível" | Release ainda não está `APROVADA`/`PUBLICADA` | Cena 2 antes da 4 |
| PDF do documento dá erro | Templates Thymeleaf faltando para FUNC/REGRAS | Marcar apenas WEB/BANCO no passo 4 (ou cadastrar templates) |

---

## Referências

- Jornada original: [`00-cenario-feliz-acme.md`](00-cenario-feliz-acme.md)
- Gaps reais para fechar pós-demo: [`06-gaps-jornada-acme.md`](06-gaps-jornada-acme.md)
- Checklist S7: [`02-checklist-por-sprint.md`](02-checklist-por-sprint.md) §Sprint 7
