package com.riftradar.watchlist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "match_notes")
public class MatchNoteEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "match_id", nullable = false, length = 40)
    private String matchId;

    @Column(nullable = false, length = 100)
    private String puuid;

    @Column(name = "mood_tag", nullable = false, length = 20)
    private String moodTag;

    @Column(name = "note_text", nullable = false, length = 500)
    private String noteText;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MatchNoteEntity() {}

    public MatchNoteEntity(
            UUID id,
            UUID userId,
            String matchId,
            String puuid,
            String moodTag,
            String noteText,
            Instant now
    ) {
        this.id = id;
        this.userId = userId;
        this.matchId = matchId;
        this.puuid = puuid;
        this.moodTag = moodTag;
        this.noteText = noteText;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String moodTag, String noteText, Instant now) {
        this.moodTag = moodTag;
        this.noteText = noteText;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getMatchId() { return matchId; }
    public String getPuuid() { return puuid; }
    public String getMoodTag() { return moodTag; }
    public String getNoteText() { return noteText; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
