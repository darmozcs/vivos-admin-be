package com.vivos.api.admin.store;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetAdminPasswordRequest(

        @NotBlank(message = "la contraseña es obligatoria")
        @Size(min = 8, max = 100, message = "la contraseña debe tener entre 8 y 100 caracteres")
        String password
) {
}
