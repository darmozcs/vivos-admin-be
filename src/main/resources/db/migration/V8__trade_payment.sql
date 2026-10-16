-- Movimientos: se agrega el Pago (a un vendedor, resta del balance como una compra y no lleva
-- cantidad de prendas). Sus estados son Pendiente y Pagado.

ALTER TABLE viv_trade ALTER COLUMN quantity DROP NOT NULL;

ALTER TABLE viv_trade DROP CONSTRAINT ck_viv_trade_type;
ALTER TABLE viv_trade ADD CONSTRAINT ck_viv_trade_type CHECK (type IN ('SALE', 'PURCHASE', 'PAYMENT'));

ALTER TABLE viv_trade DROP CONSTRAINT ck_viv_trade_status;
ALTER TABLE viv_trade ADD CONSTRAINT ck_viv_trade_status
    CHECK (status IN ('PUBLISHED', 'SOLD', 'PENDING', 'PURCHASED', 'PAID'));

ALTER TABLE viv_trade DROP CONSTRAINT ck_viv_trade_quantity;
ALTER TABLE viv_trade ADD CONSTRAINT ck_viv_trade_quantity CHECK (quantity IS NULL OR quantity > 0);

ALTER TABLE viv_trade DROP CONSTRAINT ck_viv_trade_counterpart;
ALTER TABLE viv_trade ADD CONSTRAINT ck_viv_trade_counterpart CHECK (
    (type = 'SALE' AND status IN ('PUBLISHED', 'SOLD') AND quantity IS NOT NULL
        AND client_id IS NOT NULL AND seller_id IS NULL)
    OR (type = 'PURCHASE' AND status IN ('PENDING', 'PURCHASED') AND quantity IS NOT NULL
        AND seller_id IS NOT NULL AND client_id IS NULL)
    OR (type = 'PAYMENT' AND status IN ('PENDING', 'PAID') AND quantity IS NULL
        AND seller_id IS NOT NULL AND client_id IS NULL));
