-- Nexus AI — habilita importação de documentos Word binários legados (.doc)

ALTER TABLE public.tb_ai_documento_importacao
    DROP CONSTRAINT ck_tb_ai_documento_tipo;

ALTER TABLE public.tb_ai_documento_importacao
    ADD CONSTRAINT ck_tb_ai_documento_tipo
        CHECK (tipo_arquivo IN ('DOC', 'DOCX', 'PDF', 'TXT'));
