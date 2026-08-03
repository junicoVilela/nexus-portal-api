# 04 — Checklist final de validação

Lista marcável que consolida todos os critérios de aceite das docs anteriores. Use como **definição de pronto** da validação do Release Orchestrator.

> Recomendação: copie este arquivo para uma branch ou issue, vá marcando à medida que valida, e referencie o resultado no PR/release que aprovou o smoke.

---

## A. Ambiente e infraestrutura

- [ ] Postgres up e healthy (`docker compose ps postgres`)
- [ ] Backend rodando (`curl /actuator/health` → `UP`)
- [ ] Frontend rodando (`http://localhost:4200` abre login)
- [ ] Migrations V1–V7 aplicadas (`SELECT version FROM flyway_schema_history`)
- [ ] `/actuator/health/storage` → `UP`
- [ ] Pastas `storage/{artefatos,entregas,publicacoes}` existem e graváveis
- [ ] `RELEASE_ORCHESTRATOR_ENCRYPTION_KEY` exportada (se for usar SFTP/FTP/BUCKET)
- [ ] `RELEASE_ORCHESTRATOR_WEBHOOKS_JENKINS_SECRET` exportada (se for usar webhook Jenkins)
- [ ] (Se aplicável) Jenkins up no compose (`curl http://localhost:8090/login`)
- [ ] (Se aplicável) MinIO up (`curl http://localhost:9000/minio/health/live`)
- [ ] (Se aplicável) SFTP up (`sftp -P 2222 foo@localhost`)

---

## B. Autenticação e RBAC

- [ ] Login `admin / admin` funciona
- [ ] `/auth/me` retorna grupo `ADMIN` com 50+ permissões
- [ ] Login `editor / editor` funciona com permissões reduzidas (gestão operacional)
- [ ] Login `revisor / revisor` funciona com permissões só de leitura/visualização
- [ ] Login `leitor / leitor` funciona com permissões só de `:LER`
- [ ] Token JWT persiste em `localStorage` e sobrevive a refresh

---

## C. Cadastro de Produto

- [ ] Produto `Nexus` criado, ativo
- [ ] Sigla duplicada rejeitada com 409
- [ ] Aba **GitHub**: repo + branch + regex + PAT cadastrados
- [ ] **Testar conexão GitHub** → verde
- [ ] Token nunca devolvido em GET (placeholder `••••`)
- [ ] `/actuator/health/github` → `UP`
- [ ] Aba **Jenkins**: URL + job + user + token cadastrados (se aplicável)
- [ ] **Testar conexão Jenkins** → verde
- [ ] `/actuator/health/jenkins` → `UP`
- [ ] Aba **Módulos**: 4 módulos cadastrados (NEXUS-LD, Nexus-CR, POWERMATCH, Nexus-BANCO)
- [ ] Módulos BANCO/KETTLE têm `config_especifica` populado e JSON válido
- [ ] Aba **Catálogo funcional**: pelo menos 1 domínio com funcionalidades

---

## D. Cadastro de Cliente

- [ ] Cliente `XPTO` criado com ambiente padrão `HOM`, ativo
- [ ] CNPJ válido (se preenchido) e UNIQUE
- [ ] Aba **Contatos**: ≥ 1 contato `TECNICO` com e-mail
- [ ] Aba **Produtos**: contrato com produto `Nexus` ativo
- [ ] Módulos do contrato têm `versao_atual = 1.0.0`
- [ ] Aba **Funcionalidades**: matriz preenchida conforme licença
- [ ] Aba **Config. entrega**: `PASTA` configurada para `/tmp/nexus-entregas/xpto`
- [ ] **Testar conexão** verde
- [ ] Pasta destino existe e é gravável

---

## E. Cadastro de Release

- [ ] Release `1.1.0` do produto `Nexus` criada
- [ ] Versão é UNIQUE por produto
- [ ] Aba **Itens**: ≥ 3 itens cadastrados (NOVIDADE, MELHORIA, CORRECAO)
- [ ] Aba **Artefatos**: artefatos sincronizados do GitHub OU upload manual
- [ ] Cada módulo WEB/BATCH tem ≥ 1 artefato em `tb_artefato_release_modulo`
- [ ] SHA-256 calculado e persistido
- [ ] Coluna **Versão do módulo nesta release** preenchida
- [ ] Transições: `RASCUNHO → EM_REVISAO → APROVADA → PUBLICADA`
- [ ] Aba **Histórico** mostra 3+ entradas de transição
- [ ] Release está `PUBLICADA`

---

## F. Webhook Jenkins → Badge de build (se aplicável)

- [ ] Build no Jenkins SUCCESS dispara `POST /webhooks/jenkins` com sucesso
- [ ] Webhook sem secret válido → 403 (validar)
- [ ] Badge **Build: Sucesso #N** aparece no detalhe da release em ≤ 1s
- [ ] Click no badge abre o build no Jenkins em nova aba
- [ ] `tb_release.ultimo_build_status`, `ultimo_build_numero`, `ultimo_build_url`, `ultimo_build_at` preenchidos

---

## G. Wizard de nova entrega

- [ ] Wizard abre em `/release-orchestrator/entregas/nova`
- [ ] Passo 1: dropdown de produto restringe pelos contratados do cliente
- [ ] Passo 2: dropdown de release lista apenas PUBLICADAS do produto
- [ ] Passo 3: 4 módulos listados com FROM=1.0.0 e TO=1.1.0
- [ ] Toggle ativo default `true` para módulos contratados
- [ ] Botão **Próximo** desabilitado enquanto faltam obrigatórios
- [ ] Passo 4: **Calcular delta** retorna 200 e mostra contagem por módulo
- [ ] Passo 4: contagem dos módulos WEB/BATCH = 1 cada
- [ ] Passo 4: contagem do módulo BANCO = N (delta entre tags) com header de procedência por bloco
- [ ] Passo 5: editor Markdown abre pré-preenchido
- [ ] Passo 5: apenas itens visíveis ao cliente XPTO aparecem
- [ ] **Gerar entrega** redireciona para `/entregas/{id}` em ≤ 1s

---

## H. Geração e acompanhamento

- [ ] Status `EM_GERACAO` aparece no header
- [ ] Polling de 5s funcionando (Network tab mostra `GET /entregas/{id}` recorrente)
- [ ] Histórico vai populando em tempo real
- [ ] Status muda para `CONCLUIDA` em ≤ 30s (4 módulos, dev local)
- [ ] Polling para automaticamente ao concluir
- [ ] KPIs do topo: Módulos selecionados, Itens no delta, Tamanho, SHA-256
- [ ] `tb_entrega`: `status=CONCLUIDA`, `data_inicio_geracao` e `data_conclusao` populados
- [ ] `tamanho_bytes` e `arquivo_pacote_sha256` preenchidos
- [ ] Métrica `entrega.geracao.duration.count` incrementou
- [ ] Métrica `entrega.geracao.resultado{status=sucesso}` += 1

---

## I. Pacote ZIP gerado

- [ ] ZIP existe em `storage/entregas/xpto/nexus/1.1.0/`
- [ ] Nome do arquivo segue padrão `{CLIENTE}_{PRODUTO}_{FROM}_{TO}.zip`
- [ ] `sha256sum` do arquivo bate com `tb_entrega.arquivo_pacote_sha256`
- [ ] Estrutura interna:
  - [ ] `documento-xpto-nexus-1.1.0.pdf`
  - [ ] `manifest.json`
  - [ ] `SHA256SUMS.txt`
  - [ ] `modulos/nexus-ld/nexus-ld-1.1.0.war`
  - [ ] `modulos/nexus-cr/nexus-cr-1.1.0.war`
  - [ ] `modulos/powermatch/powermatch-1.1.0.jar`
  - [ ] `modulos/nexus-banco/oracle/DDL.sql`
  - [ ] `modulos/nexus-banco/oracle/DML.sql`
  - [ ] (se multi-dialeto) `modulos/nexus-banco/sqlserver/{DDL,DML}.sql`
- [ ] `manifest.json` é JSON válido com cliente=XPTO, produto=Nexus, versões
- [ ] `sha256sum -c SHA256SUMS.txt` → todos OK

---

## J. Documento PDF release-notes

- [ ] PDF abre sem erros
- [ ] Capa: cliente, produto, versão TO corretos
- [ ] Sumário automático
- [ ] Seções por categoria (NOVIDADE/MELHORIA/CORRECAO/BREAKING_CHANGE)
- [ ] Apenas itens visíveis ao XPTO no tipo CLIENTE
- [ ] Tipo SUPORTE acrescenta seção técnica (commits, tickets)
- [ ] Tipo INTERNO inclui observações internas
- [ ] Footer com SHA-256 do pacote
- [ ] Botão **Baixar PDF** funciona no detalhe da release

---

## K. Scripts de banco (delta SQL)

- [ ] `DDL.sql` extraído tem apenas scripts com prefixo `DDL_`
- [ ] `DML.sql` extraído tem apenas scripts com prefixo `DML_`
- [ ] Cada bloco tem header com origem, tag, commit, autor
- [ ] Ordem dos blocos respeita cronologia das tags FROM → TO
- [ ] Multi-dialeto: conteúdo de `oracle/` e `sqlserver/` é diferente
- [ ] Sintaxe SQL válida (passa em `sqlfluff parse` ou parser do banco)

---

## L. Publicação remota (se destino ≠ PASTA)

- [ ] Badge **Publicação remota** = `PENDENTE` ao concluir
- [ ] Badge vira `OK` em ≤ 2 min (ciclo do `PublicacaoRetryJob`)
- [ ] Arquivo chega no destino (validado com `ls`/`mc`/`sftp`)
- [ ] SHA-256 no destino bate com `tb_entrega.arquivo_pacote_sha256`
- [ ] `tb_entrega.destino_publicacao` populado
- [ ] `tb_entrega.data_publicacao` preenchido
- [ ] Forçar falha (senha errada): badge volta a `PENDENTE`, motivo da falha visível
- [ ] Backoff exponencial respeitado (5→10→20→40→80 min)
- [ ] Após `max-tentativas` (default 5): badge vira `FALHA`
- [ ] Botão **Tentar agora** zera contador e força nova tentativa

---

## M. Histórico e auditoria

- [ ] Lista de entregas mostra a entrega gerada
- [ ] Filtros (cliente/produto/status/datas) persistem na URL
- [ ] Filtro `status=CONCLUIDA` lista apenas concluídas
- [ ] Aba **Histórico** do detalhe da entrega mostra eventos com timestamps
- [ ] `tb_entrega.created_by` e `updated_by` preenchidos com usuário logado

---

## N. Atualização do contrato pós-entrega

- [ ] `tb_cliente_produto_modulo.versao_atual` dos 4 módulos atualizou para `1.1.0`
- [ ] Próximo wizard com XPTO+Nexus mostra FROM padrão = `1.1.0`

---

## O. Reentrega

- [ ] Botão **Reentregar** abre wizard com prefill
- [ ] Nova entrega tem `entrega_original_id` apontando para a original
- [ ] Pacote da reentrega tem SHA-256 igual (se nada mudou) ou diferente (se artefatos mudaram)

---

## P. Templates (se feature está ativa)

- [ ] Template criado em `/templates`
- [ ] Aplicar template em nova release pré-preenche itens
- [ ] `tb_release_template` reflete o cadastro

---

## Q. Próximas entregas (agenda)

- [ ] **Nova entrega prevista** criada para XPTO/Nexus com data futura
- [ ] Aparece no dashboard como KPI "Pendentes"
- [ ] **Converter em entrega** abre wizard com prefill
- [ ] Após conversão, `tb_proxima_entrega.status = CONVERTIDA` e `entrega_convertida_id` preenchido

---

## R. Observabilidade

- [ ] Logs estruturados com `correlationId`, `entregaId`, `clienteId` no MDC
- [ ] `/actuator/health` retorna `UP`
- [ ] `/actuator/metrics/entrega.geracao.duration` mostra percentis 50/95/99
- [ ] `/actuator/metrics/entrega.geracao.resultado` contadores por status
- [ ] `/actuator/prometheus` exporta as métricas em formato Prometheus
- [ ] HealthIndicators: storage, github, jenkins respondem `UP`/`DOWN` conforme estado

---

## S. Cenários de erro (cobertos em `05-cenarios-de-erro.md`)

- [ ] Tentar gerar entrega sem cliente → erro 400 com mensagem clara
- [ ] Tentar gerar entrega sem módulos → bloqueio no wizard
- [ ] Tentar gerar entrega sem release → bloqueio no wizard
- [ ] Tentar gerar entrega com release `RASCUNHO` → não aparece no dropdown
- [ ] Tentar gerar para cliente inativo → não aparece no dropdown
- [ ] Sigla de cliente duplicada → 409
- [ ] Sigla de produto duplicada → 409
- [ ] Funcionalidade duplicada (mesmo domínio + código) → 409
- [ ] PAT GitHub inválido → erro 401 amigável
- [ ] Repositório GitHub inexistente → erro 404 amigável
- [ ] Webhook Jenkins sem secret válido → 403
- [ ] Pasta destino inexistente/sem permissão → falha na geração com motivo
- [ ] Encryption key faltando → backend loga `warn` na subida

---

## T. Cross-cutting

- [ ] `CanDeactivateGuard` ativa em forms longos (cliente, release, módulo)
- [ ] Toast de sucesso após cada **Salvar**
- [ ] Loading state em ações > 200ms
- [ ] Ações destrutivas (excluir, cancelar, publicar) pedem confirmação
- [ ] Filtros de lista persistem na URL onde aplicável
- [ ] Todos os 5xx mostram `correlationId` no toast
- [ ] CSP/CORS sem violações no console do navegador
- [ ] Sem warnings/errors no console do navegador durante o fluxo principal

---

## U. Definição de pronto

A validação está completa quando **todas** as seções A–T estão marcadas, OU quando os itens não marcados:
- Têm issue criada no repo apropriado com prioridade definida.
- Têm registro em [`docs/jornadas/06-gaps-jornada-acme.md`](../jornadas/06-gaps-jornada-acme.md) se for gap funcional.
- Estão fora do escopo do release/sprint em validação (justificar).

### Resumo executivo (preencher ao final)

```text
Validador:        _________________
Data:             ____ / ____ / ______
Versão validada:  ________________
Itens marcados:   ___ / ___ (___%)
Bloqueadores:     ___ (listar IDs de issue)
Não-bloqueadores: ___ (listar IDs de issue)

[ ] Aprovado para produção
[ ] Aprovado com ressalvas (listar)
[ ] Reprovado (listar bloqueadores)
```

---

## Anexos úteis

- Cenários de erro detalhados: [`05-cenarios-de-erro.md`](05-cenarios-de-erro.md)
- Fluxo completo passo a passo: [`03-fluxo-completo-entrega.md`](03-fluxo-completo-entrega.md)
- Spec por tela: [`../release-orchestrator/`](../release-orchestrator/)
- Guia operacional: [`../jornadas/09-passo-a-passo-local-tela-a-tela.md`](../jornadas/09-passo-a-passo-local-tela-a-tela.md)
