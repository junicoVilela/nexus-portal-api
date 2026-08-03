-- DocFlow — constraints & comments

ALTER TABLE ONLY public.tb_ajuda_conteudo
    ADD CONSTRAINT pk_tb_ajuda_conteudo PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_ajuda_evento
    ADD CONSTRAINT pk_tb_ajuda_evento PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_auditoria_evento
    ADD CONSTRAINT pk_tb_auditoria_evento PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_cliente
    ADD CONSTRAINT pk_tb_cliente PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_cliente_modulo
    ADD CONSTRAINT pk_tb_cliente_modulo PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_cliente_pagina
    ADD CONSTRAINT pk_tb_cliente_pagina PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_cliente_projeto
    ADD CONSTRAINT pk_tb_cliente_projeto PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_modulo
    ADD CONSTRAINT pk_tb_modulo PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_pagina
    ADD CONSTRAINT pk_tb_pagina PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_pagina_anexo
    ADD CONSTRAINT pk_tb_pagina_anexo PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_pagina_revisao
    ADD CONSTRAINT pk_tb_pagina_revisao PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_pagina_template
    ADD CONSTRAINT pk_tb_pagina_template PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_preview_token
    ADD CONSTRAINT pk_tb_preview_token PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_projeto
    ADD CONSTRAINT pk_tb_projeto PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_publicacao
    ADD CONSTRAINT pk_tb_publicacao PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_publicacao_changelog
    ADD CONSTRAINT pk_tb_publicacao_changelog PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_pagina_template_versao
    ADD CONSTRAINT tb_pagina_template_versao_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_publicacao
    ADD CONSTRAINT uk_tb_publicacao_cliente_versao UNIQUE (cliente_id, versao);

ALTER TABLE ONLY public.tb_pagina_template_versao
    ADD CONSTRAINT uq_pagina_template_versao_numero UNIQUE (template_id, numero);

ALTER TABLE ONLY public.tb_ajuda_conteudo
    ADD CONSTRAINT uq_tb_ajuda_conteudo_codigo UNIQUE (codigo);

ALTER TABLE ONLY public.tb_cliente_modulo
    ADD CONSTRAINT uq_tb_cliente_modulo UNIQUE (cliente_id, modulo_id);

ALTER TABLE ONLY public.tb_cliente_pagina
    ADD CONSTRAINT uq_tb_cliente_pagina UNIQUE (cliente_id, pagina_id);

ALTER TABLE ONLY public.tb_cliente_projeto
    ADD CONSTRAINT uq_tb_cliente_projeto UNIQUE (cliente_id, projeto_id);

ALTER TABLE ONLY public.tb_cliente
    ADD CONSTRAINT uq_tb_cliente_slug UNIQUE (slug);

ALTER TABLE ONLY public.tb_modulo
    ADD CONSTRAINT uq_tb_modulo_projeto_slug UNIQUE (projeto_id, slug);

ALTER TABLE ONLY public.tb_pagina
    ADD CONSTRAINT uq_tb_pagina_codigo_tela UNIQUE (codigo_tela);

ALTER TABLE ONLY public.tb_pagina
    ADD CONSTRAINT uq_tb_pagina_slug UNIQUE (slug);

ALTER TABLE ONLY public.tb_pagina_template
    ADD CONSTRAINT uq_tb_pagina_template_codigo UNIQUE (codigo);

ALTER TABLE ONLY public.tb_preview_token
    ADD CONSTRAINT uq_tb_preview_token_token UNIQUE (token);

ALTER TABLE ONLY public.tb_projeto
    ADD CONSTRAINT uq_tb_projeto_slug UNIQUE (slug);

ALTER TABLE ONLY public.tb_pagina_template
    ADD CONSTRAINT fk_pagina_template_cliente FOREIGN KEY (cliente_id) REFERENCES public.tb_cliente(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tb_pagina
    ADD CONSTRAINT fk_pagina_template_origem FOREIGN KEY (template_origem_id) REFERENCES public.tb_pagina_template(id) ON DELETE SET NULL;

ALTER TABLE ONLY public.tb_pagina_template
    ADD CONSTRAINT fk_pagina_template_projeto FOREIGN KEY (projeto_id) REFERENCES public.tb_projeto(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tb_pagina_template_versao
    ADD CONSTRAINT fk_pagina_template_versao_template FOREIGN KEY (template_id) REFERENCES public.tb_pagina_template(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tb_ajuda_conteudo
    ADD CONSTRAINT fk_tb_ajuda_conteudo_jornada FOREIGN KEY (jornada_codigo) REFERENCES public.tb_ajuda_conteudo(codigo) ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE ONLY public.tb_cliente_modulo
    ADD CONSTRAINT fk_tb_cliente_modulo_cliente FOREIGN KEY (cliente_id) REFERENCES public.tb_cliente(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tb_cliente_modulo
    ADD CONSTRAINT fk_tb_cliente_modulo_modulo FOREIGN KEY (modulo_id) REFERENCES public.tb_modulo(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tb_cliente_pagina
    ADD CONSTRAINT fk_tb_cliente_pagina_cliente FOREIGN KEY (cliente_id) REFERENCES public.tb_cliente(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tb_cliente_pagina
    ADD CONSTRAINT fk_tb_cliente_pagina_pagina FOREIGN KEY (pagina_id) REFERENCES public.tb_pagina(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tb_cliente_projeto
    ADD CONSTRAINT fk_tb_cliente_projeto_cliente FOREIGN KEY (cliente_id) REFERENCES public.tb_cliente(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tb_cliente_projeto
    ADD CONSTRAINT fk_tb_cliente_projeto_projeto FOREIGN KEY (projeto_id) REFERENCES public.tb_projeto(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tb_modulo
    ADD CONSTRAINT fk_tb_modulo_projeto FOREIGN KEY (projeto_id) REFERENCES public.tb_projeto(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tb_pagina_anexo
    ADD CONSTRAINT fk_tb_pagina_anexo_pagina FOREIGN KEY (pagina_id) REFERENCES public.tb_pagina(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tb_pagina
    ADD CONSTRAINT fk_tb_pagina_modulo FOREIGN KEY (modulo_id) REFERENCES public.tb_modulo(id);

ALTER TABLE ONLY public.tb_pagina
    ADD CONSTRAINT fk_tb_pagina_parent FOREIGN KEY (parent_id) REFERENCES public.tb_pagina(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tb_pagina_revisao
    ADD CONSTRAINT fk_tb_pagina_revisao_modulo FOREIGN KEY (modulo_id) REFERENCES public.tb_modulo(id) ON DELETE SET NULL;

ALTER TABLE ONLY public.tb_pagina_revisao
    ADD CONSTRAINT fk_tb_pagina_revisao_pagina FOREIGN KEY (pagina_id) REFERENCES public.tb_pagina(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tb_pagina_revisao
    ADD CONSTRAINT fk_tb_pagina_revisao_parent FOREIGN KEY (parent_id) REFERENCES public.tb_pagina(id) ON DELETE SET NULL;

ALTER TABLE ONLY public.tb_preview_token
    ADD CONSTRAINT fk_tb_preview_token_cliente FOREIGN KEY (cliente_id) REFERENCES public.tb_cliente(id);

ALTER TABLE ONLY public.tb_publicacao_changelog
    ADD CONSTRAINT fk_tb_publicacao_changelog_publicacao FOREIGN KEY (publicacao_id) REFERENCES public.tb_publicacao(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tb_publicacao
    ADD CONSTRAINT fk_tb_publicacao_cliente FOREIGN KEY (cliente_id) REFERENCES public.tb_cliente(id);

COMMENT ON COLUMN public.tb_cliente.tema_cor_primaria IS 'Cor accent do manual (manifest/PWA); padrão portal #4f46e5';

COMMENT ON COLUMN public.tb_cliente.tema_cor_fundo IS 'Cor de fundo base do tema; ex.: #f8f9fa';

COMMENT ON COLUMN public.tb_publicacao.relatorio_validacao IS 'JSON com resultado da validação do ZIP (arquivos obrigatórios, etc.)';
