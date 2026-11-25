package com.riftradar.summoner;

import com.riftradar.match.MatchRecordEntity;
import com.riftradar.match.PerformanceAnalyzer.RadarSummary;
import java.time.Instant;
import java.util.List;

public final class SummonerDtos {

    private SummonerDtos() {}

    public record SummonerDashboardResponse(
            SummonerProfileDto profile,
            RadarSummary radar,
            List<MatchItemDto> recentMatches,
            boolean cacheHit,
            String gatewayMode,
            Instant fetchedAt
    ) {}

    public record SummonerProfileDto(
            String puuid,
            String platform,
            String gameName,
            String tagLine,
            int summonerLevel,
            int profileIconId,
            String soloTier,
            String soloRank,
            int soloLp,
            int soloWins,
            int soloLosses
    ) {
        public static SummonerProfileDto from(SummonerSnapshotEntity entity) {
            return new SummonerProfileDto(
                    entity.getPuuid(),
                    entity.getPlatform(),
                    entity.getGameName(),
                    entity.getTagLine(),
                    entity.getSummonerLevel(),
                    entity.getProfileIconId(),
                    entity.getSoloTier(),
                    entity.getSoloRank(),
                    entity.getSoloLp(),
                    entity.getSoloWins(),
                    entity.getSoloLosses()
            );
        }
    }

    public record MatchItemDto(
            String matchId,
            int queueId,
            String championName,
            String lane,
            boolean win,
            int kills,
            int deaths,
            int assists,
            int csTotal,
            double csPerMin,
            int durationSeconds,
            int visionScore,
            int goldEarned,
            Instant playedAt
    ) {
        public static MatchItemDto from(MatchRecordEntity m) {
            double csPerMin = Math.round((m.getCsTotal() / Math.max(1.0, m.getDurationSeconds() / 60.0)) * 10.0) / 10.0;
            return new MatchItemDto(
                    m.getMatchId(),
                    m.getQueueId(),
                    m.getChampionName(),
                    m.getLane(),
                    m.isWin(),
                    m.getKills(),
                    m.getDeaths(),
                    m.getAssists(),
                    m.getCsTotal(),
                    csPerMin,
                    m.getDurationSeconds(),
                    m.getVisionScore(),
                    m.getGoldEarned(),
                    m.getPlayedAt()
            );
        }
    }
}
