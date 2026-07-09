-- =============================================================================
-- MÓDULO: seguranca | ADD: acesso temporário
-- Vínculo com janela de tempo entre usuário e grupo/permissão/escopo.
-- Status é derivado das datas + revogado_em; não precisa scheduler.
-- =============================================================================

CREATE TABLE tb_acesso_temporario (
  id                 UUID         NOT NULL,
  usuario_id         UUID         NOT NULL,
  grupo_id           UUID,
  permissao_id       UUID,
  escopo_id          UUID,
  inicio_em          TIMESTAMPTZ  NOT NULL,
  fim_em             TIMESTAMPTZ  NOT NULL,
  justificativa      VARCHAR(500),
  revogado_em        TIMESTAMPTZ,
  motivo_revogacao   VARCHAR(200),
  created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by         VARCHAR(120),
  CONSTRAINT pk_tb_acesso_temporario PRIMARY KEY (id),
  CONSTRAINT fk_tb_acesso_temporario_usuario
    FOREIGN KEY (usuario_id) REFERENCES tb_usuario(id) ON DELETE CASCADE,
  CONSTRAINT fk_tb_acesso_temporario_grupo
    FOREIGN KEY (grupo_id) REFERENCES tb_grupo(id) ON DELETE CASCADE,
  CONSTRAINT ck_tb_acesso_temporario_alvo
    CHECK (grupo_id IS NOT NULL OR permissao_id IS NOT NULL OR escopo_id IS NOT NULL),
  CONSTRAINT ck_tb_acesso_temporario_janela
    CHECK (fim_em > inicio_em)
);

CREATE INDEX idx_tb_acesso_temporario_usuario_janela
  ON tb_acesso_temporario (usuario_id, inicio_em, fim_em);
CREATE INDEX idx_tb_acesso_temporario_grupo
  ON tb_acesso_temporario (grupo_id);
