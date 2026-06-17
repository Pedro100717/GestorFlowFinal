-- =========================================================================
-- 1. AJUSTES NA TABELA DE VENDAS
-- =========================================================================
-- Converter todas as colunas de data para DATE puro usando conversão forçada
ALTER TABLE public.vendas
    ALTER COLUMN data_venda TYPE DATE USING data_venda::date,
    ALTER COLUMN data_vencimento TYPE DATE USING data_vencimento::date,
    ALTER COLUMN data_prevista_pagamento TYPE DATE USING data_prevista_pagamento::date;


-- =========================================================================
-- 2. AJUSTES NA TABELA DE COMPRAS
-- =========================================================================
-- Converter todas as colunas de data para DATE puro usando conversão forçada
ALTER TABLE public.compras
    ALTER COLUMN data_compra TYPE DATE USING data_compra::date,
    ALTER COLUMN data_vencimento TYPE DATE USING data_vencimento::date,
    ALTER COLUMN data_prevista_pagamento TYPE DATE USING data_prevista_pagamento::date;


-- =========================================================================
-- 3. AJUSTES NA TABELA DE DOCUMENTOS DE TESOURARIA
-- =========================================================================
-- Converter a previsão de pagamento para DATE puro (exigido pelo teu TesourariaService)
ALTER TABLE public.documentos_tesouraria
    ALTER COLUMN data_prevista_pagamento TYPE DATE USING data_prevista_pagamento::date;