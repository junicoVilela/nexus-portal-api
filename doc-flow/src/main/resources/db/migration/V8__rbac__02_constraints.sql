-- =============================================================================
-- MÓDULO: rbac | ETAPA 3: constraints e relacionamentos
-- =============================================================================

ALTER TABLE tb_dominio        ADD CONSTRAINT uq_tb_dominio_codigo UNIQUE (codigo);
ALTER TABLE tb_funcionalidade ADD CONSTRAINT uq_tb_funcionalidade_dominio_codigo UNIQUE (dominio_id, codigo);
ALTER TABLE tb_permissao      ADD CONSTRAINT uq_tb_permissao_codigo UNIQUE (codigo);
ALTER TABLE tb_permissao      ADD CONSTRAINT uq_tb_permissao_funcionalidade_acao UNIQUE (funcionalidade_id, acao);
ALTER TABLE tb_grupo          ADD CONSTRAINT uq_tb_grupo_codigo UNIQUE (codigo);

ALTER TABLE tb_funcionalidade
  ADD CONSTRAINT fk_tb_funcionalidade_dominio
  FOREIGN KEY (dominio_id) REFERENCES tb_dominio(id);

ALTER TABLE tb_permissao
  ADD CONSTRAINT fk_tb_permissao_funcionalidade
  FOREIGN KEY (funcionalidade_id) REFERENCES tb_funcionalidade(id);

ALTER TABLE tb_grupo_permissao
  ADD CONSTRAINT fk_tb_grupo_permissao_grupo
  FOREIGN KEY (grupo_id) REFERENCES tb_grupo(id) ON DELETE CASCADE;

ALTER TABLE tb_grupo_permissao
  ADD CONSTRAINT fk_tb_grupo_permissao_permissao
  FOREIGN KEY (permissao_id) REFERENCES tb_permissao(id) ON DELETE CASCADE;

ALTER TABLE tb_grupo_usuario
  ADD CONSTRAINT fk_tb_grupo_usuario_grupo
  FOREIGN KEY (grupo_id) REFERENCES tb_grupo(id) ON DELETE CASCADE;

ALTER TABLE tb_grupo_usuario
  ADD CONSTRAINT fk_tb_grupo_usuario_usuario
  FOREIGN KEY (usuario_id) REFERENCES tb_usuario(id) ON DELETE CASCADE;
