package com.vivos.api.balance;

import com.vivos.api.common.BadRequestException;
import com.vivos.api.live.LiveFilter;
import com.vivos.api.live.LiveService;
import com.vivos.api.live.LiveStatus;
import com.vivos.api.order.Amounts;
import com.vivos.api.trade.TradeFilter;
import com.vivos.api.trade.TradeService;
import com.vivos.api.trade.TradeStatus;
import com.vivos.api.trade.TradeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Une vivos finalizados, ventas vendidas y compras compradas en una sola lista.
 * Un vivo cuenta como una venta por su total recaudado (ventas no canceladas).
 */
@Service
public class BalanceService {

    static final String INVALID_DATE_RANGE = "La fecha desde no puede ser posterior a la fecha hasta";
    private static final Comparator<BalanceEntryResponse> NEWEST_FIRST =
            Comparator.comparing(BalanceEntryResponse::createdAt).reversed();

    private final TradeService tradeService;
    private final LiveService liveService;

    BalanceService(TradeService tradeService, LiveService liveService) {
        this.tradeService = tradeService;
        this.liveService = liveService;
    }

    @Transactional(readOnly = true)
    public BalanceResponse get(UUID storeId, BalanceFilter filter) {
        if (filter.from() != null && filter.to() != null && filter.from().isAfter(filter.to())) {
            throw new BadRequestException(INVALID_DATE_RANGE);
        }
        List<BalanceEntryResponse> entries = new ArrayList<>();
        if (filter.includes(BalanceEntryType.SALE)) {
            entries.addAll(trades(storeId, filter, TradeType.SALE, TradeStatus.SOLD, BalanceEntryType.SALE));
        }
        if (filter.includes(BalanceEntryType.PURCHASE)) {
            entries.addAll(trades(storeId, filter, TradeType.PURCHASE, TradeStatus.PURCHASED, BalanceEntryType.PURCHASE));
        }
        if (filter.includes(BalanceEntryType.LIVE)) {
            entries.addAll(endedLives(storeId, filter));
        }
        entries.sort(NEWEST_FIRST);
        return new BalanceResponse(entries, summarize(entries));
    }

    private List<BalanceEntryResponse> trades(UUID storeId, BalanceFilter filter, TradeType type,
                                              TradeStatus status, BalanceEntryType entryType) {
        TradeFilter tradeFilter = new TradeFilter(filter.from(), filter.to(), null, type, status, null, null);
        return tradeService.list(storeId, tradeFilter).stream()
                .map(trade -> new BalanceEntryResponse(
                        trade.id(), entryType, trade.quantity(), trade.price(), trade.createdAt()))
                .toList();
    }

    private List<BalanceEntryResponse> endedLives(UUID storeId, BalanceFilter filter) {
        LiveFilter liveFilter = new LiveFilter(filter.from(), filter.to(),
                null, null, null, null, null, null, null, null, null);
        return liveService.list(storeId, liveFilter).stream()
                .filter(live -> live.status() == LiveStatus.ENDED)
                .map(live -> new BalanceEntryResponse(
                        live.id(), BalanceEntryType.LIVE, live.units(), live.totalAmount(), live.createdAt()))
                .toList();
    }

    private static BalanceSummaryResponse summarize(List<BalanceEntryResponse> entries) {
        BigDecimal sales = BigDecimal.ZERO;
        BigDecimal purchases = BigDecimal.ZERO;
        long sold = 0;
        long bought = 0;
        for (BalanceEntryResponse entry : entries) {
            if (entry.type() == BalanceEntryType.PURCHASE) {
                purchases = purchases.add(entry.amount());
                bought += entry.quantity();
            } else {
                sales = sales.add(entry.amount());
                sold += entry.quantity();
            }
        }
        return new BalanceSummaryResponse(
                Amounts.normalize(sales),
                Amounts.normalize(purchases),
                Amounts.normalize(sales.subtract(purchases)),
                sold,
                bought,
                bought - sold);
    }
}
