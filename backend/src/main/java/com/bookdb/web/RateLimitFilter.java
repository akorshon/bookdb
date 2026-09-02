package com.bookdb.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

/**
 * Redis-backed fixed-window rate limiter, keyed by client IP. One counter key per
 * (scope, ip, one-minute window); the first increment in a window sets its expiry, so
 * abandoned windows clean themselves up instead of needing a sweep job.
 *
 * <p>Two scopes: a tight limit on {@code POST /api/auth/**} (login/register) to blunt
 * brute-force/credential-stuffing attempts, and a looser general limit on the rest of
 * the API. If Redis is unreachable, requests are allowed through (fail-open) rather than
 * taking the whole API down over a rate-limiter dependency.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final long WINDOW_SECONDS = 60;

    private final StringRedisTemplate redisTemplate;
    private final boolean enabled;
    private final int generalLimit;
    private final int authLimit;

    public RateLimitFilter(StringRedisTemplate redisTemplate,
                            @Value("${app.ratelimit.enabled:true}") boolean enabled,
                            @Value("${app.ratelimit.general-requests-per-minute:120}") int generalLimit,
                            @Value("${app.ratelimit.auth-requests-per-minute:10}") int authLimit) {
        this.redisTemplate = redisTemplate;
        this.enabled = enabled;
        this.generalLimit = generalLimit;
        this.authLimit = authLimit;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!enabled || !path.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        boolean isAuthEndpoint = "POST".equalsIgnoreCase(request.getMethod()) && path.startsWith("/api/auth/");
        String scope = isAuthEndpoint ? "auth" : "general";
        int limit = isAuthEndpoint ? authLimit : generalLimit;

        long window = Instant.now().getEpochSecond() / WINDOW_SECONDS;
        String key = "ratelimit:" + scope + ":" + request.getRemoteAddr() + ":" + window;

        Long count;
        try {
            count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, java.time.Duration.ofSeconds(WINDOW_SECONDS));
            }
        } catch (Exception e) {
            log.warn("Rate limiter could not reach Redis, allowing request through: {}", e.toString());
            chain.doFilter(request, response);
            return;
        }

        long remaining = Math.max(0, limit - count);
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));

        if (count > limit) {
            long secondsUntilReset = (window + 1) * WINDOW_SECONDS - Instant.now().getEpochSecond();
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(Math.max(1, secondsUntilReset)));
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many requests, please slow down.\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}
