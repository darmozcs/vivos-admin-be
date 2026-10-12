package com.vivos.api.admin.store;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** PATCH parcial: los campos nulos no se modifican. */
public record UpdateStoreRequest(

        @Pattern(regexp = "(?s).*\\S.*", message = "el nombre de la tienda no puede estar vacio")
        @Size(max = 60, message = "el nombre de la tienda no puede superar 60 caracteres")
        String name,

        Boolean active
) {
}
