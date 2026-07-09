-- =============================================================================
-- MÓDULO: seguranca | ADD: bloqueio de usuário
-- Adiciona colunas de controle de acesso na tb_usuario:
--   * bloqueado — flag para bloqueio administrativo (não confunde com ativo:
--                 ativo = usuário existe / bloqueado = existe mas está trancado)
--   * tentativas_invalidas — contador para bloqueio automático por falhas
--   * trocar_senha_proximo_login — força troca no próximo login (ex.: após reset)
-- =============================================================================

ALTER TABLE tb_usuario ADD COLUMN bloqueado BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tb_usuario ADD COLUMN tentativas_invalidas INT NOT NULL DEFAULT 0;
ALTER TABLE tb_usuario ADD COLUMN trocar_senha_proximo_login BOOLEAN NOT NULL DEFAULT FALSE;
