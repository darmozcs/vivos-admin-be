package com.vivos.api.balance;

import com.vivos.api.store.CurrentStore;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
public class BalanceController {

    private final BalanceService balanceService;

    BalanceController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @GetMapping("/balance")
    BalanceResponse get(@CurrentStore UUID storeId,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                        @RequestParam(required = false) BalanceEntryType type,
                        @RequestParam(required = false) List<UUID> tagIds) {
        return balanceService.get(storeId, new BalanceFilter(from, to, type, tagIds));
    }
}
