package com.vivos.api.order;

import java.math.BigDecimal;

/** Montos sin ceros decimales de sobra: 1290.00 se serializa como 1290 y 99.50 como 99.5. */
public final class Amounts {

    private Amounts() {
    }

    public static BigDecimal normalize(BigDecimal amount) {
        BigDecimal stripped = amount.stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
    }
}
