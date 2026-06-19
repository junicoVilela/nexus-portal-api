-- =============================================================================
-- MÓDULO: seguranca | ETAPA 1–2: tabelas
-- Identidade: usuários de login (JWT / roles legado)
-- =============================================================================

CREATE TABLE tb_usuario (
  id         UUID         NOT NULL,
  username   VARCHAR(80)  NOT NULL,
  password   VARCHAR(255) NOT NULL,
  nome       VARCHAR(150),
  email      VARCHAR(200),
  roles      VARCHAR(200) NOT NULL DEFAULT 'EDITOR',
  ativo      BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by VARCHAR(120),
  updated_by VARCHAR(120),
  CONSTRAINT pk_tb_usuario PRIMARY KEY (id)
);
