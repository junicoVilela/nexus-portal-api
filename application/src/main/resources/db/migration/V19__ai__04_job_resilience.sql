-- Nexus AI — progresso persistente, cancelamento e diagnóstico dos jobs

ALTER TABLE public.tb_ai_job
    ADD COLUMN etapa character varying(60) NOT NULL DEFAULT 'AGUARDANDO',
    ADD COLUMN progresso smallint NOT NULL DEFAULT 0,
    ADD COLUMN tentativa integer NOT NULL DEFAULT 1,
    ADD COLUMN heartbeat_at timestamp with time zone,
    ADD COLUMN cancel_requested_at timestamp with time zone,
    ADD COLUMN erro_detalhe text,
    ADD COLUMN diagnostico_id uuid;

ALTER TABLE public.tb_ai_job
    DROP CONSTRAINT ck_tb_ai_job_status;

ALTER TABLE public.tb_ai_job
    ADD CONSTRAINT ck_tb_ai_job_status CHECK (
        (status)::text = ANY ((ARRAY[
            'PENDENTE'::character varying,
            'PROCESSANDO'::character varying,
            'SUCESSO'::character varying,
            'ERRO'::character varying,
            'CANCELADO'::character varying
        ])::text[])
    ),
    ADD CONSTRAINT ck_tb_ai_job_progresso CHECK (progresso BETWEEN 0 AND 100),
    ADD CONSTRAINT ck_tb_ai_job_tentativa CHECK (tentativa >= 1);

CREATE UNIQUE INDEX ux_tb_ai_job_sessao_ativo
    ON public.tb_ai_job (sessao_id)
    WHERE status IN ('PENDENTE', 'PROCESSANDO');

CREATE INDEX ix_tb_ai_job_heartbeat
    ON public.tb_ai_job (heartbeat_at)
    WHERE status IN ('PENDENTE', 'PROCESSANDO');
