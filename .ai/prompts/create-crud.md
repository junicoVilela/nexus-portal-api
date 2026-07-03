# Prompt - Criar CRUD Backend

Crie um CRUD completo seguindo os padrões do projeto.

## Entrada esperada

- Nome do módulo
- Nome da entidade
- Campos
- Regras de negócio
- Endpoints desejados

## Deve gerar

- Entity
- Repository
- Service
- ServiceImpl
- Controller
- Request DTO
- Response DTO
- Mapper
- Exception de não encontrado
- Migration Flyway

## Regras

- Controller chama Service.
- Service chama Repository.
- Repository não contém regra de negócio.
- Controller não retorna Entity.
- Usar paginação na listagem.
- Usar `@Valid`.
- Usar nomes em português quando fizer sentido.
- **Cada endpoint do Controller exige `@PreAuthorize` granular** usando
  `Permissoes.<FUNC>_<ACAO>` (ver SECURITY_STANDARDS.md). Não usar
  `@PreAuthorize` no nível de classe.
- A migration de seed deve, além da entidade, **cadastrar a funcionalidade
  no catálogo RBAC**:
  - `tb_funcionalidade` com a nova funcionalidade.
  - `tb_permissao` com as 4 ações (`:LER/:CRIAR/:EDITAR/:EXCLUIR`) — o
    pattern do V5 com `CROSS JOIN (VALUES …)` faz isso idempotentemente.
  - `tb_grupo_permissao` propagando para ADMIN (todas), EDITOR (CRUD),
    LEITOR e REVISOR (`:LER`). Seguir o template do V8/V9.
- Adicionar as constantes em `shared/security/Permissoes.java`.
