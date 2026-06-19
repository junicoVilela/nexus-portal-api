-- =============================================================================
-- MÓDULO: release_orchestrator | ETAPA 1–2: tabelas
-- Produtos, releases, itens, histórico e templates
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
