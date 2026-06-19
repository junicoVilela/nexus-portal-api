-- =============================================================================
-- MÓDULO: docflow | ETAPA 4: índices
-- =============================================================================

CREATE INDEX idx_tb_modulo_projeto             ON tb_modulo              (projeto_id);
CREATE INDEX idx_tb_pagina_modulo_status       ON tb_pagina              (modulo_id, status);
CREATE INDEX idx_tb_pagina_parent_ordem        ON tb_pagina              (parent_id, ordem);
CREATE INDEX idx_tb_pagina_search_vector       ON tb_pagina              USING GIN (search_vector);
CREATE INDEX idx_tb_pagina_revisao_pagina      ON tb_pagina_revisao      (pagina_id, created_at DESC);
CREATE INDEX idx_tb_pagina_anexo_pagina        ON tb_pagina_anexo        (pagina_id, created_at DESC);
CREATE INDEX idx_tb_publicacao_cliente_created ON tb_publicacao          (cliente_id, created_at DESC);
CREATE INDEX idx_tb_auditoria_evento_entidade  ON tb_auditoria_evento    (entidade, entidade_id);
CREATE INDEX idx_tb_preview_token_cliente      ON tb_preview_token       (cliente_id);
CREATE INDEX idx_tb_publicacao_changelog_pub   ON tb_publicacao_changelog (publicacao_id, created_at DESC);
