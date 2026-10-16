package com.vivos.api.trade;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Body de POST y PATCH /trades. Una venta lleva {@code clientId}; una compra o un pago, {@code sellerId}.
 * {@code quantity} es obligatoria en ventas y compras y se ignora en los pagos.
 * El PATCH reemplaza todos los campos, pero el tipo no se puede cambiar.
 */
public record TradeRequest(

        @NotNull(message = "El tipo es obligatorio")
        TradeType type,

        @NotNull(message = "El estado es obligatorio")
        TradeStatus status,

        @Positive(message = "La cantidad debe ser al menos 1")
        Integer quantity,

        @NotNull(message = "El precio es obligatorio")
        @PositiveOrZero(message = "El precio no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El precio no es valido")
        BigDecimal price,

        @Size(max = 500, message = "La nota no puede superar 500 caracteres")
        String note,

        UUID sellerId,

        UUID clientId,

        /** Etiquetas del movimiento; ausente = sin etiquetas. */
        List<UUID> tagIds
) {

    String noteOrEmpty() {
        return note == null ? "" : note.trim();
    }
}
