package com.vivos.api.settings;

import com.vivos.api.store.CurrentStore;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/settings")
public class SettingsController {

    private final SettingsService settingsService;

    SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    StoreSettings get(@CurrentStore UUID storeId) {
        return settingsService.get(storeId);
    }

    @PutMapping
    StoreSettings update(@CurrentStore UUID storeId, @Valid @RequestBody StoreSettings request) {
        return settingsService.update(storeId, request);
    }
}
