-- =============================================================================
-- MÓDULO: seguranca | ADD: política de senha (singleton) e histórico de senhas
-- Política única para todo o sistema (row com id fixo). Histórico serve pra
-- impedir reuso das N últimas senhas por usuário.
-- =============================================================================

CREATE TABLE tb_politica_senha (
  id                        UUID        NOT NULL,
  tamanho_minimo            INT         NOT NULL DEFAULT 8,
  exigir_maiuscula          BOOLEAN     NOT NULL DEFAULT TRUE,
  exigir_minuscula          BOOLEAN     NOT NULL DEFAULT TRUE,
  exigir_numero             BOOLEAN     NOT NULL DEFAULT TRUE,
  exigir_especial           BOOLEAN     NOT NULL DEFAULT FALSE,
  expira_senha_dias         INT,
  quantidade_historico      INT         NOT NULL DEFAULT 3,
  max_tentativas_invalidas  INT         NOT NULL DEFAULT 5,
  ativo                     BOOLEAN     NOT NULL DEFAULT TRUE,
  created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by                VARCHAR(120),
  updated_by                VARCHAR(120),
  CONSTRAINT pk_tb_politica_senha PRIMARY KEY (id)
);

INSERT INTO tb_politica_senha (id, created_by, updated_by)
VALUES ('00000000-0000-0000-0000-000000000901', 'seed', 'seed');

CREATE TABLE tb_historico_senha (
  id           UUID         NOT NULL,
  usuario_id   UUID         NOT NULL,
  senha_hash   VARCHAR(255) NOT NULL,
  created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT pk_tb_historico_senha PRIMARY KEY (id),
  CONSTRAINT fk_tb_historico_senha_usuario
    FOREIGN KEY (usuario_id) REFERENCES tb_usuario(id) ON DELETE CASCADE
);

CREATE INDEX idx_tb_historico_senha_usuario
  ON tb_historico_senha (usuario_id, created_at DESC);
