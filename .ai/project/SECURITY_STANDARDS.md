# Padrões de Segurança Backend

## Objetivo

Padrões mínimos de segurança para o backend do Softon Portal API.

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
{modulo}/...             ← regra de negócio de autorização do próprio módulo
```

Importante: nenhum módulo fica sob um pacote `modules/`. Cada módulo é um
sub-pacote direto de `br.com.softon.portal`.

Anotações `@PreAuthorize("hasRole('ADMIN')")` /
`@PreAuthorize("hasAnyRole('ADMIN','EDITOR')")` ficam no Controller (ou
no método) e cobrem o nível de role exigido pelo recurso.

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
