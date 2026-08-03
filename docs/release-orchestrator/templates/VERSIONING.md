# Versionamento e release

> Copie este arquivo para `docs/VERSIONING.md` no repositório do produto e
> ajuste o que estiver entre `{{ }}`. Guia mestre: [`nexus-portal-api/docs/release-orchestrator/40-guia-versao-tag.md`](../40-guia-versao-tag.md)

---

## Convenção de tag

| Item | Valor |
|---|---|
| Formato | `vMAJOR.MINOR.PATCH` (ex.: `v1.5.0`) |
| Regex | `^v\d+\.\d+\.\d+$` |
| Pré-release | `v1.5.0-rc.1` (apenas se justificado) |
| Branch base | `{{ main }}` |
| Padrão de asset | `{{ produto-sigla }}-{{ modulo-codigo }}-{{ versao }}.{{ ext }}` |

A versão registrada no Release Orchestrator é **sem o prefixo `v`** (ex.: `1.5.0`).

---

## Fluxo de release (passo a passo)

1. **Portal**: registrar a release `{{ versao }}` no Release Orchestrator, adicionar itens, enviar para revisão, **publicar**.
2. **Repo**: garantir que `{{ main }}` está com todos os commits da release.
3. Criar a tag anotada:
   ```bash
   git checkout {{ main }}
   git pull --ff-only
   git tag -a v{{ versao }} -m "Release {{ versao }}"
   git push origin v{{ versao }}
   ```
4. **Jenkins** dispara o job `{{ produto-sigla }}-build` automaticamente (build-on-tag).
5. Validar no GitHub que o **Release** da tag contém o asset esperado.
6. **Portal**: nas próximas entregas, a tag aparece como `TO_TAG` candidata e o orchestrator pode baixar o asset (Fase 2).

---

## Pipeline do Jenkins

- Job: `{{ produto-sigla }}-build`
- Jenkinsfile: na raiz do repositório (`Jenkinsfile`)
- Credencial necessária: `github-pat-nexus` (PAT com escopo `repo`)
- Output: asset no GitHub Release nomeado conforme padrão acima

Veja o template do Jenkinsfile em [`nexus-portal-api/docs/release-orchestrator/templates/Jenkinsfile`](../templates/Jenkinsfile).

---

## Checklist antes de criar a tag

- [ ] CI verde na branch `{{ main }}`
- [ ] Release **PUBLICADA** no portal (não APROVADA)
- [ ] CHANGELOG.md atualizado (se aplicável)
- [ ] Sem commits "trabalho em andamento" após o ponto da release
- [ ] Equipe avisada (canal interno de release)

## Checklist pós-publicação

- [ ] Tag aparece em `git tag -l v\*`
- [ ] Job Jenkins concluiu com `SUCCESS`
- [ ] GitHub Release criado com asset
- [ ] Nome do asset bate com `{{ produto-sigla }}-{{ modulo-codigo }}-{{ versao }}.{{ ext }}`
- [ ] SHA256 conferido (campo `Digest` no GitHub Release)

---

## Troubleshooting

| Problema | Causa provável | Ação |
|---|---|---|
| Jenkins não disparou | Filtro de tag mal configurado | Ajustar webhook do job ou padrão de tag |
| `Tag não corresponde ao padrão` | Tag sem prefixo `v` ou com sufixo | Re-tag com `vMAJOR.MINOR.PATCH` |
| Asset ausente no GitHub Release | PAT sem escopo `repo` | Renovar PAT, atualizar credencial no Jenkins |
| Build OK mas portal não acha a tag | `repositorioGithub` no produto desalinhado | Conferir cadastro em `/produtos/:id` |
| Tag errada criada | Push pra remoto não desejável | `git push --delete origin v{{ versao }}` + recriar |

---

## Re-release (correção pós-tag)

Não sobrescreva uma tag já entregue. Se um asset estiver errado:

1. Bumpe a versão patch (`v{{ versao-patch+1 }}`).
2. Refaça o ciclo (release no portal + tag + Jenkins).
3. No portal, anote a re-release no campo de observações da próxima entrega.

Se a tag tem apenas alguns segundos de vida e não saiu para outros consumidores, deletar + recriar é tolerável — mas sempre prefira bumpar.

---

## Cross-reference

- [`nexus-portal-api/docs/release-orchestrator/40-guia-versao-tag.md`](../40-guia-versao-tag.md) — guia operacional mestre
- [`nexus-portal-api/docs/release-orchestrator/39-entregaveis-cicd-repositorios.md`](../39-entregaveis-cicd-repositorios.md) — entregáveis por tipo de módulo
- [`nexus-portal-api/docs/release-orchestrator/templates/`](../templates/) — Jenkinsfile + README
