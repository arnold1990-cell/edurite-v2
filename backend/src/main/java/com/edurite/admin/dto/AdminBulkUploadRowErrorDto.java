package com.edurite.admin.dto;

public record AdminBulkUploadRowErrorDto(
        int rowNumber,
        String message
) {
}

