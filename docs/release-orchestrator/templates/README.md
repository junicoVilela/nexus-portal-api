# Templates de CI/CD por repositório

Arquivos prontos para serem **copiados** para os repositórios de produto Softon
(ex.: `softon/dtec-ld`, `softon/dtec-cr`) no contexto da
[`Sprint 8`](../../jornadas/02-checklist-por-sprint.md) — Jenkins + GitHub.

| Arquivo | Onde colocar no repo do produto | Quando usar |
|---|---|---|
| [`Jenkinsfile`](Jenkinsfile) | raiz do repo (`/Jenkinsfile`) | Todos os repos WEB/BATCH |
| [`VERSIONING.md`](VERSIONING.md) | `/docs/VERSIONING.md` | Todos os repos |
| [`README-versionamento.md`](README-versionamento.md) | seção do `/README.md` | Cole no README principal |

Runbook operacional passo-a-passo: [`../../jornadas/07-runbook-s8-jenkins-github.md`](../../jornadas/07-runbook-s8-jenkins-github.md).

Guia de versionamento mestre: [`../40-guia-versao-tag.md`](../40-guia-versao-tag.md).

Estes templates **não são executados** pelo portal — são entregáveis de processo.

---

## Como aplicar a um repositório novo

1. Copie os três arquivos para o repo do produto, mantendo a estrutura.
2. Edite os valores entre `{{ }}` (sigla do produto, módulo, extensão).
3. No Jenkinsfile, ajuste `PRODUTO_SIGLA`, `MODULO_CODIGO`, `ARTIFACT_EXT`, `BUILD_CMD`.
4. Crie o job no Jenkins seguindo o [runbook S8](../../jornadas/07-runbook-s8-jenkins-github.md).
5. Crie uma tag de teste `v0.0.1` e valide.

---

## Checklist por tipo de módulo

### WEB

- [ ] `ARTIFACT_EXT=war` (ou `jar` se for Spring Boot embedded)
- [ ] `BUILD_CMD=mvn -B -ntp clean package -DskipTests` (ou gradle)
- [ ] `BUILD_OUTPUT_DIR=target` (ou `build/libs` no gradle)
- [ ] Asset esperado no Release: `{sigla}-{modulo}-{versao}.war`
- [ ] Cadastrar no portal: módulo tipo `WEB`, `extensoesAceitas=[war]`

### BATCH

- [ ] `ARTIFACT_EXT=jar`
- [ ] `BUILD_CMD` igual ao WEB; checar se o pom faz shaded jar
- [ ] Asset esperado: `{sigla}-{modulo}-{versao}.jar`
- [ ] Portal: módulo tipo `BATCH`, `extensoesAceitas=[jar,zip,tar.gz]`

### BANCO

- [ ] Sem Jenkinsfile padrão **se** os scripts são versionados a mão
  (sem build) — apenas commitar SQL na ordem `DDL_###`/`DML_###`
- [ ] **Se** houver build (zipping/concatenação), criar Jenkinsfile customizado
  que monta `DDL.sql` + `DML.sql` por dialeto e publica como assets
- [ ] Convenção de pasta: `db/oracle/`, `db/sqlserver/` (ou sufixo no nome)
- [ ] Portal: módulo tipo `BANCO`, `extensoesAceitas=[sql,zip]`

### KETTLE

- [ ] Sem Jenkinsfile padrão para a maioria dos casos — versionar `.ktr`/`.kjb` no repo
- [ ] Para gerar ZIP delta entre tags, criar Jenkinsfile customizado
  (ver `kettle-delta.sh` em `softon/release-pipeline-base` quando existir)
- [ ] Portal: módulo tipo `KETTLE`, `extensoesAceitas=[ktr,kjb,zip]`

### FUNCIONALIDADES / REGRAS

- [ ] **Não usam Jenkinsfile** — gerados pelo orchestrator na hora da entrega
- [ ] Repo cuida apenas dos templates Thymeleaf (`funcionalidades/`, `regras/`)
- [ ] Catálogo de funcionalidades vive no portal (ver spec `11`)

---

## Manutenção

Quando atualizar os templates:

1. Edite aqui primeiro (este diretório é a fonte da verdade).
2. Anuncie no canal interno; mantenedores de repo copiam manualmente.
3. Não há sincronização automática — cada repo pode estar em uma versão.

Para mudanças disruptivas (ex.: campo novo no portal `padraoTag` afeta a regex),
documentar **antes** em [`40-guia-versao-tag.md`](../40-guia-versao-tag.md) e
**depois** atualizar os templates.
