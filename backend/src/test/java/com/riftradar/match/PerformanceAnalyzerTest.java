package com.riftradar.match;

import static org.assertj.core.api.Assertions.assertThat;

import com.riftradar.match.PerformanceAnalyzer.RadarSummary;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PerformanceAnalyzerTest {

    private final PerformanceAnalyzer analyzer = new PerformanceAnalyzer();

    @Test
    void analyzeEmptyMatchesReturnsChillZeroedSummary() {
        RadarSummary summary = analyzer.analyze(List.of());
        assertThat(summary.totalMatches()).isZero();
        assertThat(summary.tiltScore()).isZero();
        assertThat(summary.tiltStatus()).isEqualTo("CHILL");
        assertThat(summary.topChampions()).isEmpty();
    }

    @Test
    void analyzeHighTiltLossStreakComputesMetricsAndTopChampions() {
        Instant now = Instant.parse("2026-11-25T18:00:00Z");
        List<MatchRecordEntity> matches = List.of(
                match("M1", "Ahri", false, 2, 9, 3, 160, 1800, now.minusSeconds(600)),
                match("M2", "Ahri", false, 3, 8, 4, 150, 1800, now.minusSeconds(2600)),
                match("M3", "Yasuo", false, 1, 10, 2, 140, 1800, now.minusSeconds(4600)),
                match("M4", "Ahri", true, 9, 2, 7, 210, 1800, now.minusSeconds(6600))
        );

        RadarSummary summary = analyzer.analyze(matches);

        assertThat(summary.totalMatches()).isEqualTo(4);
        assertThat(summary.wins()).isEqualTo(1);
        assertThat(summary.winRatePct()).isEqualTo(25.0);
        assertThat(summary.currentLossStreak()).isEqualTo(3);
        assertThat(summary.tiltScore()).isGreaterThanOrEqualTo(70);
        assertThat(summary.tiltStatus()).isEqualTo("HIGH_TILT");
        assertThat(summary.topChampions()).hasSize(2);
        assertThat(summary.topChampions().get(0).championName()).isEqualTo("Ahri");
        assertThat(summary.topChampions().get(0).games()).isEqualTo(3);
    }

    private static MatchRecordEntity match(
            String matchId,
            String champ,
            boolean win,
            int kills,
            int deaths,
            int assists,
            int cs,
            int durationSec,
            Instant playedAt
    ) {
        return new MatchRecordEntity(
                UUID.randomUUID(),
                matchId,
                "puuid-1",
                "BR1",
                420,
                champ,
                "MIDDLE",
                win,
                kills,
                deaths,
                assists,
                cs,
                durationSec,
                22,
                12000,
                playedAt,
                playedAt
        );
    }
}
