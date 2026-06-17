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
