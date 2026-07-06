-- V15__criar_tabela_empresas.sql

CREATE SEQUENCE public.empresas_id_seq INCREMENT BY 1 MINVALUE 1 START 1 CACHE 1 NO CYCLE;

CREATE TABLE public.empresas (
    id bigserial PRIMARY KEY,
    nome_fiscal varchar(255) NOT NULL,
    nif varchar(20) NOT NULL,
    morada_completa text NULL,
    codigo_postal varchar(20) NULL,
    localidade varchar(100) NULL,
    telefone varchar(50) NULL,
    email_geral varchar(100) NULL,
    logotipo_path varchar(500) NULL, -- Guardará o URL ou caminho do ficheiro do logo
    fuso_horario varchar(50) DEFAULT 'Europe/Lisbon',
    moeda_padrao varchar(10) DEFAULT 'EUR',

    -- 🚀 A Chave de Ouro: Ligação ao Utilizador
    utilizador_id int8 NOT NULL REFERENCES public.utilizadores(id),

    -- Restrição para garantir que cada utilizador só tem 1 empresa neste MVP
    CONSTRAINT uk_empresa_utilizador UNIQUE (utilizador_id),

    -- Campos de Auditoria padrão do teu sistema
    data_criacao_sistema timestamp(6) NULL,
    data_ultima_modificacao timestamp(6) NULL,
    criado_por varchar(255) NULL,
    modificado_por varchar(255) NULL
);

-- Index para otimizar as pesquisas quando o backend for confirmar se o user tem empresa
CREATE INDEX idx_empresas_utilizador ON public.empresas(utilizador_id);