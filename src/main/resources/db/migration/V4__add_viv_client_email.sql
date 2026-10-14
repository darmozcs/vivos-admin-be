-- Correo de la clienta. Opcional: las clientas existentes quedan con cadena vacia.
ALTER TABLE viv_client ADD COLUMN email VARCHAR(150) NOT NULL DEFAULT '';
