-- Robustez da geração de publicação do DocFlow.
--
-- 1. relatorio_validacao cresce com o número de arquivos do pacote; varchar(4000)
--    estourava só no flush, depois do ZIP já gerado. Passa a text (como arvore_paginas).
-- 2. cancelamento_solicitado permite parar uma geração em andamento de forma
--    cooperativa — o worker descarta o resultado e marca CANCELADA.
-- 3. Índice (status, updated_at) serve o watchdog que reconcilia publicações
--    que ficaram em GERANDO após uma queda do processo.

ALTER TABLE public.tb_publicacao
    ALTER COLUMN relatorio_validacao TYPE text;

ALTER TABLE public.tb_publicacao
    ADD COLUMN cancelamento_solicitado boolean DEFAULT false NOT NULL;

CREATE INDEX idx_tb_publicacao_status_updated
    ON public.tb_publicacao USING btree (status, updated_at);

COMMENT ON COLUMN public.tb_publicacao.relatorio_validacao IS
    'JSON com resultado da validação do ZIP (arquivos obrigatórios, etc.).';

COMMENT ON COLUMN public.tb_publicacao.cancelamento_solicitado IS
    'Pedido de cancelamento da geração; o worker verifica antes de persistir o resultado.';
