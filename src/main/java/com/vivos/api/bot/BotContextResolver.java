package com.vivos.api.bot;

import com.vivos.api.client.PhoneNumbers;
import com.vivos.api.common.ApiException;
import com.vivos.api.common.BadRequestException;
import com.vivos.api.common.NotFoundException;
import com.vivos.api.store.Store;
import com.vivos.api.store.StoreRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * De quien es cada consulta del bot. darmoz-whatsapp manda en cada llamada:
 * <ul>
 *   <li>{@code X-Whatsapp-Waba-Id}: la cuenta de WhatsApp Business que recibio el mensaje.
 *       Identifica a la tienda (la cargo en Configuracion).</li>
 *   <li>{@code X-Whatsapp-From}: el numero de quien escribe, verificado por Meta.</li>
 * </ul>
 */
@Component
public class BotContextResolver {

    public static final String HEADER_WABA_ID = "X-Whatsapp-Waba-Id";
    public static final String HEADER_FROM = "X-Whatsapp-From";

    static final String UNKNOWN_STORE = "Esta cuenta de WhatsApp no esta asociada a ninguna tienda";
    static final String INACTIVE_STORE = "La tienda esta desactivada";

    private final StoreRepository storeRepository;

    BotContextResolver(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    public BotContext resolve(String wabaId, String from) {
        if (wabaId == null || wabaId.isBlank()) {
            throw new BadRequestException("Falta el header " + HEADER_WABA_ID);
        }
        String phone = from == null ? "" : from.trim();
        if (!phone.matches("[0-9]{8,15}")) {
            throw new BadRequestException("El header " + HEADER_FROM + " tiene que ser un numero de WhatsApp");
        }
        Store store = storeRepository.findByWhatsappWabaId(wabaId.trim())
                .orElseThrow(() -> new NotFoundException(UNKNOWN_STORE));
        if (!store.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, INACTIVE_STORE);
        }
        return new BotContext(store, PhoneNumbers.normalize(phone));
    }

    /** Tienda del bot y telefono normalizado de quien escribe. */
    public record BotContext(Store store, String phone) {
    }
}
