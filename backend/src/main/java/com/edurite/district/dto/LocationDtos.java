package com.edurite.district.dto;

import java.util.UUID;

public final class LocationDtos {
    private LocationDtos() {
    }

    public record LocationOptionDto(
            UUID id,
            String name,
            String code
    ) {}
}
