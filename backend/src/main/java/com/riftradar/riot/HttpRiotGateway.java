package com.riftradar.riot;

import com.riftradar.common.ApiException;
import com.riftradar.config.RiftRadarProperties;
import com.riftradar.riot.RiotDtos.RiotAccountDto;
import com.riftradar.riot.RiotDtos.RiotLeagueEntryDto;
import com.riftradar.riot.RiotDtos.RiotMatchDto;
import com.riftradar.riot.RiotDtos.RiotSummonerDto;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@ConditionalOnProperty(name = "riftradar.riot.mode", havingValue = "http")
public class HttpRiotGateway implements RiotGateway {

    private static final Logger log = LoggerFactory.getLogger(HttpRiotGateway.class);

    private final RestClient restClient;
    private final String apiKey;

    public HttpRiotGateway(RestClient.Builder builder, RiftRadarProperties properties) {
        this.apiKey = properties.riot().apiKey();
        if (!StringUtils.hasText(this.apiKey) || this.apiKey.startsWith("RGAPI-replace")) {
            throw new IllegalStateException("RIOT_API_KEY must be configured when RIOT_GATEWAY_MODE=http");
        }

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.riot().connectTimeoutMs()))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(properties.riot().readTimeoutMs()));

        this.restClient = builder
                .requestFactory(requestFactory)
                .defaultHeader("X-Riot-Token", this.apiKey)
                .build();
    }

    @Override
    public RiotAccountDto findAccountByRiotId(RiotRegion region, String gameName, String tagLine) {
        String url = "https://" + region.regionalHost() + ".api.riotgames.com/riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}";
        return executeGet(url, RiotAccountDto.class, gameName, tagLine);
    }

    @Override
    public RiotSummonerDto findSummonerByPuuid(RiotRegion region, String puuid) {
        String url = "https://" + region.platformHost() + ".api.riotgames.com/lol/summoner/v4/summoners/by-puuid/{puuid}";
        return executeGet(url, RiotSummonerDto.class, puuid);
    }

    @Override
    public List<RiotLeagueEntryDto> findLeagueEntries(RiotRegion region, String summonerId, String puuid) {
        String url = "https://" + region.platformHost() + ".api.riotgames.com/lol/league/v4/entries/by-puuid/{puuid}";
        RiotLeagueEntryDto[] entries = executeGet(url, RiotLeagueEntryDto[].class, puuid);
        return entries != null ? Arrays.asList(entries) : List.of();
    }

    @Override
    public List<String> findRecentMatchIds(RiotRegion region, String puuid, int count) {
        int safeCount = Math.min(Math.max(count, 1), 20);
        String url = "https://" + region.regionalHost() + ".api.riotgames.com/lol/match/v5/matches/by-puuid/{puuid}/ids?start=0&count=" + safeCount;
        String[] ids = executeGet(url, String[].class, puuid);
        return ids != null ? Arrays.asList(ids) : List.of();
    }

    @Override
    public RiotMatchDto findMatchById(RiotRegion region, String matchId) {
        String url = "https://" + region.regionalHost() + ".api.riotgames.com/lol/match/v5/matches/{matchId}";
        return executeGet(url, RiotMatchDto.class, matchId);
    }

    @Override
    public String modeName() {
        return "http";
    }

    private <T> T executeGet(String urlTemplate, Class<T> responseType, Object... uriVariables) {
        try {
            return restClient.get()
                    .uri(urlTemplate, uriVariables)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (req, res) -> {
                        throw new ApiException(HttpStatus.NOT_FOUND, "riot_resource_not_found", "Summoner or match not found in Riot API");
                    })
                    .onStatus(status -> status.value() == 429, (req, res) -> {
                        log.warn("Riot API rate limit hit (429)");
                        throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "riot_rate_limited", "Riot API quota reached, please retry shortly");
                    })
                    .onStatus(status -> status.value() == 401 || status.value() == 403, (req, res) -> {
                        log.error("Riot API authentication rejected with status={}", res.getStatusCode().value());
                        throw new ApiException(HttpStatus.BAD_GATEWAY, "riot_auth_error", "External Riot integration unavailable");
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new ApiException(HttpStatus.BAD_GATEWAY, "riot_upstream_error", "Riot API temporarily unavailable");
                    })
                    .body(responseType);
        } catch (ApiException ex) {
            throw ex;
        } catch (RestClientException ex) {
            log.error("Network failure calling Riot API", ex);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "riot_unreachable", "Unable to reach Riot Games API");
        }
    }
}
