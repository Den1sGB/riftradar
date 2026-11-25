package com.riftradar.riot;

import com.riftradar.common.ApiException;
import java.util.Arrays;
import java.util.Locale;
import org.springframework.http.HttpStatus;

public enum RiotRegion {
    BR1("br1", "americas"),
    NA1("na1", "americas"),
    LA1("la1", "americas"),
    LA2("la2", "americas"),
    EUW1("euw1", "europe"),
    EUN1("eun1", "europe"),
    KR("kr", "asia"),
    JP1("jp1", "asia");

    private final String platformHost;
    private final String regionalHost;

    RiotRegion(String platformHost, String regionalHost) {
        this.platformHost = platformHost;
        this.regionalHost = regionalHost;
    }

    public String platformHost() {
        return platformHost;
    }

    public String regionalHost() {
        return regionalHost;
    }

    public static RiotRegion fromPlatform(String rawPlatform) {
        if (rawPlatform == null || rawPlatform.isBlank()) {
            return BR1;
        }
        String normalized = rawPlatform.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(r -> r.name().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "unsupported_platform",
                        "Unsupported Riot platform region: " + normalized
                ));
    }
}
