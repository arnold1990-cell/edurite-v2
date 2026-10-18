package com.edurite.security.service;

import org.springframework.security.core.userdetails.UserDetails;

/** Carries the persisted JWT revocation version alongside the existing principal data. */
public final class VersionedUserDetails extends org.springframework.security.core.userdetails.User {
    private static final long serialVersionUID = 1L;
    private final long tokenVersion;

    public VersionedUserDetails(UserDetails principal, long tokenVersion) {
        super(principal.getUsername(), principal.getPassword(), principal.isEnabled(),
                principal.isAccountNonExpired(), principal.isCredentialsNonExpired(),
                principal.isAccountNonLocked(), principal.getAuthorities());
        this.tokenVersion = tokenVersion;
    }

    public long getTokenVersion() { return tokenVersion; }
}
