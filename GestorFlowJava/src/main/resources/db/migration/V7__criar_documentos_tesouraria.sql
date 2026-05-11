CREATE TABLE documentos_tesouraria (
    id BIGSERIAL PRIMARY KEY,
    descricao VARCHAR(255) NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    valor_total DECIMAL(10,2) NOT NULL,
    valor_pago DECIMAL(10,2) DEFAULT 0 NOT NULL,
    data_emissao TIMESTAMP NOT NULL,
    estado_pagamento VARCHAR(50) NOT NULL,
    utilizador_id BIGINT NOT NULL REFERENCES utilizadores(id)
);

ALTER TABLE movimentos_tesouraria ADD COLUMN documento_tesouraria_id BIGINT REFERENCES documentos_tesouraria(id);