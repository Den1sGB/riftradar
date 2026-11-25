package com.riftradar.common;

import com.riftradar.config.RiftRadarProperties;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    private static final DefaultRedisScript<List> WINDOW_SCRIPT = new DefaultRedisScript<>(
            """
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            local ttl = redis.call('TTL', KEYS[1])
            return {current, ttl}
            """,
            List.class
    );

    private final StringRedisTemplate redisTemplate;
    private final RiftRadarProperties properties;

    public RateLimitService(StringRedisTemplate redisTemplate, RiftRadarProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public RateLimitDecision check(String bucketKey, int limitPerMinute) {
        if (!properties.rateLimit().enabled()) {
            return new RateLimitDecision(true, limitPerMinute, 0);
        }
        try {
            @SuppressWarnings("unchecked")
            List<Long> result = (List<Long>) redisTemplate.execute(
                    WINDOW_SCRIPT,
                    List.of("rl:" + bucketKey),
                    "60"
            );
            if (result == null || result.size() < 2) {
                return new RateLimitDecision(true, limitPerMinute, 0);
            }
            long count = result.get(0);
            long ttl = Math.max(1L, result.get(1));
            boolean allowed = count <= limitPerMinute;
            long remaining = Math.max(0L, limitPerMinute - count);
            return new RateLimitDecision(allowed, remaining, allowed ? 0 : ttl);
        } catch (DataAccessException ex) {
            log.warn("Redis unavailable during rate limit check; failing open for key={}", bucketKey);
            return new RateLimitDecision(true, limitPerMinute, 0);
        }
    }

    public record RateLimitDecision(boolean allowed, long remaining, long retryAfterSeconds) {}
}
