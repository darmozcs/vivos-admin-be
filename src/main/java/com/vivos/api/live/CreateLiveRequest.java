package com.vivos.api.live;

import jakarta.validation.constraints.Size;

/** El titulo es opcional: si queda vacio, al iniciar el vivo se le pone la fecha y hora de inicio. */
public record CreateLiveRequest(

        @Size(max = 120, message = "El titulo no puede superar 120 caracteres")
        String title
) {
}
