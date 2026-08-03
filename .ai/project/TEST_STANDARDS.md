# Padrões de Testes Backend

## Estratégia

Para projeto pequeno/médio, priorizar:

- Testes unitários de services com regra de negócio.
- Testes de controllers quando houver validação ou orquestração relevante.
- Testes de repositories apenas para queries customizadas (`@Query`,
  Specification, native query).
- Testes de integração para fluxos críticos (publicação, geração de
  pacote, autenticação).

## Ferramentas

- JUnit 5
- Mockito
- AssertJ
- Spring Boot Test
- Testcontainers (opcional, para integração com Postgres real)

## Nomenclatura

```text
ReleaseServiceTest
ProdutoRhServiceTest
ClienteServiceTest
GrupoServiceTest
PaginaServiceTest
```

Como os services são classes concretas (sem `*Impl`), o nome do teste é
o nome da classe + `Test`.

## Padrão de teste

Nome descritivo no estilo `acao_cenario_resultado`:

```java
@Test
void criar_comDadosValidos_deveCriarCliente() {
    // ...
}
```

Quando precisar de descrição mais expressiva, usar `@DisplayName`:

```java
@Test
@DisplayName("Deve recusar publicação quando release não está aprovada")
void publicar_releaseNaoAprovada_deveLancarBusinessException() {
}
```

## O que testar

- Regra de negócio.
- Validação de estado / transições de status.
- Recurso não encontrado.
- Conflito de regra (unicidade, status inválido).
- Transformação importante (slug, normalização, cor de tema).
- Filtros importantes.

Não precisa testar:

- Getters e setters (gerados pelo Lombok).
- Repository padrão do Spring Data sem query customizada.
- Código gerado automaticamente.

## Auditoria em testes unitários

`created_by`/`updated_by` são preenchidos pelo Spring Data JPA Auditing
**em runtime**. Em testes unitários puros (sem `@DataJpaTest` /
`@SpringBootTest`) esses campos ficam `null`. **Não** asserte sobre eles
em unit tests — use `@DataJpaTest` se precisar validar o preenchimento.

## Organização

```text
{modulo}/src/test/java/br/com/nexus/portal/{modulo}/
├── service/
├── controller/  (quando aplicável)
└── repository/  (quando aplicável)
```
