package com.vivos.api.balance;

import java.util.List;

/** Filas del balance (de la mas nueva a la mas vieja) y sus totales. */
public record BalanceResponse(List<BalanceEntryResponse> entries, BalanceSummaryResponse summary) {
}
