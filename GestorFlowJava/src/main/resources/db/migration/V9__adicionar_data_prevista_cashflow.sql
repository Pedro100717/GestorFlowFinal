-- Adicionar a coluna de maturidade financeira nas 3 tabelas core
ALTER TABLE compras ADD COLUMN data_prevista_pagamento TIMESTAMP;
ALTER TABLE vendas ADD COLUMN data_prevista_pagamento TIMESTAMP;
ALTER TABLE documentos_tesouraria ADD COLUMN data_prevista_pagamento TIMESTAMP;

-- Inicialização: O sistema assume que a previsão inicial é o vencimento (ou emissão)
UPDATE compras SET data_prevista_pagamento = data_compra;
UPDATE vendas SET data_prevista_pagamento = data_venda;
UPDATE documentos_tesouraria SET data_prevista_pagamento = data_emissao;