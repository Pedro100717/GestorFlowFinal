-- Cria a tabela secundária para guardar as datas ignoradas de cada plano fixo (Exceções / Regra da Máquina do Tempo)
CREATE TABLE movimentos_planeados_excecoes (
    movimento_planeado_id BIGINT NOT NULL,
    data_excecao DATE NOT NULL,
    CONSTRAINT fk_movimento_planeado_excecao
        FOREIGN KEY (movimento_planeado_id)
        REFERENCES movimentos_planeados (id)
        ON DELETE CASCADE
);