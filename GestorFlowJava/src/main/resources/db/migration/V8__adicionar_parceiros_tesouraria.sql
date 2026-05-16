ALTER TABLE movimentos_planeados ADD COLUMN cliente_id BIGINT REFERENCES clientes(id);
ALTER TABLE movimentos_planeados ADD COLUMN fornecedor_id BIGINT REFERENCES fornecedores(id);

ALTER TABLE documentos_tesouraria ADD COLUMN cliente_id BIGINT REFERENCES clientes(id);
ALTER TABLE documentos_tesouraria ADD COLUMN fornecedor_id BIGINT REFERENCES fornecedores(id);