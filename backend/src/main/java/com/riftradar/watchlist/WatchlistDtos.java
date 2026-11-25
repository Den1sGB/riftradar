package com.riftradar.watchlist;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class WatchlistDtos {

    private WatchlistDtos() {}

    public record AddWatchlistRequest(
            @NotBlank @Size(max = 10) String platform,
            @NotBlank @Size(max = 40) String gameName,
            @NotBlank @Size(max = 15) String tagLine,
            @Size(max = 50) String aliasLabel
    ) {}

    public record WatchlistEntryResponse(
            UUID id,
            String platform,
            String puuid,
            String gameName,
            String tagLine,
            String aliasLabel,
            String soloTier,
            String soloRank,
            Integer soloLp,
            Integer tiltScore,
            Instant createdAt
    ) {}

    public record CreateNoteRequest(
            @NotBlank @Size(max = 40) String matchId,
            @NotBlank @Size(max = 100) String puuid,
            @NotBlank @Pattern(regexp = "FOCUSED|CLUTCH|TILTED|REVIEW") String moodTag,
            @NotBlank @Size(min = 3, max = 500) String noteText
    ) {}

    public record UpdateNoteRequest(
            @NotBlank @Pattern(regexp = "FOCUSED|CLUTCH|TILTED|REVIEW") String moodTag,
            @NotBlank @Size(min = 3, max = 500) String noteText
    ) {}

    public record MatchNoteResponse(
            UUID id,
            String matchId,
            String puuid,
            String moodTag,
            String noteText,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static MatchNoteResponse from(MatchNoteEntity entity) {
            return new MatchNoteResponse(
                    entity.getId(),
                    entity.getMatchId(),
                    entity.getPuuid(),
                    entity.getMoodTag(),
                    entity.getNoteText(),
                    entity.getCreatedAt(),
                    entity.getUpdatedAt()
            );
        }
    }
}
