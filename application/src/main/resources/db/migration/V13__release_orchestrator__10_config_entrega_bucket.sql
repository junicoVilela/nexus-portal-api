-- F3 P2 — destino BUCKET (S3 / MinIO) na configuração de entrega.
-- Spec: docs/release-orchestrator/07-cliente-configuracoes-entrega.md
--
-- Reuso de campos:
--   * usuario        → access key
--   * senha_cifrada  → secret key (cifrada em AES-GCM, mesmo esquema FTP/SFTP)
-- Campos novos abaixo são específicos de S3:

ALTER TABLE tb_config_entrega_orchestrator
  ADD COLUMN bucket             VARCHAR(200),
  ADD COLUMN endpoint           VARCHAR(500),
  ADD COLUMN regiao             VARCHAR(60),
  -- MinIO costuma exigir path-style; AWS S3 prefere virtual-hosted (default).
  ADD COLUMN path_style_access  BOOLEAN  DEFAULT FALSE;

COMMENT ON COLUMN tb_config_entrega_orchestrator.bucket IS 'Nome do bucket S3/MinIO. Obrigatório quando tipo_destino=BUCKET.';
COMMENT ON COLUMN tb_config_entrega_orchestrator.endpoint IS 'Endpoint custom (MinIO / Backblaze / etc). Vazio = AWS S3 padrão.';
COMMENT ON COLUMN tb_config_entrega_orchestrator.regiao IS 'Região S3 (us-east-1, sa-east-1, etc). Default us-east-1 quando endpoint custom.';
COMMENT ON COLUMN tb_config_entrega_orchestrator.path_style_access IS 'TRUE para MinIO/endpoints custom; FALSE (default) para AWS S3 virtual-hosted.';
