ALTER TABLE paiol ADD COLUMN principal BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE paiol ADD COLUMN om_detentora_municao_id BIGINT;
ALTER TABLE paiol ADD CONSTRAINT fk_paiol_om_detentora_municao FOREIGN KEY (om_detentora_municao_id) REFERENCES organizacao_militar(id);
CREATE UNIQUE INDEX uk_paiol_unico_principal ON paiol (principal) WHERE principal = TRUE;
