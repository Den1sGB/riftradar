package com.riftradar.watchlist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "watchlist_entries")
public class WatchlistEntryEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 10)
    private String platform;

    @Column(nullable = false, length = 100)
    private String puuid;

    @Column(name = "game_name", nullable = false, length = 40)
    private String gameName;

    @Column(name = "tag_line", nullable = false, length = 15)
    private String tagLine;

    @Column(name = "alias_label", length = 50)
    private String aliasLabel;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected WatchlistEntryEntity() {}

    public WatchlistEntryEntity(
            UUID id,
            UUID userId,
            String platform,
            String puuid,
            String gameName,
            String tagLine,
            String aliasLabel,
            Instant createdAt
    ) {
        this.id = id;
        this.userId = userId;
        this.platform = platform;
        this.puuid = puuid;
        this.gameName = gameName;
        this.tagLine = tagLine;
        this.aliasLabel = aliasLabel;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getPlatform() { return platform; }
    public String getPuuid() { return puuid; }
    public String getGameName() { return gameName; }
    public String getTagLine() { return tagLine; }
    public String getAliasLabel() { return aliasLabel; }
    public Instant getCreatedAt() { return createdAt; }
}
