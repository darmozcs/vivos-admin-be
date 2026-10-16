package com.vivos.api.tag;

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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tags")
public class TagController {

    private final TagService tagService;

    TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    List<TagResponse> list(@CurrentStore UUID storeId) {
        return tagService.list(storeId);
    }

    @PostMapping
    ResponseEntity<TagResponse> create(@CurrentStore UUID storeId, @Valid @RequestBody TagRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tagService.create(storeId, request));
    }

    @PatchMapping("/{id}")
    TagResponse update(@CurrentStore UUID storeId, @PathVariable UUID id, @Valid @RequestBody TagRequest request) {
        return tagService.update(storeId, id, request);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@CurrentStore UUID storeId, @PathVariable UUID id) {
        tagService.delete(storeId, id);
        return ResponseEntity.noContent().build();
    }
}
