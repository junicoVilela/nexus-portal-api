-- =============================================================================
-- MÓDULO: seguranca | ADD: escopos de acesso (só metadados; queries dos
-- módulos de negócio ainda não filtram por escopo — backlog).
--
-- Um escopo restringe o alvo (usuario OU grupo) a uma combinação de
-- cliente/ambiente/produto/tipo. Somente-leitura marca o vínculo como
-- read-only (novamente: enforcement fica pros módulos consumirem).
-- =============================================================================

CREATE TABLE tb_escopo_acesso (
  id              UUID         NOT NULL,
  usuario_id      UUID,
  grupo_id        UUID,
  cliente_id      UUID,
  ambiente_id     UUID,
  produto_id      UUID,
  tipo_ambiente   VARCHAR(20),
  somente_leitura BOOLEAN      NOT NULL DEFAULT FALSE,
  ativo           BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  created_by      VARCHAR(120),
  updated_by      VARCHAR(120),
  CONSTRAINT pk_tb_escopo_acesso PRIMARY KEY (id),
  CONSTRAINT ck_tb_escopo_acesso_alvo
    CHECK (usuario_id IS NOT NULL OR grupo_id IS NOT NULL),
  CONSTRAINT fk_tb_escopo_acesso_usuario
    FOREIGN KEY (usuario_id) REFERENCES tb_usuario(id) ON DELETE CASCADE,
  CONSTRAINT fk_tb_escopo_acesso_grupo
    FOREIGN KEY (grupo_id) REFERENCES tb_grupo(id) ON DELETE CASCADE
);

CREATE INDEX idx_tb_escopo_acesso_usuario ON tb_escopo_acesso (usuario_id);
CREATE INDEX idx_tb_escopo_acesso_grupo   ON tb_escopo_acesso (grupo_id);
