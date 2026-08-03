# Padrões de Segurança Backend

## Objetivo

Padrões mínimos de segurança para o backend do Nexus Portal API.

## Autenticação

- JWT próprio (padrão atual). `JwtService` + `JwtAuthFilter` em
  `shared/config`.
- Login expõe `/api/v1/auth/login` (endpoint global, sem prefixo de
  módulo).
- Integração futura com OAuth2/OpenID Connect/Keycloak avaliada conforme
  necessidade.

## Autorização

Separação:

```text
shared/config            ← infraestrutura técnica (SecurityConfig, filtros,
                           JwtService, GatewayAuthFilter, CorrelationIdFilter)
shared/security          ← constantes de SpEL (Permissoes)
{modulo}/...             ← regra de negócio de autorização do próprio módulo
```

Importante: nenhum módulo fica sob um pacote `modules/`. Cada módulo é um
sub-pacote direto de `com.nexus.portal`.

### Modelo de autorização — granular por permissão

**A autorização padrão é por permissão granular** (`FUNCIONALIDADE:ACAO`),
não por role. Isso significa que **cada endpoint** carrega uma
`@PreAuthorize("hasAuthority('X:Y')")` no método, e o catálogo de
permissões em `tb_permissao` é a fonte da verdade.

As permissões disponíveis estão em
`shared/src/main/java/br/com/nexus/portal/shared/security/Permissoes.java`.
Use as constantes (não digite strings) — compile-time inlining mantém as
anotações com SpEL literal, sem custo extra.

#### Padrão obrigatório ao criar/alterar um Controller

1. **Não use `@PreAuthorize` no nível de classe.** Cada método anota a
   permissão que exige.
2. **Mapeie verbo HTTP → ação no catálogo:**
   - `GET` → `:LER`
   - `POST` → `:CRIAR` (ou `:EDITAR` se for ação sobre recurso existente)
   - `PUT` / `PATCH` → `:EDITAR`
   - `DELETE` → `:EXCLUIR`
3. **Sub-recursos (`/{id}/sub-coisa`) usam a permissão do agregado pai.**
   Ex.: `/api/v1/release-orchestrator/releases/{id}/itens` usa
   `RELEASE_LER` / `RELEASE_EDITAR`, não cria nova funcionalidade.
4. **Ações que escapam do CRUD ganham permissão especial** quando
   semanticamente diferentes — ex.: `USUARIO:RESETAR_SENHA`,
   `GRUPO_ACESSO:VINCULAR_PERMISSAO`. Adicione a permissão ao seed
   (próxima migration `V*__rbac__*`) e a constante em `Permissoes.java`.
5. **Adicionou uma funcionalidade nova ao módulo?** Crie a migration que
   semeia o `tb_funcionalidade` + CRUD em `tb_permissao` + vínculos aos
   grupos base (ADMIN/EDITOR/LEITOR/REVISOR), seguindo o padrão de V8/V9.
   Adicione as constantes correspondentes em `Permissoes.java`.

#### `SecurityRoles` foi removido

O modelo antigo baseado em roles CSV (coluna `tb_usuario.roles` +
`hasRole('ADMIN')`) foi eliminado em V10 — autorização é 100% via
permissão granular. Se precisar de um "modo admin puro" para uma
ação sem funcionalidade no catálogo, crie a permissão dedicada em
`tb_funcionalidade` + `tb_permissao` e vincule ao grupo ADMIN.

### Exemplo canônico

`UsuarioController` é o exemplo de referência. Estrutura completa:

```java
@RestController
@RequestMapping("/api/v1/docflow/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

  private final UsuarioService usuarioService;

  // Listagem: leitura
  @GetMapping
  @PreAuthorize(Permissoes.USUARIO_LER)
  public PageResponse<UsuarioResponse> listar(/* ... */) { /* ... */ }

  // Busca por id: leitura
  @GetMapping("/{id}")
  @PreAuthorize(Permissoes.USUARIO_LER)
  public UsuarioResponse buscar(@PathVariable UUID id) { /* ... */ }

  // Criação: escrita básica
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(Permissoes.USUARIO_CRIAR)
  public UsuarioResponse criar(@Valid @RequestBody CriarUsuarioRequest req) { /* ... */ }

  // Edição: escrita básica
  @PutMapping("/{id}")
  @PreAuthorize(Permissoes.USUARIO_EDITAR)
  public UsuarioResponse atualizar(@PathVariable UUID id,
      @Valid @RequestBody AtualizarUsuarioRequest req) { /* ... */ }

  // Ação especial: NÃO usa USUARIO_EDITAR — tem permissão própria porque
  // operacionalmente é uma ação separada (a permissão existe no seed V5
  // exatamente por isso).
  @PostMapping("/{id}/alterar-senha")
  @PreAuthorize(Permissoes.USUARIO_RESETAR)
  public void alterarSenha(@PathVariable UUID id,
      @Valid @RequestBody AlterarSenhaRequest req) { /* ... */ }
}
```

### Como o JWT carrega isso

- No login (`UsuarioService.autenticar`), o `JwtService.gerarToken` recebe
  `roles` **e** `permissoes` (estas vindas de `RbacService.permissoesDoUsuario`).
- O `JwtAuthFilter` (acesso direto) e o `GatewayAuthFilter` (via gateway)
  populam o `SecurityContext` com **dois tipos de `GrantedAuthority`**:
  - `ROLE_<ROLE>` — habilita `hasRole(...)`.
  - `<PERMISSAO>` (sem prefixo) — habilita `hasAuthority('FUNC:ACAO')`.
- O gateway (`HubDocFlowEdgeAuthFilter`) extrai as permissões do JWT e
  encaminha no header `X-Gateway-Permissoes`.

**Caveat:** o token é uma snapshot. Mudanças de permissão exigem
logout/login para refletir no `SecurityContext`. Se for caso pontual e
urgente, revogue a sessão.

## Auditoria automática

`AuditableEntity` + `JpaAuditingConfig` (em `shared`) registram o usuário
autenticado em `created_by`/`updated_by`. Quem é o usuário?

- Lido do `SecurityContext` (`Authentication#getName()`).
- Quando não há autenticação, registra `"system"`.

Logs e auditoria de negócio (`tb_auditoria_evento`) continuam por conta
do `AuditoriaService` quando se quer rastrear ações como CRIAR, ATUALIZAR,
PUBLICAR, etc.

## Regras

- Não logar senha, token ou segredo.
- Não retornar stacktrace em resposta de erro (`GlobalExceptionHandler`
  já cuida disso).
- Validar entrada com Bean Validation.
- Usar CORS restrito por ambiente (`docflow.cors.allowed-origins`).
- Proteger endpoints administrativos com `@PreAuthorize`.
- Versionar endpoints com `/api/v1`.
- Usar HTTPS em produção.
- Externalizar secrets via variável de ambiente
  (`docflow.security.jwt-secret`, `docflow.security.gateway-api-key`).

## Endpoints públicos

Liberados em `SecurityConfig`:

```text
/api/v1/auth/**
/api/v1/preview/**
/api/v1/public/publicacoes/download
/api/v1/docflow/paginas/*/anexos/*/download
/api/v1/docflow/clientes/*/logo
/api/v1/docflow/empresa/logo
/actuator/health
```

## Senhas

- BCrypt (`PasswordEncoder` em `SecurityConfig`).
- Nunca armazenar senha em texto puro.
- Nunca retornar senha em DTO.
- Nunca logar senha.

## Auditoria de ações sensíveis

Auditar via `AuditoriaService.registrar(...)`:

- login / logout
- criação de usuário
- alteração de permissões
- publicação de página / publicação de release
- start/stop de sistemas
- alteração de rota do gateway
