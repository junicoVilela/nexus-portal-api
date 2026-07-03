# Prompt - Criar Endpoint Backend

Crie um novo endpoint em um módulo existente.

## Regras

- Identificar o módulo correto.
- Atualizar Controller.
- Criar ou ajustar DTOs.
- Implementar regra no Service.
- Criar query no Repository apenas se necessário.
- Atualizar mapper se necessário.
- Criar teste se houver regra de negócio.
- Não criar abstrações desnecessárias.
- **Sempre** anotar o método com `@PreAuthorize` granular usando
  `Permissoes.X_Y` (ver SECURITY_STANDARDS.md). O default por verbo HTTP:
  - `GET` → `:LER`
  - `POST` → `:CRIAR` (ou `:EDITAR` em ações sobre recurso existente)
  - `PUT` / `PATCH` → `:EDITAR`
  - `DELETE` → `:EXCLUIR`
- Se a ação não cabe no CRUD (ex.: PUBLICAR, REVOGAR, RESETAR), crie a
  permissão especial na próxima migration `V*__rbac__*` e a constante em
  `Permissoes.java` antes de usar.
- Sub-recursos (`/{id}/algo`) reutilizam a permissão do agregado pai.

## Padrão

```text
/api/v1/{recurso}
```

Para ação específica:

```text
PATCH /api/v1/{recurso}/{id}/acao
POST  /api/v1/{recurso}/{id}/acao
```
