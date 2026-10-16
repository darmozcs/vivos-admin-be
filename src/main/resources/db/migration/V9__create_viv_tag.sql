-- Etiquetas de la tienda para relacionar vivos y movimientos (ventas, compras y pagos) y filtrarlos.
CREATE TABLE viv_tag (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL REFERENCES viv_store (id),
    name VARCHAR(60) NOT NULL,
    color VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_viv_tag_store_id ON viv_tag (store_id);
-- El nombre no se repite dentro de la tienda (sin distinguir mayusculas).
CREATE UNIQUE INDEX uq_viv_tag_store_name ON viv_tag (store_id, lower(name));

-- Un vivo o un movimiento puede tener varias etiquetas. Borrar la etiqueta la quita de todos.
CREATE TABLE viv_live_tag (
    live_id UUID NOT NULL REFERENCES viv_live (id) ON DELETE CASCADE,
    tag_id UUID NOT NULL REFERENCES viv_tag (id) ON DELETE CASCADE,
    PRIMARY KEY (live_id, tag_id)
);

CREATE INDEX idx_viv_live_tag_tag_id ON viv_live_tag (tag_id);

CREATE TABLE viv_trade_tag (
    trade_id UUID NOT NULL REFERENCES viv_trade (id) ON DELETE CASCADE,
    tag_id UUID NOT NULL REFERENCES viv_tag (id) ON DELETE CASCADE,
    PRIMARY KEY (trade_id, tag_id)
);

CREATE INDEX idx_viv_trade_tag_tag_id ON viv_trade_tag (tag_id);
