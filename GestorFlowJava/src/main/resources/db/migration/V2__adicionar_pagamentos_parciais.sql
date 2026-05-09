-- 1. Adicionar as colunas novas (caso ainda não existam)
DO $$ 
BEGIN 
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='vendas' AND column_name='valor_pago') THEN
        ALTER TABLE vendas ADD COLUMN valor_pago DECIMAL(10,2) DEFAULT 0.00;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='compras' AND column_name='valor_pago') THEN
        ALTER TABLE compras ADD COLUMN valor_pago DECIMAL(10,2) DEFAULT 0.00;
    END IF;
END $$;

-- 2. Eliminar as restrições antigas que impedem o novo estado
ALTER TABLE compras DROP CONSTRAINT IF EXISTS compras_estado_check;
ALTER TABLE vendas DROP CONSTRAINT IF EXISTS vendas_estado_check;

-- 3. Criar as novas restrições que aceitam PARCIALMENTE_PAGO
ALTER TABLE compras ADD CONSTRAINT compras_estado_check 
    CHECK (estado_pagamento IN ('PENDENTE', 'PARCIALMENTE_PAGO', 'PAGO'));

ALTER TABLE vendas ADD CONSTRAINT vendas_estado_check 
    CHECK (estado_pagamento IN ('PENDENTE', 'PARCIALMENTE_PAGO', 'PAGO'));