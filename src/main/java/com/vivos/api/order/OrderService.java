package com.vivos.api.order;

import com.vivos.api.client.ClientService;
import com.vivos.api.common.ConflictException;
import com.vivos.api.common.NotFoundException;
import com.vivos.api.live.Live;
import com.vivos.api.live.LiveService;
import com.vivos.api.live.LiveStatus;
import com.vivos.api.store.StoreRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    static final String ORDER_NOT_FOUND = "Venta no encontrada";
    static final String LIVE_NOT_STARTED = "El vivo todavía no comenzó";
    private static final Sort LIST_ORDER = Sort.by(Sort.Direction.DESC, "number");

    private final OrderRepository orderRepository;
    private final StoreRepository storeRepository;
    private final LiveService liveService;
    private final ClientService clientService;
    private final ZoneId timezone;

    OrderService(OrderRepository orderRepository, StoreRepository storeRepository,
                 LiveService liveService, ClientService clientService,
                 @Value("${vivos.timezone:America/Montevideo}") String timezone) {
        this.orderRepository = orderRepository;
        this.storeRepository = storeRepository;
        this.liveService = liveService;
        this.clientService = clientService;
        this.timezone = ZoneId.of(timezone);
    }

    /** Ventas con items que cumplen todos los filtros, de numero mayor a menor. */
    @Transactional(readOnly = true)
    public List<OrderResponse> list(UUID storeId, OrderFilter filter) {
        return orderRepository.findAll(OrderSpecifications.matching(storeId, filter, timezone), LIST_ORDER).stream()
                .map(OrderResponse::from)
                .toList();
    }

    /** Totales de las mismas ventas que devuelve {@link #list}, de cualquier estado. */
    @Transactional(readOnly = true)
    public OrderSummaryResponse summary(UUID storeId, OrderFilter filter) {
        return orderRepository.summarize(OrderSpecifications.matching(storeId, filter, timezone));
    }

    /** Todas las ventas del vivo (incluidas las vacias), por numero. */
    @Transactional(readOnly = true)
    public List<OrderResponse> listByLive(UUID storeId, UUID liveId) {
        liveService.findLive(storeId, liveId);
        return orderRepository.findByStoreIdAndLiveIdOrderByNumberAsc(storeId, liveId).stream()
                .map(OrderResponse::from)
                .toList();
    }

    /**
     * Crea o reemplaza la venta de la clienta en el vivo (upsert por vivo + clienta).
     * Al crear asigna el siguiente numero correlativo de la tienda; para eso bloquea la fila
     * de la tienda durante la transaccion.
     */
    @Transactional
    public OrderResponse save(UUID storeId, UUID liveId, UUID clientId, SaveOrderRequest request) {
        Live live = liveService.findLive(storeId, liveId);
        if (live.getStatus() == LiveStatus.NOT_STARTED) {
            throw new ConflictException(LIVE_NOT_STARTED);
        }
        clientService.findClient(storeId, clientId);

        storeRepository.lockById(storeId);
        Order order = orderRepository.findByStoreIdAndLiveIdAndClientId(storeId, liveId, clientId)
                .orElseGet(() -> new Order(storeId, orderRepository.findMaxNumber(storeId) + 1, liveId, clientId));

        order.setStatus(request.status());
        if (request.packageStatus() != null) {
            order.setPackageStatus(request.packageStatus());
        } else if (order.getPackageStatus() == null) {
            order.setPackageStatus(PackageStatus.PENDING);
        }
        order.replaceItems(request.items().stream()
                .map(item -> new OrderItem(storeId, item.codeOrEmpty(), item.nameOrEmpty(),
                        item.colorOrEmpty(), item.price(), item.quantityOrDefault()))
                .toList());
        order.touch();

        return OrderResponse.from(orderRepository.saveAndFlush(order));
    }

    @Transactional
    public OrderResponse update(UUID storeId, UUID id, UpdateOrderRequest request) {
        Order order = orderRepository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new NotFoundException(ORDER_NOT_FOUND));
        if (request.status() != null) {
            order.setStatus(request.status());
        }
        if (request.packageStatus() != null) {
            order.setPackageStatus(request.packageStatus());
        }
        order.touch();
        return OrderResponse.from(orderRepository.saveAndFlush(order));
    }
}
