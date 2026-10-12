package com.vivos.api.admin.store;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateStoreRequest(

        @NotBlank(message = "el nombre de la tienda es obligatorio")
        @Size(max = 60, message = "el nombre de la tienda no puede superar 60 caracteres")
        String name,

        @NotBlank(message = "el email del administrador es obligatorio")
        @Email(message = "el email del administrador no es valido")
        @Size(max = 150, message = "el email del administrador no puede superar 150 caracteres")
        String adminEmail,

        @NotBlank(message = "la contraseña del administrador es obligatoria")
        @Size(min = 8, max = 100, message = "la contraseña debe tener entre 8 y 100 caracteres")
        String adminPassword
) {
}
