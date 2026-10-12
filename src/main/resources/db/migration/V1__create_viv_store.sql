CREATE TABLE viv_store (
    id UUID PRIMARY KEY,
    name VARCHAR(60) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE viv_store_user (
    auth_user_id UUID PRIMARY KEY,
    store_id UUID NOT NULL REFERENCES viv_store (id),
    email VARCHAR(150) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_viv_store_user_store_id ON viv_store_user (store_id);
