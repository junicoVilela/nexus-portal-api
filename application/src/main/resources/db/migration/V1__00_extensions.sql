-- Extensões / premissas do Nexus Platform (PostgreSQL 13+).
-- Identificadores são UUID (gen_random_uuid nativo). Não há sequences SERIAL.
-- pgcrypto é opcional; tentamos criar apenas se o role tiver permissão.

DO $$
BEGIN
  CREATE EXTENSION IF NOT EXISTS pgcrypto;
EXCEPTION
  WHEN insufficient_privilege THEN
    RAISE NOTICE 'pgcrypto não criado (sem privilégio); gen_random_uuid nativo será usado.';
  WHEN OTHERS THEN
    RAISE NOTICE 'pgcrypto ignorado: %', SQLERRM;
END $$;
