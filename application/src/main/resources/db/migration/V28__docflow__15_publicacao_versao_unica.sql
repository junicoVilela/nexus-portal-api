ALTER TABLE tb_publicacao
  ADD CONSTRAINT uk_tb_publicacao_cliente_versao UNIQUE (cliente_id, versao);
