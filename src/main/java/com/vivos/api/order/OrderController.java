package com.vivos.api.order;

import com.vivos.api.store.CurrentStore;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
public class OrderController {

    private final OrderService orderService;

    OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/orders")
    List<OrderResponse> list(@CurrentStore UUID storeId,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                             @RequestParam(required = false) String liveTitle,
                             @RequestParam(required = false) UUID clientId,
                             @RequestParam(required = false) OrderStatus status,
                             @RequestParam(required = false) PackageStatus packageStatus) {
        return orderService.list(storeId, new OrderFilter(from, to, liveTitle, clientId, status, packageStatus));
    }

    /** Mismos filtros que GET /orders. */
    @GetMapping("/orders/summary")
    OrderSummaryResponse summary(@CurrentStore UUID storeId,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                 @RequestParam(required = false) String liveTitle,
                                 @RequestParam(required = false) UUID clientId,
                                 @RequestParam(required = false) OrderStatus status,
                                 @RequestParam(required = false) PackageStatus packageStatus) {
        return orderService.summary(storeId, new OrderFilter(from, to, liveTitle, clientId, status, packageStatus));
    }

    @PatchMapping("/orders/{id}")
    OrderResponse update(@CurrentStore UUID storeId, @PathVariable UUID id,
                         @RequestBody UpdateOrderRequest request) {
        return orderService.update(storeId, id, request);
    }

    @GetMapping("/lives/{liveId}/orders")
    List<OrderResponse> listByLive(@CurrentStore UUID storeId, @PathVariable UUID liveId) {
        return orderService.listByLive(storeId, liveId);
    }

    @PutMapping("/lives/{liveId}/orders/{clientId}")
    OrderResponse save(@CurrentStore UUID storeId, @PathVariable UUID liveId, @PathVariable UUID clientId,
                       @Valid @RequestBody SaveOrderRequest request) {
        return orderService.save(storeId, liveId, clientId, request);
    }
}
