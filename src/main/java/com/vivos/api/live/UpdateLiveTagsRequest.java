package com.vivos.api.live;

import java.util.List;
import java.util.UUID;

/** Body de {@code PUT /lives/{id}/tags}: las etiquetas nuevas del vivo (vacio o ausente = ninguna). */
public record UpdateLiveTagsRequest(List<UUID> tagIds) {
}
