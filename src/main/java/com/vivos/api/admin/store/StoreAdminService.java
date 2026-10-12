package com.vivos.api.admin.store;

import com.vivos.api.auth.AdminUserResponse;
import com.vivos.api.auth.DarmozAuthClient;
import com.vivos.api.common.ConflictException;
import com.vivos.api.common.NotFoundException;
import com.vivos.api.config.SecurityConfig;
import com.vivos.api.store.Store;
import com.vivos.api.store.StoreRepository;
import com.vivos.api.store.StoreUser;
import com.vivos.api.store.StoreUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Orquesta las tiendas (en vivos-be) y sus usuarios admin (en darmoz-auth).
 * <p>
 * Los metodos no son {@code @Transactional}: las llamadas HTTP a darmoz-auth quedan fuera
 * de la transaccion y la parte de BD se ejecuta con {@link TransactionTemplate}.
 */
@Service
public class StoreAdminService {

    private static final Logger log = LoggerFactory.getLogger(StoreAdminService.class);

    static final String STORE_NOT_FOUND = "Tienda no encontrada";
    static final String STORE_WITHOUT_ADMIN = "La tienda no tiene usuario administrador";

    private final StoreRepository storeRepository;
    private final StoreUserRepository storeUserRepository;
    private final DarmozAuthClient darmozAuthClient;
    private final TransactionTemplate transactionTemplate;

    StoreAdminService(StoreRepository storeRepository,
                      StoreUserRepository storeUserRepository,
                      DarmozAuthClient darmozAuthClient,
                      TransactionTemplate transactionTemplate) {
        this.storeRepository = storeRepository;
        this.storeUserRepository = storeUserRepository;
        this.darmozAuthClient = darmozAuthClient;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Lista las tiendas, mas nuevas primero. {@code adminEmailVerified} sale de UNA llamada a
     * {@code GET /apps/api/users}; si esa llamada falla, el error se propaga (502 o el status de
     * darmoz-auth) en lugar de devolver datos incompletos.
     */
    public List<StoreAdminResponse> list() {
        List<Object[]> rows = transactionTemplate.execute(status -> storeRepository.findAllWithAdmin());
        Map<UUID, AdminUserResponse> users = darmozAuthClient.listUsers().stream()
                .collect(Collectors.toMap(AdminUserResponse::id, Function.identity(), (a, b) -> a));

        return rows.stream()
                .map(row -> {
                    Store store = (Store) row[0];
                    StoreUser admin = (StoreUser) row[1];
                    AdminUserResponse user = admin != null ? users.get(admin.getAuthUserId()) : null;
                    return StoreAdminResponse.from(store, admin, user != null ? user.emailVerified() : null);
                })
                .toList();
    }

    /**
     * 1. Crea el usuario admin en darmoz-auth (queda sin verificar y darmoz-auth le envia el codigo).
     * 2. Guarda tienda + store_user en una transaccion.
     * 3. Si la BD falla, compensa borrando el usuario en darmoz-auth y relanza el error.
     */
    public StoreAdminResponse create(CreateStoreRequest request) {
        String name = request.name().trim();
        String email = request.adminEmail().trim().toLowerCase();

        AdminUserResponse user = darmozAuthClient.createUser(email, request.adminPassword(),
                List.of(SecurityConfig.ROLE_ADMIN));

        try {
            return transactionTemplate.execute(status -> {
                Store store = new Store();
                store.setName(name);
                store = storeRepository.saveAndFlush(store);

                String adminEmail = user.email() != null ? user.email() : email;
                StoreUser admin = storeUserRepository.saveAndFlush(
                        new StoreUser(user.id(), store.getId(), adminEmail));
                return StoreAdminResponse.from(store, admin, user.emailVerified());
            });
        } catch (RuntimeException ex) {
            compensateUserCreation(user.id(), ex);
            throw ex;
        }
    }

    /**
     * PATCH parcial. Si cambia {@code active}, primero se habilita/deshabilita el admin en
     * darmoz-auth (que ademas revoca sus sesiones) y despues se guarda la tienda.
     * {@code adminEmailVerified} viene de la respuesta de darmoz-auth cuando hubo llamada;
     * si solo cambio el nombre es {@code null}.
     */
    public StoreAdminResponse update(UUID id, UpdateStoreRequest request) {
        Store store = transactionTemplate.execute(status -> findStore(id));
        StoreUser admin = transactionTemplate.execute(status ->
                storeUserRepository.findFirstByStoreIdOrderByCreatedAtAsc(id).orElse(null));

        Boolean emailVerified = null;
        boolean activeChanged = request.active() != null && request.active() != store.isActive();
        if (activeChanged) {
            if (admin == null) {
                throw new ConflictException(STORE_WITHOUT_ADMIN);
            }
            emailVerified = darmozAuthClient.updateUser(admin.getAuthUserId(), request.active(), null)
                    .emailVerified();
        }

        Store saved = transactionTemplate.execute(status -> {
            Store current = findStore(id);
            if (request.name() != null) {
                current.setName(request.name().trim());
            }
            if (request.active() != null) {
                current.setActive(request.active());
            }
            return storeRepository.saveAndFlush(current);
        });
        return StoreAdminResponse.from(saved, admin, emailVerified);
    }

    public void resetAdminPassword(UUID id, String password) {
        darmozAuthClient.updateUser(findAdmin(id).getAuthUserId(), null, password);
    }

    public void resendAdminVerification(UUID id) {
        darmozAuthClient.sendVerificationEmail(findAdmin(id).getAuthUserId());
    }

    private Store findStore(UUID id) {
        return storeRepository.findById(id).orElseThrow(() -> new NotFoundException(STORE_NOT_FOUND));
    }

    private StoreUser findAdmin(UUID storeId) {
        return transactionTemplate.execute(status -> {
            findStore(storeId);
            return storeUserRepository.findFirstByStoreIdOrderByCreatedAtAsc(storeId)
                    .orElseThrow(() -> new ConflictException(STORE_WITHOUT_ADMIN));
        });
    }

    private void compensateUserCreation(UUID userId, RuntimeException cause) {
        log.warn("No se pudo guardar la tienda; se elimina el usuario {} creado en darmoz-auth", userId, cause);
        try {
            darmozAuthClient.deleteUser(userId);
        } catch (RuntimeException compensationError) {
            log.error("Fallo la compensacion: el usuario {} quedo creado en darmoz-auth sin tienda",
                    userId, compensationError);
            cause.addSuppressed(compensationError);
        }
    }
}
