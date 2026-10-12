-- Datos de entrega de la clienta. Ambos opcionales: las clientas existentes quedan en NULL.
ALTER TABLE viv_client ADD COLUMN department VARCHAR(30);
ALTER TABLE viv_client ADD COLUMN delivery_type VARCHAR(20);

ALTER TABLE viv_client ADD CONSTRAINT ck_viv_client_department CHECK (department IN (
    'ARTIGAS', 'CANELONES', 'CERRO_LARGO', 'COLONIA', 'DURAZNO', 'FLORES', 'FLORIDA',
    'LAVALLEJA', 'MALDONADO', 'MONTEVIDEO', 'PAYSANDU', 'RIO_NEGRO', 'RIVERA', 'ROCHA',
    'SALTO', 'SAN_JOSE', 'SORIANO', 'TACUAREMBO', 'TREINTA_Y_TRES'));

ALTER TABLE viv_client ADD CONSTRAINT ck_viv_client_delivery_type
    CHECK (delivery_type IN ('CADETERIA', 'AGENCIA', 'DOMICILIO'));
