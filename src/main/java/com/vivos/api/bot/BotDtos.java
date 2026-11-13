package com.vivos.api.bot;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/** Cuerpos de {@code /bot/**}. Los {@code text} vienen armados para mandarlos tal cual por WhatsApp. */
public final class BotDtos {

    private BotDtos() {
    }

    public record BotClientResponse(UUID clientId, String fullName, String storeName) {
    }

    /**
     * Deuda del ultimo vivo en que compro. Sin compras: {@code hasPurchase} false, montos en 0
     * y el resto null.
     */
    public record BotDebtResponse(boolean hasPurchase, Integer liveNumber, String liveTitle, Integer packageNumber,
                                  BigDecimal total, BigDecimal owed, boolean paid, int items, String text) {
    }

    /**
     * Auto-registro. Como lo completa la clienta por WhatsApp, {@code department} y
     * {@code deliveryType} son texto libre: si no se reconocen quedan vacios.
     */
    public record BotRegisterRequest(

            @NotNull(message = "El nombre es obligatorio")
            @Pattern(regexp = "(?s).*\\S.*", message = "El nombre es obligatorio")
            @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
            String fullName,

            @Size(max = 100, message = "La cuenta no puede superar 100 caracteres")
            String account,

            @Size(max = 150, message = "El correo no puede superar 150 caracteres")
            String email,

            @Size(max = 255, message = "La direccion no puede superar 255 caracteres")
            String address,

            @Size(max = 60)
            String department,

            @Size(max = 30)
            String deliveryType
    ) {
    }
}
