package com.markettrust.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IP-based rate limiting filter using Bucket4j token-bucket algorithm.
 * Allows 100 requests per minute per originating IP address.
 * Returns HTTP 429 Too Many Requests when the bucket is exhausted.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RateLimitingFilter.class);

    /** One bucket per client IP — lazily created, concurrently safe. */
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /** 100 tokens refilled every 60 seconds, greedy (as fast as possible). */
    private static final int REQUESTS_PER_MINUTE = 100;
    private static final Duration REFILL_PERIOD = Duration.ofMinutes(1);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String clientIp = resolveClientIp(request);
        Bucket bucket = buckets.computeIfAbsent(clientIp, ip -> createBucket());

        if (bucket.tryConsume(1)) {
            // Add rate-limit info headers
            long availableTokens = bucket.getAvailableTokens();
            response.setHeader("X-RateLimit-Limit", String.valueOf(REQUESTS_PER_MINUTE));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(availableTokens));
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP: {} on {} {}",
                    clientIp, request.getMethod(), request.getRequestURI());
            sendRateLimitResponse(response);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.simple(REQUESTS_PER_MINUTE, REFILL_PERIOD);
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Resolves the real client IP respecting common reverse-proxy headers.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // Take the first (leftmost) IP — the original client
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }

    private void sendRateLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", "60");
        response.getWriter().write("""
                {
                  "status": 429,
                  "error": "Too Many Requests",
                  "message": "Rate limit exceeded. Maximum 100 requests per minute allowed.",
                  "retryAfterSeconds": 60
                }
                """);
    }
}
