package com.riftradar.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "riftradar")
public record RiftRadarProperties(
        Jwt jwt,
        Cors cors,
        RateLimit rateLimit,
        Riot riot
) {
    public record Jwt(
            @NotBlank @Size(min = 32, message = "JWT_SECRET must be at least 32 characters") String secret,
            @NotBlank String issuer,
            @Min(60) long accessTtlSeconds,
            @Min(300) long refreshTtlSeconds
    ) {}

    public record Cors(
            List<String> allowedOrigins
    ) {}

    public record RateLimit(
            boolean enabled,
            @Min(1) int publicRequestsPerMinute,
            @Min(1) int authRequestsPerMinute,
            @Min(1) int riotRequestsPerMinute
    ) {}

    public record Riot(
            @NotBlank String mode,
            String apiKey,
            @NotBlank String defaultPlatform,
            @NotBlank String defaultRouting,
            @Min(10) long summonerCacheTtlSeconds,
            @Min(60) long matchCacheTtlSeconds,
            @Min(500) int connectTimeoutMs,
            @Min(500) int readTimeoutMs
    ) {}
}
