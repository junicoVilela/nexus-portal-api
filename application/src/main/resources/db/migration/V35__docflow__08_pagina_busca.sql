-- A coluna search_vector e o índice GIN existiam desde a V7/V9, mas nada
-- populava a coluna e nenhum código consultava o índice: a busca de páginas
-- caía em LIKE '%termo%' sobre quatro colunas.
--
-- Passa a ser uma coluna gerada — sempre em dia, sem trigger. to_tsvector com
-- a configuração literal é IMMUTABLE, requisito do GENERATED STORED.

DROP INDEX IF EXISTS public.idx_tb_pagina_search_vector;

ALTER TABLE public.tb_pagina
    DROP COLUMN search_vector;

ALTER TABLE public.tb_pagina
    ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (
        setweight(to_tsvector('portuguese', coalesce(titulo, '')), 'A') ||
        setweight(to_tsvector('portuguese', coalesce(codigo_tela, '')), 'A') ||
        setweight(to_tsvector('portuguese', coalesce(slug, '')), 'B') ||
        setweight(to_tsvector('portuguese', coalesce(resumo, '')), 'C')
    ) STORED;

CREATE INDEX idx_tb_pagina_search_vector
    ON public.tb_pagina USING gin (search_vector);

COMMENT ON COLUMN public.tb_pagina.search_vector IS
    'Busca textual de título, código da tela, slug e resumo. Gerada pelo banco.';
