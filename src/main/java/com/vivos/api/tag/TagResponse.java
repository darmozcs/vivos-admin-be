package com.vivos.api.tag;

import java.util.UUID;

public record TagResponse(UUID id, String name, String color) {

    static TagResponse from(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName(), tag.getColor());
    }
}
