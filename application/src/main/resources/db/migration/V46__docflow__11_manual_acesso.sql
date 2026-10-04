-- Onda D (INT-403): chave de integração do manual por cliente. Os sistemas do cliente a usam
-- para abrir a ajuda da tela atual, perguntar ao manual e conectar agentes (MCP).
--
-- O valor (nxm_…) só aparece na criação: aqui fica o sha256. Origens limitam o uso pelo
-- navegador (CORS); vazio = sem restrição de origem (uso servidor a servidor).

CREATE TABLE public.tb_manual_acesso (
    id uuid NOT NULL,
    cliente_id uuid NOT NULL,
    nome character varying(120) NOT NULL,
    prefixo character varying(16) NOT NULL,
    token_hash character varying(64) NOT NULL,
    origens jsonb DEFAULT '[]'::jsonb NOT NULL,
    ativo boolean DEFAULT true NOT NULL,
    expira_em timestamp with time zone,
    ultimo_uso_em timestamp with time zone,
    revogado_em timestamp with time zone,
    revogado_por character varying(120),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    CONSTRAINT pk_tb_manual_acesso PRIMARY KEY (id),
    CONSTRAINT uk_tb_manual_acesso_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_tb_manual_acesso_cliente FOREIGN KEY (cliente_id) REFERENCES public.tb_cliente(id) ON DELETE CASCADE
);

CREATE INDEX ix_tb_manual_acesso_cliente ON public.tb_manual_acesso (cliente_id);

COMMENT ON TABLE public.tb_manual_acesso IS
  'Chaves de integração do manual (leitura do manual vigente, help-bridge, perguntas e MCP).';
COMMENT ON COLUMN public.tb_manual_acesso.prefixo IS 'Começo do token, para identificar a chave na tela sem expor o valor.';
COMMENT ON COLUMN public.tb_manual_acesso.origens IS 'Origens (https://app.cliente.com) liberadas no CORS; vazio = qualquer.';
