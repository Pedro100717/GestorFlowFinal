-- Adicionar as colunas
ALTER TABLE compras ADD COLUMN data_vencimento TIMESTAMP;
ALTER TABLE vendas ADD COLUMN data_vencimento TIMESTAMP;

-- Proteger o passado: Faturas antigas assumem que o vencimento é igual à data de emissão
UPDATE compras SET data_vencimento = data_compra WHERE data_vencimento IS NULL;
UPDATE vendas SET data_vencimento = data_venda WHERE data_vencimento IS NULL;