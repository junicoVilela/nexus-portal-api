-- F3.0 — destinos remotos (FTP/SFTP/BUCKET) para a configuração de entrega
-- por cliente. Spec: docs/release-orchestrator/07-cliente-configuracoes-entrega.md

ALTER TABLE tb_config_entrega_orchestrator
  ADD COLUMN host                VARCHAR(200),
  ADD COLUMN porta               INTEGER,
  ADD COLUMN usuario             VARCHAR(120),
  -- senha cifrada em AES-GCM (base64). Nunca devolvida ao frontend; UI usa
  -- flag senha_configurada em ConfigEntregaResponse.
  ADD COLUMN senha_cifrada       VARCHAR(1000),
  ADD COLUMN modo_passivo        BOOLEAN  DEFAULT TRUE,
  -- SFTP: se TRUE, valida a fingerprint do host antes de conectar.
  ADD COLUMN strict_host_check   BOOLEAN  DEFAULT TRUE;

COMMENT ON COLUMN tb_config_entrega_orchestrator.host IS 'Host do FTP/SFTP/bucket. Nulo quando tipo_destino=PASTA.';
COMMENT ON COLUMN tb_config_entrega_orchestrator.porta IS 'Porta do servidor remoto. Default 21 (FTP) ou 22 (SFTP).';
COMMENT ON COLUMN tb_config_entrega_orchestrator.usuario IS 'Usuário para autenticação no destino remoto.';
COMMENT ON COLUMN tb_config_entrega_orchestrator.senha_cifrada IS 'Senha cifrada em AES-GCM (base64). Em branco preserva a atual no PUT.';
COMMENT ON COLUMN tb_config_entrega_orchestrator.modo_passivo IS 'FTP em modo passivo (FTPS PASV). Default TRUE.';
COMMENT ON COLUMN tb_config_entrega_orchestrator.strict_host_check IS 'SFTP: valida fingerprint do servidor. Default TRUE.';
