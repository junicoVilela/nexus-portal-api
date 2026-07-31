-- Alinha tema padrão dos clientes à paleta indigo do portal / templates de edição.
UPDATE tb_cliente
SET tema_cor_primaria = '#4f46e5',
    tema_cor_fundo = '#f7f8fa'
WHERE tema_cor_primaria IS NULL
   OR tema_cor_primaria IN ('#1a73e8', '#1A73E8')
   OR tema_cor_fundo IS NULL
   OR tema_cor_fundo IN ('#f8f9fa', '#F8F9FA');

COMMENT ON COLUMN tb_cliente.tema_cor_primaria IS 'Cor accent do manual (manifest/PWA); padrão portal #4f46e5';
