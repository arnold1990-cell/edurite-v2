package com.edurite.security.service;

import com.edurite.company.repository.CompanyProfileRepository;
import com.edurite.security.filter.JwtAuthenticationFilter;
import com.edurite.user.entity.*;
import com.edurite.user.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtPasswordResetRevocationTest {
    private static final String SECRET = "test-only-revocation-key-that-is-at-least-32-bytes";
    private final JwtService jwt = new JwtService(SECRET, 3600, 604800);
    private final UserRepository users = mock(UserRepository.class);
    private final CustomUserDetailsService details = new CustomUserDetailsService(users, mock(CompanyProfileRepository.class));
    private final User account = new User();

    private UserDetails principal() {
        account.setId(UUID.randomUUID()); account.setEmail("revocation@example.com");
        account.setPasswordHash("fixture-hash"); account.setStatus(UserStatus.ACTIVE); account.setEmailVerified(true);
        Role role = new Role(); role.setName("ROLE_STUDENT"); account.getRoles().add(role);
        when(users.findByEmailIgnoreCase(account.getEmail())).thenReturn(Optional.of(account));
        return details.loadUserByUsername(account.getEmail());
    }
    @AfterEach void clearAuthentication() { SecurityContextHolder.clearContext(); }

    @Test void accessAndRefreshTokensAreRevokedAfterVersionIncrementAndNewTokensWork() {
        UserDetails original = principal();
        String access = jwt.generateAccessToken(account);
        String refresh = jwt.generateRefreshToken(account);
        assertThat(jwt.isTokenValid(access, original)).isTrue();
        assertThat(jwt.isTokenValid(refresh, original)).isTrue();
        account.setTokenVersion(1);
        UserDetails current = details.loadUserByUsername(account.getEmail());
        assertThat(jwt.isTokenValid(access, current)).isFalse();
        assertThat(jwt.isTokenValid(refresh, current)).isFalse();
        assertThat(jwt.isTokenValid(jwt.generateAccessToken(account, List.of("ROLE_STUDENT"), null, "BASIC"), current)).isTrue();
        assertThat(jwt.isTokenValid(jwt.generateRefreshToken(account), current)).isTrue();
        assertThat(account.getRoles()).extracting(Role::getName).containsExactly("ROLE_STUDENT");
    }

    @Test void tokensIssuedBeforeVersionClaimsRemainCompatibleUntilReset() {
        UserDetails original = principal();
        String legacy = Jwts.builder().subject(account.getEmail()).issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
        assertThat(jwt.isTokenValid(legacy, original)).isTrue();
        account.setTokenVersion(1);
        assertThat(jwt.isTokenValid(legacy, details.loadUserByUsername(account.getEmail()))).isFalse();
    }

    @Test void userDetailsTokenOverloadsCarryCurrentVersion() {
        principal(); account.setTokenVersion(2);
        UserDetails current = details.loadUserByUsername(account.getEmail());
        assertThat(jwt.isTokenValid(jwt.generateAccessToken(current), current)).isTrue();
        assertThat(jwt.isTokenValid(jwt.generateRefreshToken(current), current)).isTrue();
        account.setTokenVersion(3);
        assertThat(jwt.isTokenValid(jwt.generateAccessToken(current), details.loadUserByUsername(account.getEmail()))).isFalse();
    }

    @Test void authenticatedRequestsRejectOldAccessTokenAfterReset() throws Exception {
        principal();
        String old = jwt.generateAccessToken(account);
        var filter = new JwtAuthenticationFilter(jwt, details);
        assertThat(authenticate(filter, old)).isTrue();
        account.setTokenVersion(1);
        assertThat(authenticate(filter, old)).isFalse();
        assertThat(authenticate(filter, jwt.generateAccessToken(account))).isTrue();
    }

    private boolean authenticate(JwtAuthenticationFilter filter, String token) throws Exception {
        SecurityContextHolder.clearContext();
        var request = new MockHttpServletRequest("GET", "/api/v1/student/profile");
        request.addHeader("Authorization", "Bearer " + token);
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        return SecurityContextHolder.getContext().getAuthentication() != null;
    }
}
