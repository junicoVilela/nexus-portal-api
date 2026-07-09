-- =============================================================================
-- MÓDULO: seguranca | ADD: histórico de tentativas de login
-- Registra toda tentativa de autenticação (sucesso ou falha) — usado para
-- auditoria, detecção de brute force e bloqueio automático.
-- Diferente de tb_auditoria_evento (mutações genéricas): aqui só login.
-- =============================================================================

CREATE TABLE tb_historico_login (
  id              UUID         NOT NULL,
  usuario_id      UUID,
  login_informado VARCHAR(120) NOT NULL,
  ip_origem       VARCHAR(45),
  user_agent      VARCHAR(500),
  sucesso         BOOLEAN      NOT NULL,
  motivo_falha    VARCHAR(200),
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT pk_tb_historico_login PRIMARY KEY (id),
  CONSTRAINT fk_tb_historico_login_usuario
    FOREIGN KEY (usuario_id) REFERENCES tb_usuario(id) ON DELETE SET NULL
);

CREATE INDEX idx_tb_historico_login_usuario_data
  ON tb_historico_login (usuario_id, created_at DESC);
CREATE INDEX idx_tb_historico_login_login_data
  ON tb_historico_login (login_informado, created_at DESC);
CREATE INDEX idx_tb_historico_login_data
  ON tb_historico_login (created_at DESC);
