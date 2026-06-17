# Padrões de Estilo Java

Diretrizes de estilo e boas práticas para geração, revisão e refatoração de código Java no projeto.

---

## Diretriz principal

Sempre que possível, utilize recursos modernos do Java para deixar o código mais limpo, legível e fácil de manter.

Recursos modernos não devem ser usados apenas para deixar o código "mais sofisticado". A clareza da regra de negócio deve sempre vir antes da redução de linhas de código.

---

## Preferências de implementação

Ao gerar ou refatorar código Java, priorize:

- `Streams` para manipulação de coleções quando isso melhorar a legibilidade.
- `Lambda Expressions` para operações simples e objetivas.
- `Optional` para evitar retornos nulos em cenários apropriados.
- `var` apenas quando o tipo for óbvio e não prejudicar a leitura.
- `record` para objetos imutáveis simples, DTOs, Responses, Requests e Value Objects simples (ver `BACKEND_STANDARDS.md` para padrão de DTOs do projeto).
- `EnumMap` e `EnumSet` quando a chave ou conjunto for baseado em `enum`.
- `Collectors`, `groupingBy`, `mapping`, `filtering` e `partitioningBy` quando fizer sentido.
- `Map.of`, `List.of` e `Set.of` para coleções pequenas e imutáveis.
- `Comparator.comparing` para ordenações.
- `Objects.requireNonNull` para validações simples de obrigatoriedade.
- `StringBuilder` apenas em concatenações complexas ou dentro de loops.
- Métodos privados com nomes claros para quebrar regras complexas.
- Código simples, testável e com baixo acoplamento.

---

## Streams e Lambdas

Priorize `Streams` e `Lambdas` para transformar, filtrar, ordenar, agrupar ou mapear listas.

### Exemplo recomendado

```java
List<UsuarioResponse> usuariosAtivos = usuarios.stream()
    .filter(Usuario::isAtivo)
    .sorted(Comparator.comparing(Usuario::getNome))
    .map(usuarioMapper::toResponse)
    .toList();
```

### Quando evitar Stream

Evite usar `Stream` quando um `for` simples deixar o código mais claro, principalmente quando houver:

- Muitas regras de negócio.
- Validações encadeadas.
- Efeitos colaterais.
- Chamadas externas.
- Tratamento de exceções.
- Fluxos difíceis de ler em uma única pipeline.

### Exemplo aceitável com `for`

```java
for (Usuario usuario : usuarios) {
    if (!usuario.isAtivo()) {
        continue;
    }

    validarPermissoes(usuario);
    notificarUsuario(usuario);
}
```

---

## Boas práticas com Streams

- Não usar `stream()` apenas para parecer moderno.
- Evitar `peek()` para regra de negócio.
- Evitar streams muito longos ou difíceis de ler.
- Quebrar pipelines complexos em métodos privados com nomes claros.
- Evitar efeitos colaterais dentro de `map`, `filter` ou `forEach`.
- Preferir `toList()` ao invés de `collect(Collectors.toList())` quando estiver usando Java 16+.
- Usar `parallelStream()` somente com justificativa clara.
- Não misturar transformação de dados com persistência ou chamadas externas dentro do mesmo stream.

### Exemplo de pipeline quebrada em métodos privados

```java
List<UsuarioResponse> usuariosAtivos = usuarios.stream()
    .filter(this::usuarioPodeAcessarSistema)
    .sorted(Comparator.comparing(Usuario::getNome))
    .map(usuarioMapper::toResponse)
    .toList();

private boolean usuarioPodeAcessarSistema(Usuario usuario) {
    return usuario.isAtivo() && usuario.possuiPermissaoDeAcesso();
}
```

---

## Optional

Utilize `Optional` principalmente em retornos onde o valor pode não existir.

### Exemplo recomendado

```java
public Optional<Usuario> buscarPorId(Long id) {
    return usuarioRepository.findById(id);
}
```

### Evite usar Optional em

- Atributos de entidades JPA.
- DTOs.
- Parâmetros obrigatórios.
- Campos de request.
- Serialização JSON.

---

## Records

Utilize `record` para DTOs simples, imutáveis e sem comportamento complexo.

### Bons usos para record

- Responses de API.
- Requests simples.
- DTOs de consulta.
- Value Objects simples.
- Projeções de leitura.
- Objetos imutáveis sem regra complexa.

### Evite record para

- Entidades JPA.
- Objetos com mutabilidade controlada.
- Classes com muitos comportamentos internos.
- Objetos que precisam de construtor vazio para frameworks.

> Para padrão de nomes e exemplos de Request/Response, consulte `BACKEND_STANDARDS.md`.

---

## Var

O uso de `var` é permitido quando o tipo estiver evidente no lado direito da atribuição.

### Exemplo aceitável

```java
var usuario = usuarioRepository.findById(id)
    .orElseThrow(() -> new BusinessException("Usuário não encontrado."));
```

### Evite

```java
var resultado = processar(dados);
```

Nesse caso, o tipo não está claro e pode prejudicar a leitura.

---

## Collections modernas

Use coleções imutáveis quando os dados não precisarem ser alterados.

### Exemplos

```java
List<String> perfisPadrao = List.of("ADMIN", "EDITOR", "AUDITOR");

Set<String> permissoes = Set.of("USUARIO_CRIAR", "USUARIO_EDITAR");

Map<String, String> mensagens = Map.of(
    "USUARIO_NAO_ENCONTRADO", "Usuário não encontrado.",
    "ACESSO_NEGADO", "Acesso negado."
);
```

---

## EnumMap e EnumSet

Quando a chave de um mapa for um `enum`, prefira `EnumMap`.

```java
EnumMap<StatusUsuario, String> descricoes = new EnumMap<>(StatusUsuario.class);
descricoes.put(StatusUsuario.ATIVO, "Usuário ativo");
descricoes.put(StatusUsuario.BLOQUEADO, "Usuário bloqueado");
```

Quando precisar de um conjunto baseado em enum, prefira `EnumSet`.

```java
EnumSet<Permissao> permissoesAdministrativas = EnumSet.of(
    Permissao.CRIAR_USUARIO,
    Permissao.EDITAR_USUARIO,
    Permissao.BLOQUEAR_USUARIO
);
```

---

## Comparator

Utilize `Comparator.comparing` para ordenações simples e legíveis.

```java
List<Usuario> usuariosOrdenados = usuarios.stream()
    .sorted(Comparator.comparing(Usuario::getNome))
    .toList();
```

Para múltiplos critérios:

```java
List<Usuario> usuariosOrdenados = usuarios.stream()
    .sorted(
        Comparator.comparing(Usuario::getNome)
            .thenComparing(Usuario::getEmail)
    )
    .toList();
```

---

## Tratamento de null

Evite retornar `null` em métodos de busca ou consulta.

Prefira:

```java
public Optional<Usuario> buscarUsuario(Long id) {
    return usuarioRepository.findById(id);
}
```

Quando o valor for obrigatório, lance uma exceção de negócio clara:

```java
public Usuario obterUsuarioObrigatorio(Long id) {
    return usuarioRepository.findById(id)
        .orElseThrow(() -> new BusinessException("Usuário não encontrado."));
}
```

---

## Validações e legibilidade

O código deve priorizar clareza. Recursos modernos devem ser usados apenas quando melhorarem a leitura e a manutenção.

```java
if (usuario == null) {
    throw new BusinessException("Usuário não encontrado.");
}
```

Evite soluções excessivamente complexas apenas para evitar `if`.

---

## Regras gerais de código limpo

- Código limpo é mais importante que código curto.
- Use programação funcional com moderação.
- Evite aninhamentos profundos.
- Extraia métodos privados com nomes de negócio.
- Evite duplicidade.
- Mantenha métodos pequenos e objetivos.
- Prefira nomes explícitos em português ou inglês, conforme o padrão do projeto.
- Evite abreviações.
- Evite métodos genéricos demais sem necessidade.
- Separe regra de negócio de infraestrutura.
- Não misture validação, persistência, transformação e notificação no mesmo método.

---

## O que evitar

- Não transformar todo `for` em `stream` automaticamente.
- Não usar `Optional` em campos de entidades JPA.
- Não usar `parallelStream()` sem necessidade real.
- Não criar lambdas grandes com várias regras de negócio.
- Não usar `var` quando o tipo não for evidente.
- Não criar métodos genéricos demais sem necessidade.
- Não misturar regra de negócio com código de infraestrutura.
- Não usar `try/catch` genérico sem tratamento adequado.
- Não retornar `null` quando existir uma alternativa mais segura.
- Não criar código excessivamente funcional que dificulte manutenção.
- Não usar `peek()` para executar regra de negócio.
- Não usar stream para alterar estado de objetos de forma oculta.
