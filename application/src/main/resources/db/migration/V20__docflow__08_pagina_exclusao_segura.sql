ALTER TABLE tb_pagina_revisao
  DROP CONSTRAINT fk_tb_pagina_revisao_modulo;

ALTER TABLE tb_pagina_revisao
  ALTER COLUMN modulo_id DROP NOT NULL;

ALTER TABLE tb_pagina_revisao
  ADD CONSTRAINT fk_tb_pagina_revisao_modulo
  FOREIGN KEY (modulo_id) REFERENCES tb_modulo(id) ON DELETE SET NULL;

ALTER TABLE tb_pagina_revisao
  DROP CONSTRAINT fk_tb_pagina_revisao_parent;

ALTER TABLE tb_pagina_revisao
  ADD CONSTRAINT fk_tb_pagina_revisao_parent
  FOREIGN KEY (parent_id) REFERENCES tb_pagina(id) ON DELETE SET NULL;
