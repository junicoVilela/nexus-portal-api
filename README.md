# Nexus Portal API - AI Kit

Este pacote contém arquivos de contexto, regras, skills e prompts para orientar ferramentas de IA como Cursor, Codex e Claude Code no desenvolvimento do backend.

Arquitetura desejada:

- Monólito modular simples
- Sem arquitetura hexagonal
- Sem DDD pesado
- Organização por módulos de negócio
- Fluxo principal: Controller -> Service -> Repository -> Database
- Backend separado do frontend
- Projeto principal com módulos atuais:
  - Gateway
  - Manual de Usuário / DocFlow
- Projeto preparado para novos módulos futuros

Estrutura recomendada no projeto:

```text
nexus-portal-api/
├── .ai/
│   ├── project/
│   ├── modules/
│   └── prompts/
├── .cursor/rules/
├── AGENTS.md
├── CLAUDE.md
└── src/main/java/com/nexus/portal/
```

Como usar:

1. Copie a pasta `.ai` para a raiz do backend.
2. Copie `.cursor/rules` se usar Cursor.
3. Copie `AGENTS.md` se usar Codex.
4. Copie `CLAUDE.md` se usar Claude Code.
5. Ajuste nomes de pacotes e módulos conforme o projeto real.
