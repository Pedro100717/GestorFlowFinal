CREATE SEQUENCE public.movimentos_planeados_id_seq INCREMENT BY 1 MINVALUE 1 START 1 CACHE 1 NO CYCLE;

CREATE TABLE public.movimentos_planeados (
    id bigserial PRIMARY KEY,
    descricao varchar(255) NOT NULL,
    tipo varchar(50) NOT NULL,
    frequencia varchar(50) NOT NULL,
    valor_base numeric(19, 2) NOT NULL,
    taxa_iva numeric(19, 2) NOT NULL,
    data_inicio date NOT NULL,
    data_fim date NULL,
    ativo bool NOT NULL DEFAULT true,

    -- Ligações (exatamente com os nomes das tuas tabelas)
    centro_custo_id int8 NOT NULL REFERENCES public.centro_custo(id),
    seccao_homo_id int8 NOT NULL REFERENCES public.seccao_homo(id),
    cliente_id int8 REFERENCES public.clientes(id),
    fornecedor_id int8 REFERENCES public.fornecedores(id),
    utilizador_id int8 NOT NULL REFERENCES public.utilizadores(id),

    -- Auditoria (Os teus nomes oficiais da V1)
    data_criacao_sistema timestamp(6) NULL,
    data_ultima_modificacao timestamp(6) NULL,
    criado_por varchar(255) NULL,
    modificado_por varchar(255) NULL
);