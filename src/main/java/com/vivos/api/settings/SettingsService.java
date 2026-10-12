package com.vivos.api.settings;

import com.vivos.api.common.NotFoundException;
import com.vivos.api.store.Store;
import com.vivos.api.store.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SettingsService {

    private final StoreRepository storeRepository;

    SettingsService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    @Transactional(readOnly = true)
    public StoreSettings get(UUID storeId) {
        return new StoreSettings(findStore(storeId).getName());
    }

    @Transactional
    public StoreSettings update(UUID storeId, StoreSettings request) {
        Store store = findStore(storeId);
        store.setName(request.storeName().trim());
        return new StoreSettings(storeRepository.save(store).getName());
    }

    /** El storeId siempre viene de {@code @CurrentStore}, nunca del cliente. */
    private Store findStore(UUID storeId) {
        return storeRepository.findById(storeId).orElseThrow(() -> new NotFoundException("Tienda no encontrada"));
    }
}
