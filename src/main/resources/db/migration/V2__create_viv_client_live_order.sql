CREATE TABLE viv_client (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL REFERENCES viv_store (id),
    full_name VARCHAR(150) NOT NULL,
    account VARCHAR(100) NOT NULL DEFAULT '',
    phone VARCHAR(30) NOT NULL DEFAULT '',
    address VARCHAR(255) NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_viv_client_store_id ON viv_client (store_id);

CREATE TABLE viv_live (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL REFERENCES viv_store (id),
    title VARCHAR(120) NOT NULL,
    platform VARCHAR(60) NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    accumulated_ms BIGINT NOT NULL DEFAULT 0,
    running_since TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_viv_live_status CHECK (status IN ('NOT_STARTED', 'RUNNING', 'PAUSED', 'ENDED'))
);

CREATE INDEX idx_viv_live_store_id ON viv_live (store_id);
CREATE INDEX idx_viv_live_store_created_at ON viv_live (store_id, created_at);

CREATE TABLE viv_order (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL REFERENCES viv_store (id),
    number INTEGER NOT NULL,
    live_id UUID NOT NULL REFERENCES viv_live (id),
    client_id UUID NOT NULL REFERENCES viv_client (id),
    status VARCHAR(20) NOT NULL,
    package_status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_viv_order_store_number UNIQUE (store_id, number),
    CONSTRAINT uq_viv_order_live_client UNIQUE (live_id, client_id),
    CONSTRAINT ck_viv_order_status CHECK (status IN ('PENDING', 'PAID', 'CANCELLED')),
    CONSTRAINT ck_viv_order_package_status CHECK (package_status IN ('PENDING', 'PREPARED', 'SHIPPED', 'DELIVERED'))
);

CREATE INDEX idx_viv_order_store_id ON viv_order (store_id);
CREATE INDEX idx_viv_order_live_id ON viv_order (live_id);
CREATE INDEX idx_viv_order_client_id ON viv_order (client_id);

CREATE TABLE viv_order_item (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL REFERENCES viv_store (id),
    order_id UUID NOT NULL REFERENCES viv_order (id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(150) NOT NULL,
    color VARCHAR(60) NOT NULL DEFAULT '',
    price NUMERIC(12, 2) NOT NULL,
    quantity INTEGER NOT NULL,
    CONSTRAINT ck_viv_order_item_price CHECK (price >= 0),
    CONSTRAINT ck_viv_order_item_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_viv_order_item_store_id ON viv_order_item (store_id);
CREATE INDEX idx_viv_order_item_order_id ON viv_order_item (order_id);
