-- Onda F (INT-601): a captura sabe de qual tela é. Quando um PR ou uma release mexe na
-- tela, dá para listar as capturas possivelmente desatualizadas.
-- O código vem da página no upload e pode ser trocado (ex.: print de outra tela citado aqui).

ALTER TABLE public.tb_pagina_anexo
  ADD COLUMN codigo_tela character varying(120),
  ADD COLUMN seletor character varying(300);

UPDATE public.tb_pagina_anexo a
   SET codigo_tela = p.codigo_tela
  FROM public.tb_pagina p
 WHERE p.id = a.pagina_id;

CREATE INDEX ix_tb_pagina_anexo_codigo_tela ON public.tb_pagina_anexo (upper(codigo_tela));

COMMENT ON COLUMN public.tb_pagina_anexo.codigo_tela IS 'Tela que a captura mostra (padrão: a da página).';
COMMENT ON COLUMN public.tb_pagina_anexo.seletor IS 'Seletor CSS opcional da região capturada (para recapturar).';
