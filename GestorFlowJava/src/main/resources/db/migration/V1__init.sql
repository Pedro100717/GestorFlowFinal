-- 1. SEQUÊNCIAS (Mantidas conforme o teu padrão)
CREATE SEQUENCE public.utilizadores_id_seq INCREMENT BY 1 MINVALUE 1 START 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE public.tx_iva_id_seq INCREMENT BY 1 MINVALUE 1 START 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE public.familias_id_seq INCREMENT BY 1 MINVALUE 1 START 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE public.centro_custo_id_seq INCREMENT BY 1 MINVALUE 1 START 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE public.seccao_homo_id_seq INCREMENT BY 1 MINVALUE 1 START 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE public.clientes_id_seq INCREMENT BY 1 MINVALUE 1 START 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE public.fornecedores_id_seq INCREMENT BY 1 MINVALUE 1 START 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE public.contas_bancarias_id_seq INCREMENT BY 1 MINVALUE 1 START 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE public.artigos_id_seq INCREMENT BY 1 MINVALUE 1 START 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE public.patrimonio_id_seq INCREMENT BY 1 MINVALUE 1 START 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE public.tarefas_id_seq INCREMENT BY 1 MINVALUE 1 START 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE public.orcamentos_id_seq INCREMENT BY 1 MINVALUE 1 START 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE public.orcamento_linhas_id_seq INCREMENT BY 1 MINVALUE 1 START 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE public.compras_id_seq INCREMENT BY 1 MINVALUE 1 START 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE public.movimentos_stock_id_seq INCREMENT BY 1 MINVALUE 1 START 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE public.vendas_id_seq INCREMENT BY 1 MINVALUE 1 START 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE public.linhas_venda_id_seq INCREMENT BY 1 MINVALUE 1 START 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE public.movimentos_tesouraria_id_seq INCREMENT BY 1 MINVALUE 1 START 1 CACHE 1 NO CYCLE;

-- 2. TABELAS (Ordem correta para Foreign Keys)

CREATE TABLE public.utilizadores (
    id bigserial PRIMARY KEY,
    nome_utilizador varchar(50) NOT NULL UNIQUE,
    senha varchar(255) NOT NULL,
    email varchar(100) NOT NULL UNIQUE,
    verificado bool DEFAULT false,
    codigo_verificacao varchar(255) NULL,
    -- Auditoria
    data_criacao_sistema timestamp(6) NULL,
    data_ultima_modificacao timestamp(6) NULL,
    criado_por varchar(255) NULL,
    modificado_por varchar(255) NULL
);

CREATE TABLE public.tx_iva (
    id bigserial PRIMARY KEY,
    descricao varchar(50) NOT NULL UNIQUE,
    valor numeric(5, 2) NOT NULL,
    -- Auditoria
    data_criacao_sistema timestamp(6) NULL,
    data_ultima_modificacao timestamp(6) NULL,
    criado_por varchar(255) NULL,
    modificado_por varchar(255) NULL
);

CREATE TABLE public.familias (
    id bigserial PRIMARY KEY,
    nome varchar(255) NOT NULL,
    utilizador_id int8 NOT NULL REFERENCES public.utilizadores(id),
    -- Auditoria
    data_criacao_sistema timestamp(6) NULL,
    data_ultima_modificacao timestamp(6) NULL,
    criado_por varchar(255) NULL,
    modificado_por varchar(255) NULL
);

CREATE TABLE public.centro_custo (
    id bigserial PRIMARY KEY,
    nome varchar(255) NOT NULL,
    codigo varchar(255) UNIQUE,
    utilizador_id int8 NOT NULL REFERENCES public.utilizadores(id),
    -- Auditoria
    data_criacao_sistema timestamp(6) NULL,
    data_ultima_modificacao timestamp(6) NULL,
    criado_por varchar(255) NULL,
    modificado_por varchar(255) NULL
);

CREATE TABLE public.seccao_homo (
    id bigserial PRIMARY KEY,
    nome varchar(255) NOT NULL,
    codigo varchar(50) NOT NULL UNIQUE,
    utilizador_id int8 NOT NULL REFERENCES public.utilizadores(id),
    -- Auditoria
    data_criacao_sistema timestamp(6) NULL,
    data_ultima_modificacao timestamp(6) NULL,
    criado_por varchar(255) NULL,
    modificado_por varchar(255) NULL
);

CREATE TABLE public.clientes (
    id bigserial PRIMARY KEY,
    nome varchar(255) NOT NULL,
    nif varchar(20) NULL,
    email varchar(255) NULL,
    telefone varchar(255) NULL,
    morada text NULL,
    anotacoes text NULL,
    utilizador_id int8 NOT NULL REFERENCES public.utilizadores(id),
    -- Auditoria
    data_criacao_sistema timestamp(6) NULL,
    data_ultima_modificacao timestamp(6) NULL,
    criado_por varchar(255) NULL,
    modificado_por varchar(255) NULL
);

CREATE TABLE public.fornecedores (
    id bigserial PRIMARY KEY,
    nome varchar(255) NOT NULL,
    nif varchar(20) NULL,
    email varchar(255) NULL,
    telefone varchar(255) NULL,
    morada varchar(255) NULL,
    website varchar(255) NULL,
    utilizador_id int8 NOT NULL REFERENCES public.utilizadores(id),
    -- Auditoria
    data_criacao_sistema timestamp(6) NULL,
    data_ultima_modificacao timestamp(6) NULL,
    criado_por varchar(255) NULL,
    modificado_por varchar(255) NULL
);

CREATE TABLE public.contas_bancarias (
    id bigserial PRIMARY KEY,
    nome varchar(255) NOT NULL,
    iban varchar(255) NULL,
    saldo numeric(12, 2) NOT NULL DEFAULT 0,
    utilizador_id int8 NOT NULL REFERENCES public.utilizadores(id),
    version int8 DEFAULT 0, -- 🛡️ Essencial para o @Version no Java
    -- Auditoria
    data_criacao_sistema timestamp(6) NULL,
    data_ultima_modificacao timestamp(6) NULL,
    criado_por varchar(255) NULL,
    modificado_por varchar(255) NULL
);

CREATE TABLE public.artigos (
    id bigserial PRIMARY KEY,
    tipo_artigo varchar(31) NOT NULL,
    nome varchar(255) NOT NULL,
    codigo_barras varchar(255) NULL,
    preco numeric(10, 2) NULL,
    ultimo_preco_custo numeric(10, 2) NULL,
    familia_id int8 REFERENCES public.familias(id),
    utilizador_id int8 NOT NULL REFERENCES public.utilizadores(id),
    version int8 DEFAULT 0, -- 🛡️ Essencial para o @Version no Java
    -- Específicos de Mercadoria
    stock_atual numeric(10, 3) NULL DEFAULT 0,
    stock_minimo numeric(10, 3) NULL DEFAULT 0,
    -- Específicos de Serviço
    duracao_minutos_estimada int4 NULL, -- 🛡️ Detetado no Servico.java
    -- Auditoria
    data_criacao_sistema timestamp(6) NULL,
    data_ultima_modificacao timestamp(6) NULL,
    criado_por varchar(255) NULL,
    modificado_por varchar(255) NULL
);

CREATE TABLE public.patrimonio (
    id bigserial PRIMARY KEY,
    nome varchar(255) NOT NULL,
    data_aquisicao date NULL,
    valor_aquisicao numeric(12, 2) NULL,
    ativo bool NOT NULL DEFAULT true,
    utilizador_id int8 NOT NULL REFERENCES public.utilizadores(id),
    -- Auditoria
    data_criacao_sistema timestamp(6) NULL,
    data_ultima_modificacao timestamp(6) NULL,
    criado_por varchar(255) NULL,
    modificado_por varchar(255) NULL
);

CREATE TABLE public.patrimonio_ferramentas (
    id int8 PRIMARY KEY REFERENCES public.patrimonio(id),
    numero_serie varchar(100) NULL,
    estado_conservacao varchar(50) NULL
);

CREATE TABLE public.patrimonio_imoveis (
    id int8 PRIMARY KEY REFERENCES public.patrimonio(id),
    morada text NULL,
    artigo_matricial varchar(50) NULL,
    tipo varchar(50) NULL
);

CREATE TABLE public.patrimonio_viaturas (
    id int8 PRIMARY KEY REFERENCES public.patrimonio(id),
    matricula varchar(20) NOT NULL UNIQUE,
    marca varchar(50) NULL,
    modelo varchar(100) NULL,
    validade_seguro date NULL,
    proxima_inspecao date NULL
);

CREATE TABLE public.tarefas (
    id bigserial PRIMARY KEY,
    titulo varchar(150) NOT NULL,
    descricao text NULL,
    estado varchar(20) NOT NULL DEFAULT 'PENDENTE',
    prioridade varchar(20) NOT NULL DEFAULT 'NORMAL',
    data_limite date NULL,
    data_conclusao timestamp(6) NULL,
    cliente_id int8 REFERENCES public.clientes(id),
    utilizador_id int8 NOT NULL REFERENCES public.utilizadores(id),
    -- Auditoria
    data_criacao_sistema timestamp(6) NULL,
    data_ultima_modificacao timestamp(6) NULL,
    criado_por varchar(255) NULL,
    modificado_por varchar(255) NULL
);

CREATE TABLE public.orcamentos (
    id bigserial PRIMARY KEY,
    data_emissao date NOT NULL,
    data_validade date NULL,
    estado varchar(50) NOT NULL DEFAULT 'RASCUNHO',
    notas text NULL,
    total_custo numeric(12, 2) DEFAULT 0,
    total_sem_iva numeric(12, 2) DEFAULT 0,
    total_com_iva numeric(12, 2) DEFAULT 0,
    cliente_id int8 NOT NULL REFERENCES public.clientes(id),
    utilizador_id int8 NOT NULL REFERENCES public.utilizadores(id),
    -- Auditoria
    data_criacao_sistema timestamp(6) NULL,
    data_ultima_modificacao timestamp(6) NULL,
    criado_por varchar(255) NULL,
    modificado_por varchar(255) NULL
);

CREATE TABLE public.orcamento_linhas (
    id bigserial PRIMARY KEY,
    orcamento_id int8 NOT NULL REFERENCES public.orcamentos(id),
    artigo_id int8 NOT NULL REFERENCES public.artigos(id),
    tx_iva_id int8 NOT NULL REFERENCES public.tx_iva(id),
    quantidade numeric(10, 3) NOT NULL,
    preco_custo_unitario numeric(10, 2) NOT NULL,
    preco_venda_unitario numeric(10, 2) NOT NULL,
    margem_lucro_percentual numeric(10, 2) NULL,
    total_linha_sem_iva numeric(10, 2) NULL,
    total_linha_com_iva numeric(10, 2) NULL
);

CREATE TABLE public.compras (
    id bigserial PRIMARY KEY,
    data_compra timestamp(6) NOT NULL,
    numero_fatura_fornecedor varchar(255) NULL,
    designacao varchar(255) NOT NULL,
    quantidade numeric(10, 3) NOT NULL,
    preco_unitario numeric(10, 2) NOT NULL,
    total numeric(10, 2) NULL,
    estado_pagamento varchar(20) NOT NULL DEFAULT 'PENDENTE',
    tx_iva_id int8 NOT NULL REFERENCES public.tx_iva(id),
    fornecedor_id int8 NOT NULL REFERENCES public.fornecedores(id),
    artigo_id int8 NOT NULL REFERENCES public.artigos(id),
    centro_custo_id int8 REFERENCES public.centro_custo(id),
    seccao_homo_id int8 REFERENCES public.seccao_homo(id),
    utilizador_id int8 NOT NULL REFERENCES public.utilizadores(id),
    conta_bancaria_id int8 REFERENCES public.contas_bancarias(id),
    -- Auditoria
    data_criacao_sistema timestamp(6) NULL,
    data_ultima_modificacao timestamp(6) NULL,
    criado_por varchar(255) NULL,
    modificado_por varchar(255) NULL,
    CONSTRAINT compras_estado_check CHECK (estado_pagamento IN ('PENDENTE', 'PAGO'))
);

CREATE TABLE public.movimentos_stock (
    id bigserial PRIMARY KEY,
    data_movimento timestamp(6) NOT NULL,
    tipo varchar(20) NOT NULL,
    quantidade numeric(10, 3) NOT NULL,
    motivo varchar(255) NOT NULL,
    stock_apos_movimento numeric(10, 3) NULL,
    mercadoria_id int8 NOT NULL REFERENCES public.artigos(id),
    utilizador_id int8 NOT NULL REFERENCES public.utilizadores(id),
    -- Auditoria
    data_criacao_sistema timestamp(6) NULL,
    data_ultima_modificacao timestamp(6) NULL,
    criado_por varchar(255) NULL,
    modificado_por varchar(255) NULL
);

CREATE TABLE public.vendas (
    id bigserial PRIMARY KEY,
    data_venda timestamp(6) NOT NULL,
    total_sem_iva numeric(10, 2) NULL,
    total_com_iva numeric(10, 2) NULL,
    estado_pagamento varchar(20) NOT NULL DEFAULT 'PENDENTE',
    cliente_id int8 NOT NULL REFERENCES public.clientes(id),
    centro_custo_id int8 REFERENCES public.centro_custo(id),
    seccao_homo_id int8 REFERENCES public.seccao_homo(id),
    conta_bancaria_id int8 REFERENCES public.contas_bancarias(id),
    utilizador_id int8 NOT NULL REFERENCES public.utilizadores(id),
    -- Auditoria
    data_criacao_sistema timestamp(6) NULL,
    data_ultima_modificacao timestamp(6) NULL,
    criado_por varchar(255) NULL,
    modificado_por varchar(255) NULL,
    CONSTRAINT vendas_estado_check CHECK (estado_pagamento IN ('PENDENTE', 'PAGO'))
);

CREATE TABLE public.linhas_venda (
    id bigserial PRIMARY KEY,
    venda_id int8 NOT NULL REFERENCES public.vendas(id),
    artigo_id int8 NOT NULL REFERENCES public.artigos(id),
    tx_iva_id int8 NOT NULL REFERENCES public.tx_iva(id),
    quantidade numeric(10, 3) NOT NULL,
    preco_unitario numeric(10, 2) NOT NULL,
    total_linha_sem_iva numeric(10, 2) NOT NULL,
    total_linha_com_iva numeric(10, 2) NOT NULL,
    designacao_personalizada varchar(255) NULL
);

CREATE TABLE public.movimentos_tesouraria (
    id bigserial PRIMARY KEY,
    data_movimento timestamp(6) NOT NULL,
    descricao varchar(255) NOT NULL,
    tipo varchar(20) NOT NULL,
    valor numeric(12, 2) NOT NULL,
    saldo_apos numeric(12, 2) NULL,
    conta_bancaria_id int8 NOT NULL REFERENCES public.contas_bancarias(id),
    utilizador_id int8 NOT NULL REFERENCES public.utilizadores(id),
    compra_id int8 REFERENCES public.compras(id),
    fornecedor_id int8 REFERENCES public.fornecedores(id),
    venda_id int8 REFERENCES public.vendas(id),
    cliente_id int8 REFERENCES public.clientes(id),
    -- Auditoria
    data_criacao_sistema timestamp(6) NULL,
    data_ultima_modificacao timestamp(6) NULL,
    criado_por varchar(255) NULL,
    modificado_por varchar(255) NULL
);

INSERT INTO public.tx_iva (descricao, valor, data_criacao_sistema)
VALUES
    ('Taxa Normal (23%)', 23.00, CURRENT_TIMESTAMP),
    ('Taxa Intermédia (13%)', 13.00, CURRENT_TIMESTAMP),
    ('Taxa Reduzida (6%)', 6.00, CURRENT_TIMESTAMP),
    ('Isento (0%)', 0.00, CURRENT_TIMESTAMP);