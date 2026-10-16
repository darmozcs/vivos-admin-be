package com.vivos.api.live;

import com.vivos.api.order.LiveTotals;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** {@code LiveSummary} del front: el vivo mas los totales de venta calculados por el servidor. */
public record LiveSummaryResponse(
        UUID id,
        int number,
        String title,
        String platform,
        LiveStatus status,
        Instant startedAt,
        Instant endedAt,
        long accumulatedMs,
        Instant runningSince,
        Instant createdAt,
        long buyers,
        long units,
        BigDecimal totalAmount,
        List<UUID> tagIds
) {

    public static LiveSummaryResponse from(Live live, LiveTotals totals) {
        LiveTotals safe = totals != null ? totals : LiveTotals.empty(live.getId());
        return new LiveSummaryResponse(
                live.getId(),
                live.getNumber(),
                live.getTitle(),
                live.getPlatform(),
                live.getStatus(),
                live.getStartedAt(),
                live.getEndedAt(),
                live.getAccumulatedMs(),
                live.getRunningSince(),
                live.getCreatedAt(),
                safe.buyers(),
                safe.units(),
                safe.totalAmount(),
                live.getTagIds().stream().sorted().toList()
        );
    }
}
