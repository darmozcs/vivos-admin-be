package com.vivos.api.bot;

import com.vivos.api.bot.BotContextResolver.BotContext;
import com.vivos.api.bot.BotDtos.BotClientResponse;
import com.vivos.api.bot.BotDtos.BotDebtResponse;
import com.vivos.api.bot.BotDtos.BotRegisterRequest;
import com.vivos.api.client.Client;
import com.vivos.api.client.ClientRepository;
import com.vivos.api.client.ClientRequest;
import com.vivos.api.client.ClientResponse;
import com.vivos.api.client.ClientService;
import com.vivos.api.client.DeliveryType;
import com.vivos.api.client.Department;
import com.vivos.api.client.PhoneNumbers;
import com.vivos.api.common.ConflictException;
import com.vivos.api.common.NotFoundException;
import com.vivos.api.live.Live;
import com.vivos.api.live.LiveRepository;
import com.vivos.api.order.Amounts;
import com.vivos.api.order.Order;
import com.vivos.api.order.OrderItem;
import com.vivos.api.order.OrderRepository;
import com.vivos.api.order.OrderStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Consultas del bot de WhatsApp. Quien escribe se identifica por su numero: tiene que
 * ser clienta de la tienda del bot (la del WABA ID), si no es 404.
 */
@Service
public class BotService {

    static final String NOT_A_CLIENT = "No encontramos tu numero como clienta de esta tienda";
    static final String ALREADY_REGISTERED = "Ya estas registrada en esta tienda";

    private final ClientRepository clientRepository;
    private final ClientService clientService;
    private final OrderRepository orderRepository;
    private final LiveRepository liveRepository;

    BotService(ClientRepository clientRepository, ClientService clientService, OrderRepository orderRepository,
               LiveRepository liveRepository) {
        this.clientRepository = clientRepository;
        this.clientService = clientService;
        this.orderRepository = orderRepository;
        this.liveRepository = liveRepository;
    }

    @Transactional(readOnly = true)
    public BotClientResponse me(BotContext context) {
        Client client = clientsOf(context).getFirst();
        return new BotClientResponse(client.getId(), client.getFullName(), context.store().getName());
    }

    /**
     * Pedido del ultimo vivo (el de numero mas alto) en que compro, sin contar los cancelados.
     * Si esta pendiente debe el total; si esta pago no debe nada. Si hay varias clientas con
     * el mismo numero en la tienda se toman como la misma persona.
     */
    @Transactional(readOnly = true)
    public BotDebtResponse debt(BotContext context) {
        UUID storeId = context.store().getId();
        Optional<Order> last = clientsOf(context).stream()
                .flatMap(client -> orderRepository.findByStoreIdAndClientId(storeId, client.getId()).stream())
                .filter(order -> order.getStatus() != OrderStatus.CANCELLED)
                .max(Comparator.comparingInt(Order::getLiveNumber).thenComparing(Order::getCreatedAt));
        if (last.isEmpty()) {
            return new BotDebtResponse(false, null, null, null, BigDecimal.ZERO, BigDecimal.ZERO, false, 0,
                    "Todavía no tenés compras en nuestros vivos.");
        }

        Order order = last.get();
        BigDecimal total = order.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int items = order.getItems().stream().mapToInt(OrderItem::getQuantity).sum();
        boolean paid = order.getStatus() == OrderStatus.PAID;
        BigDecimal owed = paid ? BigDecimal.ZERO : total;
        String liveTitle = liveRepository.findByIdAndStoreId(order.getLiveId(), storeId).map(Live::getTitle)
                .orElse(null);
        return new BotDebtResponse(true, order.getLiveNumber(), liveTitle, order.getNumber(), Amounts.normalize(total),
                Amounts.normalize(owed), paid,
                items, debtText(order, liveTitle, total, items, paid));
    }

    /** Alta de la clienta con el numero de WhatsApp (ya verificado por Meta), sin duplicarla. */
    @Transactional
    public BotClientResponse register(BotContext context, BotRegisterRequest request) {
        UUID storeId = context.store().getId();
        if (!clientRepository.findByStoreIdAndPhoneNormalizedOrderByCreatedAtAscIdAsc(storeId, context.phone())
                .isEmpty()) {
            throw new ConflictException(ALREADY_REGISTERED);
        }
        ClientResponse created = clientService.create(storeId, new ClientRequest(
                request.fullName(),
                request.account(),
                PhoneNumbers.toLocal(context.phone()),
                request.email(),
                request.address(),
                parseEnum(Department.class, request.department()),
                parseEnum(DeliveryType.class, request.deliveryType())));
        return new BotClientResponse(created.id(), created.fullName(), context.store().getName());
    }

    private List<Client> clientsOf(BotContext context) {
        List<Client> clients = clientRepository.findByStoreIdAndPhoneNormalizedOrderByCreatedAtAscIdAsc(
                context.store().getId(), context.phone());
        if (clients.isEmpty()) {
            throw new NotFoundException(NOT_A_CLIENT);
        }
        return clients;
    }

    private static String debtText(Order order, String liveTitle, BigDecimal total, int items, boolean paid) {
        String live = "vivo #" + order.getLiveNumber() + (liveTitle == null ? "" : " (" + liveTitle + ")");
        String garments = items + (items == 1 ? " prenda" : " prendas");
        String pack = order.getNumber() == null ? "" : " Tu paquete es el número " + order.getNumber() + ".";
        if (paid) {
            return "Tu compra del " + live + " ya está paga ✅ (" + money(total) + " por " + garments + ")." + pack;
        }
        return "Del " + live + " debés " + money(total) + " por " + garments + "." + pack;
    }

    /** Formato de Uruguay: $ 1.250 o $ 1.250,50. */
    static String money(BigDecimal amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat format = new DecimalFormat("#,##0.##", symbols);
        format.setMinimumFractionDigits(amount.stripTrailingZeros().scale() > 0 ? 2 : 0);
        return "$ " + format.format(amount);
    }

    /** "Treinta y Tres", "paysandú" o "CERRO_LARGO" sirven igual. Si no coincide: null. */
    static <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String key = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_|_$", "");
        return Arrays.stream(type.getEnumConstants()).filter(constant -> constant.name().equals(key)).findFirst()
                .orElse(null);
    }
}
