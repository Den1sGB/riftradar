package com.riftradar.riot;

import java.util.List;

public final class RiotDtos {

    private RiotDtos() {}

    public record RiotAccountDto(
            String puuid,
            String gameName,
            String tagLine
    ) {}

    public record RiotSummonerDto(
            String id,
            String puuid,
            int profileIconId,
            long summonerLevel
    ) {}

    public record RiotLeagueEntryDto(
            String queueType,
            String tier,
            String rank,
            int leaguePoints,
            int wins,
            int losses
    ) {}

    public record RiotMatchDto(
            Metadata metadata,
            Info info
    ) {
        public record Metadata(String matchId) {}

        public record Info(
                long gameCreation,
                long gameDuration,
                int queueId,
                List<Participant> participants
        ) {}

        public record Participant(
                String puuid,
                String championName,
                String teamPosition,
                boolean win,
                int kills,
                int deaths,
                int assists,
                int totalMinionsKilled,
                int neutralMinionsKilled,
                int visionScore,
                int goldEarned
        ) {}
    }
}
