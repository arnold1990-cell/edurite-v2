package com.edurite.security.service;

import com.edurite.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

// @Service marks a class that contains business logic.
@Service
/**
 * This class named JwtService is part of the Spring Boot application.
 * It groups related logic so the project stays organized and easier to learn.
 */
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey signingKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.access-token-expiration:3600}") long accessTokenExpiration,
            @Value("${security.jwt.refresh-token-expiration:604800}") long refreshTokenExpiration
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    /**
     * this method handles the "generateAccessToken" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public String generateAccessToken(UserDetails userDetails) {
        List<String> roles = userDetails.getAuthorities().stream().map(Object::toString).sorted().toList();
        return generateToken(userDetails.getUsername(), accessTokenExpiration, buildAccessClaims(roles));
    }

    /**
     * this method handles the "generateRefreshToken" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public String generateRefreshToken(UserDetails userDetails) {
        return generateToken(userDetails.getUsername(), refreshTokenExpiration, Map.of("type", "refresh"));
    }

    /**
     * this method handles the "generateAccessToken" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public String generateAccessToken(User user) {
        List<String> roles = user.getRoles().stream().map(role -> role.getName().trim().toUpperCase()).sorted().toList();
        log.info("[auth] jwt issued username={} jwtRoles={}", user.getEmail(), roles);
        return generateToken(user.getEmail(), accessTokenExpiration, buildAccessClaims(roles));
    }

    public String generateAccessToken(User user, Collection<String> roles, String approvalStatus) {
        return generateAccessToken(user, roles, approvalStatus, null);
    }

    public String generateAccessToken(User user, Collection<String> roles, String approvalStatus, String planType) {
        List<String> normalizedRoles = roles.stream().map(role -> role.trim().toUpperCase()).sorted().toList();
        log.info("[auth] jwt issued username={} jwtRoles={} approvalStatus={} planType={}", user.getEmail(), normalizedRoles, approvalStatus, planType);
        return generateToken(user.getEmail(), accessTokenExpiration, buildAccessClaims(normalizedRoles, approvalStatus, planType));
    }

    /**
     * this method handles the "generateRefreshToken" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public String generateRefreshToken(User user) {
        return generateToken(user.getEmail(), refreshTokenExpiration, Map.of("type", "refresh"));
    }

    private Map<String, Object> buildAccessClaims(List<String> roles) {
        return buildAccessClaims(roles, null, null);
    }

    private Map<String, Object> buildAccessClaims(List<String> roles, String approvalStatus) {
        return buildAccessClaims(roles, approvalStatus, null);
    }

    private Map<String, Object> buildAccessClaims(List<String> roles, String approvalStatus, String planType) {
        LinkedHashMap<String, Object> claims = new LinkedHashMap<>();
        claims.put("roles", roles);
        String primaryRole = roles.stream()
                .filter(role -> java.util.Set.of(
                        "ROLE_ADMIN",
                        "ROLE_DISTRICT_ADMIN",
                        "ROLE_DISTRICT_DIRECTOR",
                        "ROLE_CIRCUIT_MANAGER",
                        "ROLE_SUBJECT_ADVISOR",
                        "ROLE_SCHOOL_ADMIN",
                        "ROLE_TEACHER",
                        "ROLE_COMPANY",
                        "ROLE_SCHOOL_STUDENT",
                        "ROLE_STUDENT"
                ).contains(role))
                .findFirst()
                .orElse(null);
        if (primaryRole != null) {
            claims.put("primaryRole", primaryRole);
            claims.put("role", primaryRole.replace("ROLE_", ""));
        }
        if (approvalStatus != null && !approvalStatus.isBlank()) {
            claims.put("approvalStatus", approvalStatus);
        }
        if (planType != null && !planType.isBlank()) {
            claims.put("planType", planType);
        }
        return claims;
    }

    /**
     * this method handles the "accessTokenExpirationSeconds" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public long accessTokenExpirationSeconds() {
        return accessTokenExpiration;
    }

    /**
     * this method handles the "extractUsername" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * this method handles the "extractRoles" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public java.util.List<String> extractRoles(String token) {
        Object roles = extractAllClaims(token).get("roles");
        if (roles instanceof Collection<?> collection) {
            return collection.stream().filter(String.class::isInstance).map(String.class::cast).toList();
        }
        return java.util.List.of();
    }


    /**
     * this method handles the "extractRole" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public String extractRole(String token) {
        Object role = extractAllClaims(token).get("role");
        return role instanceof String value ? value : null;
    }

    /**
     * this method handles the "isTokenValid" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * this method handles the "isRefreshToken" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    public boolean isRefreshToken(String token) {
        Object type = extractAllClaims(token).get("type");
        return "refresh".equals(type);
    }

    /**
     * this method handles the "generateToken" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private String generateToken(String subject, long expirationSeconds, Map<String, Object> claims) {
        Instant now = Instant.now();
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * this method handles the "extractClaim" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }

    /**
     * this method handles the "isTokenExpired" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    /**
     * this method handles the "extractAllClaims" step of the feature.
     * It exists to keep this class focused and reusable.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

