-- V6__limpar_tabela_planeamento.sql

-- 1. Limpeza radical na tabela de planeamento (Transformação em Cash Flow puro)
ALTER TABLE movimentos_planeados
DROP COLUMN IF EXISTS artigo_id,
DROP COLUMN IF EXISTS centro_custo_id,
DROP COLUMN IF EXISTS seccao_homo_id,
DROP COLUMN IF EXISTS cliente_id,
DROP COLUMN IF EXISTS fornecedor_id;

-- 2. Isolamento das Vendas (Remove o cordão umbilical com o planeamento)
ALTER TABLE vendas
DROP COLUMN IF EXISTS movimento_planeado_id;

-- 3. Isolamento das Compras (Remove o cordão umbilical com o planeamento)
ALTER TABLE compras
DROP COLUMN IF EXISTS movimento_planeado_id;