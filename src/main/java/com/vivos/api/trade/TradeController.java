package com.vivos.api.trade;

import com.vivos.api.store.CurrentStore;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/trades")
public class TradeController {

    private final TradeService tradeService;

    TradeController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @GetMapping
    List<TradeResponse> list(@CurrentStore UUID storeId,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                           @RequestParam(required = false) String search,
                           @RequestParam(required = false) TradeType type,
                           @RequestParam(required = false) TradeStatus status,
                           @RequestParam(required = false) UUID sellerId,
                           @RequestParam(required = false) UUID clientId,
                           @RequestParam(required = false) List<UUID> tagIds) {
        return tradeService.list(storeId, new TradeFilter(from, to, search, type, status, sellerId, clientId, tagIds));
    }

    /** Mismos filtros que GET /trades. */
    @GetMapping("/summary")
    TradeSummaryResponse summary(@CurrentStore UUID storeId,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                               @RequestParam(required = false) String search,
                               @RequestParam(required = false) TradeType type,
                               @RequestParam(required = false) TradeStatus status,
                               @RequestParam(required = false) UUID sellerId,
                               @RequestParam(required = false) UUID clientId,
                               @RequestParam(required = false) List<UUID> tagIds) {
        return tradeService.summary(storeId, new TradeFilter(from, to, search, type, status, sellerId, clientId, tagIds));
    }

    @PostMapping
    ResponseEntity<TradeResponse> create(@CurrentStore UUID storeId, @Valid @RequestBody TradeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tradeService.create(storeId, request));
    }

    @PatchMapping("/{id}")
    TradeResponse update(@CurrentStore UUID storeId, @PathVariable UUID id, @Valid @RequestBody TradeRequest request) {
        return tradeService.update(storeId, id, request);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@CurrentStore UUID storeId, @PathVariable UUID id) {
        tradeService.delete(storeId, id);
        return ResponseEntity.noContent().build();
    }
}
