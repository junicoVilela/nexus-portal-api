# 40 — Guia Versão / Tag

> **Estado**: 📋 Especificado — documento operacional para equipes de produto e release.  
> **Onde publicar**: copiar/adaptar para wiki do produto ou `docs/VERSIONING.md` em cada repositório.  
> **Relacionado**: [`39-entregaveis-cicd-repositorios.md`](39-entregaveis-cicd-repositorios.md), [`09-produtos-cadastro.md`](09-produtos-cadastro.md), [`20-range-manual-delta.md`](20-range-manual-delta.md).

---

## 1. Objetivo

Padronizar como **criar, publicar e consumir tags** de versão para que:

- Jenkins dispare builds (`build-on-tag`);
- GitHub Releases recebam assets (WAR, JAR, SQL, Kettle ZIP);
- O Release Orchestrator vincule `ReleaseModuloVersao` à tag real;
- O orchestrator calcule delta `FROM_TAG..TO_TAG` por cliente.

---

## 2. Convenção de versão

| Elemento | Regra |
|---|---|
| Formato da tag Git | `vMAJOR.MINOR.PATCH` (ex.: `v1.5.0`) |
| Regex no portal | `^v\d+\.\d+\.\d+$` (configurável em `09`) |
| Versão na release (portal) | Sem prefixo `v` no campo `versao` (ex.: `1.5.0`) — normalizar ao comparar |
| Pré-release (opcional) | `v1.5.0-beta.1` — só se regex do produto permitir |
| Semântica | MAJOR/MINOR/PATCH alinhados ao `TipoRelease` escolhido no portal |

---

## 3. Fluxo ponta a ponta

```text
1. Desenvolvimento na branch padrão (main)
2. Release notes registradas no portal (itens, revisão, PUBLICADA)
3. Módulos da release com versão alvo (ReleaseModuloVersao)
4. Criar tag anotada no Git:
      git tag -a v1.5.0 -m "Release 1.5.0"
      git push origin v1.5.0
5. Jenkins (build-on-tag) compila e publica assets no GitHub Release
6. Validar assets no GitHub (WAR, DDL.sql, etc.)
7. Portal: opcional — vincular tag GitHub aos módulos (Fase 2)
8. Orchestrator: entrega ao cliente usa TO_TAG = v1.5.0
   FROM_TAG = última tag entregue ao cliente (histórico 23)
```

---

## 4. Checklist por módulo

### WEB / BATCH

- [ ] Tag criada após merge na branch de release
- [ ] Jenkins concluiu com sucesso
- [ ] GitHub Release contém asset esperado (`.war` ou `.jar`)
- [ ] Nome do asset segue padrão `{sigla}-{versao}.{ext}`
- [ ] SHA do asset registrado (GitHub ou portal Fase 2)

### BANCO

- [ ] Scripts commitados na ordem DDL → DML
- [ ] Pipeline gera `DDL.sql` e `DML.sql` (ou ZIP) na release
- [ ] Testado em Oracle **ou** SQL Server conforme cliente alvo
- [ ] Tag aponta para commit que contém todos os scripts da versão

### KETTLE

- [ ] Jobs alterados commitados
- [ ] Pipeline gera ZIP delta ou pacote completo na release
- [ ] Dependências (subjobs) incluídas no ZIP

### FUNCIONALIDADES / REGRAS

- [ ] Não usam tag de repo para entrega — gerados na entrega pelo orchestrator
- [ ] Catálogo de domínios/funcionalidades atualizado no portal ([`11`](11-produtos-catalogo-funcional.md), matriz em [`05`](05-cliente-dominios-funcionalidades.md))

---

## 5. Exemplo: NEXUS-LD v1.5.0

| Passo | Ação |
|---|---|
| 1 | Release `NEXUS-LD 1.5.0` **PUBLICADA** no portal com itens revisados |
| 2 | Módulos: `nexus-web` (WEB), `nexus-db` (BANCO), `nexus-etl` (KETTLE) |
| 3 | Tag `v1.5.0` nos repos `nexus-ld`, `nexus-ld-db`, `nexus-ld-kettle` |
| 4 | Jenkins publica assets no GitHub Release de cada repo |
| 5 | Cliente ACME: última entrega foi `v1.4.0` → delta `v1.4.0..v1.5.0` |
| 6 | Assistente de entrega (`18`) seleciona módulos; operador confirma range (`20`) |
| 7 | Geração (`21`) monta pacote `ACME_NEXUSLD_1.5.0_*.zip` |

---

## 6. Range manual (FROM_TAG .. TO_TAG)

Quando o operador **altera** o range padrão:

| Campo | Default | Quando mudar |
|---|---|---|
| `FROM_TAG` | Última tag entregue ao cliente (`ClienteProdutoModulo`) | Cliente pulou versões ou hotfix pontual |
| `TO_TAG` | Tag da release-alvo | Correção de tag errada |
| Justificativa | — | **Obrigatória** se FROM/TO ≠ default (`20`) |

**WEB/BATCH**: `FROM_TAG` é histórico; pacote leva asset completo da `TO_TAG`.  
**BANCO/KETTLE**: delta real entre tags — ver [`20-range-manual-delta.md`](20-range-manual-delta.md).

---

## 7. Troubleshooting

| Problema | Ação |
|---|---|
| Jenkins não disparou | Verificar webhook/tag pattern vs `padraoTag` do produto |
| Asset ausente no GitHub Release | Reexecutar job; verificar permissão do PAT |
| Tag existe mas portal não encontra | Conferir `repositorioGithub` e credencial em `09` |
| Delta vazio entre tags | Verificar se houve commit entre tags no path do módulo (`10` § config) |
| Cliente com versão desconhecida | Atualizar `ClienteProdutoModulo.versaoAtual` manualmente antes da entrega |

---

## 8. Template para README do repositório

Cada produto pode incluir no repo:

```markdown
## Versionamento

- Tags: `vMAJOR.MINOR.PATCH`
- Release: criar tag após aprovação no Release Orchestrator
- Jenkins: job `{produto}-build` (build-on-tag)
- Assets: publicados no GitHub Release da tag
- Guia completo: nexus-portal-api/docs/release-orchestrator/40-guia-versao-tag.md
```

---

## Cross-reference

- [`39-entregaveis-cicd-repositorios.md`](39-entregaveis-cicd-repositorios.md) — Jenkinsfile e entregáveis por repo.
- [`14-release-orchestrator-detalhe.md`](14-release-orchestrator-detalhe.md) — Vínculo release ↔ módulos/artefatos.
- [`38-glossario.md`](38-glossario.md) — Termos Tag, Asset, Delta.
