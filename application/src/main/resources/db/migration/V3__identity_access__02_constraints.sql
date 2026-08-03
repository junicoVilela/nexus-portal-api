-- Identity Access — constraints & comments

ALTER TABLE ONLY public.tb_acesso_temporario
    ADD CONSTRAINT pk_tb_acesso_temporario PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_dominio
    ADD CONSTRAINT pk_tb_dominio PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_escopo_acesso
    ADD CONSTRAINT pk_tb_escopo_acesso PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_funcionalidade
    ADD CONSTRAINT pk_tb_funcionalidade PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_grupo
    ADD CONSTRAINT pk_tb_grupo PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_grupo_permissao
    ADD CONSTRAINT pk_tb_grupo_permissao PRIMARY KEY (grupo_id, permissao_id);

ALTER TABLE ONLY public.tb_grupo_usuario
    ADD CONSTRAINT pk_tb_grupo_usuario PRIMARY KEY (grupo_id, usuario_id);

ALTER TABLE ONLY public.tb_historico_login
    ADD CONSTRAINT pk_tb_historico_login PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_historico_senha
    ADD CONSTRAINT pk_tb_historico_senha PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_permissao
    ADD CONSTRAINT pk_tb_permissao PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_politica_senha
    ADD CONSTRAINT pk_tb_politica_senha PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_sessao
    ADD CONSTRAINT pk_tb_sessao PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_usuario
    ADD CONSTRAINT pk_tb_usuario PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_dominio
    ADD CONSTRAINT uq_tb_dominio_codigo UNIQUE (codigo);

ALTER TABLE ONLY public.tb_funcionalidade
    ADD CONSTRAINT uq_tb_funcionalidade_dominio_codigo UNIQUE (dominio_id, codigo);

ALTER TABLE ONLY public.tb_grupo
    ADD CONSTRAINT uq_tb_grupo_codigo UNIQUE (codigo);

ALTER TABLE ONLY public.tb_permissao
    ADD CONSTRAINT uq_tb_permissao_codigo UNIQUE (codigo);

ALTER TABLE ONLY public.tb_permissao
    ADD CONSTRAINT uq_tb_permissao_funcionalidade_acao UNIQUE (funcionalidade_id, acao);

ALTER TABLE ONLY public.tb_sessao
    ADD CONSTRAINT uq_tb_sessao_jti UNIQUE (jti);

ALTER TABLE ONLY public.tb_usuario
    ADD CONSTRAINT uq_tb_usuario_username UNIQUE (username);

ALTER TABLE ONLY public.tb_acesso_temporario
    ADD CONSTRAINT fk_tb_acesso_temporario_grupo FOREIGN KEY (grupo_id) REFERENCES public.tb_grupo(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tb_acesso_temporario
    ADD CONSTRAINT fk_tb_acesso_temporario_usuario FOREIGN KEY (usuario_id) REFERENCES public.tb_usuario(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tb_escopo_acesso
    ADD CONSTRAINT fk_tb_escopo_acesso_grupo FOREIGN KEY (grupo_id) REFERENCES public.tb_grupo(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tb_escopo_acesso
    ADD CONSTRAINT fk_tb_escopo_acesso_usuario FOREIGN KEY (usuario_id) REFERENCES public.tb_usuario(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tb_funcionalidade
    ADD CONSTRAINT fk_tb_funcionalidade_dominio FOREIGN KEY (dominio_id) REFERENCES public.tb_dominio(id);

ALTER TABLE ONLY public.tb_grupo_permissao
    ADD CONSTRAINT fk_tb_grupo_permissao_grupo FOREIGN KEY (grupo_id) REFERENCES public.tb_grupo(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tb_grupo_permissao
    ADD CONSTRAINT fk_tb_grupo_permissao_permissao FOREIGN KEY (permissao_id) REFERENCES public.tb_permissao(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tb_grupo_usuario
    ADD CONSTRAINT fk_tb_grupo_usuario_grupo FOREIGN KEY (grupo_id) REFERENCES public.tb_grupo(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tb_grupo_usuario
    ADD CONSTRAINT fk_tb_grupo_usuario_usuario FOREIGN KEY (usuario_id) REFERENCES public.tb_usuario(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tb_historico_login
    ADD CONSTRAINT fk_tb_historico_login_usuario FOREIGN KEY (usuario_id) REFERENCES public.tb_usuario(id) ON DELETE SET NULL;

ALTER TABLE ONLY public.tb_historico_senha
    ADD CONSTRAINT fk_tb_historico_senha_usuario FOREIGN KEY (usuario_id) REFERENCES public.tb_usuario(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tb_permissao
    ADD CONSTRAINT fk_tb_permissao_funcionalidade FOREIGN KEY (funcionalidade_id) REFERENCES public.tb_funcionalidade(id);

ALTER TABLE ONLY public.tb_sessao
    ADD CONSTRAINT fk_tb_sessao_usuario FOREIGN KEY (usuario_id) REFERENCES public.tb_usuario(id) ON DELETE CASCADE;
