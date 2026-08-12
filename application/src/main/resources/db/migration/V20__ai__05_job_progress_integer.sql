-- Alinha o tipo físico ao Integer usado pela entidade JPA.
ALTER TABLE public.tb_ai_job
    ALTER COLUMN progresso TYPE integer;
