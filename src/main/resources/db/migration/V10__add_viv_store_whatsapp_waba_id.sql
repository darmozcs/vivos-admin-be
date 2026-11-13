-- Cuenta de WhatsApp Business (WABA ID de Meta) de la tienda. El bot de WhatsApp
-- (darmoz-whatsapp) manda este id en cada consulta y asi se sabe de que tienda es.
ALTER TABLE viv_store ADD COLUMN whatsapp_waba_id VARCHAR(30);

-- Una cuenta de WhatsApp pertenece a una sola tienda.
CREATE UNIQUE INDEX uq_viv_store_whatsapp_waba_id ON viv_store (whatsapp_waba_id)
    WHERE whatsapp_waba_id IS NOT NULL;
