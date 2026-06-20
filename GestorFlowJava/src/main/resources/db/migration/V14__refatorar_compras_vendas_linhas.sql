-- =========================================================================
-- 1. CRIAR A NOVA ESTRUTURA PARA AS LINHAS DE COMPRA
-- =========================================================================
CREATE SEQUENCE public.linhas_compra_id_seq INCREMENT BY 1 MINVALUE 1 START 1 CACHE 1 NO CYCLE;

CREATE TABLE public.linhas_compra (
    id bigserial PRIMARY KEY,
    compra_id int8 NOT NULL REFERENCES public.compras(id) ON DELETE CASCADE,
    artigo_id int8 NOT NULL REFERENCES public.artigos(id),
    tx_iva_id int8 NOT NULL REFERENCES public.tx_iva(id),
    quantidade numeric(10, 3) NOT NULL,
    preco_unitario numeric(10, 2) NOT NULL,
    total_linha numeric(10, 2) NULL,
    centro_custo_id int8 REFERENCES public.centro_custo(id),
    seccao_homo_id int8 REFERENCES public.seccao_homo(id),
    designacao_personalizada varchar(255) NULL
);

-- =========================================================================
-- 2. AJUSTAR AS LINHAS DE VENDA PARA A NOVA CONTABILIDADE ANALÍTICA
-- =========================================================================
ALTER TABLE public.linhas_venda ADD COLUMN centro_custo_id int8 REFERENCES public.centro_custo(id);
ALTER TABLE public.linhas_venda ADD COLUMN seccao_homo_id int8 REFERENCES public.seccao_homo(id);


-- =========================================================================
-- 3. MIGRAÇÃO CIRÚRGICA DE DADOS (PROTEGER O HISTÓRICO EXISTENTE)
-- =========================================================================

-- 3.1. Mover os centros de custo do cabeçalho da Venda para as respetivas Linhas
UPDATE public.linhas_venda lv
SET centro_custo_id = v.centro_custo_id,
    seccao_homo_id = v.seccao_homo_id
FROM public.vendas v
WHERE lv.venda_id = v.id;

-- 3.2. Transformar as compras antigas "monolíticas" na sua 1ª linha automática
INSERT INTO public.linhas_compra (
    compra_id,
    artigo_id,
    tx_iva_id,
    quantidade,
    preco_unitario,
    total_linha,
    centro_custo_id,
    seccao_homo_id,
    designacao_personalizada
)
SELECT
    id,
    artigo_id,
    tx_iva_id,
    quantidade,
    preco_unitario,
    total,
    centro_custo_id,
    seccao_homo_id,
    designacao
FROM public.compras;


-- =========================================================================
-- 4. LIMPEZA DOS CABEÇALHOS (APAGAR COLUNAS OBSOLETAS)
-- =========================================================================
-- O valor "total" geral mantém-se na compra para sumarizar a fatura completa!

ALTER TABLE public.compras
    DROP COLUMN artigo_id,
    DROP COLUMN tx_iva_id,
    DROP COLUMN quantidade,
    DROP COLUMN preco_unitario,
    DROP COLUMN designacao,
    DROP COLUMN centro_custo_id,
    DROP COLUMN seccao_homo_id;

ALTER TABLE public.vendas
    DROP COLUMN centro_custo_id,
    DROP COLUMN seccao_homo_id;