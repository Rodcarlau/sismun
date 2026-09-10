ALTER TABLE lote_municao
    DROP CONSTRAINT uk_lote_municao;

ALTER TABLE lote_municao
    ADD CONSTRAINT uk_lote_municao
    UNIQUE (municao_id, lote, virola);
