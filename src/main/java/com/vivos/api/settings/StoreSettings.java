package com.vivos.api.settings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Configuracion de la tienda. {@code storeName} es {@code viv_store.name}. */
public record StoreSettings(

        @NotBlank(message = "El nombre de la tienda es obligatorio")
        @Size(max = 60, message = "El nombre de la tienda no puede superar 60 caracteres")
        String storeName
) {
}
