<!-- Snippet pra colar no README.md do repo do produto -->

## Versionamento e release

Este repositório faz parte do **Softon Release Orchestrator**.

- **Tag**: `vMAJOR.MINOR.PATCH` (ex.: `v1.5.0`)
- **Trigger**: ao criar uma tag, o Jenkins compila e publica os assets no GitHub Release correspondente.
- **Job Jenkins**: `{{ produto-sigla }}-build`
- **Guia operacional**: [`docs/VERSIONING.md`](docs/VERSIONING.md)
- **Guia mestre**: [softon-portal-api/docs/release-orchestrator/40-guia-versao-tag.md](https://github.com/softon/softon-portal-api/blob/main/docs/release-orchestrator/40-guia-versao-tag.md)

Para criar uma release nova:

```bash
git checkout main && git pull --ff-only
git tag -a v1.5.0 -m "Release 1.5.0"
git push origin v1.5.0
```

A release **deve estar PUBLICADA** no Release Orchestrator antes da tag.
