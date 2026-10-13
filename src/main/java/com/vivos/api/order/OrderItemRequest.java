package com.vivos.api.order;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Solo {@code price} es obligatorio: los textos faltantes se guardan vacios y la cantidad por defecto es 1. */
public record OrderItemRequest(

        @Size(max = 30, message = "El codigo del articulo no puede superar 30 caracteres")
        String code,

        @Size(max = 150, message = "El nombre del articulo no puede superar 150 caracteres")
        String name,

        @Size(max = 60, message = "El color no puede superar 60 caracteres")
        String color,

        @NotNull(message = "El precio es obligatorio")
        @PositiveOrZero(message = "El precio no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El precio no es valido")
        BigDecimal price,

        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        Integer quantity
) {

    static final int DEFAULT_QUANTITY = 1;

    String codeOrEmpty() {
        return trimOrEmpty(code);
    }

    String nameOrEmpty() {
        return trimOrEmpty(name);
    }

    String colorOrEmpty() {
        return trimOrEmpty(color);
    }

    int quantityOrDefault() {
        return quantity == null ? DEFAULT_QUANTITY : quantity;
    }

    private static String trimOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
