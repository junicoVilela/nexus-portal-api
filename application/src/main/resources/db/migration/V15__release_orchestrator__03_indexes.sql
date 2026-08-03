-- Release Orchestrator — indexes

CREATE INDEX idx_tb_artefato_release_modulo_release_modulo ON public.tb_artefato_release_modulo USING btree (release_id, modulo_produto_id);

CREATE INDEX idx_tb_artefato_release_modulo_sha256 ON public.tb_artefato_release_modulo USING btree (sha256);

CREATE INDEX idx_tb_cliente_funcionalidade_produto_cliente ON public.tb_cliente_funcionalidade_produto USING btree (cliente_id);

CREATE INDEX idx_tb_cliente_funcionalidade_produto_funcionalidade ON public.tb_cliente_funcionalidade_produto USING btree (funcionalidade_produto_id);

CREATE INDEX idx_tb_cliente_funcionalidade_produto_habilitada ON public.tb_cliente_funcionalidade_produto USING btree (cliente_id, habilitada);

CREATE INDEX idx_tb_cliente_orchestrator_ativo ON public.tb_cliente_orchestrator USING btree (ativo);

CREATE INDEX idx_tb_cliente_orchestrator_nome ON public.tb_cliente_orchestrator USING btree (lower((nome)::text));

CREATE INDEX idx_tb_cliente_produto_cliente ON public.tb_cliente_produto USING btree (cliente_id);

CREATE INDEX idx_tb_cliente_produto_modulo_cliente_produto ON public.tb_cliente_produto_modulo USING btree (cliente_produto_id);

CREATE INDEX idx_tb_cliente_produto_modulo_modulo ON public.tb_cliente_produto_modulo USING btree (modulo_produto_id);

CREATE INDEX idx_tb_cliente_produto_produto ON public.tb_cliente_produto USING btree (produto_id);

CREATE INDEX idx_tb_contato_orchestrator_cliente ON public.tb_contato_orchestrator USING btree (cliente_id);

CREATE INDEX idx_tb_contato_orchestrator_email ON public.tb_contato_orchestrator USING btree (lower((email)::text));

CREATE INDEX idx_tb_dominio_produto_ativo ON public.tb_dominio_produto USING btree (ativo);

CREATE INDEX idx_tb_dominio_produto_produto ON public.tb_dominio_produto USING btree (produto_id);

CREATE INDEX idx_tb_dominio_produto_produto_ordem ON public.tb_dominio_produto USING btree (produto_id, ordem);

CREATE INDEX idx_tb_entrega_cliente ON public.tb_entrega USING btree (cliente_id);

CREATE INDEX idx_tb_entrega_created ON public.tb_entrega USING btree (created_at DESC);

CREATE INDEX idx_tb_entrega_modulo_artefato_artefato ON public.tb_entrega_modulo_artefato USING btree (artefato_release_modulo_id);

CREATE INDEX idx_tb_entrega_modulo_artefato_em ON public.tb_entrega_modulo_artefato USING btree (entrega_modulo_id, ordem);

CREATE INDEX idx_tb_entrega_modulo_entrega ON public.tb_entrega_modulo USING btree (entrega_id, ordem);

CREATE INDEX idx_tb_entrega_modulo_modulo ON public.tb_entrega_modulo USING btree (modulo_produto_id);

CREATE INDEX idx_tb_entrega_original ON public.tb_entrega USING btree (entrega_original_id);

CREATE INDEX idx_tb_entrega_produto ON public.tb_entrega USING btree (produto_id);

CREATE INDEX idx_tb_entrega_proxima_entrega ON public.tb_entrega USING btree (proxima_entrega_id);

CREATE INDEX idx_tb_entrega_release ON public.tb_entrega USING btree (release_id);

CREATE INDEX idx_tb_entrega_status ON public.tb_entrega USING btree (status);

CREATE INDEX idx_tb_funcionalidade_produto_ativo ON public.tb_funcionalidade_produto USING btree (ativo);

CREATE INDEX idx_tb_funcionalidade_produto_dominio ON public.tb_funcionalidade_produto USING btree (dominio_produto_id);

CREATE INDEX idx_tb_funcionalidade_produto_dominio_ordem ON public.tb_funcionalidade_produto USING btree (dominio_produto_id, ordem);

CREATE INDEX idx_tb_modulo_produto_ativo ON public.tb_modulo_produto USING btree (ativo);

CREATE INDEX idx_tb_modulo_produto_produto ON public.tb_modulo_produto USING btree (produto_id);

CREATE INDEX idx_tb_modulo_produto_produto_ordem ON public.tb_modulo_produto USING btree (produto_id, ordem);

CREATE INDEX idx_tb_proxima_entrega_cliente ON public.tb_proxima_entrega USING btree (cliente_id);

CREATE INDEX idx_tb_proxima_entrega_data_prevista ON public.tb_proxima_entrega USING btree (data_prevista);

CREATE INDEX idx_tb_proxima_entrega_produto ON public.tb_proxima_entrega USING btree (produto_id);

CREATE INDEX idx_tb_proxima_entrega_release ON public.tb_proxima_entrega USING btree (release_id);

CREATE INDEX idx_tb_proxima_entrega_status ON public.tb_proxima_entrega USING btree (status);

CREATE INDEX idx_tb_release_historico_release ON public.tb_release_historico USING btree (release_id, created_at DESC);

CREATE INDEX idx_tb_release_item_release ON public.tb_release_item USING btree (release_id, ordem);

CREATE INDEX idx_tb_release_modulo_versao_modulo ON public.tb_release_modulo_versao USING btree (modulo_produto_id);

CREATE INDEX idx_tb_release_modulo_versao_release ON public.tb_release_modulo_versao USING btree (release_id);

CREATE INDEX idx_tb_release_produto ON public.tb_release USING btree (produto_id);

CREATE INDEX idx_tb_release_status ON public.tb_release USING btree (status);

CREATE INDEX idx_tb_release_updated ON public.tb_release USING btree (updated_at DESC);

CREATE INDEX ix_entrega_publicacao_pendente ON public.tb_entrega USING btree (proxima_tentativa_em) WHERE ((status_publicacao)::text = 'PENDENTE'::text);
