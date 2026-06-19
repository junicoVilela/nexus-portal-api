-- =============================================================================
-- MÓDULO: release_orchestrator | BASELINE: schema (tabelas + constraints + índices)
-- Produtos, releases, itens, histórico, templates e módulos por produto (F0.3)
-- =============================================================================


CREATE TABLE tb_produto_rh (
  id             UUID         NOT NULL DEFAULT gen_random_uuid(),
  nome           VARCHAR(200) NOT NULL,
  sigla          VARCHAR(20)  NOT NULL,
  descricao      VARCHAR(500),
  cor            VARCHAR(20)  NOT NULL DEFAULT '#2563eb',
  responsavel_id UUID,
  ativo          BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by     VARCHAR(120),
  updated_by     VARCHAR(120),
  CONSTRAINT pk_tb_produto_rh PRIMARY KEY (id)
);

CREATE TABLE tb_release (
  id              UUID         NOT NULL DEFAULT gen_random_uuid(),
  produto_id      UUID         NOT NULL,
  versao          VARCHAR(50)  NOT NULL,
  titulo          VARCHAR(200) NOT NULL,
  tipo            VARCHAR(30)  NOT NULL,
  status          VARCHAR(30)  NOT NULL DEFAULT 'RASCUNHO',
  data_prevista   DATE,
  data_publicacao DATE,
  publicado_por   VARCHAR(120),
  responsavel_id  UUID,
  resumo          TEXT,
  observacoes     TEXT,
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by      VARCHAR(120),
  updated_by      VARCHAR(120),
  CONSTRAINT pk_tb_release PRIMARY KEY (id)
);

CREATE TABLE tb_release_item (
  id             UUID         NOT NULL DEFAULT gen_random_uuid(),
  release_id     UUID         NOT NULL,
  categoria      VARCHAR(40)  NOT NULL,
  titulo         VARCHAR(300) NOT NULL,
  descricao      TEXT,
  visibilidade   VARCHAR(20)  NOT NULL DEFAULT 'TODOS',
  ordem          INTEGER      NOT NULL DEFAULT 0,
  ticket         VARCHAR(100),
  commit_hash    VARCHAR(100),
  pull_request   VARCHAR(100),
  responsavel_id UUID,
  created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by     VARCHAR(120),
  updated_by     VARCHAR(120),
  CONSTRAINT pk_tb_release_item PRIMARY KEY (id)
);

CREATE TABLE tb_release_historico (
  id              UUID         NOT NULL DEFAULT gen_random_uuid(),
  release_id      UUID         NOT NULL,
  acao            VARCHAR(40)  NOT NULL,
  descricao       VARCHAR(500),
  status_anterior VARCHAR(30),
  status_novo     VARCHAR(30),
  usuario         VARCHAR(120),
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT pk_tb_release_historico PRIMARY KEY (id)
);

CREATE TABLE tb_release_template (
  id           UUID         NOT NULL DEFAULT gen_random_uuid(),
  nome         VARCHAR(200) NOT NULL,
  descricao    VARCHAR(500),
  tipo_release VARCHAR(30),
  produto_id   UUID,
  estrutura    TEXT,
  ativo        BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by   VARCHAR(120),
  updated_by   VARCHAR(120),
  CONSTRAINT pk_tb_release_template PRIMARY KEY (id)
);

-- -- constraints -- --

ALTER TABLE tb_produto_rh ADD CONSTRAINT uq_tb_produto_rh_sigla UNIQUE (sigla);
ALTER TABLE tb_release      ADD CONSTRAINT uq_tb_release_produto_versao UNIQUE (produto_id, versao);

ALTER TABLE tb_release
  ADD CONSTRAINT fk_tb_release_produto
  FOREIGN KEY (produto_id) REFERENCES tb_produto_rh(id);

ALTER TABLE tb_release_item
  ADD CONSTRAINT fk_tb_release_item_release
  FOREIGN KEY (release_id) REFERENCES tb_release(id) ON DELETE CASCADE;

ALTER TABLE tb_release_historico
  ADD CONSTRAINT fk_tb_release_historico_release
  FOREIGN KEY (release_id) REFERENCES tb_release(id) ON DELETE CASCADE;

-- -- índices -- --

CREATE INDEX idx_tb_release_produto           ON tb_release           (produto_id);
CREATE INDEX idx_tb_release_status            ON tb_release           (status);
CREATE INDEX idx_tb_release_updated           ON tb_release           (updated_at DESC);
CREATE INDEX idx_tb_release_item_release      ON tb_release_item      (release_id, ordem);
CREATE INDEX idx_tb_release_historico_release ON tb_release_historico (release_id, created_at DESC);

-- -- catálogo de módulos por produto (F0.3) -- --

CREATE TABLE tb_modulo_produto (
  id                 UUID         NOT NULL DEFAULT gen_random_uuid(),
  produto_id         UUID         NOT NULL,
  codigo             VARCHAR(80)  NOT NULL,
  nome               VARCHAR(200) NOT NULL,
  tipo               VARCHAR(30)  NOT NULL,
  gera_delta         BOOLEAN      NOT NULL,
  obrigatorio        BOOLEAN      NOT NULL,
  ordem              INT          NOT NULL DEFAULT 0,
  ativo              BOOLEAN      NOT NULL DEFAULT TRUE,
  config_especifica  TEXT,
  created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by         VARCHAR(120),
  updated_by         VARCHAR(120),
  CONSTRAINT pk_tb_modulo_produto PRIMARY KEY (id)
);

ALTER TABLE tb_modulo_produto
  ADD CONSTRAINT uq_tb_modulo_produto_codigo UNIQUE (produto_id, codigo);

ALTER TABLE tb_modulo_produto
  ADD CONSTRAINT fk_tb_modulo_produto_produto
  FOREIGN KEY (produto_id) REFERENCES tb_produto_rh(id);

ALTER TABLE tb_modulo_produto
  ADD CONSTRAINT ck_tb_modulo_produto_tipo
  CHECK (tipo IN ('WEB', 'BATCH', 'BANCO', 'KETTLE', 'FUNCIONALIDADES', 'REGRAS'));

CREATE INDEX idx_tb_modulo_produto_produto       ON tb_modulo_produto (produto_id);
CREATE INDEX idx_tb_modulo_produto_produto_ordem ON tb_modulo_produto (produto_id, ordem);
CREATE INDEX idx_tb_modulo_produto_ativo         ON tb_modulo_produto (ativo);
