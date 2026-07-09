-- =============================================================================
-- MÓDULO: seguranca | ADD: sessões (JWT rastreado por jti)
-- Cada login gera um jti persistido em tb_sessao. Revogar uma sessão apaga
-- o registro (ou marca como encerrada) — JwtAuthFilter passa a validar que
-- o jti do token está ativo na tabela.
-- =============================================================================

CREATE TABLE tb_sessao (
  id                    UUID         NOT NULL,
  jti                   VARCHAR(120) NOT NULL,
  usuario_id            UUID         NOT NULL,
  ip_origem             VARCHAR(45),
  user_agent            VARCHAR(500),
  ativa                 BOOLEAN      NOT NULL DEFAULT TRUE,
  revogada              BOOLEAN      NOT NULL DEFAULT FALSE,
  motivo_encerramento   VARCHAR(200),
  iniciada_em           TIMESTAMPTZ  NOT NULL DEFAULT now(),
  encerrada_em          TIMESTAMPTZ,
  expira_em             TIMESTAMPTZ,
  CONSTRAINT pk_tb_sessao PRIMARY KEY (id),
  CONSTRAINT uq_tb_sessao_jti UNIQUE (jti),
  CONSTRAINT fk_tb_sessao_usuario
    FOREIGN KEY (usuario_id) REFERENCES tb_usuario(id) ON DELETE CASCADE
);

CREATE INDEX idx_tb_sessao_usuario_ativa ON tb_sessao (usuario_id, ativa);
CREATE INDEX idx_tb_sessao_iniciada     ON tb_sessao (iniciada_em DESC);
