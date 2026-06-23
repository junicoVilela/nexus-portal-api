-- F2.10 / S11 P2 — build status reportado pelo Jenkins via webhook
-- Spec: docs/release-orchestrator/02-checklist-por-sprint.md S11

ALTER TABLE tb_release
  ADD COLUMN ultimo_build_status   VARCHAR(30),
  ADD COLUMN ultimo_build_numero   INTEGER,
  ADD COLUMN ultimo_build_url      VARCHAR(500),
  ADD COLUMN ultimo_build_at       TIMESTAMPTZ;

ALTER TABLE tb_release
  ADD CONSTRAINT ck_tb_release_build_status
  CHECK (ultimo_build_status IS NULL
         OR ultimo_build_status IN ('EM_ANDAMENTO', 'SUCCESS', 'FAILED',
                                     'UNSTABLE', 'ABORTED'));

COMMENT ON COLUMN tb_release.ultimo_build_status IS 'Status do último build reportado pelo Jenkins via webhook.';
COMMENT ON COLUMN tb_release.ultimo_build_numero IS 'Número da execução Jenkins (#42).';
COMMENT ON COLUMN tb_release.ultimo_build_url IS 'URL absoluta do build no Jenkins para abrir os logs.';
COMMENT ON COLUMN tb_release.ultimo_build_at IS 'Timestamp da última atualização do status pelo webhook.';
