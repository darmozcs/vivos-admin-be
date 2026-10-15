-- Numero de paquete: se asigna cuando la clienta recibe su primera compra en el vivo y se
-- pierde si la venta se queda sin items. Cada vivo tiene un contador que solo sube.

ALTER TABLE viv_order ALTER COLUMN number DROP NOT NULL;

-- Las ventas sin items no tienen numero.
UPDATE viv_order o SET number = NULL
WHERE NOT EXISTS (SELECT 1 FROM viv_order_item i WHERE i.order_id = o.id);

ALTER TABLE viv_live ADD COLUMN next_package_number INTEGER NOT NULL DEFAULT 1;
UPDATE viv_live l SET next_package_number = COALESCE(
    (SELECT MAX(o.number) FROM viv_order o WHERE o.live_id = l.id), 0) + 1;
