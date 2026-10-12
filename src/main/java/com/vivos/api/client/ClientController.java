package com.vivos.api.client;

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
@RequestMapping("/clients")
public class ClientController {

    private final ClientService clientService;

    ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    List<ClientResponse> list(@CurrentStore UUID storeId,
                              @RequestParam(required = false) String search,
                              @RequestParam(required = false) String fullName,
                              @RequestParam(required = false) String account,
                              @RequestParam(required = false) String phone,
                              @RequestParam(required = false) Department department,
                              @RequestParam(required = false) DeliveryType deliveryType) {
        return clientService.list(storeId,
                new ClientFilter(search, fullName, account, phone, department, deliveryType));
    }

    @PostMapping
    ResponseEntity<ClientResponse> create(@CurrentStore UUID storeId, @Valid @RequestBody ClientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.create(storeId, request));
    }

    @PatchMapping("/{id}")
    ClientResponse update(@CurrentStore UUID storeId, @PathVariable UUID id,
                          @Valid @RequestBody ClientRequest request) {
        return clientService.update(storeId, id, request);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@CurrentStore UUID storeId, @PathVariable UUID id) {
        clientService.delete(storeId, id);
        return ResponseEntity.noContent().build();
    }
}
