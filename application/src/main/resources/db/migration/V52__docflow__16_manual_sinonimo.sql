-- Sinônimos da busca do manual por cliente: "NF" acha "nota fiscal". Cada linha é um grupo de
-- termos equivalentes. Vale na hora para perguntas, MCP e manual hospedado; o ZIP leva uma cópia
-- (sinonimos.json) para a busca offline.

CREATE TABLE public.tb_manual_sinonimo (
    id uuid NOT NULL,
    cliente_id uuid NOT NULL,
    termos jsonb DEFAULT '[]'::jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT pk_tb_manual_sinonimo PRIMARY KEY (id),
    CONSTRAINT fk_tb_manual_sinonimo_cliente FOREIGN KEY (cliente_id) REFERENCES public.tb_cliente(id) ON DELETE CASCADE
);

CREATE INDEX ix_tb_manual_sinonimo_cliente ON public.tb_manual_sinonimo (cliente_id);

COMMENT ON TABLE public.tb_manual_sinonimo IS
  'Grupos de termos equivalentes na busca do manual de um cliente (ex.: ["nota fiscal", "NF", "NF-e"]).';
COMMENT ON COLUMN public.tb_manual_sinonimo.termos IS 'Termos como o usuário digitou; a busca compara sem acento e sem caixa.';
