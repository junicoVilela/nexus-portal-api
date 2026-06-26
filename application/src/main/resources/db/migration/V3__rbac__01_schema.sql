-- =============================================================================
-- MÓDULO: rbac | BASELINE: schema (tabelas + constraints + índices)
-- Catálogo: domínio → funcionalidade → permissão (código FUNC:ACAO)
-- Grupos de acesso e vínculos N:N (depende de tb_usuario de V2)
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Tabelas
-- -----------------------------------------------------------------------------

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
  CONSTRAINT pk_tb_dominio PRIMARY KEY (id),
  CONSTRAINT uq_tb_dominio_codigo UNIQUE (codigo)
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
  CONSTRAINT pk_tb_funcionalidade PRIMARY KEY (id),
  CONSTRAINT uq_tb_funcionalidade_dominio_codigo UNIQUE (dominio_id, codigo),
  CONSTRAINT fk_tb_funcionalidade_dominio FOREIGN KEY (dominio_id) REFERENCES tb_dominio(id)
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
  CONSTRAINT pk_tb_permissao PRIMARY KEY (id),
  CONSTRAINT uq_tb_permissao_codigo UNIQUE (codigo),
  CONSTRAINT uq_tb_permissao_funcionalidade_acao UNIQUE (funcionalidade_id, acao),
  CONSTRAINT fk_tb_permissao_funcionalidade FOREIGN KEY (funcionalidade_id) REFERENCES tb_funcionalidade(id)
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
  CONSTRAINT pk_tb_grupo PRIMARY KEY (id),
  CONSTRAINT uq_tb_grupo_codigo UNIQUE (codigo)
);

CREATE TABLE tb_grupo_permissao (
  grupo_id     UUID NOT NULL,
  permissao_id UUID NOT NULL,
  CONSTRAINT pk_tb_grupo_permissao PRIMARY KEY (grupo_id, permissao_id),
  CONSTRAINT fk_tb_grupo_permissao_grupo     FOREIGN KEY (grupo_id)     REFERENCES tb_grupo(id)     ON DELETE CASCADE,
  CONSTRAINT fk_tb_grupo_permissao_permissao FOREIGN KEY (permissao_id) REFERENCES tb_permissao(id) ON DELETE CASCADE
);

CREATE TABLE tb_grupo_usuario (
  grupo_id   UUID NOT NULL,
  usuario_id UUID NOT NULL,
  CONSTRAINT pk_tb_grupo_usuario PRIMARY KEY (grupo_id, usuario_id),
  CONSTRAINT fk_tb_grupo_usuario_grupo   FOREIGN KEY (grupo_id)   REFERENCES tb_grupo(id)   ON DELETE CASCADE,
  CONSTRAINT fk_tb_grupo_usuario_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuario(id) ON DELETE CASCADE
);

-- -----------------------------------------------------------------------------
-- Índices
-- -----------------------------------------------------------------------------

CREATE INDEX idx_tb_funcionalidade_dominio    ON tb_funcionalidade  (dominio_id);
CREATE INDEX idx_tb_funcionalidade_codigo     ON tb_funcionalidade  (codigo);
CREATE INDEX idx_tb_permissao_funcionalidade  ON tb_permissao       (funcionalidade_id);
CREATE INDEX idx_tb_grupo_nome                ON tb_grupo           (nome);
CREATE INDEX idx_tb_grupo_permissao_grupo     ON tb_grupo_permissao (grupo_id);
CREATE INDEX idx_tb_grupo_permissao_permissao ON tb_grupo_permissao (permissao_id);
CREATE INDEX idx_tb_grupo_usuario_grupo       ON tb_grupo_usuario   (grupo_id);
CREATE INDEX idx_tb_grupo_usuario_usuario     ON tb_grupo_usuario   (usuario_id);
