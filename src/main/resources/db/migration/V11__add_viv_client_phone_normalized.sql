-- Telefono de la clienta en formato internacional sin simbolos (59899123456), el mismo
-- que manda WhatsApp. Lo calcula PhoneNumbers.normalize al guardar; aca se completa
-- para las clientas existentes con la misma regla.
ALTER TABLE viv_client ADD COLUMN phone_normalized VARCHAR(30) NOT NULL DEFAULT '';

UPDATE viv_client c
SET phone_normalized = CASE
        WHEN s.x LIKE '598%' THEN s.x
        WHEN s.x ~ '^0[0-9]{8}$' THEN '598' || substr(s.x, 2)
        WHEN s.x ~ '^9[0-9]{7}$' THEN '598' || s.x
        ELSE s.x
    END
FROM (SELECT id, regexp_replace(regexp_replace(phone, '[^0-9]', '', 'g'), '^00', '') AS x
      FROM viv_client) s
WHERE s.id = c.id;

CREATE INDEX idx_viv_client_store_phone_normalized ON viv_client (store_id, phone_normalized);
