package com.vivos.api.admin.store;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Gestion de tiendas del Super Admin ({@code SUPER_ADMIN}, ver SecurityConfig). */
@RestController
@RequestMapping("/admin/stores")
public class StoreAdminController {

    private final StoreAdminService storeAdminService;

    StoreAdminController(StoreAdminService storeAdminService) {
        this.storeAdminService = storeAdminService;
    }

    @GetMapping
    List<StoreAdminResponse> list() {
        return storeAdminService.list();
    }

    @PostMapping
    ResponseEntity<StoreAdminResponse> create(@Valid @RequestBody CreateStoreRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(storeAdminService.create(request));
    }

    @PatchMapping("/{id}")
    StoreAdminResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateStoreRequest request) {
        return storeAdminService.update(id, request);
    }

    @PutMapping("/{id}/admin-password")
    ResponseEntity<Void> resetAdminPassword(@PathVariable UUID id,
                                            @Valid @RequestBody ResetAdminPasswordRequest request) {
        storeAdminService.resetAdminPassword(id, request.password());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/admin-verification-email")
    ResponseEntity<Void> resendAdminVerification(@PathVariable UUID id) {
        storeAdminService.resendAdminVerification(id);
        return ResponseEntity.noContent().build();
    }
}
