package com.vivos.api.live;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Acciones de {@code POST /lives/{id}/{action}}: estados desde los que se permiten y
 * cambios que aplican (igual que TRANSITIONS en lives.handlers.ts).
 */
public enum LiveAction {

    START("start", EnumSet.of(LiveStatus.NOT_STARTED)) {
        @Override
        void apply(Live live, Instant now) {
            live.setStatus(LiveStatus.RUNNING);
            live.setStartedAt(now);
            live.setRunningSince(now);
            live.setAccumulatedMs(0);
        }
    },
    PAUSE("pause", EnumSet.of(LiveStatus.RUNNING)) {
        @Override
        void apply(Live live, Instant now) {
            live.setStatus(LiveStatus.PAUSED);
            live.setAccumulatedMs(live.getAccumulatedMs() + elapsedSince(live.getRunningSince(), now));
            live.setRunningSince(null);
        }
    },
    RESUME("resume", EnumSet.of(LiveStatus.PAUSED)) {
        @Override
        void apply(Live live, Instant now) {
            live.setStatus(LiveStatus.RUNNING);
            live.setRunningSince(now);
        }
    },
    END("end", EnumSet.of(LiveStatus.RUNNING, LiveStatus.PAUSED)) {
        @Override
        void apply(Live live, Instant now) {
            live.setStatus(LiveStatus.ENDED);
            live.setAccumulatedMs(live.getAccumulatedMs() + elapsedSince(live.getRunningSince(), now));
            live.setRunningSince(null);
            live.setEndedAt(now);
        }
    };

    private final String path;
    private final Set<LiveStatus> from;

    LiveAction(String path, Set<LiveStatus> from) {
        this.path = path;
        this.from = from;
    }

    abstract void apply(Live live, Instant now);

    boolean isAllowedFrom(LiveStatus status) {
        return from.contains(status);
    }

    static Optional<LiveAction> fromPath(String value) {
        return Arrays.stream(values()).filter(action -> action.path.equals(value)).findFirst();
    }

    private static long elapsedSince(Instant since, Instant now) {
        return since == null ? 0 : Duration.between(since, now).toMillis();
    }
}
