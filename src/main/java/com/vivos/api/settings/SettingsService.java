package com.vivos.api.settings;

import com.vivos.api.common.ConflictException;
import com.vivos.api.common.NotFoundException;
import com.vivos.api.store.Store;
import com.vivos.api.store.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SettingsService {

    static final String WABA_ID_IN_USE = "Esa cuenta de WhatsApp ya esta asociada a otra tienda";

    private final StoreRepository storeRepository;

    SettingsService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    @Transactional(readOnly = true)
    public StoreSettings get(UUID storeId) {
        return toSettings(findStore(storeId));
    }

    @Transactional
    public StoreSettings update(UUID storeId, StoreSettings request) {
        Store store = findStore(storeId);
        String wabaId = blankToNull(request.whatsappWabaId());
        if (wabaId != null && storeRepository.existsByWhatsappWabaIdAndIdNot(wabaId, storeId)) {
            throw new ConflictException(WABA_ID_IN_USE);
        }
        store.setName(request.storeName().trim());
        store.setWhatsappWabaId(wabaId);
        return toSettings(storeRepository.save(store));
    }

    /** El storeId siempre viene de {@code @CurrentStore}, nunca del cliente. */
    private Store findStore(UUID storeId) {
        return storeRepository.findById(storeId).orElseThrow(() -> new NotFoundException("Tienda no encontrada"));
    }

    private static StoreSettings toSettings(Store store) {
        return new StoreSettings(store.getName(), store.getWhatsappWabaId());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
