ALTER TABLE militar ADD COLUMN funcao VARCHAR(50);

ALTER TABLE movimentacao
    ADD COLUMN militar_paiol_retirada_id BIGINT,
    ADD COLUMN militar_paiol_recebimento_id BIGINT,
    ADD COLUMN oficial_municao_om_detentora_id BIGINT;

ALTER TABLE movimentacao
    ADD CONSTRAINT fk_movimentacao_militar_paiol_retirada
        FOREIGN KEY (militar_paiol_retirada_id) REFERENCES militar(id),
    ADD CONSTRAINT fk_movimentacao_militar_paiol_recebimento
        FOREIGN KEY (militar_paiol_recebimento_id) REFERENCES militar(id),
    ADD CONSTRAINT fk_movimentacao_oficial_municao_detentora
        FOREIGN KEY (oficial_municao_om_detentora_id) REFERENCES militar(id);

CREATE INDEX idx_movimentacao_militar_paiol_retirada ON movimentacao(militar_paiol_retirada_id);
CREATE INDEX idx_movimentacao_militar_paiol_recebimento ON movimentacao(militar_paiol_recebimento_id);
CREATE INDEX idx_movimentacao_oficial_municao_detentora ON movimentacao(oficial_municao_om_detentora_id);
CREATE INDEX idx_militar_funcao_ativo_om ON militar(funcao, ativo, organizacao_militar_id);
