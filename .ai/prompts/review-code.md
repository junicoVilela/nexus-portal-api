# Prompt - Revisar Código Backend

Revise o código considerando os padrões do projeto.

## Verificar

- O código está no módulo correto?
- A estrutura segue Controller/Service/Repository?
- Há arquitetura hexagonal indevida?
- Há ports/adapters/use cases desnecessários?
- Controller chama apenas Service?
- Controller retorna DTO?
- Entity está sendo exposta indevidamente?
- Service concentra regra de negócio?
- Repository está sem regra de negócio?
- Request usa Bean Validation?
- Erros são tratados corretamente?
- Há nomes claros?
- Há complexidade desnecessária?
- Há dependência indevida entre módulos?
- Há acesso direto a Repository de outro módulo?

## Resposta esperada

- Problemas encontrados
- Sugestões de melhoria
- Código corrigido quando possível
- Melhorias futuras