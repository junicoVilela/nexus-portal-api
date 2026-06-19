-- =============================================================================
-- MÓDULO: release_orchestrator | ETAPA 3: constraints
-- =============================================================================

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
