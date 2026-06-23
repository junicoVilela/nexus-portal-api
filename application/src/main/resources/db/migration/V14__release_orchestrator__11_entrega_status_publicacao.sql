-- F3 P2 — controle de publicação remota com retry.
-- Spec: docs/release-orchestrator/22-detalhes-entrega.md
--
-- Modelo:
--   * status_publicacao  ∈ NAO_APLICAVEL (PASTA) | PENDENTE | OK | FALHA
--   * PENDENTE = job vai tentar quando proxima_tentativa_em <= now()
--   * OK       = publicação concluída com sucesso
--   * FALHA    = esgotou max_tentativas; precisa de intervenção manual
--
-- Entregas legadas com status=CONCLUIDA ficam NAO_APLICAVEL (já foram
-- "publicadas" no fluxo síncrono anterior; histórico preservado).

ALTER TABLE tb_entrega
  ADD COLUMN status_publicacao        VARCHAR(20)  NOT NULL DEFAULT 'NAO_APLICAVEL',
  ADD COLUMN tentativas_publicacao    INTEGER      NOT NULL DEFAULT 0,
  ADD COLUMN proxima_tentativa_em     TIMESTAMPTZ,
  ADD COLUMN ultima_falha_publicacao  TEXT,
  ADD COLUMN data_publicacao          TIMESTAMPTZ,
  ADD COLUMN destino_publicacao       VARCHAR(700);

-- Índice parcial: o job filtra por (status_publicacao=PENDENTE AND proxima_tentativa_em<=now()).
CREATE INDEX ix_entrega_publicacao_pendente
  ON tb_entrega (proxima_tentativa_em)
  WHERE status_publicacao = 'PENDENTE';

COMMENT ON COLUMN tb_entrega.status_publicacao IS 'NAO_APLICAVEL (PASTA), PENDENTE, OK ou FALHA. Controla retry job F3 P2.';
COMMENT ON COLUMN tb_entrega.tentativas_publicacao IS 'Quantas vezes o job tentou publicar. Backoff exponencial.';
COMMENT ON COLUMN tb_entrega.proxima_tentativa_em IS 'Quando o job pode tentar de novo. Nulo quando OK/NAO_APLICAVEL ou esgotada.';
COMMENT ON COLUMN tb_entrega.ultima_falha_publicacao IS 'Mensagem da última tentativa falha (para diagnóstico no detalhe da entrega).';
COMMENT ON COLUMN tb_entrega.data_publicacao IS 'Quando a publicação foi concluída com sucesso.';
COMMENT ON COLUMN tb_entrega.destino_publicacao IS 'URL/path final no destino (ftp://, sftp://, s3://, /pasta/...).';
