package com.riftradar.summoner;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "summoner_snapshots")
public class SummonerSnapshotEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String puuid;

    @Column(nullable = false, length = 10)
    private String platform;

    @Column(name = "game_name", nullable = false, length = 40)
    private String gameName;

    @Column(name = "tag_line", nullable = false, length = 15)
    private String tagLine;

    @Column(name = "summoner_level", nullable = false)
    private int summonerLevel;

    @Column(name = "profile_icon_id", nullable = false)
    private int profileIconId;

    @Column(name = "solo_tier", length = 20)
    private String soloTier;

    @Column(name = "solo_rank", length = 10)
    private String soloRank;

    @Column(name = "solo_lp", nullable = false)
    private int soloLp;

    @Column(name = "solo_wins", nullable = false)
    private int soloWins;

    @Column(name = "solo_losses", nullable = false)
    private int soloLosses;

    @Column(name = "tilt_score", nullable = false)
    private int tiltScore;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    protected SummonerSnapshotEntity() {}

    public SummonerSnapshotEntity(
            UUID id,
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
            int soloLosses,
            int tiltScore,
            Instant fetchedAt
    ) {
        this.id = id;
        this.puuid = puuid;
        this.platform = platform;
        this.gameName = gameName;
        this.tagLine = tagLine;
        this.summonerLevel = summonerLevel;
        this.profileIconId = profileIconId;
        this.soloTier = soloTier;
        this.soloRank = soloRank;
        this.soloLp = soloLp;
        this.soloWins = soloWins;
        this.soloLosses = soloLosses;
        this.tiltScore = tiltScore;
        this.fetchedAt = fetchedAt;
    }

    public void updateSnapshot(
            String gameName,
            String tagLine,
            int summonerLevel,
            int profileIconId,
            String soloTier,
            String soloRank,
            int soloLp,
            int soloWins,
            int soloLosses,
            int tiltScore,
            Instant fetchedAt
    ) {
        this.gameName = gameName;
        this.tagLine = tagLine;
        this.summonerLevel = summonerLevel;
        this.profileIconId = profileIconId;
        this.soloTier = soloTier;
        this.soloRank = soloRank;
        this.soloLp = soloLp;
        this.soloWins = soloWins;
        this.soloLosses = soloLosses;
        this.tiltScore = tiltScore;
        this.fetchedAt = fetchedAt;
    }

    public UUID getId() { return id; }
    public String getPuuid() { return puuid; }
    public String getPlatform() { return platform; }
    public String getGameName() { return gameName; }
    public String getTagLine() { return tagLine; }
    public int getSummonerLevel() { return summonerLevel; }
    public int getProfileIconId() { return profileIconId; }
    public String getSoloTier() { return soloTier; }
    public String getSoloRank() { return soloRank; }
    public int getSoloLp() { return soloLp; }
    public int getSoloWins() { return soloWins; }
    public int getSoloLosses() { return soloLosses; }
    public int getTiltScore() { return tiltScore; }
    public Instant getFetchedAt() { return fetchedAt; }
}
