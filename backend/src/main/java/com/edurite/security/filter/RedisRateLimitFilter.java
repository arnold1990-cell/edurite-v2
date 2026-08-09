package com.edurite.security.filter;

import com.edurite.security.config.RateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@ConditionalOnBean(StringRedisTemplate.class)
public class RedisRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimitFilter.class);
    private static final String KEY_PREFIX = "edurite:rate-limit:";

    private final StringRedisTemplate redisTemplate;
    private final RateLimitProperties properties;

    public RedisRateLimitFilter(StringRedisTemplate redisTemplate, RateLimitProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return true;
        }
        String path = request.getRequestURI();
        return path == null || !(path.startsWith("/api/") || path.equals("/api") || path.startsWith("/api/v1/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        RateRuleMatch match = matchRule(request.getRequestURI());
        if (match == null) {
            filterChain.doFilter(request, response);
            return;
        }

        long window = currentWindow(match.rule().getWindowSeconds());
        String key = KEY_PREFIX + match.name() + ":" + clientKey(request) + ":" + window;
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, java.time.Duration.ofSeconds(match.rule().getWindowSeconds() + 2));
            }
            long remaining = Math.max(0, match.rule().getLimit() - (count == null ? 0 : count));
            response.setHeader("X-RateLimit-Limit", Long.toString(match.rule().getLimit()));
            response.setHeader("X-RateLimit-Remaining", Long.toString(remaining));
            response.setHeader("X-RateLimit-Window-Seconds", Long.toString(match.rule().getWindowSeconds()));
            if (count != null && count > match.rule().getLimit()) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"message\":\"Too many requests. Please retry later.\"}");
                return;
            }
        } catch (RedisConnectionFailureException ex) {
            if (!properties.isFailOpen()) {
                response.setStatus(503);
                response.setContentType("application/json");
                response.getWriter().write("{\"message\":\"Rate limiting is temporarily unavailable.\"}");
                return;
            }
            log.warn("Redis rate limiter failed open: rule={}, message={}", match.name(), ex.getMessage());
        } catch (RuntimeException ex) {
            if (!properties.isFailOpen()) {
                throw ex;
            }
            log.warn("Redis rate limiter failed open: rule={}, type={}, message={}",
                    match.name(), ex.getClass().getSimpleName(), ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private RateRuleMatch matchRule(String path) {
        if (path == null) {
            return null;
        }
        for (Map.Entry<String, RateLimitProperties.Rule> entry : properties.getRules().entrySet()) {
            boolean matched = Arrays.stream(entry.getValue().getPathPrefixes().split(","))
                    .map(String::trim)
                    .filter(prefix -> !prefix.isEmpty())
                    .anyMatch(path::startsWith);
            if (matched) {
                return new RateRuleMatch(entry.getKey(), entry.getValue());
            }
        }
        return null;
    }

    private long currentWindow(long windowSeconds) {
        return Instant.now().getEpochSecond() / windowSeconds;
    }

    private String clientKey(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.toLowerCase(Locale.ROOT).startsWith("bearer ")) {
            return "token:" + sha256(bearer.substring(7));
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        String ip = forwardedFor == null || forwardedFor.isBlank()
                ? request.getRemoteAddr()
                : forwardedFor.split(",", 2)[0].trim();
        return "ip:" + sha256(ip == null ? "unknown" : ip);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private record RateRuleMatch(String name, RateLimitProperties.Rule rule) {}
}
