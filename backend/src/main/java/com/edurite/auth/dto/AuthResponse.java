package com.edurite.auth.dto;

import java.util.Set;
import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long accessTokenExpiresIn,
        String role,
        String primaryRole,
        String approvalStatus,
        Boolean mustChangePassword,
        UserSummary user,
        String message
) {
    public AuthResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long accessTokenExpiresIn,
            String role,
            String primaryRole,
            String approvalStatus,
            Boolean mustChangePassword,
            UserSummary user
    ) {
        this(
                accessToken,
                refreshToken,
                tokenType,
                accessTokenExpiresIn,
                role,
                primaryRole,
                approvalStatus,
                mustChangePassword,
                user,
                null
        );
    }

    /**
     * this method handles the "UserSummary" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public record UserSummary(
            UUID id,
            String email,
            String fullName,
            String companyName,
            String schoolName,
            String username,
            Set<String> roles,
            String role,
            String primaryRole,
            String approvalStatus,
            Boolean verified,
            String planType,
            Boolean mustChangePassword,
            Boolean profileCompleted,
            Integer profileCompleteness
    ) {
        public UserSummary(
                UUID id,
                String email,
                String fullName,
                String companyName,
                String username,
                Set<String> roles,
                String role,
                String primaryRole,
                String approvalStatus,
                Boolean verified,
                String planType,
                Boolean mustChangePassword,
                Boolean profileCompleted,
                Integer profileCompleteness
        ) {
            this(
                    id,
                    email,
                    fullName,
                    companyName,
                    null,
                    username,
                    roles,
                    role,
                    primaryRole,
                    approvalStatus,
                    verified,
                    planType,
                    mustChangePassword,
                    profileCompleted,
                    profileCompleteness
            );
        }
    }
}

