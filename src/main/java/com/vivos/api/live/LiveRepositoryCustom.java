package com.vivos.api.live;

import com.vivos.api.order.LiveTotals;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/** Consultas de {@link LiveRepository} que Spring Data no deriva solo. */
public interface LiveRepositoryCustom {

    /**
     * Totales de los vivos de la tienda que cumplen {@code filter}, del mas nuevo al mas viejo.
     * Las fechas se interpretan como dias completos en {@code zone}; la duracion de un vivo en
     * curso se calcula hasta {@code now}.
     */
    List<LiveTotals> findMatchingTotals(UUID storeId, LiveFilter filter, ZoneId zone, Instant now);

    /** Totales agregados de los mismos vivos que {@link #findMatchingTotals}. */
    LivesSummaryResponse summarize(UUID storeId, LiveFilter filter, ZoneId zone, Instant now);
}
