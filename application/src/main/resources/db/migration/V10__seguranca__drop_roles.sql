-- =============================================================================
-- MÓDULO: seguranca | CLEANUP: remove coluna legada tb_usuario.roles
-- Autorização agora é 100% via RBAC (Grupo → Permissao). A coluna CSV `roles`
-- não é lida por nenhum @PreAuthorize (todos usam hasAuthority) e o JWT
-- também deixa de emitir/aceitar o claim "roles".
-- =============================================================================

ALTER TABLE tb_usuario DROP COLUMN roles;
