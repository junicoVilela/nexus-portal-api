-- PLAT-02: a página sabe se é um artigo (conteúdo) ou um menu (pasta que agrupa subpáginas).
-- Antes o tipo só existia como modelo de criação no front, e o pacote tratava a pasta como
-- artigo. Páginas existentes ficam como ARTIGO; o autor marca as pastas no editor.

ALTER TABLE public.tb_pagina
  ADD COLUMN tipo character varying(20) DEFAULT 'ARTIGO' NOT NULL;

ALTER TABLE public.tb_pagina
  ADD CONSTRAINT ck_tb_pagina_tipo CHECK (tipo IN ('ARTIGO', 'MENU'));

COMMENT ON COLUMN public.tb_pagina.tipo IS
  'ARTIGO = conteúdo da tela; MENU = pasta de navegação que agrupa subpáginas.';
