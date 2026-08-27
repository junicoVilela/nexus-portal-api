-- Inventário operacional da instalação (RF-006) e alvos de entrega (RF-008).
-- deploy(releaseId, instalacaoId): a entrega aponta para uma ou mais instalações.

ALTER TABLE public.tb_instalacao_cliente
    ADD COLUMN versao_atual character varying(80),
    ADD COLUMN health character varying(20) DEFAULT 'DESCONHECIDO'::character varying NOT NULL,
    ADD COLUMN ultima_verificacao timestamp with time zone,
    ADD COLUMN ultimo_erro text;

ALTER TABLE public.tb_instalacao_cliente
    ADD CONSTRAINT ck_tb_instalacao_cliente_health CHECK (((health)::text = ANY ((ARRAY['DESCONHECIDO'::character varying, 'SAUDAVEL'::character varying, 'DEGRADADO'::character varying, 'INDISPONIVEL'::character varying])::text[])));

CREATE TABLE public.tb_entrega_instalacao (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    entrega_id uuid NOT NULL,
    instalacao_id uuid NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(120),
    updated_by character varying(120)
);

ALTER TABLE ONLY public.tb_entrega_instalacao
    ADD CONSTRAINT pk_tb_entrega_instalacao PRIMARY KEY (id);

ALTER TABLE ONLY public.tb_entrega_instalacao
    ADD CONSTRAINT uq_tb_entrega_instalacao UNIQUE (entrega_id, instalacao_id);

ALTER TABLE ONLY public.tb_entrega_instalacao
    ADD CONSTRAINT fk_tb_entrega_instalacao_entrega FOREIGN KEY (entrega_id)
        REFERENCES public.tb_entrega(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.tb_entrega_instalacao
    ADD CONSTRAINT fk_tb_entrega_instalacao_instalacao FOREIGN KEY (instalacao_id)
        REFERENCES public.tb_instalacao_cliente(id);

CREATE INDEX idx_tb_entrega_instalacao_entrega ON public.tb_entrega_instalacao USING btree (entrega_id);

CREATE INDEX idx_tb_entrega_instalacao_instalacao ON public.tb_entrega_instalacao USING btree (instalacao_id);

CREATE INDEX idx_tb_instalacao_cliente_health ON public.tb_instalacao_cliente USING btree (health);

COMMENT ON COLUMN public.tb_instalacao_cliente.versao_atual IS 'Versão do produto atualmente nesta instalação (RF-006).';

COMMENT ON COLUMN public.tb_instalacao_cliente.health IS 'Último health conhecido: DESCONHECIDO, SAUDAVEL, DEGRADADO, INDISPONIVEL.';

COMMENT ON TABLE public.tb_entrega_instalacao IS 'Alvos de uma entrega: instalações específicas, não o host (RF-008).';
