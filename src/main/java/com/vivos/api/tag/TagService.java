package com.vivos.api.tag;

import com.vivos.api.common.BadRequestException;
import com.vivos.api.common.ConflictException;
import com.vivos.api.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class TagService {

    static final String TAG_NOT_FOUND = "Etiqueta no encontrada";
    static final String NAME_IN_USE = "Ya existe una etiqueta con ese nombre";
    static final String UNKNOWN_TAGS = "Alguna etiqueta no existe";

    private final TagRepository tagRepository;

    TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    /** Etiquetas de la tienda, por nombre. */
    @Transactional(readOnly = true)
    public List<TagResponse> list(UUID storeId) {
        return tagRepository.findByStoreIdOrderByNameAsc(storeId).stream().map(TagResponse::from).toList();
    }

    @Transactional
    public TagResponse create(UUID storeId, TagRequest request) {
        String name = request.name().trim();
        if (tagRepository.existsByStoreIdAndNameIgnoreCase(storeId, name)) {
            throw new ConflictException(NAME_IN_USE);
        }
        Tag tag = new Tag(storeId);
        apply(tag, name, request);
        return TagResponse.from(tagRepository.save(tag));
    }

    @Transactional
    public TagResponse update(UUID storeId, UUID id, TagRequest request) {
        Tag tag = tagRepository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new NotFoundException(TAG_NOT_FOUND));
        String name = request.name().trim();
        if (tagRepository.existsByStoreIdAndNameIgnoreCaseAndIdNot(storeId, name, id)) {
            throw new ConflictException(NAME_IN_USE);
        }
        apply(tag, name, request);
        return TagResponse.from(tagRepository.saveAndFlush(tag));
    }

    /** Borrar una etiqueta la quita de todos los vivos y movimientos que la tenian. */
    @Transactional
    public void delete(UUID storeId, UUID id) {
        Tag tag = tagRepository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new NotFoundException(TAG_NOT_FOUND));
        tagRepository.delete(tag);
    }

    /**
     * Valida que todas las etiquetas sean de la tienda y devuelve el conjunto sin repetidos.
     * {@code null} se toma como "sin etiquetas".
     */
    public Set<UUID> requireStoreTags(UUID storeId, Collection<UUID> tagIds) {
        Set<UUID> ids = tagIds == null ? new LinkedHashSet<>() : new LinkedHashSet<>(tagIds);
        if (!ids.isEmpty() && tagRepository.countByStoreIdAndIdIn(storeId, ids) != ids.size()) {
            throw new BadRequestException(UNKNOWN_TAGS);
        }
        return ids;
    }

    private static void apply(Tag tag, String name, TagRequest request) {
        tag.setName(name);
        tag.setColor(request.color().toUpperCase(Locale.ROOT));
    }
}
