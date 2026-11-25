package com.riftradar.common;

import com.riftradar.common.RateLimitService.RateLimitDecision;
import com.riftradar.config.RiftRadarProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final RiftRadarProperties properties;

    public RateLimitFilter(RateLimitService rateLimitService, RiftRadarProperties properties) {
        this.rateLimitService = rateLimitService;
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        String clientIp = resolveClientIp(request);

        String scope;
        int limit;
        if (path.startsWith("/api/v1/auth/login") || path.startsWith("/api/v1/auth/register")) {
            scope = "auth";
            limit = Math.min(15, properties.rateLimit().publicRequestsPerMinute());
        } else if (path.startsWith("/api/v1/summoners")) {
            scope = "riot";
            limit = properties.rateLimit().riotRequestsPerMinute();
        } else {
            scope = "api";
            limit = properties.rateLimit().authRequestsPerMinute();
        }

        RateLimitDecision decision = rateLimitService.check(scope + ":" + clientIp, limit);
        response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));

        if (!decision.allowed()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()));
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
            String body = """
                    {"type":"https://riftradar.dev/errors/rate_limit_exceeded","title":"Too Many Requests","status":429,"detail":"Rate limit exceeded. Please wait before retrying.","errorCode":"rate_limit_exceeded","retryAfterSeconds":%d,"correlationId":"%s"}"""
                    .formatted(decision.retryAfterSeconds(), correlationId != null ? correlationId : "");
            response.getWriter().write(body);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
