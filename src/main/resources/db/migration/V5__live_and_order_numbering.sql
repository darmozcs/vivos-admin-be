-- Numero correlativo del vivo por tienda; las ventas pasan a numerarse dentro de su vivo
-- y se identifican como "<numero del vivo>-<numero de la venta>".

-- Vivos existentes: se numeran por fecha de creacion dentro de cada tienda.
ALTER TABLE viv_live ADD COLUMN number INTEGER;
UPDATE viv_live l SET number = n.rn
FROM (SELECT id, row_number() OVER (PARTITION BY store_id ORDER BY created_at, id) AS rn FROM viv_live) n
WHERE l.id = n.id;
ALTER TABLE viv_live ALTER COLUMN number SET NOT NULL;
ALTER TABLE viv_live ADD CONSTRAINT uq_viv_live_store_number UNIQUE (store_id, number);

-- Ventas existentes: copian el numero de su vivo y se renumeran dentro de el, en el orden que tenian.
ALTER TABLE viv_order DROP CONSTRAINT uq_viv_order_store_number;
ALTER TABLE viv_order ADD COLUMN live_number INTEGER;
UPDATE viv_order o SET live_number = l.number, number = n.rn
FROM viv_live l,
     (SELECT id, row_number() OVER (PARTITION BY live_id ORDER BY number, id) AS rn FROM viv_order) n
WHERE o.live_id = l.id AND o.id = n.id;
ALTER TABLE viv_order ALTER COLUMN live_number SET NOT NULL;
ALTER TABLE viv_order ADD CONSTRAINT uq_viv_order_live_number UNIQUE (live_id, number);
CREATE INDEX idx_viv_order_store_live_number ON viv_order (store_id, live_number, number);
