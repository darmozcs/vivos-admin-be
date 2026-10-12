package com.vivos.api.live;

import java.math.BigDecimal;

/**
 * Totales de {@code GET /lives/summary} sobre los vivos filtrados: lives = cantidad de vivos;
 * buyers, units y totalAmount = suma de los totales de cada vivo (una clienta que compro en
 * tres vivos cuenta tres veces).
 */
public record LivesSummaryResponse(long lives, long buyers, long units, BigDecimal totalAmount) {
}
