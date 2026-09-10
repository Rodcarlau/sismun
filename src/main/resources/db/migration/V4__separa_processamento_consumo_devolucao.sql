ALTER TABLE item_devolucao
    ADD COLUMN consumo_processado BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN devolucao_processada BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE item_devolucao
SET consumo_processado = processado,
    devolucao_processada = processado;
