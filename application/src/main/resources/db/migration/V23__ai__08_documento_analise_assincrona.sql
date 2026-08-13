-- Nexus AI — estado transitório da análise semântica assíncrona de documentos

ALTER TABLE public.tb_ai_documento_importacao
    DROP CONSTRAINT ck_tb_ai_documento_status;

ALTER TABLE public.tb_ai_documento_importacao
    ADD CONSTRAINT ck_tb_ai_documento_status CHECK (
        status IN ('ANALISANDO_ESTRUTURA', 'PRONTO_PARA_REVISAO', 'EM_REVISAO', 'CONCLUIDA')
    );
