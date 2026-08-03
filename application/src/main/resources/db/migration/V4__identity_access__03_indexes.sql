-- Identity Access — indexes

CREATE INDEX idx_tb_acesso_temporario_grupo ON public.tb_acesso_temporario USING btree (grupo_id);

CREATE INDEX idx_tb_acesso_temporario_usuario_janela ON public.tb_acesso_temporario USING btree (usuario_id, inicio_em, fim_em);

CREATE INDEX idx_tb_escopo_acesso_grupo ON public.tb_escopo_acesso USING btree (grupo_id);

CREATE INDEX idx_tb_escopo_acesso_usuario ON public.tb_escopo_acesso USING btree (usuario_id);

CREATE INDEX idx_tb_funcionalidade_codigo ON public.tb_funcionalidade USING btree (codigo);

CREATE INDEX idx_tb_funcionalidade_dominio ON public.tb_funcionalidade USING btree (dominio_id);

CREATE INDEX idx_tb_grupo_nome ON public.tb_grupo USING btree (nome);

CREATE INDEX idx_tb_grupo_permissao_grupo ON public.tb_grupo_permissao USING btree (grupo_id);

CREATE INDEX idx_tb_grupo_permissao_permissao ON public.tb_grupo_permissao USING btree (permissao_id);

CREATE INDEX idx_tb_grupo_usuario_grupo ON public.tb_grupo_usuario USING btree (grupo_id);

CREATE INDEX idx_tb_grupo_usuario_usuario ON public.tb_grupo_usuario USING btree (usuario_id);

CREATE INDEX idx_tb_historico_login_data ON public.tb_historico_login USING btree (created_at DESC);

CREATE INDEX idx_tb_historico_login_login_data ON public.tb_historico_login USING btree (login_informado, created_at DESC);

CREATE INDEX idx_tb_historico_login_usuario_data ON public.tb_historico_login USING btree (usuario_id, created_at DESC);

CREATE INDEX idx_tb_historico_senha_usuario ON public.tb_historico_senha USING btree (usuario_id, created_at DESC);

CREATE INDEX idx_tb_permissao_funcionalidade ON public.tb_permissao USING btree (funcionalidade_id);

CREATE INDEX idx_tb_sessao_iniciada ON public.tb_sessao USING btree (iniciada_em DESC);

CREATE INDEX idx_tb_sessao_usuario_ativa ON public.tb_sessao USING btree (usuario_id, ativa);
