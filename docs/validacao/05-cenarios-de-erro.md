# 05 — Cenários de erro

Casos negativos que **precisam ser provocados** para validar que o sistema reage como esperado. Para cada um: ação, resultado esperado, mensagem, onde validar, como corrigir, critério de aceite.

> Convenção: rodar cada cenário em ambiente limpo (banco com seeds + sem entregas em andamento). Resetar entre cenários se necessário.

---

## Índice

### Erros de validação de formulário
1. [Cadastrar cliente sem sigla](#1-cadastrar-cliente-sem-sigla)
2. [Cadastrar cliente com CNPJ inválido](#2-cadastrar-cliente-com-cnpj-inválido)
3. [Cadastrar cliente com sigla duplicada](#3-cadastrar-cliente-com-sigla-duplicada)
4. [Cadastrar produto com sigla duplicada](#4-cadastrar-produto-com-sigla-duplicada)
5. [Cadastrar módulo duplicado no produto](#5-cadastrar-módulo-duplicado-no-produto)
6. [Cadastrar funcionalidade duplicada](#6-cadastrar-funcionalidade-duplicada)
7. [Salvar módulo BANCO com JSON inválido](#7-salvar-módulo-banco-com-json-inválido)
8. [Cadastrar release com versão duplicada](#8-cadastrar-release-com-versão-duplicada)

### Erros do wizard de entrega
9. [Tentar gerar entrega sem cliente](#9-tentar-gerar-entrega-sem-cliente)
10. [Tentar gerar entrega sem módulo](#10-tentar-gerar-entrega-sem-módulo)
11. [Tentar gerar entrega sem release publicada](#11-tentar-gerar-entrega-sem-release-publicada)
12. [Tentar usar cliente inativo](#12-tentar-usar-cliente-inativo)
13. [Tentar usar release não publicada](#13-tentar-usar-release-não-publicada)
14. [Delta de módulo BANCO sem config](#14-delta-de-módulo-banco-sem-config)
15. [Delta sem artefatos na release](#15-delta-sem-artefatos-na-release)
16. [Wizard com cliente sem produto contratado](#16-wizard-com-cliente-sem-produto-contratado)

### Erros de integração
17. [PAT GitHub inválido](#17-pat-github-inválido)
18. [Repositório GitHub inexistente](#18-repositório-github-inexistente)
19. [Regex de tag inválido](#19-regex-de-tag-inválido)
20. [Sincronizar do GitHub sem release publicada](#20-sincronizar-do-github-sem-release-publicada)
21. [Asset GitHub não bate com nome esperado](#21-asset-github-não-bate-com-nome-esperado)
22. [Jenkins URL inacessível](#22-jenkins-url-inacessível)
23. [Webhook Jenkins sem secret válido](#23-webhook-jenkins-sem-secret-válido)

### Erros de geração / publicação
24. [Pasta destino inexistente / sem permissão](#24-pasta-destino-inexistente)
25. [SFTP com host inválido](#25-sftp-com-host-inválido)
26. [SFTP com credenciais erradas](#26-sftp-com-credenciais-erradas)
27. [Bucket S3 inexistente](#27-bucket-s3-inexistente)
28. [Encryption key faltando](#28-encryption-key-faltando)
29. [Storage cheio durante geração](#29-storage-cheio-durante-geração)
30. [Cancelar entrega em geração](#30-cancelar-entrega-em-geração)

### Erros transversais
31. [Token expirado](#31-token-expirado)
32. [Acesso a recurso sem permissão](#32-acesso-a-recurso-sem-permissão)
33. [Sair de form com mudanças não salvas](#33-sair-de-form-com-mudanças-não-salvas)
34. [Excluir entidade referenciada](#34-excluir-entidade-referenciada)
35. [Concurrent edit do mesmo recurso](#35-concurrent-edit-do-mesmo-recurso)

---

## Erros de validação de formulário

### 1. Cadastrar cliente sem sigla

| Item | Valor |
|---|---|
| **Ação** | `/clientes/novo` → preencher apenas Nome → **Salvar** |
| **Resultado esperado** | Submit bloqueado client-side |
| **Mensagem** | "Sigla é obrigatória" abaixo do campo + botão Salvar desabilitado |
| **HTTP** | Nenhuma requisição enviada (validação client-side) |
| **Como corrigir** | Preencher Sigla |
| **Critério de aceite** | [ ] Mensagem aparece em vermelho · [ ] POST não vai pro backend (Network tab vazia) |

### 2. Cadastrar cliente com CNPJ inválido

| Item | Valor |
|---|---|
| **Ação** | Preencher CNPJ com `11111111111111` (dígito verificador inválido) → **Salvar** |
| **Resultado esperado** | Validação client-side OU 400 do backend |
| **Mensagem** | "CNPJ inválido" |
| **HTTP** | 400 se passar pelo client-side |
| **Como corrigir** | Preencher CNPJ válido ou deixar vazio (campo opcional) |
| **Critério de aceite** | [ ] Mensagem clara · [ ] Form não persiste valor inválido |

### 3. Cadastrar cliente com sigla duplicada

| Item | Valor |
|---|---|
| **Ação** | Criar cliente `XPTO`. Tentar criar outro com `XPTO`. |
| **Resultado esperado** | Backend retorna 409 |
| **Mensagem** | "Sigla XPTO já cadastrada" (toast vermelho) |
| **HTTP** | 409 Conflict, body com `{"erro": "...", "campo": "sigla"}` |
| **Como corrigir** | Usar outra sigla ou editar o existente |
| **Critério de aceite** | [ ] Toast aparece · [ ] Form não reseta · [ ] Banco continua com apenas 1 cliente XPTO |

### 4. Cadastrar produto com sigla duplicada

| Item | Valor |
|---|---|
| **Ação** | Criar produto `Nexus`. Tentar criar outro com `Nexus`. |
| **Resultado esperado** | 409 Conflict |
| **Mensagem** | "Sigla Nexus já cadastrada" |
| **HTTP** | 409 |
| **Como corrigir** | Outra sigla |
| **Critério de aceite** | [ ] Toast · [ ] `tb_produto_rh` continua com apenas 1 Nexus |

### 5. Cadastrar módulo duplicado no produto

| Item | Valor |
|---|---|
| **Ação** | Produto Nexus tem módulo `nexus-ld`. Tentar criar outro `nexus-ld` no mesmo produto. |
| **Resultado esperado** | 409 (UNIQUE produto_id+codigo) |
| **Mensagem** | "Já existe um módulo com este código neste produto" |
| **HTTP** | 409 |
| **Como corrigir** | Editar o existente ou usar outro código |
| **Critério de aceite** | [ ] Toast · [ ] Conta de módulos do produto não muda |

### 6. Cadastrar funcionalidade duplicada

| Item | Valor |
|---|---|
| **Ação** | Domínio `CREDITO` tem funcionalidade `SIMULACAO`. Tentar criar outra `SIMULACAO` no mesmo domínio. |
| **Resultado esperado** | 409 (UNIQUE dominio_produto_id+codigo) |
| **Mensagem** | "Já existe uma funcionalidade com este código neste domínio" |
| **HTTP** | 409 |
| **Como corrigir** | Outro código ou outro domínio |
| **Critério de aceite** | [ ] Toast · [ ] Banco preserva apenas 1 |

### 7. Salvar módulo BANCO com JSON inválido

| Item | Valor |
|---|---|
| **Ação** | Módulo `nexus-banco` → campo `config_especifica` → colar `{ caminhoRepo: "db/nexus"` (sem aspas + sem fechar `}`) |
| **Resultado esperado** | Validação client-side bloqueia · OU backend 400 |
| **Mensagem** | "JSON inválido — verifique aspas e chaves" + linha/coluna se possível |
| **HTTP** | 400 (se passar do client-side) |
| **Como corrigir** | Validar com `jq` antes de colar |
| **Critério de aceite** | [ ] Mensagem clara · [ ] `config_especifica` permanece como antes |

### 8. Cadastrar release com versão duplicada

| Item | Valor |
|---|---|
| **Ação** | Produto Nexus tem release `1.0.0`. Tentar criar outra `1.0.0` para o mesmo produto. |
| **Resultado esperado** | 409 (UNIQUE produto_id+versao) |
| **Mensagem** | "Versão 1.0.0 já existe para o produto Nexus" |
| **HTTP** | 409 |
| **Como corrigir** | Usar versão diferente |
| **Critério de aceite** | [ ] Toast · [ ] `tb_release` preserva apenas 1 com versao=1.0.0 |

---

## Erros do wizard de entrega

### 9. Tentar gerar entrega sem cliente

| Item | Valor |
|---|---|
| **Ação** | Wizard passo 1 sem selecionar cliente → tentar avançar |
| **Resultado esperado** | Botão **Próximo** desabilitado |
| **Mensagem** | Tooltip no botão: "Selecione cliente e produto antes de continuar" |
| **HTTP** | Nenhuma |
| **Como corrigir** | Selecionar cliente |
| **Critério de aceite** | [ ] Botão fica cinza · [ ] Não tem como pular o passo |

### 10. Tentar gerar entrega sem módulo

| Item | Valor |
|---|---|
| **Ação** | Wizard passo 3 com todos os módulos desmarcados → tentar avançar |
| **Resultado esperado** | Botão **Próximo** desabilitado |
| **Mensagem** | "Selecione pelo menos um módulo" |
| **HTTP** | Nenhuma |
| **Como corrigir** | Marcar ≥ 1 módulo |
| **Critério de aceite** | [ ] Botão desabilitado · [ ] Mensagem visível |

### 11. Tentar gerar entrega sem release publicada

| Item | Valor |
|---|---|
| **Ação** | Produto sem nenhuma release `PUBLICADA` → wizard passo 2 |
| **Resultado esperado** | Dropdown de release vazio |
| **Mensagem** | "Nenhuma release publicada para este produto. Publique uma release primeiro." + link para `/releases/nova` |
| **HTTP** | `GET /releases?produtoId=...&status=PUBLICADA` retorna lista vazia |
| **Como corrigir** | Publicar uma release |
| **Critério de aceite** | [ ] Mensagem orientativa · [ ] Botão Próximo desabilitado |

### 12. Tentar usar cliente inativo

| Item | Valor |
|---|---|
| **Ação** | Marcar cliente XPTO como inativo. Abrir wizard. |
| **Resultado esperado** | XPTO **não aparece** no dropdown |
| **Mensagem** | n/a (filtro silencioso) |
| **HTTP** | `GET /clientes?ativo=true` |
| **Como corrigir** | Reativar o cliente |
| **Critério de aceite** | [ ] XPTO some · [ ] Reativar faz reaparecer |

### 13. Tentar usar release não publicada

| Item | Valor |
|---|---|
| **Ação** | Release `1.2.0` em `RASCUNHO`. Abrir wizard passo 2 com produto Nexus. |
| **Resultado esperado** | Apenas releases `PUBLICADA` aparecem; `1.2.0` não está na lista |
| **Mensagem** | n/a |
| **HTTP** | Filtro `status=PUBLICADA` |
| **Como corrigir** | Publicar a release (RASCUNHO → EM_REVISAO → APROVADA → PUBLICADA) |
| **Critério de aceite** | [ ] Lista filtrada corretamente |

### 14. Delta de módulo BANCO sem config

| Item | Valor |
|---|---|
| **Ação** | Módulo `nexus-banco` sem `config_especifica` populado. Wizard passo 4 → **Calcular delta**. |
| **Resultado esperado** | Linha do módulo aparece com **erro** ou contagem 0 |
| **Mensagem** | "Configuração BANCO ausente — defina `caminhoRepo`, `prefixoDDL`, `prefixoDML`" |
| **HTTP** | 200 com warning no payload OU 400 dependendo da política |
| **Como corrigir** | Editar o módulo, popular `config_especifica` |
| **Critério de aceite** | [ ] Erro visível na tabela · [ ] Outros módulos calculam normal |

### 15. Delta sem artefatos na release

| Item | Valor |
|---|---|
| **Ação** | Release `1.1.0` publicada mas sem artefatos na aba Artefatos. Wizard passo 4. |
| **Resultado esperado** | Módulos WEB/BATCH aparecem com `+0` e badge **"Sem artefato"** |
| **Mensagem** | "Nenhum artefato encontrado para este módulo na release. Faça upload ou sincronize do GitHub." |
| **HTTP** | 200 com lista vazia |
| **Como corrigir** | Sincronizar do GitHub ou fazer upload manual |
| **Critério de aceite** | [ ] Badge amarelo · [ ] Geração da entrega permite continuar (mas pacote sai sem o módulo) |

### 16. Wizard com cliente sem produto contratado

| Item | Valor |
|---|---|
| **Ação** | Cliente `XPTO` recém-cadastrado sem nenhum produto contratado. Wizard passo 1 → selecionar XPTO. |
| **Resultado esperado** | Dropdown de produto vazio |
| **Mensagem** | "Este cliente não tem produtos contratados. Acesse [Detalhe do cliente → Produtos] para contratar." + link |
| **HTTP** | `GET /clientes/{id}/produtos` lista vazia |
| **Como corrigir** | Cadastrar contrato em `/clientes/{id}` → aba Produtos |
| **Critério de aceite** | [ ] Mensagem orientativa · [ ] Link funciona |

---

## Erros de integração

### 17. PAT GitHub inválido

| Item | Valor |
|---|---|
| **Ação** | Aba GitHub do produto → colar PAT inválido (`ghp_invalid`) → **Testar conexão** |
| **Resultado esperado** | Toast vermelho |
| **Mensagem** | "GitHub retornou 401 Unauthorized — verifique o PAT (escopo necessário: repo)" |
| **HTTP** | `GET /produtos/{id}/testar-github` → 502 (gateway) ou 400 com erro detalhado |
| **Como corrigir** | Gerar novo PAT em github.com → Settings → Tokens com escopo `repo` |
| **Critério de aceite** | [ ] Toast claro · [ ] PAT inválido não é salvo (form mantém valor digitado para corrigir) |

### 18. Repositório GitHub inexistente

| Item | Valor |
|---|---|
| **Ação** | Aba GitHub → repositório = `nexus/repo-que-nao-existe` → **Testar conexão** |
| **Resultado esperado** | Toast vermelho |
| **Mensagem** | "Repositório não encontrado — confira owner/repo e permissões do PAT" |
| **HTTP** | 404 do GitHub propagado |
| **Como corrigir** | Verificar grafia + acesso do PAT ao repo |
| **Critério de aceite** | [ ] Distingue 401 de 404 nas mensagens |

### 19. Regex de tag inválido

| Item | Valor |
|---|---|
| **Ação** | Campo "Regex de tag" = `[invalid(regex` |
| **Resultado esperado** | Validação client-side bloqueia |
| **Mensagem** | "Regex inválido — exemplo válido: `^v\d+\.\d+\.\d+$`" |
| **HTTP** | Nenhuma |
| **Como corrigir** | Usar regex válido |
| **Critério de aceite** | [ ] Salvar bloqueado · [ ] Helper text mostra exemplo |

### 20. Sincronizar do GitHub sem release publicada

| Item | Valor |
|---|---|
| **Ação** | Aba Artefatos da release `1.1.0` → **Sincronizar do GitHub**. Mas no GitHub não há `v1.1.0`. |
| **Resultado esperado** | Toast amarelo (warning, não erro) |
| **Mensagem** | "Nenhuma release encontrada no GitHub com tag v1.1.0. Crie a release no GitHub primeiro." |
| **HTTP** | 200 com lista vazia OU 404 amigável |
| **Como corrigir** | Criar release no GitHub via CI (push tag) ou `gh release create` |
| **Critério de aceite** | [ ] Mensagem não é "erro técnico" · [ ] Aba Artefatos não fica corrompida |

### 21. Asset GitHub não bate com nome esperado

| Item | Valor |
|---|---|
| **Ação** | Release no GitHub `v1.1.0` tem asset chamado `nexus-ld.war` (sem versão). Esperado: `Nexus-nexus-ld-1.1.0.war`. |
| **Resultado esperado** | Sync ignora assets que não batem com padrão `{SIGLA}-{MODULO}-{VERSAO}.{EXT}` |
| **Mensagem** | "0 artefatos sincronizados. Conferir nome dos assets no GitHub Release." + lista de assets ignorados |
| **HTTP** | 200 com `{ sincronizados: 0, ignorados: ["nexus-ld.war"] }` |
| **Como corrigir** | Ajustar Jenkinsfile para nomear o asset corretamente |
| **Critério de aceite** | [ ] Mostra exatamente quais assets foram ignorados · [ ] Não cria artefato com nome errado |

### 22. Jenkins URL inacessível

| Item | Valor |
|---|---|
| **Ação** | Aba Jenkins → URL = `http://jenkins-offline.local:8080` → **Testar conexão** |
| **Resultado esperado** | Toast vermelho |
| **Mensagem** | "Não foi possível conectar ao Jenkins (timeout). Verifique URL e rede." |
| **HTTP** | 502 ou 504 |
| **Como corrigir** | Conferir URL e disponibilidade do Jenkins |
| **Critério de aceite** | [ ] Mensagem distingue timeout de 401/404 |

### 23. Webhook Jenkins sem secret válido

| Item | Valor |
|---|---|
| **Ação** | Disparar `POST /api/v1/release-orchestrator/webhooks/jenkins` com header `X-Webhook-Secret: errado` |
| **Resultado esperado** | Backend retorna 403 |
| **Mensagem** | log do backend: `Webhook Jenkins recebeu secret divergente — esperado vs recebido` |
| **HTTP** | 403 Forbidden |
| **Como corrigir** | Sincronizar `RELEASE_ORCHESTRATOR_WEBHOOKS_JENKINS_SECRET` (backend) com `PORTAL_WEBHOOK_SECRET` (Jenkins JCasC) |
| **Critério de aceite** | [ ] 403 retornado · [ ] log do backend cita o motivo · [ ] release não recebe build status falso |

Teste manual:
```bash
curl -i -X POST http://localhost:8080/api/v1/release-orchestrator/webhooks/jenkins \
  -H "Content-Type: application/json" \
  -H "X-Webhook-Secret: secret_errado" \
  -d '{"produto":"Nexus","versao":"1.1.0","status":"SUCCESS","numero":42,"url":"http://localhost:8090/job/x/42/"}'
# esperado: HTTP/1.1 403 Forbidden
```

---

## Erros de geração / publicação

### 24. Pasta destino inexistente

| Item | Valor |
|---|---|
| **Ação** | Config. entrega de XPTO = PASTA, `caminho_base = /tmp/pasta-inexistente`. Gerar entrega. |
| **Resultado esperado** | Geração falha · status `FALHA` · `falha_motivo` preenchido |
| **Mensagem (UI)** | "Falha ao escrever pacote: pasta de destino não existe ou não é gravável" |
| **HTTP** | 200 na criação · `EM_GERACAO` por ~3s · vira `FALHA` |
| **Como corrigir** | `mkdir -p /tmp/pasta-inexistente` ou ajustar config |
| **Critério de aceite** | [ ] `tb_entrega.status = FALHA` · [ ] `falha_motivo` legível · [ ] log JSON com stack trace |

### 25. SFTP com host inválido

| Item | Valor |
|---|---|
| **Ação** | Config. entrega = SFTP, host = `sftp.que-nao-existe.local` → **Testar conexão** |
| **Resultado esperado** | Toast vermelho |
| **Mensagem** | "Falha na conexão SFTP: UnknownHost" |
| **HTTP** | 400 |
| **Como corrigir** | Conferir host |
| **Critério de aceite** | [ ] Distinguir UnknownHost de senha errada |

### 26. SFTP com credenciais erradas

| Item | Valor |
|---|---|
| **Ação** | SFTP com senha errada → **Testar conexão** OU gerar entrega |
| **Resultado esperado** | **Testar conexão** → toast vermelho. **Gerar entrega** → publicação fica `PENDENTE` por 5 ciclos do job e depois vira `FALHA` |
| **Mensagem** | "Autenticação SFTP falhou — usuário ou senha inválidos" |
| **HTTP** | 400 |
| **Como corrigir** | Corrigir senha (em edição, **preencher senha** para sobrescrever; vazio mantém atual) |
| **Critério de aceite** | [ ] Badge `PENDENTE` mostra motivo · [ ] Após `max-tentativas`, vira `FALHA` · [ ] **Tentar agora** zera contador |

### 27. Bucket S3 inexistente

| Item | Valor |
|---|---|
| **Ação** | Config. = BUCKET, bucket = `bucket-que-nao-existe` |
| **Resultado esperado** | Testar conexão e/ou publicação falham com `NoSuchBucket` |
| **Mensagem** | "Bucket 'bucket-que-nao-existe' não encontrado em {endpoint}" |
| **HTTP** | 400 (testar) / `FALHA` na publicação |
| **Como corrigir** | Criar bucket no MinIO/S3 |
| **Critério de aceite** | [ ] Mensagem distingue bucket inexistente de erro de credencial |

### 28. Encryption key faltando

| Item | Valor |
|---|---|
| **Ação** | Subir backend sem `RELEASE_ORCHESTRATOR_ENCRYPTION_KEY`. Cadastrar SFTP. Reiniciar. Tentar publicar. |
| **Resultado esperado** | Backend sobe com `WARN: release-orchestrator.encryption.key NÃO configurada — usando key DEV. Não use isso em produção.`. Senhas cifradas com chave DEV continuam funcionando enquanto a chave for a mesma. Se trocar a chave, decifrar quebra. |
| **Mensagem** | (no log) `WARN ... usando key DEV` |
| **HTTP** | Backend sobe normalmente · publicações podem falhar com `Bad AES tag` |
| **Como corrigir** | Exportar chave fixa antes de subir: `export RELEASE_ORCHESTRATOR_ENCRYPTION_KEY=$(openssl rand -base64 32)` |
| **Critério de aceite** | [ ] WARN aparece no log · [ ] Documentação avisa do risco |

### 29. Storage cheio durante geração

| Item | Valor |
|---|---|
| **Ação** | Encher `/tmp/nexus-entregas/xpto` até quase cheio. Gerar entrega grande. |
| **Resultado esperado** | Geração falha com `IOException: No space left on device`. Status `FALHA`, motivo preenchido. |
| **Mensagem** | "Espaço em disco insuficiente para gerar o pacote" |
| **HTTP** | n/a (assíncrono) |
| **Como corrigir** | Liberar espaço, rodar `RetencaoPacotesJob` ou ajustar `retencao-dias` |
| **Critério de aceite** | [ ] `tb_entrega.status = FALHA` · [ ] Arquivos parciais não ficam órfãos · [ ] Healthcheck `/actuator/health/storage` vira `DOWN` |

### 30. Cancelar entrega em geração

| Item | Valor |
|---|---|
| **Ação** | Iniciar geração de uma entrega. Antes de concluir, clicar **Cancelar** no detalhe. |
| **Resultado esperado** | Status vira `CANCELADA`. Worker interrompe na próxima checkpoint (best-effort). |
| **Mensagem** | "Entrega cancelada pelo operador" |
| **HTTP** | `POST /entregas/{id}/cancelar` → 202 |
| **Como corrigir** | n/a (é ação esperada) |
| **Critério de aceite** | [ ] Status `CANCELADA` · [ ] Arquivos parciais limpos · [ ] Não pode reverter para CONCLUIDA depois |

---

## Erros transversais

### 31. Token expirado

| Item | Valor |
|---|---|
| **Ação** | Deixar a sessão aberta por > 24h (default `JWT_EXPIRATION_MS=86400000`). Tentar uma ação. |
| **Resultado esperado** | Backend retorna 401. Interceptor Angular redireciona para `/login`. |
| **Mensagem** | "Sessão expirada. Faça login novamente." |
| **HTTP** | 401 |
| **Como corrigir** | Refazer login |
| **Critério de aceite** | [ ] Redirecionamento automático · [ ] Estado de form longo (wizard) **não preservado** (limitação aceita) |

### 32. Acesso a recurso sem permissão

| Item | Valor |
|---|---|
| **Ação** | Login como `leitor / leitor`. Tentar criar produto via `POST /produtos`. |
| **Resultado esperado** | 403 Forbidden. UI esconde botão **Novo produto** (defesa em profundidade). |
| **Mensagem** | "Você não tem permissão para esta ação." |
| **HTTP** | 403 |
| **Como corrigir** | Solicitar permissão `PRODUTO:CRIAR` ao admin |
| **Critério de aceite** | [ ] Botão escondido pra LEITOR · [ ] Bypass via URL direta retorna 403 |

### 33. Sair de form com mudanças não salvas

| Item | Valor |
|---|---|
| **Ação** | Editar form de cliente (mexer em qualquer campo). Clicar em outro item do menu sem salvar. |
| **Resultado esperado** | `CanDeactivateGuard` abre confirm |
| **Mensagem** | "Você tem mudanças não salvas. Deseja sair mesmo assim?" |
| **HTTP** | n/a |
| **Como corrigir** | Confirmar (descarta) ou cancelar (volta pro form) |
| **Critério de aceite** | [ ] Confirm aparece · [ ] Cancelar mantém form intacto · [ ] Confirmar descarta sem persistir |

### 34. Excluir entidade referenciada

| Item | Valor |
|---|---|
| **Ação** | Tentar excluir produto `Nexus` que tem releases publicadas. |
| **Resultado esperado** | 409 Conflict |
| **Mensagem** | "Não é possível excluir o produto Nexus: existem 3 releases vinculadas" |
| **HTTP** | 409 |
| **Como corrigir** | Excluir/arquivar dependentes primeiro OU inativar o produto |
| **Critério de aceite** | [ ] Mensagem cita exatamente o que está bloqueando · [ ] Sugere alternativa (inativar) |

### 35. Concurrent edit do mesmo recurso

| Item | Valor |
|---|---|
| **Ação** | Dois usuários editam o mesmo cliente simultaneamente. User A salva. User B salva depois. |
| **Resultado esperado** | "Last write wins" no MVP atual (sem optimistic locking). Mudança de A é sobrescrita. |
| **Mensagem** | n/a (sem detecção no MVP) |
| **HTTP** | 200 nas duas |
| **Como corrigir** | (Backlog) Adicionar `@Version` nas entidades e devolver 409 quando version diverge |
| **Critério de aceite** | [ ] Documentado como limitação · [ ] Toast genérico de sucesso (não engana usuário B) |

---

## Como reportar quando algo não bate

Quando um cenário diverge do esperado:

1. **Coletar:**
   - URL e payload da request (DevTools → Network → Copy as cURL).
   - Resposta HTTP completa (status + body).
   - `correlationId` (header de resposta ou toast).
   - Stack trace do log do backend filtrado por `correlationId`:
     ```bash
     docker compose logs backend 2>&1 | grep <correlationId>
     ```
   - Screenshot da UI no momento do erro.

2. **Classificar:**
   - **Bug:** comportamento diverge do esperado nesta doc → issue no repo apropriado.
   - **Spec divergente:** doc desatualizada → PR atualizando esta doc.
   - **Gap funcional:** comportamento não implementado → [`docs/jornadas/06-gaps-jornada-acme.md`](../jornadas/06-gaps-jornada-acme.md).

3. **Reportar:**
   - Issue com label `validacao` + referência ao cenário (ex.: "Cenário 17 — PAT GitHub inválido").
   - Anexar bundle de coleta do passo 1.
   - Marcar no checklist final como `[ ]` com link pra issue.

---

## Cenários adicionais que vale provocar (depois)

Não cobertos em profundidade aqui, mas vale validar à medida que a base evolui:

- **Rate limiting** em endpoints públicos (login, webhook).
- **CSRF** em ações sensíveis (excluir, publicar).
- **XSS** em campos textuais que viram PDF (resumo, observações, itens da release).
- **SQL injection** em filtros de busca (testar com `' OR 1=1--`).
- **Path traversal** em uploads de artefato (tentar nome `../../etc/passwd`).
- **Upload de arquivo gigante** (>2GB) — deve respeitar limite configurável.
- **Replay attack** no webhook (mesmo payload duas vezes — deve ser idempotente).
- **Webhook fora de ordem** (status `SUCCESS` chegando antes de `EM_ANDAMENTO`).
- **Browser sem cookies / JS desabilitado** — degradação graciosa.

Próximo passo: voltar ao [`04-checklist-final.md`](04-checklist-final.md) e marcar tudo que passou.
