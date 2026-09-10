ALTER TABLE item_devolucao ADD COLUMN paiol_destino_id BIGINT;
ALTER TABLE item_devolucao ADD CONSTRAINT fk_item_devolucao_paiol_destino
    FOREIGN KEY (paiol_destino_id) REFERENCES paiol(id);
CREATE INDEX idx_item_devolucao_paiol_destino ON item_devolucao(paiol_destino_id);
