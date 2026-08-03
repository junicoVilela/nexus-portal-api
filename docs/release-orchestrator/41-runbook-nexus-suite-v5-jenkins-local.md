# Runbook — validação local do Nexus Suite V5 com Jenkins

Este guia valida o caminho de homologação do módulo **WEB** usando o
repositório `nexus-org/nexus-suite`, sem alterar `main` nem `release/v5.4.1`.
O branch de trabalho é:

```text
test/release-orchestrator-v5.4.1
```

## 1. O que este fluxo valida

```text
GitHub (branch de teste)
  → Jenkins local
  → build do JAR WEB
  → GitHub Release (asset JAR)
  → Release Orchestrator
  → entrega HOM, ZIP e PDF
```

O Jenkins produz o artefato genérico da versão. O Release Orchestrator monta
o delta e o pacote específico de cada cliente; ele não compila o Nexus Suite.

## 2. Segredos e credenciais

Nunca versione tokens ou o secret em Git.

### GitHub PAT

No GitHub: **Settings → Developer settings → Personal access tokens →
Fine-grained tokens → Generate new token**.

- *Resource owner*: `nexus-org`.
- *Repository access*: somente `nexus-suite`.
- Para clone: `Contents: Read-only`.
- Para publicar/atualizar asset em GitHub Release: `Contents: Read and write`.
- Use expiração curta (por exemplo, 90 dias).

Se a organização exigir aprovação do token fine-grained, aguarde a aprovação
antes de testar o job.

### Secret do webhook

Gere localmente:

```bash
openssl rand -hex 32
```

O mesmo valor deve existir nos dois lados:

```text
# infra/docker/.env
PORTAL_WEBHOOK_SECRET=<valor-gerado>

# processo do backend
RELEASE_ORCHESTRATOR_WEBHOOKS_JENKINS_SECRET=<valor-gerado>
```

## 3. Subir o Jenkins local

O Jenkins é opcional no Compose, portanto `docker compose up -d` não o sobe.
Como pode já existir um PostgreSQL local usando a porta 5432, inicie somente o
Jenkins:

```bash
cd nexus-portal-api/infra/docker
cp .env.example .env
# Preencha GITHUB_USER, GITHUB_PAT e PORTAL_WEBHOOK_SECRET no .env.

docker compose --profile ci up -d --no-deps jenkins
curl -fsS http://localhost:8090/login >/dev/null && echo 'Jenkins OK'
```

Se o container antigo falhar com `network ... not found`, recrie somente ele
(o volume `nexus-jenkins-home` é preservado):

```bash
docker compose --profile ci up -d --force-recreate --no-deps jenkins
```

Acesse `http://localhost:8090`. As credenciais padrão são `admin` / `admin`
somente se não tiverem sido sobrescritas no `.env` ou no volume já existente.

Após alterar o `.env`, recarregue as credenciais provisionadas via JCasC:

```bash
docker compose restart jenkins
```

## 4. Preparar o agente que executará o build

O Nexus Suite V5 exige requisitos diferentes do Portal:

| Requisito | Versão/observação |
| --- | --- |
| Java | 17 |
| Gradle | wrapper `8.10.2` do repositório |
| Node.js | 12.x |
| Yarn | 1.x |
| `nexus-ai-rag` e `nexus-ai-web` | versão `2.0.0` em Maven local ou repositório Maven interno |

O Jenkins do Compose usa JDK 21 no controlador; ele **não deve ser assumido
como agente compatível** com o Nexus Suite. Configure um agente/label de build
com os requisitos acima e execute o job nele. A primeira etapa do `Jenkinsfile`
verifica esses pré-requisitos e falha com uma mensagem objetiva se algo faltar.

Para uma validação local, as bibliotecas `nexus-ai` podem estar no Maven local
do agente. Para ambiente produtivo, publique-as em um repositório Maven
interno; não dependa de diretórios pessoais de desenvolvedores.

## 5. Criar o job no Jenkins

1. Acesse **New Item → Pipeline**.
2. Nome: `nexus-suite-v5-web-hom`.
3. Em **Pipeline**, selecione **Pipeline script from SCM**.
4. SCM: **Git**.
5. URL: `https://github.com/nexus-org/nexus-suite.git`.
6. Credencial: `github-user-pat` (provisionada pelo `.env`).
7. Branch: `*/test/release-orchestrator-v5.4.1`.
8. Script path: `Jenkinsfile`.
9. Restrinja o job ao label do agente preparado na seção anterior.
10. Salve.

O pipeline recebe os parâmetros abaixo:

| Parâmetro | Valor de homologação |
| --- | --- |
| `PORTAL_URL` | `http://host.docker.internal:8080` |
| `PORTAL_PRODUCT_SIGLA` | sigla cadastrada para o Nexus Suite, por exemplo `NexusSUITE` |
| `RELEASE_VERSION` | `5.4.1` |

O Docker Compose já mapeia `host.docker.internal` para a máquina host. Caso o
Portal esteja em outra porta, ajuste `PORTAL_URL` ao disparar o build.

## 6. Configurar o produto no Release Orchestrator

No Portal, em **Produtos**, cadastre ou edite o Nexus Suite:

- Sigla: use exatamente a mesma informada em `PORTAL_PRODUCT_SIGLA`.
- Repositório GitHub: `nexus-org/nexus-suite`.
- Branch padrão: `test/release-orchestrator-v5.4.1`.
- Padrão de tag: ajuste ao padrão de homologação adotado.
- Jenkins URL: `http://localhost:8090` quando o Portal roda na máquina host.
- Job Jenkins: `nexus-suite-v5-web-hom`.
- Usuário e API token: token de API do usuário Jenkins, não a senha de login.

Crie o módulo do produto:

| Campo | Valor |
| --- | --- |
| Código | `WEB` |
| Nome | `Nexus Suite V5` |
| Tipo | `WEB` |
| Gera delta | não |
| Obrigatório | sim |

Depois crie o cliente de homologação, vincule o produto e o módulo e informe
a versão atualmente instalada (`5.4.0`, por exemplo). Crie a release `5.4.1`
e vincule a versão `5.4.1` ao módulo WEB.

Use **Testar Jenkins** no cadastro do produto para confirmar que o Portal
enxerga o job e seu último build.

## 7. Executar e validar

1. No Jenkins, abra `nexus-suite-v5-web-hom` e clique em **Build with
   Parameters**.
2. Informe a versão `5.4.1` e a sigla do produto cadastrada no Portal.
3. Acompanhe o Console Output.
4. O JAR esperado é:

   ```text
   nexus-application/build/libs/nexus-application-5.4.1.jar
   ```

5. Confirme no Portal que a release mostra o build `EM_ANDAMENTO` e depois
   `SUCCESS`, com link para o Jenkins.
6. Crie uma entrega para o cliente de HOM, inicialize a seleção, calcule o
   delta e gere o pacote.
7. Baixe o ZIP e o PDF da entrega e valide o JAR WEB dentro do ZIP.

## 8. Publicação do artefato: etapa obrigatória para sincronização automática

Arquivar o JAR no Jenkins permite auditoria do build, mas não o torna
automaticamente disponível ao Release Orchestrator. Para o Portal sincronizar
um módulo WEB automaticamente, o JAR precisa estar anexado à **GitHub Release**
da mesma versão/tag configurada no produto.

Enquanto o estágio de publicação de asset não estiver habilitado no pipeline,
faça uma destas opções:

1. envie o JAR pela tela de artefatos da release para validar o restante do
   fluxo; ou
2. publique manualmente o JAR como asset da GitHub Release correspondente.

O passo seguinte de evolução é adicionar ao `Jenkinsfile` o upload com `gh
release upload` usando a credencial `github-pat-nexus`; para isso o PAT precisa
de `Contents: Read and write`.

## 9. Diagnóstico rápido

| Sintoma | Verificação/correção |
| --- | --- |
| Jenkins não aparece | use `docker compose --profile ci up -d --no-deps jenkins` |
| `network ... not found` | recrie com `--force-recreate --no-deps` |
| Job não clona | valide `GITHUB_USER`, `GITHUB_PAT` e a aprovação organizacional do token |
| Node incompatível | o agente deve usar Node 12.x |
| `nexus-ai` ausente | instale no Maven do agente ou publique em repositório interno |
| Portal retorna 403 no webhook | os dois secrets não são idênticos |
| Portal não encontra o JAR | anexe-o à GitHub Release da versão ou faça upload manual temporário |

## Referências

- [Cadastro de produto](09-produtos-cadastro.md)
- [Módulos e artefatos](10-produtos-modulos-artefatos.md)
- [Nova entrega](18-nova-entrega-assistente.md)
- [Geração do pacote](21-geracao-pacote.md)
- [Fluxo local genérico com Jenkins](../jornadas/10-fluxo-local-completo-com-jenkins.md)
