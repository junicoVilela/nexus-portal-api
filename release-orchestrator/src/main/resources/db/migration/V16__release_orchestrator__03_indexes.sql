-- =============================================================================
-- MÓDULO: release_orchestrator | ETAPA 4: índices
-- =============================================================================

CREATE INDEX idx_tb_release_produto           ON tb_release           (produto_id);
CREATE INDEX idx_tb_release_status            ON tb_release           (status);
CREATE INDEX idx_tb_release_updated           ON tb_release           (updated_at DESC);
CREATE INDEX idx_tb_release_item_release      ON tb_release_item      (release_id, ordem);
CREATE INDEX idx_tb_release_historico_release ON tb_release_historico (release_id, created_at DESC);
