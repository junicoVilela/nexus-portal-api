ALTER TABLE tb_pagina_template
  ADD COLUMN personalizado BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN projeto_id UUID,
  ADD COLUMN cliente_id UUID;

ALTER TABLE tb_pagina_template
  ADD CONSTRAINT fk_pagina_template_projeto
    FOREIGN KEY (projeto_id) REFERENCES tb_projeto (id) ON DELETE CASCADE,
  ADD CONSTRAINT fk_pagina_template_cliente
    FOREIGN KEY (cliente_id) REFERENCES tb_cliente (id) ON DELETE CASCADE,
  ADD CONSTRAINT ck_pagina_template_escopo
    CHECK (
      (personalizado = FALSE AND projeto_id IS NULL AND cliente_id IS NULL)
      OR
      (personalizado = TRUE AND ((projeto_id IS NULL) <> (cliente_id IS NULL)))
    );

CREATE INDEX idx_pagina_template_projeto
  ON tb_pagina_template (projeto_id, ativo, nome)
  WHERE projeto_id IS NOT NULL;

CREATE INDEX idx_pagina_template_cliente
  ON tb_pagina_template (cliente_id, ativo, nome)
  WHERE cliente_id IS NOT NULL;
