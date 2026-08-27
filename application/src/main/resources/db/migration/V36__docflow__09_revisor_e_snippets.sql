-- Fluxo editorial com dono e trechos reutilizáveis.
--
-- 1. A revisão passa a ter responsável e prazo: hoje qualquer editor aprova
--    qualquer página e não há fila de "o que é meu".
-- 2. Snippets são trechos de HTML resolvidos na geração do pacote, para não
--    duplicar o mesmo aviso em dezenas de páginas.

ALTER TABLE public.tb_pagina
    ADD COLUMN revisor_username character varying(120),
    ADD COLUMN prazo_revisao timestamp with time zone;

CREATE INDEX idx_tb_pagina_revisor
    ON public.tb_pagina USING btree (revisor_username, status)
    WHERE (revisor_username IS NOT NULL);

COMMENT ON COLUMN public.tb_pagina.revisor_username IS
    'Responsável pela revisão editorial; quando preenchido, é quem aprova a página.';

CREATE TABLE public.tb_pagina_snippet (
    id uuid NOT NULL,
    codigo character varying(60) NOT NULL,
    titulo character varying(200) NOT NULL,
    descricao character varying(300),
    conteudo_html text NOT NULL,
    ativo boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120)
);

ALTER TABLE ONLY public.tb_pagina_snippet
    ADD CONSTRAINT pk_tb_pagina_snippet PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_pagina_snippet
    ADD CONSTRAINT uk_tb_pagina_snippet_codigo UNIQUE (codigo);

COMMENT ON TABLE public.tb_pagina_snippet IS
    'Trechos reutilizáveis referenciados no conteúdo como {{snippet:CODIGO}} e resolvidos na geração.';
