-- =============================================================================
-- MÓDULO: release_orchestrator | BASELINE: schema (tabelas + constraints + índices)
-- Produtos, releases, itens, histórico, templates, módulos por produto (F0.3),
-- vínculo release ↔ versão por módulo (F0.4), artefatos uploadados (F0.5) e
-- cliente operacional (F1.2).
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

-- -- vínculo release ↔ versão por módulo (F0.4) -- --

CREATE TABLE tb_release_modulo_versao (
  id                 UUID         NOT NULL DEFAULT gen_random_uuid(),
  release_id         UUID         NOT NULL,
  modulo_produto_id  UUID         NOT NULL,
  versao             VARCHAR(80)  NOT NULL,
  created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by         VARCHAR(120),
  updated_by         VARCHAR(120),
  CONSTRAINT pk_tb_release_modulo_versao PRIMARY KEY (id)
);

ALTER TABLE tb_release_modulo_versao
  ADD CONSTRAINT uq_tb_release_modulo_versao_release_modulo UNIQUE (release_id, modulo_produto_id);

ALTER TABLE tb_release_modulo_versao
  ADD CONSTRAINT fk_tb_release_modulo_versao_release
  FOREIGN KEY (release_id) REFERENCES tb_release(id) ON DELETE CASCADE;

ALTER TABLE tb_release_modulo_versao
  ADD CONSTRAINT fk_tb_release_modulo_versao_modulo
  FOREIGN KEY (modulo_produto_id) REFERENCES tb_modulo_produto(id);

CREATE INDEX idx_tb_release_modulo_versao_release ON tb_release_modulo_versao (release_id);
CREATE INDEX idx_tb_release_modulo_versao_modulo  ON tb_release_modulo_versao (modulo_produto_id);

-- -- artefatos uploadados manualmente (F0.5) -- --

CREATE TABLE tb_artefato_release_modulo (
  id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
  release_id          UUID         NOT NULL,
  modulo_produto_id   UUID         NOT NULL,
  nome_arquivo        VARCHAR(255) NOT NULL,
  caminho_armazenado  VARCHAR(700) NOT NULL,
  sha256              VARCHAR(64)  NOT NULL,
  tamanho_bytes       BIGINT       NOT NULL,
  observacao          TEXT,
  created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by          VARCHAR(120),
  updated_by          VARCHAR(120),
  CONSTRAINT pk_tb_artefato_release_modulo PRIMARY KEY (id)
);

ALTER TABLE tb_artefato_release_modulo
  ADD CONSTRAINT fk_tb_artefato_release_modulo_release
  FOREIGN KEY (release_id) REFERENCES tb_release(id) ON DELETE CASCADE;

ALTER TABLE tb_artefato_release_modulo
  ADD CONSTRAINT fk_tb_artefato_release_modulo_modulo
  FOREIGN KEY (modulo_produto_id) REFERENCES tb_modulo_produto(id);

CREATE INDEX idx_tb_artefato_release_modulo_release_modulo
  ON tb_artefato_release_modulo (release_id, modulo_produto_id);

CREATE INDEX idx_tb_artefato_release_modulo_sha256
  ON tb_artefato_release_modulo (sha256);

-- -- cliente operacional (F1.2) -- --

CREATE TABLE tb_cliente_orchestrator (
  id                        UUID         NOT NULL DEFAULT gen_random_uuid(),
  nome                      VARCHAR(200) NOT NULL,
  razao_social              VARCHAR(300),
  cnpj                      VARCHAR(18),
  sigla                     VARCHAR(20)  NOT NULL,
  ativo                     BOOLEAN      NOT NULL DEFAULT TRUE,
  responsavel_comercial_id  UUID,
  ambiente_padrao           VARCHAR(20)  NOT NULL,
  tipo_banco                VARCHAR(20),
  codificacao               VARCHAR(30),
  fuso_horario              VARCHAR(60),
  observacoes               TEXT,
  created_at                TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at                TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by                VARCHAR(120),
  updated_by                VARCHAR(120),
  CONSTRAINT pk_tb_cliente_orchestrator PRIMARY KEY (id)
);

ALTER TABLE tb_cliente_orchestrator
  ADD CONSTRAINT uq_tb_cliente_orchestrator_sigla UNIQUE (sigla);

ALTER TABLE tb_cliente_orchestrator
  ADD CONSTRAINT uq_tb_cliente_orchestrator_cnpj UNIQUE (cnpj);

ALTER TABLE tb_cliente_orchestrator
  ADD CONSTRAINT ck_tb_cliente_orchestrator_ambiente
  CHECK (ambiente_padrao IN ('PROD', 'HOM', 'DEV', 'TEST'));

ALTER TABLE tb_cliente_orchestrator
  ADD CONSTRAINT ck_tb_cliente_orchestrator_banco
  CHECK (tipo_banco IS NULL OR tipo_banco IN ('ORACLE', 'SQLSERVER', 'POSTGRES'));

CREATE INDEX idx_tb_cliente_orchestrator_nome   ON tb_cliente_orchestrator (lower(nome));
CREATE INDEX idx_tb_cliente_orchestrator_ativo  ON tb_cliente_orchestrator (ativo);

-- -- contatos do cliente operacional (F1.2b) -- --

CREATE TABLE tb_contato_orchestrator (
  id          UUID         NOT NULL DEFAULT gen_random_uuid(),
  cliente_id  UUID         NOT NULL,
  nome        VARCHAR(200) NOT NULL,
  papel       VARCHAR(20)  NOT NULL,
  email       VARCHAR(200) NOT NULL,
  telefone    VARCHAR(40),
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by  VARCHAR(120),
  updated_by  VARCHAR(120),
  CONSTRAINT pk_tb_contato_orchestrator PRIMARY KEY (id)
);

ALTER TABLE tb_contato_orchestrator
  ADD CONSTRAINT fk_tb_contato_orchestrator_cliente
  FOREIGN KEY (cliente_id) REFERENCES tb_cliente_orchestrator(id) ON DELETE CASCADE;

ALTER TABLE tb_contato_orchestrator
  ADD CONSTRAINT ck_tb_contato_orchestrator_papel
  CHECK (papel IN ('TECNICO', 'COMERCIAL', 'OPERACIONAL', 'FINANCEIRO', 'OUTRO'));

CREATE INDEX idx_tb_contato_orchestrator_cliente ON tb_contato_orchestrator (cliente_id);
CREATE INDEX idx_tb_contato_orchestrator_email   ON tb_contato_orchestrator (lower(email));

-- -- config de entrega 1:1 com cliente operacional (F1.2b) -- --

CREATE TABLE tb_config_entrega_orchestrator (
  id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
  cliente_id          UUID         NOT NULL,
  tipo_destino        VARCHAR(20)  NOT NULL,
  caminho_base        VARCHAR(500),
  exigir_aprovacao    BOOLEAN      NOT NULL DEFAULT FALSE,
  emails_notificacao  VARCHAR(1000),
  created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by          VARCHAR(120),
  updated_by          VARCHAR(120),
  CONSTRAINT pk_tb_config_entrega_orchestrator PRIMARY KEY (id)
);

ALTER TABLE tb_config_entrega_orchestrator
  ADD CONSTRAINT uq_tb_config_entrega_orchestrator_cliente UNIQUE (cliente_id);

ALTER TABLE tb_config_entrega_orchestrator
  ADD CONSTRAINT fk_tb_config_entrega_orchestrator_cliente
  FOREIGN KEY (cliente_id) REFERENCES tb_cliente_orchestrator(id) ON DELETE CASCADE;

ALTER TABLE tb_config_entrega_orchestrator
  ADD CONSTRAINT ck_tb_config_entrega_orchestrator_tipo
  CHECK (tipo_destino IN ('PASTA', 'FTP', 'SFTP', 'BUCKET'));

-- -- catálogo funcional por produto: domínios + funcionalidades (F1.3) -- --

CREATE TABLE tb_dominio_produto (
  id             UUID         NOT NULL DEFAULT gen_random_uuid(),
  produto_id     UUID         NOT NULL,
  nome           VARCHAR(150) NOT NULL,
  codigo         VARCHAR(80)  NOT NULL,
  codigo_legado  VARCHAR(80),
  descricao      VARCHAR(1000),
  ordem          INT          NOT NULL DEFAULT 0,
  ativo          BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by     VARCHAR(120),
  updated_by     VARCHAR(120),
  CONSTRAINT pk_tb_dominio_produto PRIMARY KEY (id)
);

ALTER TABLE tb_dominio_produto
  ADD CONSTRAINT uq_tb_dominio_produto_codigo UNIQUE (produto_id, codigo);

ALTER TABLE tb_dominio_produto
  ADD CONSTRAINT fk_tb_dominio_produto_produto
  FOREIGN KEY (produto_id) REFERENCES tb_produto_rh(id) ON DELETE CASCADE;

CREATE INDEX idx_tb_dominio_produto_produto       ON tb_dominio_produto (produto_id);
CREATE INDEX idx_tb_dominio_produto_produto_ordem ON tb_dominio_produto (produto_id, ordem);
CREATE INDEX idx_tb_dominio_produto_ativo         ON tb_dominio_produto (ativo);

CREATE TABLE tb_funcionalidade_produto (
  id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
  dominio_produto_id  UUID         NOT NULL,
  nome                VARCHAR(200) NOT NULL,
  codigo              VARCHAR(80)  NOT NULL,
  codigo_legado       VARCHAR(80),
  codigo_operacao     VARCHAR(80),
  descricao           VARCHAR(1000),
  critica             BOOLEAN      NOT NULL DEFAULT FALSE,
  ordem               INT          NOT NULL DEFAULT 0,
  ativo               BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by          VARCHAR(120),
  updated_by          VARCHAR(120),
  CONSTRAINT pk_tb_funcionalidade_produto PRIMARY KEY (id)
);

ALTER TABLE tb_funcionalidade_produto
  ADD CONSTRAINT uq_tb_funcionalidade_produto_codigo UNIQUE (dominio_produto_id, codigo);

ALTER TABLE tb_funcionalidade_produto
  ADD CONSTRAINT fk_tb_funcionalidade_produto_dominio
  FOREIGN KEY (dominio_produto_id) REFERENCES tb_dominio_produto(id) ON DELETE CASCADE;

CREATE INDEX idx_tb_funcionalidade_produto_dominio        ON tb_funcionalidade_produto (dominio_produto_id);
CREATE INDEX idx_tb_funcionalidade_produto_dominio_ordem  ON tb_funcionalidade_produto (dominio_produto_id, ordem);
CREATE INDEX idx_tb_funcionalidade_produto_ativo          ON tb_funcionalidade_produto (ativo);

-- -- matriz cliente × funcionalidade do produto (F1.4) -- --

CREATE TABLE tb_cliente_funcionalidade_produto (
  id                         UUID         NOT NULL DEFAULT gen_random_uuid(),
  cliente_id                 UUID         NOT NULL,
  funcionalidade_produto_id  UUID         NOT NULL,
  habilitada                 BOOLEAN      NOT NULL,
  origem                     VARCHAR(20)  NOT NULL,
  created_at                 TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at                 TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by                 VARCHAR(120),
  updated_by                 VARCHAR(120),
  CONSTRAINT pk_tb_cliente_funcionalidade_produto PRIMARY KEY (id)
);

ALTER TABLE tb_cliente_funcionalidade_produto
  ADD CONSTRAINT uq_tb_cliente_funcionalidade_produto_par
  UNIQUE (cliente_id, funcionalidade_produto_id);

ALTER TABLE tb_cliente_funcionalidade_produto
  ADD CONSTRAINT fk_tb_cliente_funcionalidade_produto_cliente
  FOREIGN KEY (cliente_id) REFERENCES tb_cliente_orchestrator(id) ON DELETE CASCADE;

ALTER TABLE tb_cliente_funcionalidade_produto
  ADD CONSTRAINT fk_tb_cliente_funcionalidade_produto_funcionalidade
  FOREIGN KEY (funcionalidade_produto_id) REFERENCES tb_funcionalidade_produto(id) ON DELETE CASCADE;

ALTER TABLE tb_cliente_funcionalidade_produto
  ADD CONSTRAINT ck_tb_cliente_funcionalidade_produto_origem
  CHECK (origem IN ('MANUAL', 'TEMPLATE', 'HERDADA'));

CREATE INDEX idx_tb_cliente_funcionalidade_produto_cliente
  ON tb_cliente_funcionalidade_produto (cliente_id);

CREATE INDEX idx_tb_cliente_funcionalidade_produto_funcionalidade
  ON tb_cliente_funcionalidade_produto (funcionalidade_produto_id);

CREATE INDEX idx_tb_cliente_funcionalidade_produto_habilitada
  ON tb_cliente_funcionalidade_produto (cliente_id, habilitada);

-- -- contratos cliente × produto + módulos contratados (F1.5) -- --

CREATE TABLE tb_cliente_produto (
  id          UUID         NOT NULL DEFAULT gen_random_uuid(),
  cliente_id  UUID         NOT NULL,
  produto_id  UUID         NOT NULL,
  ambiente    VARCHAR(20)  NOT NULL,
  ativo       BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by  VARCHAR(120),
  updated_by  VARCHAR(120),
  CONSTRAINT pk_tb_cliente_produto PRIMARY KEY (id)
);

ALTER TABLE tb_cliente_produto
  ADD CONSTRAINT uq_tb_cliente_produto_par UNIQUE (cliente_id, produto_id);

ALTER TABLE tb_cliente_produto
  ADD CONSTRAINT fk_tb_cliente_produto_cliente
  FOREIGN KEY (cliente_id) REFERENCES tb_cliente_orchestrator(id) ON DELETE CASCADE;

ALTER TABLE tb_cliente_produto
  ADD CONSTRAINT fk_tb_cliente_produto_produto
  FOREIGN KEY (produto_id) REFERENCES tb_produto_rh(id);

ALTER TABLE tb_cliente_produto
  ADD CONSTRAINT ck_tb_cliente_produto_ambiente
  CHECK (ambiente IN ('PROD', 'HOM', 'DEV', 'TEST'));

CREATE INDEX idx_tb_cliente_produto_cliente ON tb_cliente_produto (cliente_id);
CREATE INDEX idx_tb_cliente_produto_produto ON tb_cliente_produto (produto_id);

CREATE TABLE tb_cliente_produto_modulo (
  id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
  cliente_produto_id  UUID         NOT NULL,
  modulo_produto_id   UUID         NOT NULL,
  versao_atual        VARCHAR(80),
  ativo               BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by          VARCHAR(120),
  updated_by          VARCHAR(120),
  CONSTRAINT pk_tb_cliente_produto_modulo PRIMARY KEY (id)
);

ALTER TABLE tb_cliente_produto_modulo
  ADD CONSTRAINT uq_tb_cliente_produto_modulo_par
  UNIQUE (cliente_produto_id, modulo_produto_id);

ALTER TABLE tb_cliente_produto_modulo
  ADD CONSTRAINT fk_tb_cliente_produto_modulo_cliente_produto
  FOREIGN KEY (cliente_produto_id) REFERENCES tb_cliente_produto(id) ON DELETE CASCADE;

ALTER TABLE tb_cliente_produto_modulo
  ADD CONSTRAINT fk_tb_cliente_produto_modulo_modulo
  FOREIGN KEY (modulo_produto_id) REFERENCES tb_modulo_produto(id);

CREATE INDEX idx_tb_cliente_produto_modulo_cliente_produto
  ON tb_cliente_produto_modulo (cliente_produto_id);

CREATE INDEX idx_tb_cliente_produto_modulo_modulo
  ON tb_cliente_produto_modulo (modulo_produto_id);

-- -- agenda de próximas entregas (F1.7) -- --

CREATE TABLE tb_proxima_entrega (
  id                    UUID         NOT NULL DEFAULT gen_random_uuid(),
  cliente_id            UUID         NOT NULL,
  produto_id            UUID         NOT NULL,
  release_id            UUID,
  data_prevista         DATE         NOT NULL,
  ambiente              VARCHAR(20)  NOT NULL,
  prioridade            VARCHAR(20)  NOT NULL,
  status                VARCHAR(20)  NOT NULL,
  responsavel_id        UUID,
  observacoes           TEXT,
  entrega_convertida_id UUID,
  created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by            VARCHAR(120),
  updated_by            VARCHAR(120),
  CONSTRAINT pk_tb_proxima_entrega PRIMARY KEY (id)
);

ALTER TABLE tb_proxima_entrega
  ADD CONSTRAINT fk_tb_proxima_entrega_cliente
  FOREIGN KEY (cliente_id) REFERENCES tb_cliente_orchestrator(id);

ALTER TABLE tb_proxima_entrega
  ADD CONSTRAINT fk_tb_proxima_entrega_produto
  FOREIGN KEY (produto_id) REFERENCES tb_produto_rh(id);

ALTER TABLE tb_proxima_entrega
  ADD CONSTRAINT fk_tb_proxima_entrega_release
  FOREIGN KEY (release_id) REFERENCES tb_release(id);

ALTER TABLE tb_proxima_entrega
  ADD CONSTRAINT ck_tb_proxima_entrega_ambiente
  CHECK (ambiente IN ('PROD', 'HOM', 'DEV', 'TEST'));

ALTER TABLE tb_proxima_entrega
  ADD CONSTRAINT ck_tb_proxima_entrega_prioridade
  CHECK (prioridade IN ('BAIXA', 'MEDIA', 'ALTA', 'CRITICA'));

ALTER TABLE tb_proxima_entrega
  ADD CONSTRAINT ck_tb_proxima_entrega_status
  CHECK (status IN ('PLANEJADA', 'AGENDADA', 'REPLANEJADA', 'ATRASADA', 'CONVERTIDA', 'CANCELADA'));

CREATE INDEX idx_tb_proxima_entrega_cliente       ON tb_proxima_entrega (cliente_id);
CREATE INDEX idx_tb_proxima_entrega_produto       ON tb_proxima_entrega (produto_id);
CREATE INDEX idx_tb_proxima_entrega_release       ON tb_proxima_entrega (release_id);
CREATE INDEX idx_tb_proxima_entrega_data_prevista ON tb_proxima_entrega (data_prevista);
CREATE INDEX idx_tb_proxima_entrega_status        ON tb_proxima_entrega (status);

-- -- entrega executada (F1.8) -- --

CREATE TABLE tb_entrega (
  id                       UUID         NOT NULL DEFAULT gen_random_uuid(),
  cliente_id               UUID         NOT NULL,
  produto_id               UUID         NOT NULL,
  release_id               UUID         NOT NULL,
  proxima_entrega_id       UUID,
  entrega_original_id      UUID,
  ambiente                 VARCHAR(20)  NOT NULL,
  status                   VARCHAR(20)  NOT NULL,
  data_inicio_geracao      TIMESTAMPTZ,
  data_conclusao           TIMESTAMPTZ,
  responsavel_id           UUID,
  arquivo_pacote_caminho   VARCHAR(700),
  arquivo_pacote_sha256    VARCHAR(64),
  tamanho_bytes            BIGINT,
  observacoes              TEXT,
  falha_motivo             TEXT,
  created_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by               VARCHAR(120),
  updated_by               VARCHAR(120),
  CONSTRAINT pk_tb_entrega PRIMARY KEY (id)
);

ALTER TABLE tb_entrega
  ADD CONSTRAINT fk_tb_entrega_cliente
  FOREIGN KEY (cliente_id) REFERENCES tb_cliente_orchestrator(id);

ALTER TABLE tb_entrega
  ADD CONSTRAINT fk_tb_entrega_produto
  FOREIGN KEY (produto_id) REFERENCES tb_produto_rh(id);

ALTER TABLE tb_entrega
  ADD CONSTRAINT fk_tb_entrega_release
  FOREIGN KEY (release_id) REFERENCES tb_release(id);

ALTER TABLE tb_entrega
  ADD CONSTRAINT fk_tb_entrega_proxima_entrega
  FOREIGN KEY (proxima_entrega_id) REFERENCES tb_proxima_entrega(id);

ALTER TABLE tb_entrega
  ADD CONSTRAINT fk_tb_entrega_original
  FOREIGN KEY (entrega_original_id) REFERENCES tb_entrega(id);

ALTER TABLE tb_entrega
  ADD CONSTRAINT ck_tb_entrega_ambiente
  CHECK (ambiente IN ('PROD', 'HOM', 'DEV', 'TEST'));

ALTER TABLE tb_entrega
  ADD CONSTRAINT ck_tb_entrega_status
  CHECK (status IN ('RASCUNHO', 'EM_GERACAO', 'CONCLUIDA', 'FALHA', 'CANCELADA'));

CREATE INDEX idx_tb_entrega_cliente            ON tb_entrega (cliente_id);
CREATE INDEX idx_tb_entrega_produto            ON tb_entrega (produto_id);
CREATE INDEX idx_tb_entrega_release            ON tb_entrega (release_id);
CREATE INDEX idx_tb_entrega_proxima_entrega    ON tb_entrega (proxima_entrega_id);
CREATE INDEX idx_tb_entrega_original           ON tb_entrega (entrega_original_id);
CREATE INDEX idx_tb_entrega_status             ON tb_entrega (status);
CREATE INDEX idx_tb_entrega_created            ON tb_entrega (created_at DESC);

-- -- seleção de módulos por entrega (F1.9) -- --

CREATE TABLE tb_entrega_modulo (
  id                 UUID         NOT NULL DEFAULT gen_random_uuid(),
  entrega_id         UUID         NOT NULL,
  modulo_produto_id  UUID         NOT NULL,
  versao_from        VARCHAR(80),
  versao_to          VARCHAR(80),
  selecionado        BOOLEAN      NOT NULL,
  fora_contrato      BOOLEAN      NOT NULL DEFAULT FALSE,
  ordem              INT          NOT NULL DEFAULT 0,
  created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by         VARCHAR(120),
  updated_by         VARCHAR(120),
  CONSTRAINT pk_tb_entrega_modulo PRIMARY KEY (id)
);

ALTER TABLE tb_entrega_modulo
  ADD CONSTRAINT uq_tb_entrega_modulo_par UNIQUE (entrega_id, modulo_produto_id);

ALTER TABLE tb_entrega_modulo
  ADD CONSTRAINT fk_tb_entrega_modulo_entrega
  FOREIGN KEY (entrega_id) REFERENCES tb_entrega(id) ON DELETE CASCADE;

ALTER TABLE tb_entrega_modulo
  ADD CONSTRAINT fk_tb_entrega_modulo_modulo
  FOREIGN KEY (modulo_produto_id) REFERENCES tb_modulo_produto(id);

CREATE INDEX idx_tb_entrega_modulo_entrega ON tb_entrega_modulo (entrega_id, ordem);
CREATE INDEX idx_tb_entrega_modulo_modulo  ON tb_entrega_modulo (modulo_produto_id);
