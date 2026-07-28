ALTER TABLE tb_ajuda_conteudo
  ADD CONSTRAINT fk_tb_ajuda_conteudo_jornada
  FOREIGN KEY (jornada_codigo) REFERENCES tb_ajuda_conteudo (codigo)
  ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE tb_ajuda_conteudo
  ADD CONSTRAINT ck_tb_ajuda_conteudo_jornada_tipo
  CHECK (
    (tipo = 'ETAPA' AND jornada_codigo IS NOT NULL)
    OR (tipo <> 'ETAPA' AND jornada_codigo IS NULL)
  );

CREATE INDEX idx_tb_ajuda_evento_created_at ON tb_ajuda_evento (created_at);
