package com.vivos.api.seller;

import com.vivos.api.client.DeliveryType;
import com.vivos.api.client.Department;

import com.vivos.api.store.CurrentStore;
import jakarta.validation.Valid;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/sellers")
public class SellerController {

    private final SellerService sellerService;

    SellerController(SellerService sellerService) {
        this.sellerService = sellerService;
    }

    @GetMapping
    List<SellerResponse> list(@CurrentStore UUID storeId,
                              @RequestParam(required = false) String search,
                              @RequestParam(required = false) String fullName,
                              @RequestParam(required = false) String account,
                              @RequestParam(required = false) String phone,
                              @RequestParam(required = false) String email,
                              @RequestParam(required = false) Department department,
                              @RequestParam(required = false) DeliveryType deliveryType) {
        return sellerService.list(storeId,
                new SellerFilter(search, fullName, account, phone, email, department, deliveryType));
    }

    @PostMapping
    ResponseEntity<SellerResponse> create(@CurrentStore UUID storeId, @Valid @RequestBody SellerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sellerService.create(storeId, request));
    }

    @PatchMapping("/{id}")
    SellerResponse update(@CurrentStore UUID storeId, @PathVariable UUID id,
                          @Valid @RequestBody SellerRequest request) {
        return sellerService.update(storeId, id, request);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@CurrentStore UUID storeId, @PathVariable UUID id) {
        sellerService.delete(storeId, id);
        return ResponseEntity.noContent().build();
    }
}
