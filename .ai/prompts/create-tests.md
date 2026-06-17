# Prompt - Criar Testes Backend

Crie testes para o código informado.

## Prioridade

1. Testes de Service
2. Testes de regras de negócio
3. Testes de exceções
4. Testes de Controller quando houver validação relevante
5. Testes de Repository apenas para query customizada

## Ferramentas

- JUnit 5
- Mockito
- AssertJ

## Nomenclatura

```java
deveCriarManualQuandoDadosValidos();
deveLancarErroQuandoManualNaoEncontrado();
naoDevePublicarManualSemSecoes();
```
