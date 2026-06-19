-- =============================================================================
-- MÓDULO: rbac | ETAPA 1–2: tabelas
-- Catálogo: domínio → funcionalidade → permissão (código FUNC:ACAO)
-- Grupos de acesso e vínculos N:N
-- =============================================================================

CREATE TABLE tb_dominio (
  id          UUID         NOT NULL,
  codigo      VARCHAR(80)  NOT NULL,
  nome        VARCHAR(150) NOT NULL,
  descricao   VARCHAR(500),
  ativo       BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by  VARCHAR(120),
  updated_by  VARCHAR(120),
  CONSTRAINT pk_tb_dominio PRIMARY KEY (id)
);

CREATE TABLE tb_funcionalidade (
  id           UUID         NOT NULL,
  dominio_id   UUID         NOT NULL,
  codigo       VARCHAR(80)  NOT NULL,
  nome         VARCHAR(150) NOT NULL,
  descricao    VARCHAR(500),
  ativo        BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by   VARCHAR(120),
  updated_by   VARCHAR(120),
  CONSTRAINT pk_tb_funcionalidade PRIMARY KEY (id)
);

CREATE TABLE tb_permissao (
  id                  UUID         NOT NULL,
  funcionalidade_id   UUID         NOT NULL,
  acao                VARCHAR(40)  NOT NULL,
  codigo              VARCHAR(120) NOT NULL,
  descricao           VARCHAR(500),
  ativo               BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by          VARCHAR(120),
  updated_by          VARCHAR(120),
  CONSTRAINT pk_tb_permissao PRIMARY KEY (id)
);

CREATE TABLE tb_grupo (
  id          UUID         NOT NULL,
  codigo      VARCHAR(80)  NOT NULL,
  nome        VARCHAR(150) NOT NULL,
  descricao   VARCHAR(500),
  ativo       BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by  VARCHAR(120),
  updated_by  VARCHAR(120),
  CONSTRAINT pk_tb_grupo PRIMARY KEY (id)
);

CREATE TABLE tb_grupo_permissao (
  grupo_id     UUID NOT NULL,
  permissao_id UUID NOT NULL,
  CONSTRAINT pk_tb_grupo_permissao PRIMARY KEY (grupo_id, permissao_id)
);

CREATE TABLE tb_grupo_usuario (
  grupo_id   UUID NOT NULL,
  usuario_id UUID NOT NULL,
  CONSTRAINT pk_tb_grupo_usuario PRIMARY KEY (grupo_id, usuario_id)
);
