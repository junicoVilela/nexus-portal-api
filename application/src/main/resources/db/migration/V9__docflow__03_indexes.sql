-- DocFlow — indexes

CREATE INDEX idx_pagina_template_ativo_escopo ON public.tb_pagina_template USING btree (ativo, personalizado, projeto_id, cliente_id);

CREATE INDEX idx_pagina_template_cliente ON public.tb_pagina_template USING btree (cliente_id, ativo, nome) WHERE (cliente_id IS NOT NULL);

CREATE INDEX idx_pagina_template_origem ON public.tb_pagina USING btree (template_origem_id, template_origem_versao) WHERE (template_origem_id IS NOT NULL);

CREATE INDEX idx_pagina_template_projeto ON public.tb_pagina_template USING btree (projeto_id, ativo, nome) WHERE (projeto_id IS NOT NULL);

CREATE INDEX idx_pagina_template_versao_template ON public.tb_pagina_template_versao USING btree (template_id, numero DESC);

CREATE INDEX idx_tb_ajuda_conteudo_jornada ON public.tb_ajuda_conteudo USING btree (jornada_codigo, ordem);

CREATE INDEX idx_tb_ajuda_conteudo_rota ON public.tb_ajuda_conteudo USING btree (rota_contexto);

CREATE INDEX idx_tb_ajuda_conteudo_tipo_ordem ON public.tb_ajuda_conteudo USING btree (tipo, ordem);

CREATE INDEX idx_tb_ajuda_evento_conteudo ON public.tb_ajuda_evento USING btree (conteudo_codigo, created_at DESC);

CREATE INDEX idx_tb_ajuda_evento_created_at ON public.tb_ajuda_evento USING btree (created_at);

CREATE INDEX idx_tb_ajuda_evento_tipo_created ON public.tb_ajuda_evento USING btree (tipo, created_at DESC);

CREATE INDEX idx_tb_auditoria_evento_entidade ON public.tb_auditoria_evento USING btree (entidade, entidade_id);

CREATE INDEX idx_tb_modulo_projeto ON public.tb_modulo USING btree (projeto_id);

CREATE INDEX idx_tb_pagina_anexo_pagina ON public.tb_pagina_anexo USING btree (pagina_id, created_at DESC);

CREATE INDEX idx_tb_pagina_modulo_status ON public.tb_pagina USING btree (modulo_id, status);

CREATE INDEX idx_tb_pagina_parent_ordem ON public.tb_pagina USING btree (parent_id, ordem);

CREATE INDEX idx_tb_pagina_revisao_pagina ON public.tb_pagina_revisao USING btree (pagina_id, created_at DESC);

CREATE INDEX idx_tb_pagina_revisao_pagina_tipo ON public.tb_pagina_revisao USING btree (pagina_id, tipo, numero DESC);

CREATE INDEX idx_tb_pagina_search_vector ON public.tb_pagina USING gin (search_vector);

CREATE INDEX idx_tb_pagina_template_ativo_ordem ON public.tb_pagina_template USING btree (ativo, ordem);

CREATE INDEX idx_tb_preview_token_cliente ON public.tb_preview_token USING btree (cliente_id);

CREATE INDEX idx_tb_publicacao_changelog_pub ON public.tb_publicacao_changelog USING btree (publicacao_id, created_at DESC);

CREATE INDEX idx_tb_publicacao_cliente_created ON public.tb_publicacao USING btree (cliente_id, created_at DESC);
