package com.riftradar.summoner;

import com.riftradar.common.ApiException;
import com.riftradar.config.RiftRadarProperties;
import com.riftradar.match.MatchRecordEntity;
import com.riftradar.match.MatchRecordRepository;
import com.riftradar.match.PerformanceAnalyzer;
import com.riftradar.match.PerformanceAnalyzer.RadarSummary;
import com.riftradar.riot.RiotDtos.RiotAccountDto;
import com.riftradar.riot.RiotDtos.RiotLeagueEntryDto;
import com.riftradar.riot.RiotDtos.RiotMatchDto;
import com.riftradar.riot.RiotDtos.RiotMatchDto.Participant;
import com.riftradar.riot.RiotDtos.RiotSummonerDto;
import com.riftradar.riot.RiotGateway;
import com.riftradar.riot.RiotRegion;
import com.riftradar.summoner.SummonerDtos.MatchItemDto;
import com.riftradar.summoner.SummonerDtos.SummonerDashboardResponse;
import com.riftradar.summoner.SummonerDtos.SummonerProfileDto;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SummonerService {

    private static final Logger log = LoggerFactory.getLogger(SummonerService.class);
    private static final int RECENT_MATCH_COUNT = 12;

    private final RiotGateway riotGateway;
    private final SummonerSnapshotRepository snapshotRepository;
    private final MatchRecordRepository matchRepository;
    private final PerformanceAnalyzer performanceAnalyzer;
    private final StringRedisTemplate redisTemplate;
    private final RiftRadarProperties properties;
    private final Clock clock;

    public SummonerService(
            RiotGateway riotGateway,
            SummonerSnapshotRepository snapshotRepository,
            MatchRecordRepository matchRepository,
            PerformanceAnalyzer performanceAnalyzer,
            StringRedisTemplate redisTemplate,
            RiftRadarProperties properties,
            Clock clock
    ) {
        this.riotGateway = riotGateway;
        this.snapshotRepository = snapshotRepository;
        this.matchRepository = matchRepository;
        this.performanceAnalyzer = performanceAnalyzer;
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public SummonerDashboardResponse getDashboard(String platformInput, String gameName, String tagLine, boolean forceRefresh) {
        RiotRegion region = RiotRegion.fromPlatform(platformInput);
        String cleanGameName = sanitizeSegment(gameName, "gameName");
        String cleanTagLine = sanitizeSegment(tagLine, "tagLine");
        Instant now = clock.instant();

        Optional<SummonerSnapshotEntity> cachedSnapshot = snapshotRepository.findByRiotId(
                region.name(),
                cleanGameName,
                cleanTagLine
        );

        String cacheKey = buildFreshKey(region.name(), cleanGameName, cleanTagLine);
        if (!forceRefresh && cachedSnapshot.isPresent() && isCacheWarm(cacheKey, cachedSnapshot.get(), now)) {
            SummonerSnapshotEntity snapshot = cachedSnapshot.get();
            List<MatchRecordEntity> storedMatches = matchRepository.findTop20ByPuuidOrderByPlayedAtDesc(snapshot.getPuuid());
            RadarSummary radar = performanceAnalyzer.analyze(storedMatches);
            return buildResponse(snapshot, radar, storedMatches, true);
        }

        if (forceRefresh && !acquireRefreshCooldown(region.name(), cleanGameName, cleanTagLine)) {
            throw new ApiException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "refresh_cooldown_active",
                    "Please wait 30 seconds before refreshing this summoner again"
            );
        }

        RiotAccountDto account = riotGateway.findAccountByRiotId(region, cleanGameName, cleanTagLine);
        RiotSummonerDto summoner = riotGateway.findSummonerByPuuid(region, account.puuid());
        List<RiotLeagueEntryDto> leagues = riotGateway.findLeagueEntries(region, summoner.id(), account.puuid());
        List<String> matchIds = riotGateway.findRecentMatchIds(region, account.puuid(), RECENT_MATCH_COUNT);

        for (String matchId : matchIds) {
            if (!matchRepository.existsByMatchIdAndPuuid(matchId, account.puuid())) {
                RiotMatchDto matchDto = riotGateway.findMatchById(region, matchId);
                toMatchRecord(matchDto, account.puuid(), region.name(), now)
                        .ifPresent(matchRepository::save);
            }
        }

        List<MatchRecordEntity> recentMatches = matchRepository.findTop20ByPuuidOrderByPlayedAtDesc(account.puuid());
        RadarSummary radar = performanceAnalyzer.analyze(recentMatches);

        RiotLeagueEntryDto soloQueue = leagues.stream()
                .filter(entry -> "RANKED_SOLO_5x5".equalsIgnoreCase(entry.queueType()))
                .findFirst()
                .orElse(null);

        SummonerSnapshotEntity snapshot = snapshotRepository
                .findByPlatformAndPuuid(region.name(), account.puuid())
                .orElseGet(() -> new SummonerSnapshotEntity(
                        UUID.randomUUID(),
                        account.puuid(),
                        region.name(),
                        account.gameName(),
                        account.tagLine(),
                        (int) summoner.summonerLevel(),
                        summoner.profileIconId(),
                        soloQueue != null ? soloQueue.tier() : "UNRANKED",
                        soloQueue != null ? soloQueue.rank() : "",
                        soloQueue != null ? soloQueue.leaguePoints() : 0,
                        soloQueue != null ? soloQueue.wins() : 0,
                        soloQueue != null ? soloQueue.losses() : 0,
                        radar.tiltScore(),
                        now
                ));

        snapshot.updateSnapshot(
                account.gameName(),
                account.tagLine(),
                (int) summoner.summonerLevel(),
                summoner.profileIconId(),
                soloQueue != null ? soloQueue.tier() : "UNRANKED",
                soloQueue != null ? soloQueue.rank() : "",
                soloQueue != null ? soloQueue.leaguePoints() : 0,
                soloQueue != null ? soloQueue.wins() : 0,
                soloQueue != null ? soloQueue.losses() : 0,
                radar.tiltScore(),
                now
        );
        snapshotRepository.save(snapshot);
        markCacheWarm(cacheKey);

        return buildResponse(snapshot, radar, recentMatches, false);
    }

    private SummonerDashboardResponse buildResponse(
            SummonerSnapshotEntity snapshot,
            RadarSummary radar,
            List<MatchRecordEntity> matches,
            boolean cacheHit
    ) {
        return new SummonerDashboardResponse(
                SummonerProfileDto.from(snapshot),
                radar,
                matches.stream().map(MatchItemDto::from).toList(),
                cacheHit,
                riotGateway.modeName(),
                snapshot.getFetchedAt()
        );
    }

    private Optional<MatchRecordEntity> toMatchRecord(RiotMatchDto matchDto, String puuid, String platform, Instant now) {
        if (matchDto == null || matchDto.info() == null || matchDto.info().participants() == null) {
            return Optional.empty();
        }
        List<Participant> participants = matchDto.info().participants();
        Participant participant = participants.stream()
                .filter(p -> puuid.equals(p.puuid()))
                .findFirst()
                .orElse(participants.size() == 1 ? participants.get(0) : null);

        if (participant == null) {
            return Optional.empty();
        }

        String lane = (participant.teamPosition() == null || participant.teamPosition().isBlank())
                ? "FILL"
                : participant.teamPosition();

        return Optional.of(new MatchRecordEntity(
                UUID.randomUUID(),
                matchDto.metadata().matchId(),
                puuid,
                platform,
                matchDto.info().queueId(),
                participant.championName(),
                lane,
                participant.win(),
                participant.kills(),
                participant.deaths(),
                participant.assists(),
                participant.totalMinionsKilled() + participant.neutralMinionsKilled(),
                (int) Math.max(60, matchDto.info().gameDuration()),
                participant.visionScore(),
                participant.goldEarned(),
                Instant.ofEpochMilli(matchDto.info().gameCreation()),
                now
        ));
    }

    private boolean isCacheWarm(String cacheKey, SummonerSnapshotEntity snapshot, Instant now) {
        try {
            if (Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
                return true;
            }
        } catch (DataAccessException ex) {
            log.debug("Redis unavailable checking summoner cache; falling back to DB timestamp");
        }
        return snapshot.getFetchedAt()
                .plusSeconds(properties.riot().summonerCacheTtlSeconds())
                .isAfter(now);
    }

    private void markCacheWarm(String cacheKey) {
        try {
            redisTemplate.opsForValue().set(
                    cacheKey,
                    "1",
                    Duration.ofSeconds(properties.riot().summonerCacheTtlSeconds())
            );
        } catch (DataAccessException ex) {
            log.debug("Redis unavailable marking summoner cache warm");
        }
    }

    private boolean acquireRefreshCooldown(String platform, String gameName, String tagLine) {
        String key = "cooldown:refresh:" + platform + ":" + gameName.toLowerCase(Locale.ROOT) + ":" + tagLine.toLowerCase(Locale.ROOT);
        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(30));
            return Boolean.TRUE.equals(acquired);
        } catch (DataAccessException ex) {
            return true;
        }
    }

    private static String buildFreshKey(String platform, String gameName, String tagLine) {
        return "cache:summoner:" + platform + ":" + gameName.toLowerCase(Locale.ROOT) + "#" + tagLine.toLowerCase(Locale.ROOT);
    }

    private static String sanitizeSegment(String value, String fieldName) {
        if (value == null || value.isBlank() || value.length() > 40) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_" + fieldName, "Invalid " + fieldName);
        }
        return value.trim();
    }
}
