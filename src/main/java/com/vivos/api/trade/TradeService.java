package com.vivos.api.trade;

import com.vivos.api.client.Client;
import com.vivos.api.client.ClientRepository;
import com.vivos.api.common.BadRequestException;
import com.vivos.api.common.NotFoundException;
import com.vivos.api.seller.Seller;
import com.vivos.api.seller.SellerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TradeService {

    static final String TRADE_NOT_FOUND = "Compra/venta no encontrada";
    static final String STATUS_NOT_ALLOWED = "El estado no corresponde al tipo de compra/venta";
    static final String TYPE_NOT_EDITABLE = "No se puede cambiar el tipo de una compra/venta";
    static final String CLIENT_REQUIRED = "La venta necesita una clienta";
    static final String SELLER_REQUIRED = "La compra necesita un vendedor";
    static final String SELLER_NOT_FOUND = "Vendedor no encontrado";
    static final String CLIENT_NOT_FOUND = "Clienta no encontrada";
    private static final Sort LIST_ORDER = Sort.by(Sort.Direction.DESC, "createdAt", "id");

    private final TradeRepository tradeRepository;
    private final SellerRepository sellerRepository;
    private final ClientRepository clientRepository;
    private final ZoneId timezone;

    TradeService(TradeRepository tradeRepository, SellerRepository sellerRepository, ClientRepository clientRepository,
               @Value("${vivos.timezone:America/Montevideo}") String timezone) {
        this.tradeRepository = tradeRepository;
        this.sellerRepository = sellerRepository;
        this.clientRepository = clientRepository;
        this.timezone = ZoneId.of(timezone);
    }

    /** Compras/ventas de la tienda que cumplen todos los filtros, del mas nuevo al mas viejo. */
    @Transactional(readOnly = true)
    public List<TradeResponse> list(UUID storeId, TradeFilter filter) {
        List<Trade> trades = tradeRepository.findAll(TradeSpecifications.matching(storeId, filter, timezone), LIST_ORDER);
        Map<UUID, String> sellerNames = sellerRepository.findAllById(ids(trades, Trade::getSellerId)).stream()
                .collect(Collectors.toMap(Seller::getId, Seller::getFullName));
        Map<UUID, String> clientNames = clientRepository.findAllById(ids(trades, Trade::getClientId)).stream()
                .collect(Collectors.toMap(Client::getId, Client::getFullName));
        return trades.stream()
                .map(trade -> TradeResponse.from(trade, sellerNames.get(trade.getSellerId()), clientNames.get(trade.getClientId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public TradeSummaryResponse summary(UUID storeId, TradeFilter filter) {
        return tradeRepository.summarize(TradeSpecifications.matching(storeId, filter, timezone));
    }

    @Transactional
    public TradeResponse create(UUID storeId, TradeRequest request) {
        Trade trade = new Trade(storeId, request.type());
        apply(storeId, trade, request);
        return toResponse(tradeRepository.save(trade));
    }

    @Transactional
    public TradeResponse update(UUID storeId, UUID id, TradeRequest request) {
        Trade trade = findTrade(storeId, id);
        if (trade.getType() != request.type()) {
            throw new BadRequestException(TYPE_NOT_EDITABLE);
        }
        apply(storeId, trade, request);
        return toResponse(tradeRepository.saveAndFlush(trade));
    }

    @Transactional
    public void delete(UUID storeId, UUID id) {
        tradeRepository.delete(findTrade(storeId, id));
    }

    private Trade findTrade(UUID storeId, UUID id) {
        return tradeRepository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new NotFoundException(TRADE_NOT_FOUND));
    }

    /** Valida estado y contraparte segun el tipo: venta a una clienta, compra a un vendedor. */
    private void apply(UUID storeId, Trade trade, TradeRequest request) {
        if (!request.status().allowedFor(trade.getType())) {
            throw new BadRequestException(STATUS_NOT_ALLOWED);
        }
        if (trade.getType() == TradeType.SALE) {
            if (request.clientId() == null) {
                throw new BadRequestException(CLIENT_REQUIRED);
            }
            if (!clientRepository.existsByIdAndStoreId(request.clientId(), storeId)) {
                throw new BadRequestException(CLIENT_NOT_FOUND);
            }
            trade.setClientId(request.clientId());
            trade.setSellerId(null);
        } else {
            if (request.sellerId() == null) {
                throw new BadRequestException(SELLER_REQUIRED);
            }
            if (!sellerRepository.existsByIdAndStoreId(request.sellerId(), storeId)) {
                throw new BadRequestException(SELLER_NOT_FOUND);
            }
            trade.setSellerId(request.sellerId());
            trade.setClientId(null);
        }
        trade.setStatus(request.status());
        trade.setQuantity(request.quantity());
        trade.setPrice(request.price());
        trade.setNote(request.noteOrEmpty());
    }

    private TradeResponse toResponse(Trade trade) {
        String sellerName = trade.getSellerId() == null ? null
                : sellerRepository.findById(trade.getSellerId()).map(Seller::getFullName).orElse(null);
        String clientName = trade.getClientId() == null ? null
                : clientRepository.findById(trade.getClientId()).map(Client::getFullName).orElse(null);
        return TradeResponse.from(trade, sellerName, clientName);
    }

    private static List<UUID> ids(Collection<Trade> trades, Function<Trade, UUID> getter) {
        return trades.stream().map(getter).filter(Objects::nonNull).distinct().toList();
    }
}
