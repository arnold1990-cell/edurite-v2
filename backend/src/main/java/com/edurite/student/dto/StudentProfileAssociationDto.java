package com.edurite.student.dto;

import java.util.UUID;

public record StudentProfileAssociationDto(Type type, UUID targetId, String title, String linkedAt, String careerName) {
    public StudentProfileAssociationDto(Type type, UUID targetId, String title, String linkedAt) {
        this(type, targetId, title, linkedAt, null);
    }
    public enum Type { COURSE, CAREER, PROGRAMME }
}
