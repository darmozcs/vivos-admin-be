package com.vivos.api.live;

import java.time.Instant;
import java.util.UUID;

/** {@code Live} del front (models/live.ts). */
public record LiveResponse(
        UUID id,
        int number,
        String title,
        String platform,
        LiveStatus status,
        Instant startedAt,
        Instant endedAt,
        long accumulatedMs,
        Instant runningSince,
        Instant createdAt
) {

    public static LiveResponse from(Live live) {
        return new LiveResponse(
                live.getId(),
                live.getNumber(),
                live.getTitle(),
                live.getPlatform(),
                live.getStatus(),
                live.getStartedAt(),
                live.getEndedAt(),
                live.getAccumulatedMs(),
                live.getRunningSince(),
                live.getCreatedAt()
        );
    }
}
