package com.riftradar.match;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class PerformanceAnalyzer {

    public RadarSummary analyze(List<MatchRecordEntity> matches) {
        if (matches == null || matches.isEmpty()) {
            return new RadarSummary(0, 0, 0.0, 0.0, 0.0, 0.0, 0, 0, "CHILL", List.of());
        }

        int total = matches.size();
        int wins = (int) matches.stream().filter(MatchRecordEntity::isWin).count();
        double winRate = round1((wins * 100.0) / total);

        double totalKda = matches.stream()
                .mapToDouble(m -> (m.getKills() + m.getAssists()) / (double) Math.max(1, m.getDeaths()))
                .sum();
        double avgKda = round2(totalKda / total);

        double totalCsPerMin = matches.stream()
                .mapToDouble(m -> m.getCsTotal() / Math.max(1.0, m.getDurationSeconds() / 60.0))
                .sum();
        double avgCsPerMin = round1(totalCsPerMin / total);

        double avgVision = round1(matches.stream().mapToInt(MatchRecordEntity::getVisionScore).average().orElse(0));

        int lossStreak = 0;
        for (MatchRecordEntity m : matches) {
            if (!m.isWin()) {
                lossStreak++;
            } else {
                break;
            }
        }

        double recentDeaths = matches.stream().limit(5).mapToInt(MatchRecordEntity::getDeaths).average().orElse(0);
        int recentLosses = (int) matches.stream().limit(5).filter(m -> !m.isWin()).count();
        int tiltScore = computeTiltScore(lossStreak, recentLosses, recentDeaths, winRate);
        String tiltStatus = resolveTiltStatus(tiltScore);

        Map<String, List<MatchRecordEntity>> byChampion = matches.stream()
                .collect(Collectors.groupingBy(MatchRecordEntity::getChampionName));

        List<ChampionStat> topChampions = byChampion.entrySet().stream()
                .map(entry -> {
                    List<MatchRecordEntity> champMatches = entry.getValue();
                    int champGames = champMatches.size();
                    int champWins = (int) champMatches.stream().filter(MatchRecordEntity::isWin).count();
                    double champWinRate = round1((champWins * 100.0) / champGames);
                    double champKda = round2(champMatches.stream()
                            .mapToDouble(m -> (m.getKills() + m.getAssists()) / (double) Math.max(1, m.getDeaths()))
                            .average().orElse(0));
                    return new ChampionStat(entry.getKey(), champGames, champWins, champWinRate, champKda);
                })
                .sorted(Comparator.comparingInt(ChampionStat::games).reversed()
                        .thenComparing(Comparator.comparingDouble(ChampionStat::winRatePct).reversed()))
                .limit(5)
                .toList();

        return new RadarSummary(
                total,
                wins,
                winRate,
                avgKda,
                avgCsPerMin,
                avgVision,
                lossStreak,
                tiltScore,
                tiltStatus,
                topChampions
        );
    }

    private int computeTiltScore(int lossStreak, int recentLosses, double recentDeaths, double overallWinRate) {
        int score = 0;
        score += Math.min(45, lossStreak * 15);
        score += Math.min(25, recentLosses * 5);
        if (recentDeaths >= 6.0) {
            score += Math.min(20, (int) Math.round((recentDeaths - 5.0) * 4));
        }
        if (overallWinRate < 45.0) {
            score += 10;
        }
        return Math.min(100, Math.max(0, score));
    }

    private String resolveTiltStatus(int tiltScore) {
        if (tiltScore >= 70) {
            return "HIGH_TILT";
        }
        if (tiltScore >= 45) {
            return "TILT_RISK";
        }
        if (tiltScore >= 20) {
            return "WARMING_UP";
        }
        return "CHILL";
    }

    private static double round1(double val) {
        return Math.round(val * 10.0) / 10.0;
    }

    private static double round2(double val) {
        return Math.round(val * 100.0) / 100.0;
    }

    public record RadarSummary(
            int totalMatches,
            int wins,
            double winRatePct,
            double avgKda,
            double avgCsPerMin,
            double avgVisionScore,
            int currentLossStreak,
            int tiltScore,
            String tiltStatus,
            List<ChampionStat> topChampions
    ) {}

    public record ChampionStat(
            String championName,
            int games,
            int wins,
            double winRatePct,
            double avgKda
    ) {}
}
