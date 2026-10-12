package com.vivos.api.order;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record OrderItemRequest(

        @NotBlank(message = "El codigo del articulo es obligatorio")
        @Size(max = 30, message = "El codigo del articulo no puede superar 30 caracteres")
        String code,

        @NotBlank(message = "El nombre del articulo es obligatorio")
        @Size(max = 150, message = "El nombre del articulo no puede superar 150 caracteres")
        String name,

        @Size(max = 60, message = "El color no puede superar 60 caracteres")
        String color,

        @NotNull(message = "El precio es obligatorio")
        @PositiveOrZero(message = "El precio no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El precio no es valido")
        BigDecimal price,

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        Integer quantity
) {
}
