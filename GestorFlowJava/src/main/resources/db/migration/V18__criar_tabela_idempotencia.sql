-- V18__criar_tabela_idempotencia.sql

CREATE TABLE public.chaves_idempotencia (
    chave varchar(36) PRIMARY KEY,

    -- 🚀 Mantemos a coerência perfeita com o resto da BD
    utilizador_id int8 NOT NULL REFERENCES public.utilizadores(id),

    data_criacao timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index para garantir que pesquisas de limpeza antigas por utilizador sejam super rápidas
CREATE INDEX idx_chaves_idemp_utilizador ON public.chaves_idempotencia(utilizador_id);