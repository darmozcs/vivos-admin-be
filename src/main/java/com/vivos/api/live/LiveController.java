package com.vivos.api.live;

import com.vivos.api.store.CurrentStore;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/lives")
public class LiveController {

    private final LiveService liveService;

    LiveController(LiveService liveService) {
        this.liveService = liveService;
    }

    /** Query params opcionales, ver {@link LiveFilter}. */
    @GetMapping
    List<LiveSummaryResponse> list(@CurrentStore UUID storeId, LiveFilter filter) {
        return liveService.list(storeId, filter);
    }

    /** Mismos filtros que GET /lives. */
    @GetMapping("/summary")
    LivesSummaryResponse summary(@CurrentStore UUID storeId, LiveFilter filter) {
        return liveService.summary(storeId, filter);
    }

    @GetMapping("/current")
    LiveResponse current(@CurrentStore UUID storeId) {
        return liveService.current(storeId);
    }

    @GetMapping("/{id}")
    LiveSummaryResponse get(@CurrentStore UUID storeId, @PathVariable UUID id) {
        return liveService.get(storeId, id);
    }

    @PostMapping
    ResponseEntity<LiveResponse> create(@CurrentStore UUID storeId, @Valid @RequestBody CreateLiveRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(liveService.create(storeId, request));
    }

    /** Reemplaza las etiquetas del vivo. */
    @PutMapping("/{id}/tags")
    LiveSummaryResponse updateTags(@CurrentStore UUID storeId, @PathVariable UUID id,
                                   @RequestBody UpdateLiveTagsRequest request) {
        return liveService.updateTags(storeId, id, request);
    }

    @PostMapping("/{id}/{action}")
    LiveResponse action(@CurrentStore UUID storeId, @PathVariable UUID id, @PathVariable String action) {
        return liveService.applyAction(storeId, id, action);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@CurrentStore UUID storeId, @PathVariable UUID id) {
        liveService.delete(storeId, id);
        return ResponseEntity.noContent().build();
    }
}
