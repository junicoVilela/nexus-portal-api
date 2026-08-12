ALTER TABLE tb_ai_proposta
  ADD COLUMN page_spec_json JSONB;

COMMENT ON COLUMN tb_ai_proposta.page_spec_json IS
  'Especificação estruturada e auditável usada para renderizar o HTML da proposta.';
