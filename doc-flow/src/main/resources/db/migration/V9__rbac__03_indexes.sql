-- =============================================================================
-- MÓDULO: rbac | ETAPA 4: índices
-- =============================================================================

CREATE INDEX idx_tb_funcionalidade_dominio    ON tb_funcionalidade  (dominio_id);
CREATE INDEX idx_tb_funcionalidade_codigo     ON tb_funcionalidade  (codigo);
CREATE INDEX idx_tb_permissao_funcionalidade  ON tb_permissao       (funcionalidade_id);
CREATE INDEX idx_tb_grupo_nome                ON tb_grupo           (nome);
CREATE INDEX idx_tb_grupo_permissao_grupo     ON tb_grupo_permissao (grupo_id);
CREATE INDEX idx_tb_grupo_permissao_permissao ON tb_grupo_permissao (permissao_id);
CREATE INDEX idx_tb_grupo_usuario_grupo       ON tb_grupo_usuario   (grupo_id);
CREATE INDEX idx_tb_grupo_usuario_usuario     ON tb_grupo_usuario   (usuario_id);
