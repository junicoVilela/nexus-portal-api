ALTER TABLE tb_pagina_template
  ADD COLUMN versao_atual INTEGER NOT NULL DEFAULT 1;

CREATE TABLE tb_pagina_template_versao (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  template_id UUID NOT NULL,
  numero INTEGER NOT NULL,
  nome VARCHAR(120) NOT NULL,
  descricao VARCHAR(300),
  conteudo_html TEXT NOT NULL,
  ativo BOOLEAN NOT NULL,
  projeto_id UUID,
  cliente_id UUID,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by VARCHAR(150) NOT NULL DEFAULT 'system',
  updated_by VARCHAR(150) NOT NULL DEFAULT 'system',
  CONSTRAINT fk_pagina_template_versao_template
    FOREIGN KEY (template_id) REFERENCES tb_pagina_template (id) ON DELETE CASCADE,
  CONSTRAINT uq_pagina_template_versao_numero UNIQUE (template_id, numero),
  CONSTRAINT ck_pagina_template_versao_escopo
    CHECK ((projeto_id IS NULL) <> (cliente_id IS NULL) OR (projeto_id IS NULL AND cliente_id IS NULL))
);

INSERT INTO tb_pagina_template_versao (
  template_id, numero, nome, descricao, conteudo_html, ativo, projeto_id, cliente_id,
  created_at, updated_at, created_by, updated_by
)
SELECT id, 1, nome, descricao, conteudo_html, ativo, projeto_id, cliente_id,
       created_at, updated_at, created_by, updated_by
FROM tb_pagina_template;

ALTER TABLE tb_pagina
  ADD COLUMN template_origem_id UUID,
  ADD COLUMN template_origem_versao INTEGER;

ALTER TABLE tb_pagina
  ADD CONSTRAINT fk_pagina_template_origem
    FOREIGN KEY (template_origem_id) REFERENCES tb_pagina_template (id) ON DELETE SET NULL;

CREATE INDEX idx_pagina_template_versao_template
  ON tb_pagina_template_versao (template_id, numero DESC);

CREATE INDEX idx_pagina_template_ativo_escopo
  ON tb_pagina_template (ativo, personalizado, projeto_id, cliente_id);

CREATE INDEX idx_pagina_template_origem
  ON tb_pagina (template_origem_id, template_origem_versao)
  WHERE template_origem_id IS NOT NULL;
