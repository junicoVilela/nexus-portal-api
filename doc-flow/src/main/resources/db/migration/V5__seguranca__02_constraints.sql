-- =============================================================================
-- MÓDULO: seguranca | ETAPA 3: constraints
-- =============================================================================

ALTER TABLE tb_usuario ADD CONSTRAINT uq_tb_usuario_username UNIQUE (username);
