ALTER TABLE tb_pagina
  ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE tb_pagina_revisao
  ADD COLUMN tipo VARCHAR(40) NOT NULL DEFAULT 'SALVAMENTO_MANUAL',
  ADD COLUMN descricao VARCHAR(300);

UPDATE tb_pagina_revisao
SET descricao = 'Versão histórica anterior à classificação de eventos.'
WHERE descricao IS NULL;

CREATE INDEX idx_tb_pagina_revisao_pagina_tipo
  ON tb_pagina_revisao (pagina_id, tipo, numero DESC);
