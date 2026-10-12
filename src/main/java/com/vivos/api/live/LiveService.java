package com.vivos.api.live;

import com.vivos.api.common.ConflictException;
import com.vivos.api.common.NotFoundException;
import com.vivos.api.order.LiveTotals;
import com.vivos.api.order.OrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LiveService {

    public static final String LIVE_NOT_FOUND = "Vivo no encontrado";
    static final String NO_LIVES = "Todavia no hay vivos";
    static final String ACTION_NOT_ALLOWED = "La acción no está permitida en este estado";
    /** Plataforma fija, igual que MOCK_PLATFORM. */
    static final String PLATFORM = "Instagram Live";

    private final LiveRepository liveRepository;
    private final OrderRepository orderRepository;
    private final ZoneId timezone;

    LiveService(LiveRepository liveRepository, OrderRepository orderRepository,
                @Value("${vivos.timezone:America/Montevideo}") String timezone) {
        this.liveRepository = liveRepository;
        this.orderRepository = orderRepository;
        this.timezone = ZoneId.of(timezone);
    }

    /**
     * Historial de vivos que cumplen los filtros, mas nuevos primero, con sus totales de venta.
     * Filtrado y totales salen de una consulta; los vivos se cargan con otra.
     */
    @Transactional(readOnly = true)
    public List<LiveSummaryResponse> list(UUID storeId, LiveFilter filter) {
        filter.validate();
        List<LiveTotals> totals = liveRepository.findMatchingTotals(storeId, filter, timezone, Instant.now());
        if (totals.isEmpty()) {
            return List.of();
        }
        Map<UUID, Live> lives = liveRepository
                .findByStoreIdAndIdIn(storeId, totals.stream().map(LiveTotals::liveId).toList()).stream()
                .collect(Collectors.toMap(Live::getId, Function.identity()));
        return totals.stream()
                .map(row -> LiveSummaryResponse.from(lives.get(row.liveId()), row))
                .toList();
    }

    /** Totales de los mismos vivos que devuelve {@link #list}. */
    @Transactional(readOnly = true)
    public LivesSummaryResponse summary(UUID storeId, LiveFilter filter) {
        filter.validate();
        return liveRepository.summarize(storeId, filter, timezone, Instant.now());
    }

    /** El vivo actual (el ultimo creado). 404 si la tienda todavia no tiene vivos. */
    @Transactional(readOnly = true)
    public LiveResponse current(UUID storeId) {
        return liveRepository.findFirstByStoreIdOrderByCreatedAtDescIdDesc(storeId)
                .map(LiveResponse::from)
                .orElseThrow(() -> new NotFoundException(NO_LIVES));
    }

    @Transactional(readOnly = true)
    public LiveSummaryResponse get(UUID storeId, UUID id) {
        Live live = findLive(storeId, id);
        LiveTotals totals = orderRepository.totalsByLive(storeId, id).stream().findFirst().orElse(null);
        return LiveSummaryResponse.from(live, totals);
    }

    @Transactional
    public LiveResponse create(UUID storeId, CreateLiveRequest request) {
        Live live = new Live(storeId, request.title().trim(), PLATFORM);
        return LiveResponse.from(liveRepository.saveAndFlush(live));
    }

    @Transactional
    public LiveResponse applyAction(UUID storeId, UUID id, String actionPath) {
        LiveAction action = LiveAction.fromPath(actionPath)
                .orElseThrow(() -> new NotFoundException(LIVE_NOT_FOUND));
        Live live = findLive(storeId, id);
        if (!action.isAllowedFrom(live.getStatus())) {
            throw new ConflictException(ACTION_NOT_ALLOWED);
        }
        // Postgres guarda microsegundos: se trunca a milisegundos para que accumulatedMs cuadre.
        action.apply(live, Instant.now().truncatedTo(ChronoUnit.MILLIS));
        return LiveResponse.from(liveRepository.saveAndFlush(live));
    }

    public Live findLive(UUID storeId, UUID id) {
        return liveRepository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new NotFoundException(LIVE_NOT_FOUND));
    }
}
