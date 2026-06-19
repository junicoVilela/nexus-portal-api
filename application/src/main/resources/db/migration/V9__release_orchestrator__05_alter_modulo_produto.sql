-- =============================================================================
-- MÓDULO: release_orchestrator | ETAPA 5: alteração incremental
-- Adiciona catálogo de módulos por produto (F0.3).
-- Spec: docs/release-orchestrator/10-produtos-modulos-artefatos.md
-- =============================================================================

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
