# Padrões de API REST

## Prefixo padrão

```text
/api/v1
```

## Prefixo de módulo

Endpoints privados de um módulo de negócio são prefixados por módulo:

```text
/api/v1/docflow/...
/api/v1/release-orchestrator/...
/api/v1/gateway/...
```

Endpoints globais (não pertencem a um módulo) ficam sem prefixo de módulo:

```text
/api/v1/auth/...
/api/v1/public/...
```

## Recursos em plural

Usar:

```text
/api/v1/docflow/clientes
/api/v1/docflow/paginas
/api/v1/release-orchestrator/releases
/api/v1/release-orchestrator/produtos
/api/v1/gateway/routes
```

Evitar:

```text
/api/v1/criarUsuario
/api/v1/listarManual
```

## Padrão CRUD

```text
GET    /api/v1/{modulo}/recursos
GET    /api/v1/{modulo}/recursos/{id}
POST   /api/v1/{modulo}/recursos
PUT    /api/v1/{modulo}/recursos/{id}
PATCH  /api/v1/{modulo}/recursos/{id}/status
DELETE /api/v1/{modulo}/recursos/{id}
```

## Ações específicas

Para ações fora do CRUD puro:

```text
PATCH /api/v1/docflow/paginas/{id}/publicar
POST  /api/v1/release-orchestrator/releases/{id}/publicar
POST  /api/v1/release-orchestrator/releases/{id}/cancelar
POST  /api/v1/gateway/routes/{id}/testar
POST  /api/v1/docflow/publicacoes/{id}/reprocessar
```

## Status HTTP

```text
200 OK              consulta, atualização, ação executada
201 Created         criação
204 No Content      exclusão sem corpo
400 Bad Request     request inválida
401 Unauthorized    não autenticado
403 Forbidden       sem permissão
404 Not Found       recurso não encontrado
409 Conflict        conflito de regra de negócio
422 Unprocessable   violação de regra de negócio (usado pelo BusinessException)
500 Internal Error  erro inesperado
```

## Resposta de erro

`GlobalExceptionHandler` produz:

```json
{
  "timestamp": "2026-06-06T10:15:30Z",
  "status": 422,
  "message": "Já existe cliente com o slug informado.",
  "errors": []
}
```

Erros de validação retornam o campo + mensagem em `errors`.

## Paginação

Endpoints de listagem aceitam:

```text
?page=1&size=20&sort=createdAt&direction=DESC
```

Retorno padrão (`PageResponse` do `shared`):

```json
{
  "content": [],
  "page": 1,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5
}
```

## Versionamento

Versionamento no path:

```text
/api/v1
```

Não criar `/api/v2` sem necessidade real.

## Documentação

Usar OpenAPI/Swagger.

Todo endpoint relevante deve ter:

- resumo
- descrição
- status possíveis
- request/response claros
