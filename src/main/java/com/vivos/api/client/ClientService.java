package com.vivos.api.client;

import com.vivos.api.common.BadRequestException;
import com.vivos.api.common.ConflictException;
import com.vivos.api.common.NotFoundException;
import com.vivos.api.order.OrderRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ClientService {

    public static final String CLIENT_NOT_FOUND = "Clienta no encontrada";
    static final String CLIENT_HAS_PURCHASES = "No se puede eliminar una clienta con compras";
    private static final String ACCOUNT_PREFIX = "@";
    private static final int ACCOUNT_MAX = 100;
    private static final Sort LIST_ORDER = Sort.by("createdAt", "id");

    private final ClientRepository clientRepository;
    private final OrderRepository orderRepository;

    ClientService(ClientRepository clientRepository, OrderRepository orderRepository) {
        this.clientRepository = clientRepository;
        this.orderRepository = orderRepository;
    }

    /** Clientas de la tienda que cumplen todos los filtros, en orden de alta. */
    @Transactional(readOnly = true)
    public List<ClientResponse> list(UUID storeId, ClientFilter filter) {
        return clientRepository.findAll(ClientSpecifications.matching(storeId, filter), LIST_ORDER).stream()
                .map(ClientResponse::from)
                .toList();
    }

    @Transactional
    public ClientResponse create(UUID storeId, ClientRequest request) {
        Client client = new Client(storeId);
        apply(client, request);
        return ClientResponse.from(clientRepository.save(client));
    }

    @Transactional
    public ClientResponse update(UUID storeId, UUID id, ClientRequest request) {
        Client client = findClient(storeId, id);
        apply(client, request);
        return ClientResponse.from(clientRepository.saveAndFlush(client));
    }

    /**
     * Las clientas con compras (ventas con items) no se borran, para mantener el historial.
     * Las ventas vacias de la clienta se eliminan junto con ella.
     */
    @Transactional
    public void delete(UUID storeId, UUID id) {
        Client client = findClient(storeId, id);
        if (orderRepository.existsWithItemsByStoreIdAndClientId(storeId, id)) {
            throw new ConflictException(CLIENT_HAS_PURCHASES);
        }
        orderRepository.deleteAll(orderRepository.findByStoreIdAndClientId(storeId, id));
        orderRepository.flush();
        clientRepository.delete(client);
    }

    public Client findClient(UUID storeId, UUID id) {
        return clientRepository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new NotFoundException(CLIENT_NOT_FOUND));
    }

    private void apply(Client client, ClientRequest request) {
        String account = normalizeAccount(request.account());
        if (account.length() > ACCOUNT_MAX) {
            throw new BadRequestException("La cuenta no puede superar " + ACCOUNT_MAX + " caracteres");
        }
        client.setFullName(request.fullName().trim());
        client.setAccount(account);
        client.setPhone(trimToEmpty(request.phone()));
        client.setAddress(trimToEmpty(request.address()));
        client.setDepartment(request.department());
        client.setDeliveryType(request.deliveryType());
    }

    /** Igual que el mock: la cuenta se guarda siempre con "@" adelante (o vacia). */
    static String normalizeAccount(String value) {
        String trimmed = trimToEmpty(value);
        if (trimmed.isEmpty()) {
            return "";
        }
        return trimmed.startsWith(ACCOUNT_PREFIX) ? trimmed : ACCOUNT_PREFIX + trimmed;
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
