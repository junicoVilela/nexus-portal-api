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

## Padrão

```text
/api/v1/{recurso}
```

Para ação específica:

```text
PATCH /api/v1/{recurso}/{id}/acao
POST  /api/v1/{recurso}/{id}/acao
```
