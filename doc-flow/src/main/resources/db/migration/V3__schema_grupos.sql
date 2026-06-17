-- =============================================================================
-- GRUPOS E PERMISSÕES
-- controle de acesso baseado em grupos
-- =============================================================================

CREATE TABLE tb_grupo (
  id         UUID         PRIMARY KEY,
  nome       VARCHAR(150) NOT NULL,
  descricao  VARCHAR(500),
  ativo      BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ  NOT NULL,
  updated_at TIMESTAMPTZ  NOT NULL,
  created_by VARCHAR(120),
  updated_by VARCHAR(120)
);

-- -----------------------------------------------------------------------------

CREATE TABLE tb_grupo_permissao (
  grupo_id     UUID         NOT NULL REFERENCES tb_grupo(id) ON DELETE CASCADE,
  permissao_id VARCHAR(100) NOT NULL,
  PRIMARY KEY  (grupo_id, permissao_id)
);

-- -----------------------------------------------------------------------------

CREATE TABLE tb_grupo_usuario (
  grupo_id   UUID NOT NULL REFERENCES tb_grupo(id) ON DELETE CASCADE,
  usuario_id UUID NOT NULL,
  PRIMARY KEY (grupo_id, usuario_id)
);

-- -----------------------------------------------------------------------------
-- Índices
-- -----------------------------------------------------------------------------

CREATE INDEX idx_tb_grupo_nome           ON tb_grupo            (nome);
CREATE INDEX idx_tb_grupo_permissao_grupo ON tb_grupo_permissao (grupo_id);
CREATE INDEX idx_tb_grupo_usuario_grupo   ON tb_grupo_usuario   (grupo_id);
CREATE INDEX idx_tb_grupo_usuario_usuario ON tb_grupo_usuario   (usuario_id);
