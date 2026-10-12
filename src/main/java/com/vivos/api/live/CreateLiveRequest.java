package com.vivos.api.live;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateLiveRequest(

        @NotNull(message = "El titulo es obligatorio")
        @Pattern(regexp = "(?s).*\\S.*", message = "El titulo es obligatorio")
        @Size(max = 120, message = "El titulo no puede superar 120 caracteres")
        String title
) {
}
