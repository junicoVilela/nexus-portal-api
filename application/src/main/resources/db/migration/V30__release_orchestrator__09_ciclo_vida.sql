-- Start/stop da instalação (scripts/start.sh|stop.sh ou docker start/stop).
-- Não altera o contrato deploy(releaseId, instalacaoId).

ALTER TABLE public.tb_deploy_instalacao
    DROP CONSTRAINT ck_tb_deploy_instalacao_operacao;

ALTER TABLE public.tb_deploy_instalacao
    ADD CONSTRAINT ck_tb_deploy_instalacao_operacao CHECK (((operacao)::text = ANY ((ARRAY[
        'CRIAR'::character varying,
        'ATUALIZAR'::character varying,
        'INICIAR'::character varying,
        'PARAR'::character varying
    ])::text[])));

COMMENT ON TABLE public.tb_deploy_instalacao IS
    'Histórico de deploy(releaseId, instalacaoId) e ciclo de vida (INICIAR/PARAR). Dry-run não altera o host.';
