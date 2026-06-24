package com.edurite.admin.dto;

import java.util.List;

public record AdminBulkUploadResultDto(
        int totalRows,
        int successfulRows,
        int failedRows,
        List<AdminUserDto> createdUsers,
        List<AdminBulkUploadRowErrorDto> errors
) {
}

