-- Vendedores: mismos datos que una clienta. Son la contraparte de las compras.
CREATE TABLE viv_seller (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL REFERENCES viv_store (id),
    full_name VARCHAR(150) NOT NULL,
    account VARCHAR(100) NOT NULL DEFAULT '',
    phone VARCHAR(30) NOT NULL DEFAULT '',
    email VARCHAR(150) NOT NULL DEFAULT '',
    address VARCHAR(255) NOT NULL DEFAULT '',
    department VARCHAR(30),
    delivery_type VARCHAR(20),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_viv_seller_department CHECK (department IN (
        'ARTIGAS', 'CANELONES', 'CERRO_LARGO', 'COLONIA', 'DURAZNO', 'FLORES', 'FLORIDA',
        'LAVALLEJA', 'MALDONADO', 'MONTEVIDEO', 'PAYSANDU', 'RIO_NEGRO', 'RIVERA', 'ROCHA',
        'SALTO', 'SAN_JOSE', 'SORIANO', 'TACUAREMBO', 'TREINTA_Y_TRES')),
    CONSTRAINT ck_viv_seller_delivery_type CHECK (delivery_type IN ('CADETERIA', 'AGENCIA', 'DOMICILIO'))
);

CREATE INDEX idx_viv_seller_store_id ON viv_seller (store_id);

-- Compras y ventas del negocio: una venta (a una clienta) o una compra (a un vendedor).
CREATE TABLE viv_trade (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL REFERENCES viv_store (id),
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    quantity INTEGER NOT NULL,
    price NUMERIC(12, 2) NOT NULL,
    note VARCHAR(500) NOT NULL DEFAULT '',
    seller_id UUID REFERENCES viv_seller (id),
    client_id UUID REFERENCES viv_client (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_viv_trade_type CHECK (type IN ('SALE', 'PURCHASE')),
    CONSTRAINT ck_viv_trade_status CHECK (status IN ('PUBLISHED', 'SOLD', 'PENDING', 'PURCHASED')),
    CONSTRAINT ck_viv_trade_quantity CHECK (quantity > 0),
    CONSTRAINT ck_viv_trade_price CHECK (price >= 0),
    CONSTRAINT ck_viv_trade_counterpart CHECK (
        (type = 'SALE' AND status IN ('PUBLISHED', 'SOLD') AND client_id IS NOT NULL AND seller_id IS NULL)
        OR (type = 'PURCHASE' AND status IN ('PENDING', 'PURCHASED') AND seller_id IS NOT NULL AND client_id IS NULL))
);

CREATE INDEX idx_viv_trade_store_id ON viv_trade (store_id);
CREATE INDEX idx_viv_trade_seller_id ON viv_trade (seller_id);
CREATE INDEX idx_viv_trade_client_id ON viv_trade (client_id);
