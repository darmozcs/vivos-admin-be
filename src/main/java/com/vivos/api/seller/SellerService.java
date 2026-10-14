package com.vivos.api.seller;

import com.vivos.api.client.ClientService;
import com.vivos.api.common.BadRequestException;
import com.vivos.api.common.ConflictException;
import com.vivos.api.common.NotFoundException;
import com.vivos.api.trade.TradeRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SellerService {

    public static final String SELLER_NOT_FOUND = "Vendedor no encontrado";
    static final String SELLER_HAS_TRADES = "No se puede eliminar un vendedor con compras/ventas";
    private static final int ACCOUNT_MAX = 100;
    private static final Sort LIST_ORDER = Sort.by("createdAt", "id");

    private final SellerRepository sellerRepository;
    private final TradeRepository tradeRepository;

    SellerService(SellerRepository sellerRepository, TradeRepository tradeRepository) {
        this.sellerRepository = sellerRepository;
        this.tradeRepository = tradeRepository;
    }

    /** Vendedores de la tienda que cumplen todos los filtros, en orden de alta. */
    @Transactional(readOnly = true)
    public List<SellerResponse> list(UUID storeId, SellerFilter filter) {
        return sellerRepository.findAll(SellerSpecifications.matching(storeId, filter), LIST_ORDER).stream()
                .map(SellerResponse::from)
                .toList();
    }

    @Transactional
    public SellerResponse create(UUID storeId, SellerRequest request) {
        Seller seller = new Seller(storeId);
        apply(seller, request);
        return SellerResponse.from(sellerRepository.save(seller));
    }

    @Transactional
    public SellerResponse update(UUID storeId, UUID id, SellerRequest request) {
        Seller seller = findSeller(storeId, id);
        apply(seller, request);
        return SellerResponse.from(sellerRepository.saveAndFlush(seller));
    }

    /** Los vendedores con compras/ventas no se borran, para mantener el historial. */
    @Transactional
    public void delete(UUID storeId, UUID id) {
        Seller seller = findSeller(storeId, id);
        if (tradeRepository.existsByStoreIdAndSellerId(storeId, id)) {
            throw new ConflictException(SELLER_HAS_TRADES);
        }
        sellerRepository.delete(seller);
    }

    public Seller findSeller(UUID storeId, UUID id) {
        return sellerRepository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new NotFoundException(SELLER_NOT_FOUND));
    }

    private void apply(Seller seller, SellerRequest request) {
        String account = ClientService.normalizeAccount(request.account());
        if (account.length() > ACCOUNT_MAX) {
            throw new BadRequestException("La cuenta no puede superar " + ACCOUNT_MAX + " caracteres");
        }
        String email = trimToEmpty(request.email());
        if (!email.isEmpty() && !ClientService.isValidEmail(email)) {
            throw new BadRequestException(ClientService.EMAIL_INVALID);
        }
        seller.setFullName(request.fullName().trim());
        seller.setAccount(account);
        seller.setPhone(trimToEmpty(request.phone()));
        seller.setEmail(email);
        seller.setAddress(trimToEmpty(request.address()));
        seller.setDepartment(request.department());
        seller.setDeliveryType(request.deliveryType());
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
