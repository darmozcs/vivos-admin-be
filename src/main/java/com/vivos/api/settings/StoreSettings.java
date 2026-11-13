package com.vivos.api.settings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Configuracion de la tienda. {@code storeName} es {@code viv_store.name}.
 * {@code whatsappWabaId} es la cuenta de WhatsApp Business (WABA ID de Meta) que usa el
 * bot de la tienda; opcional, vacio la desasocia.
 */
public record StoreSettings(

        @NotBlank(message = "El nombre de la tienda es obligatorio")
        @Size(max = 60, message = "El nombre de la tienda no puede superar 60 caracteres")
        String storeName,

        @Pattern(regexp = "\\s*|\\s*[0-9]{1,30}\\s*", message = "El ID de la cuenta de WhatsApp solo puede tener numeros")
        String whatsappWabaId
) {
}
