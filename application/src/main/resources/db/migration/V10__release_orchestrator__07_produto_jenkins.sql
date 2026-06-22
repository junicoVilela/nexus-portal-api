-- F2.5 — campos Jenkins no Produto.
-- Spec: docs/release-orchestrator/09-produtos-cadastro.md §4.3

ALTER TABLE tb_produto_rh
  ADD COLUMN jenkins_url          VARCHAR(300),
  ADD COLUMN jenkins_job          VARCHAR(200),
  ADD COLUMN jenkins_user         VARCHAR(120),
  ADD COLUMN jenkins_token        VARCHAR(500),
  ADD COLUMN jenkins_trigger_mode VARCHAR(30) DEFAULT 'BUILD_ON_TAG';

ALTER TABLE tb_produto_rh
  ADD CONSTRAINT ck_tb_produto_rh_jenkins_trigger
  CHECK (jenkins_trigger_mode IS NULL
         OR jenkins_trigger_mode IN ('BUILD_ON_TAG', 'MANUAL'));

COMMENT ON COLUMN tb_produto_rh.jenkins_url IS 'URL base do Jenkins (ex.: https://jenkins.softon.local). Nulo = sem integração.';
COMMENT ON COLUMN tb_produto_rh.jenkins_job IS 'Nome do job que builda o produto (ex.: dtec-ld-build).';
COMMENT ON COLUMN tb_produto_rh.jenkins_user IS 'Usuário Jenkins para autenticação básica.';
COMMENT ON COLUMN tb_produto_rh.jenkins_token IS 'API token Jenkins. MVP: texto plano.';
COMMENT ON COLUMN tb_produto_rh.jenkins_trigger_mode IS 'BUILD_ON_TAG = job dispara em tag git; MANUAL = operador inicia.';
