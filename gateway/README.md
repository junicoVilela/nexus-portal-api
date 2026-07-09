# softon-portal-gateway

Biblioteca de **filtros servlet** para o prefixo hub `/api/doc-flow/**`:

- valida JWT (mesmo segredo que `docflow.security.jwt-secret`);
- injeta `X-Gateway-Key`, `X-Gateway-User`, `X-Gateway-Permissoes` para o `GatewayAuthFilter` do Doc Flow.

**Não** há `public static void main` nem segunda porta. O arranque é só `SoftonPortalApplication` no módulo `application`.
