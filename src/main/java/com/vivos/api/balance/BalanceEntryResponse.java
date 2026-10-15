package com.vivos.api.balance;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Una fila del balance. {@code amount} va siempre positivo: el signo depende de {@code type}. */
public record BalanceEntryResponse(
        UUID id,
        BalanceEntryType type,
        long quantity,
        BigDecimal amount,
        Instant createdAt
) {
}
