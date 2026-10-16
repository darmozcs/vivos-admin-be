package com.vivos.api.tag;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Body de POST y PATCH /tags. El color es un hexadecimal "#RRGGBB". */
public record TagRequest(

        @NotNull(message = "El nombre es obligatorio")
        @Pattern(regexp = "(?s).*\\S.*", message = "El nombre es obligatorio")
        @Size(max = 60, message = "El nombre no puede superar 60 caracteres")
        String name,

        @NotNull(message = "El color es obligatorio")
        @Pattern(regexp = "#[0-9a-fA-F]{6}", message = "El color no es valido")
        String color
) {
}
