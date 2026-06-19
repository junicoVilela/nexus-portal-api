-- =============================================================================
-- MÓDULO: docflow | ETAPA 3: constraints e relacionamentos
-- =============================================================================

-- Unicidade
ALTER TABLE tb_cliente  ADD CONSTRAINT uq_tb_cliente_slug        UNIQUE (slug);
ALTER TABLE tb_projeto  ADD CONSTRAINT uq_tb_projeto_slug        UNIQUE (slug);
ALTER TABLE tb_modulo   ADD CONSTRAINT uq_tb_modulo_projeto_slug UNIQUE (projeto_id, slug);
ALTER TABLE tb_pagina   ADD CONSTRAINT uq_tb_pagina_slug         UNIQUE (slug);
ALTER TABLE tb_pagina   ADD CONSTRAINT uq_tb_pagina_codigo_tela  UNIQUE (codigo_tela);
ALTER TABLE tb_preview_token ADD CONSTRAINT uq_tb_preview_token_token UNIQUE (token);

ALTER TABLE tb_cliente_projeto ADD CONSTRAINT uq_tb_cliente_projeto UNIQUE (cliente_id, projeto_id);
ALTER TABLE tb_cliente_modulo  ADD CONSTRAINT uq_tb_cliente_modulo  UNIQUE (cliente_id, modulo_id);
ALTER TABLE tb_cliente_pagina  ADD CONSTRAINT uq_tb_cliente_pagina  UNIQUE (cliente_id, pagina_id);

-- Chaves estrangeiras
ALTER TABLE tb_modulo
  ADD CONSTRAINT fk_tb_modulo_projeto
  FOREIGN KEY (projeto_id) REFERENCES tb_projeto(id) ON DELETE CASCADE;

ALTER TABLE tb_pagina
  ADD CONSTRAINT fk_tb_pagina_modulo
  FOREIGN KEY (modulo_id) REFERENCES tb_modulo(id);

ALTER TABLE tb_pagina
  ADD CONSTRAINT fk_tb_pagina_parent
  FOREIGN KEY (parent_id) REFERENCES tb_pagina(id) ON DELETE CASCADE;

ALTER TABLE tb_pagina_revisao
  ADD CONSTRAINT fk_tb_pagina_revisao_pagina
  FOREIGN KEY (pagina_id) REFERENCES tb_pagina(id) ON DELETE CASCADE;

ALTER TABLE tb_pagina_revisao
  ADD CONSTRAINT fk_tb_pagina_revisao_modulo
  FOREIGN KEY (modulo_id) REFERENCES tb_modulo(id);

ALTER TABLE tb_pagina_revisao
  ADD CONSTRAINT fk_tb_pagina_revisao_parent
  FOREIGN KEY (parent_id) REFERENCES tb_pagina(id);

ALTER TABLE tb_pagina_anexo
  ADD CONSTRAINT fk_tb_pagina_anexo_pagina
  FOREIGN KEY (pagina_id) REFERENCES tb_pagina(id) ON DELETE CASCADE;

ALTER TABLE tb_publicacao
  ADD CONSTRAINT fk_tb_publicacao_cliente
  FOREIGN KEY (cliente_id) REFERENCES tb_cliente(id);

ALTER TABLE tb_cliente_projeto
  ADD CONSTRAINT fk_tb_cliente_projeto_cliente
  FOREIGN KEY (cliente_id) REFERENCES tb_cliente(id) ON DELETE CASCADE;

ALTER TABLE tb_cliente_projeto
  ADD CONSTRAINT fk_tb_cliente_projeto_projeto
  FOREIGN KEY (projeto_id) REFERENCES tb_projeto(id) ON DELETE CASCADE;

ALTER TABLE tb_cliente_modulo
  ADD CONSTRAINT fk_tb_cliente_modulo_cliente
  FOREIGN KEY (cliente_id) REFERENCES tb_cliente(id) ON DELETE CASCADE;

ALTER TABLE tb_cliente_modulo
  ADD CONSTRAINT fk_tb_cliente_modulo_modulo
  FOREIGN KEY (modulo_id) REFERENCES tb_modulo(id) ON DELETE CASCADE;

ALTER TABLE tb_cliente_pagina
  ADD CONSTRAINT fk_tb_cliente_pagina_cliente
  FOREIGN KEY (cliente_id) REFERENCES tb_cliente(id) ON DELETE CASCADE;

ALTER TABLE tb_cliente_pagina
  ADD CONSTRAINT fk_tb_cliente_pagina_pagina
  FOREIGN KEY (pagina_id) REFERENCES tb_pagina(id) ON DELETE CASCADE;

ALTER TABLE tb_preview_token
  ADD CONSTRAINT fk_tb_preview_token_cliente
  FOREIGN KEY (cliente_id) REFERENCES tb_cliente(id);

ALTER TABLE tb_publicacao_changelog
  ADD CONSTRAINT fk_tb_publicacao_changelog_publicacao
  FOREIGN KEY (publicacao_id) REFERENCES tb_publicacao(id) ON DELETE CASCADE;

-- Comentários de coluna
COMMENT ON COLUMN tb_cliente.tema_cor_primaria IS 'Cor accent do manual (manifest/PWA); ex.: #1a73e8';
COMMENT ON COLUMN tb_cliente.tema_cor_fundo    IS 'Cor de fundo base do tema; ex.: #f8f9fa';
COMMENT ON COLUMN tb_publicacao.relatorio_validacao IS 'JSON com resultado da validação do ZIP (arquivos obrigatórios, etc.)';
