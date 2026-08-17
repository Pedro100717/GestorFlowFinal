CREATE SEQUENCE public.reports_suporte_id_seq INCREMENT BY 1 MINVALUE 1 START 1 CACHE 1 NO CYCLE;

CREATE TABLE public.reports_suporte (
    id bigserial PRIMARY KEY,
    tipo varchar(50) NOT NULL,
    descricao text NOT NULL,
    pagina_origem varchar(255) NULL,
    email_utilizador varchar(255) NULL,
    utilizador_id int8 NULL REFERENCES public.utilizadores(id),
    estado varchar(20) NOT NULL DEFAULT 'ABERTO',
    data_criacao_sistema timestamp(6) NULL DEFAULT CURRENT_TIMESTAMP,
    data_ultima_modificacao timestamp(6) NULL,
    criado_por varchar(255) NULL,
    modificado_por varchar(255) NULL
);

CREATE INDEX idx_reports_estado ON public.reports_suporte(estado);
CREATE INDEX idx_reports_utilizador ON public.reports_suporte(utilizador_id);