package com.riftradar.meta;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class DataDragonService {

    private static final Logger log = LoggerFactory.getLogger(DataDragonService.class);
    private static final String VERSIONS_URL = "https://ddragon.leagueoflegends.com/api/versions.json";
    private static final String REDIS_KEY = "ddragon:latest_version";
    private static final Duration CACHE_TTL = Duration.ofHours(6);
    private static final String FALLBACK_VERSION = "15.10.1";

    private final RestClient restClient;
    private final StringRedisTemplate redisTemplate;

    public DataDragonService(RestClient.Builder builder, StringRedisTemplate redisTemplate) {
        this.restClient = builder.build();
        this.redisTemplate = redisTemplate;
    }

    /**
     * Returns the latest Data Dragon patch version (e.g. "15.10.1"),
     * served from Redis cache (TTL 6h) or fetched live on cache miss.
     */
    public String getLatestVersion() {
        try {
            String cached = redisTemplate.opsForValue().get(REDIS_KEY);
            if (cached != null && !cached.isBlank()) {
                return cached;
            }
        } catch (DataAccessException ex) {
            log.debug("Redis unavailable reading ddragon version cache; fetching live");
        }

        try {
            String[] versions = restClient.get()
                    .uri(VERSIONS_URL)
                    .retrieve()
                    .body(String[].class);

            if (versions != null && versions.length > 0) {
                String latest = versions[0];
                try {
                    redisTemplate.opsForValue().set(REDIS_KEY, latest, CACHE_TTL);
                } catch (DataAccessException ex) {
                    log.debug("Redis unavailable writing ddragon version cache");
                }
                log.info("Fetched latest Data Dragon version: {}", latest);
                return latest;
            }
        } catch (RestClientException ex) {
            log.warn("Could not fetch Data Dragon versions; using fallback {}", FALLBACK_VERSION);
        }

        return FALLBACK_VERSION;
    }

    public String championIconUrl(String version, String championName) {
        // Data Dragon uses PascalCase with no spaces, e.g. "Nunu & Willump" -> "Nunu" on old patches,
        // but modern patches use the internal champion key. We sanitize spaces and apostrophes.
        String clean = championName.replaceAll("[^a-zA-Z0-9]", "");
        return "https://ddragon.leagueoflegends.com/cdn/" + version + "/img/champion/" + clean + ".png";
    }

    public String profileIconUrl(String version, int profileIconId) {
        return "https://ddragon.leagueoflegends.com/cdn/" + version + "/img/profileicon/" + profileIconId + ".png";
    }
}
