-- Release Orchestrator — constraints & comments

ALTER TABLE ONLY public.tb_artefato_release_modulo
    ADD CONSTRAINT pk_tb_artefato_release_modulo PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_cliente_funcionalidade_produto
    ADD CONSTRAINT pk_tb_cliente_funcionalidade_produto PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_cliente_orchestrator
    ADD CONSTRAINT pk_tb_cliente_orchestrator PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_cliente_produto
    ADD CONSTRAINT pk_tb_cliente_produto PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_cliente_produto_modulo
    ADD CONSTRAINT pk_tb_cliente_produto_modulo PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_config_entrega_orchestrator
    ADD CONSTRAINT pk_tb_config_entrega_orchestrator PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_contato_orchestrator
    ADD CONSTRAINT pk_tb_contato_orchestrator PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_dominio_produto
    ADD CONSTRAINT pk_tb_dominio_produto PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_entrega
    ADD CONSTRAINT pk_tb_entrega PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_entrega_modulo
    ADD CONSTRAINT pk_tb_entrega_modulo PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_entrega_modulo_artefato
    ADD CONSTRAINT pk_tb_entrega_modulo_artefato PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_funcionalidade_produto
    ADD CONSTRAINT pk_tb_funcionalidade_produto PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_modulo_produto
    ADD CONSTRAINT pk_tb_modulo_produto PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_produto_rh
    ADD CONSTRAINT pk_tb_produto_rh PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_proxima_entrega
    ADD CONSTRAINT pk_tb_proxima_entrega PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_release
    ADD CONSTRAINT pk_tb_release PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_release_historico
    ADD CONSTRAINT pk_tb_release_historico PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_release_item
    ADD CONSTRAINT pk_tb_release_item PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_release_modulo_versao
    ADD CONSTRAINT pk_tb_release_modulo_versao PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_release_template
    ADD CONSTRAINT pk_tb_release_template PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_cliente_funcionalidade_produto
    ADD CONSTRAINT uq_tb_cliente_funcionalidade_produto_par UNIQUE (cliente_id, funcionalidade_produto_id);

ALTER TABLE ONLY public.tb_cliente_orchestrator
    ADD CONSTRAINT uq_tb_cliente_orchestrator_cnpj UNIQUE (cnpj);

ALTER TABLE ONLY public.tb_cliente_orchestrator
    ADD CONSTRAINT uq_tb_cliente_orchestrator_sigla UNIQUE (sigla);

ALTER TABLE ONLY public.tb_cliente_produto_modulo
    ADD CONSTRAINT uq_tb_cliente_produto_modulo_par UNIQUE (cliente_produto_id, modulo_produto_id);

ALTER TABLE ONLY public.tb_cliente_produto
    ADD CONSTRAINT uq_tb_cliente_produto_par UNIQUE (cliente_id, produto_id);

ALTER TABLE ONLY public.tb_config_entrega_orchestrator
    ADD CONSTRAINT uq_tb_config_entrega_orchestrator_cliente UNIQUE (cliente_id);

ALTER TABLE ONLY public.tb_dominio_produto
    ADD CONSTRAINT uq_tb_dominio_produto_codigo UNIQUE (produto_id, codigo);

ALTER TABLE ONLY public.tb_entrega_modulo_artefato
    ADD CONSTRAINT uq_tb_entrega_modulo_artefato_par UNIQUE (entrega_modulo_id, artefato_release_modulo_id);

ALTER TABLE ONLY public.tb_entrega_modulo
    ADD CONSTRAINT uq_tb_entrega_modulo_par UNIQUE (entrega_id, modulo_produto_id);

ALTER TABLE ONLY public.tb_funcionalidade_produto
    ADD CONSTRAINT uq_tb_funcionalidade_produto_codigo UNIQUE (dominio_produto_id, codigo);

ALTER TABLE ONLY public.tb_modulo_produto
    ADD CONSTRAINT uq_tb_modulo_produto_codigo UNIQUE (produto_id, codigo);

ALTER TABLE ONLY public.tb_produto_rh
    ADD CONSTRAINT uq_tb_produto_rh_sigla UNIQUE (sigla);

ALTER TABLE ONLY public.tb_release_modulo_versao
    ADD CONSTRAINT uq_tb_release_modulo_versao_release_modulo UNIQUE (release_id, modulo_produto_id);

ALTER TABLE ONLY public.tb_release
    ADD CONSTRAINT uq_tb_release_produto_versao UNIQUE (produto_id, versao);

ALTER TABLE ONLY public.tb_artefato_release_modulo
    ADD CONSTRAINT fk_tb_artefato_release_modulo_modulo FOREIGN KEY (modulo_produto_id) REFERENCES public.tb_modulo_produto(id);

ALTER TABLE ONLY public.tb_artefato_release_modulo
    ADD CONSTRAINT fk_tb_artefato_release_modulo_release FOREIGN KEY (release_id) REFERENCES public.tb_release(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tb_cliente_funcionalidade_produto
    ADD CONSTRAINT fk_tb_cliente_funcionalidade_produto_cliente FOREIGN KEY (cliente_id) REFERENCES public.tb_cliente_orchestrator(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tb_cliente_funcionalidade_produto
    ADD CONSTRAINT fk_tb_cliente_funcionalidade_produto_funcionalidade FOREIGN KEY (funcionalidade_produto_id) REFERENCES public.tb_funcionalidade_produto(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tb_cliente_produto
    ADD CONSTRAINT fk_tb_cliente_produto_cliente FOREIGN KEY (cliente_id) REFERENCES public.tb_cliente_orchestrator(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tb_cliente_produto_modulo
    ADD CONSTRAINT fk_tb_cliente_produto_modulo_cliente_produto FOREIGN KEY (cliente_produto_id) REFERENCES public.tb_cliente_produto(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tb_cliente_produto_modulo
    ADD CONSTRAINT fk_tb_cliente_produto_modulo_modulo FOREIGN KEY (modulo_produto_id) REFERENCES public.tb_modulo_produto(id);

ALTER TABLE ONLY public.tb_cliente_produto
    ADD CONSTRAINT fk_tb_cliente_produto_produto FOREIGN KEY (produto_id) REFERENCES public.tb_produto_rh(id);

ALTER TABLE ONLY public.tb_config_entrega_orchestrator
    ADD CONSTRAINT fk_tb_config_entrega_orchestrator_cliente FOREIGN KEY (cliente_id) REFERENCES public.tb_cliente_orchestrator(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tb_contato_orchestrator
    ADD CONSTRAINT fk_tb_contato_orchestrator_cliente FOREIGN KEY (cliente_id) REFERENCES public.tb_cliente_orchestrator(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tb_dominio_produto
    ADD CONSTRAINT fk_tb_dominio_produto_produto FOREIGN KEY (produto_id) REFERENCES public.tb_produto_rh(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tb_entrega
    ADD CONSTRAINT fk_tb_entrega_cliente FOREIGN KEY (cliente_id) REFERENCES public.tb_cliente_orchestrator(id);

ALTER TABLE ONLY public.tb_entrega_modulo_artefato
    ADD CONSTRAINT fk_tb_entrega_modulo_artefato_artefato FOREIGN KEY (artefato_release_modulo_id) REFERENCES public.tb_artefato_release_modulo(id);

ALTER TABLE ONLY public.tb_entrega_modulo_artefato
    ADD CONSTRAINT fk_tb_entrega_modulo_artefato_em FOREIGN KEY (entrega_modulo_id) REFERENCES public.tb_entrega_modulo(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tb_entrega_modulo
    ADD CONSTRAINT fk_tb_entrega_modulo_entrega FOREIGN KEY (entrega_id) REFERENCES public.tb_entrega(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tb_entrega_modulo
    ADD CONSTRAINT fk_tb_entrega_modulo_modulo FOREIGN KEY (modulo_produto_id) REFERENCES public.tb_modulo_produto(id);

ALTER TABLE ONLY public.tb_entrega
    ADD CONSTRAINT fk_tb_entrega_original FOREIGN KEY (entrega_original_id) REFERENCES public.tb_entrega(id);

ALTER TABLE ONLY public.tb_entrega
    ADD CONSTRAINT fk_tb_entrega_produto FOREIGN KEY (produto_id) REFERENCES public.tb_produto_rh(id);

ALTER TABLE ONLY public.tb_entrega
    ADD CONSTRAINT fk_tb_entrega_proxima_entrega FOREIGN KEY (proxima_entrega_id) REFERENCES public.tb_proxima_entrega(id);

ALTER TABLE ONLY public.tb_entrega
    ADD CONSTRAINT fk_tb_entrega_release FOREIGN KEY (release_id) REFERENCES public.tb_release(id);

ALTER TABLE ONLY public.tb_funcionalidade_produto
    ADD CONSTRAINT fk_tb_funcionalidade_produto_dominio FOREIGN KEY (dominio_produto_id) REFERENCES public.tb_dominio_produto(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tb_modulo_produto
    ADD CONSTRAINT fk_tb_modulo_produto_produto FOREIGN KEY (produto_id) REFERENCES public.tb_produto_rh(id);

ALTER TABLE ONLY public.tb_proxima_entrega
    ADD CONSTRAINT fk_tb_proxima_entrega_cliente FOREIGN KEY (cliente_id) REFERENCES public.tb_cliente_orchestrator(id);

ALTER TABLE ONLY public.tb_proxima_entrega
    ADD CONSTRAINT fk_tb_proxima_entrega_produto FOREIGN KEY (produto_id) REFERENCES public.tb_produto_rh(id);

ALTER TABLE ONLY public.tb_proxima_entrega
    ADD CONSTRAINT fk_tb_proxima_entrega_release FOREIGN KEY (release_id) REFERENCES public.tb_release(id);

ALTER TABLE ONLY public.tb_release_historico
    ADD CONSTRAINT fk_tb_release_historico_release FOREIGN KEY (release_id) REFERENCES public.tb_release(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tb_release_item
    ADD CONSTRAINT fk_tb_release_item_release FOREIGN KEY (release_id) REFERENCES public.tb_release(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tb_release_modulo_versao
    ADD CONSTRAINT fk_tb_release_modulo_versao_modulo FOREIGN KEY (modulo_produto_id) REFERENCES public.tb_modulo_produto(id);

ALTER TABLE ONLY public.tb_release_modulo_versao
    ADD CONSTRAINT fk_tb_release_modulo_versao_release FOREIGN KEY (release_id) REFERENCES public.tb_release(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tb_release
    ADD CONSTRAINT fk_tb_release_produto FOREIGN KEY (produto_id) REFERENCES public.tb_produto_rh(id);

COMMENT ON COLUMN public.tb_config_entrega_orchestrator.host IS 'Host do FTP/SFTP/bucket. Nulo quando tipo_destino=PASTA.';

COMMENT ON COLUMN public.tb_config_entrega_orchestrator.porta IS 'Porta do servidor remoto. Default 21 (FTP) ou 22 (SFTP).';

COMMENT ON COLUMN public.tb_config_entrega_orchestrator.usuario IS 'Usuário FTP/SFTP, ou access key S3.';

COMMENT ON COLUMN public.tb_config_entrega_orchestrator.senha_cifrada IS 'Senha (ou secret key S3) cifrada em AES-GCM (base64). Em branco preserva a atual no PUT.';

COMMENT ON COLUMN public.tb_config_entrega_orchestrator.modo_passivo IS 'FTP em modo passivo (FTPS PASV). Default TRUE.';

COMMENT ON COLUMN public.tb_config_entrega_orchestrator.strict_host_check IS 'SFTP: valida fingerprint do servidor. Default TRUE.';

COMMENT ON COLUMN public.tb_config_entrega_orchestrator.bucket IS 'Nome do bucket S3/MinIO. Obrigatório quando tipo_destino=BUCKET.';

COMMENT ON COLUMN public.tb_config_entrega_orchestrator.endpoint IS 'Endpoint custom (MinIO / Backblaze / etc). Vazio = AWS S3 padrão.';

COMMENT ON COLUMN public.tb_config_entrega_orchestrator.regiao IS 'Região S3 (us-east-1, sa-east-1, etc). Default us-east-1 quando endpoint custom.';

COMMENT ON COLUMN public.tb_config_entrega_orchestrator.path_style_access IS 'TRUE para MinIO/endpoints custom; FALSE (default) para AWS S3 virtual-hosted.';

COMMENT ON COLUMN public.tb_entrega.status_publicacao IS 'NAO_APLICAVEL (PASTA), PENDENTE, OK ou FALHA. Controla retry job F3 P2.';

COMMENT ON COLUMN public.tb_entrega.tentativas_publicacao IS 'Quantas vezes o job tentou publicar. Backoff exponencial.';

COMMENT ON COLUMN public.tb_entrega.proxima_tentativa_em IS 'Quando o job pode tentar de novo. Nulo quando OK/NAO_APLICAVEL ou esgotada.';

COMMENT ON COLUMN public.tb_entrega.ultima_falha_publicacao IS 'Mensagem da última tentativa falha (para diagnóstico no detalhe da entrega).';

COMMENT ON COLUMN public.tb_entrega.data_publicacao IS 'Quando a publicação foi concluída com sucesso.';

COMMENT ON COLUMN public.tb_entrega.destino_publicacao IS 'URL/path final no destino (ftp://, sftp://, s3://, /pasta/...).';

COMMENT ON COLUMN public.tb_produto_rh.repositorio_github IS 'owner/repo no GitHub (ex.: nexus/produto-exemplo). Nulo desabilita integração.';

COMMENT ON COLUMN public.tb_produto_rh.branch_padrao IS 'Branch base para comparar tags. Default main.';

COMMENT ON COLUMN public.tb_produto_rh.padrao_tag IS 'Regex que tags válidas devem atender. Default ^v\d+\.\d+\.\d+$.';

COMMENT ON COLUMN public.tb_produto_rh.github_token IS 'PAT GitHub (escopo repo). MVP: texto plano. Pós-MVP: referência a credencial.';

COMMENT ON COLUMN public.tb_produto_rh.jenkins_url IS 'URL base do Jenkins (ex.: https://jenkins.nexus.local). Nulo = sem integração.';

COMMENT ON COLUMN public.tb_produto_rh.jenkins_job IS 'Nome do job que builda o produto (ex.: produto-exemplo-build).';

COMMENT ON COLUMN public.tb_produto_rh.jenkins_user IS 'Usuário Jenkins para autenticação básica.';

COMMENT ON COLUMN public.tb_produto_rh.jenkins_token IS 'API token Jenkins. MVP: texto plano.';

COMMENT ON COLUMN public.tb_produto_rh.jenkins_trigger_mode IS 'BUILD_ON_TAG = job dispara em tag git; MANUAL = operador inicia.';

COMMENT ON COLUMN public.tb_release.ultimo_build_status IS 'Status do último build reportado pelo Jenkins via webhook.';

COMMENT ON COLUMN public.tb_release.ultimo_build_numero IS 'Número da execução Jenkins (#42).';

COMMENT ON COLUMN public.tb_release.ultimo_build_url IS 'URL absoluta do build no Jenkins para abrir os logs.';

COMMENT ON COLUMN public.tb_release.ultimo_build_at IS 'Timestamp da última atualização do status pelo webhook.';
