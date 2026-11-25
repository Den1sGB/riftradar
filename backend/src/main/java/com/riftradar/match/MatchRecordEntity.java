package com.riftradar.match;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "match_records")
public class MatchRecordEntity {

    @Id
    private UUID id;

    @Column(name = "match_id", nullable = false, length = 40)
    private String matchId;

    @Column(nullable = false, length = 100)
    private String puuid;

    @Column(nullable = false, length = 10)
    private String platform;

    @Column(name = "queue_id", nullable = false)
    private int queueId;

    @Column(name = "champion_name", nullable = false, length = 40)
    private String championName;

    @Column(nullable = false, length = 20)
    private String lane;

    @Column(nullable = false)
    private boolean win;

    @Column(nullable = false)
    private int kills;

    @Column(nullable = false)
    private int deaths;

    @Column(nullable = false)
    private int assists;

    @Column(name = "cs_total", nullable = false)
    private int csTotal;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Column(name = "vision_score", nullable = false)
    private int visionScore;

    @Column(name = "gold_earned", nullable = false)
    private int goldEarned;

    @Column(name = "played_at", nullable = false)
    private Instant playedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MatchRecordEntity() {}

    public MatchRecordEntity(
            UUID id,
            String matchId,
            String puuid,
            String platform,
            int queueId,
            String championName,
            String lane,
            boolean win,
            int kills,
            int deaths,
            int assists,
            int csTotal,
            int durationSeconds,
            int visionScore,
            int goldEarned,
            Instant playedAt,
            Instant createdAt
    ) {
        this.id = id;
        this.matchId = matchId;
        this.puuid = puuid;
        this.platform = platform;
        this.queueId = queueId;
        this.championName = championName;
        this.lane = lane;
        this.win = win;
        this.kills = kills;
        this.deaths = deaths;
        this.assists = assists;
        this.csTotal = csTotal;
        this.durationSeconds = durationSeconds;
        this.visionScore = visionScore;
        this.goldEarned = goldEarned;
        this.playedAt = playedAt;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getMatchId() { return matchId; }
    public String getPuuid() { return puuid; }
    public String getPlatform() { return platform; }
    public int getQueueId() { return queueId; }
    public String getChampionName() { return championName; }
    public String getLane() { return lane; }
    public boolean isWin() { return win; }
    public int getKills() { return kills; }
    public int getDeaths() { return deaths; }
    public int getAssists() { return assists; }
    public int getCsTotal() { return csTotal; }
    public int getDurationSeconds() { return durationSeconds; }
    public int getVisionScore() { return visionScore; }
    public int getGoldEarned() { return goldEarned; }
    public Instant getPlayedAt() { return playedAt; }
}
