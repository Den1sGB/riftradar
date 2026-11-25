package com.riftradar.watchlist;

import com.riftradar.common.ApiException;
import com.riftradar.riot.RiotRegion;
import com.riftradar.summoner.SummonerDtos.SummonerDashboardResponse;
import com.riftradar.summoner.SummonerService;
import com.riftradar.summoner.SummonerSnapshotEntity;
import com.riftradar.summoner.SummonerSnapshotRepository;
import com.riftradar.watchlist.WatchlistDtos.AddWatchlistRequest;
import com.riftradar.watchlist.WatchlistDtos.CreateNoteRequest;
import com.riftradar.watchlist.WatchlistDtos.MatchNoteResponse;
import com.riftradar.watchlist.WatchlistDtos.UpdateNoteRequest;
import com.riftradar.watchlist.WatchlistDtos.WatchlistEntryResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WatchlistService {

    private static final int MAX_WATCHLIST_PER_USER = 25;

    private final WatchlistEntryRepository watchlistRepository;
    private final MatchNoteRepository noteRepository;
    private final SummonerSnapshotRepository snapshotRepository;
    private final SummonerService summonerService;
    private final Clock clock;

    public WatchlistService(
            WatchlistEntryRepository watchlistRepository,
            MatchNoteRepository noteRepository,
            SummonerSnapshotRepository snapshotRepository,
            SummonerService summonerService,
            Clock clock
    ) {
        this.watchlistRepository = watchlistRepository;
        this.noteRepository = noteRepository;
        this.snapshotRepository = snapshotRepository;
        this.summonerService = summonerService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<WatchlistEntryResponse> listWatchlist(UUID userId) {
        return watchlistRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::enrichEntry)
                .toList();
    }

    @Transactional
    public WatchlistEntryResponse addWatchlist(UUID userId, AddWatchlistRequest request) {
        RiotRegion region = RiotRegion.fromPlatform(request.platform());
        if (watchlistRepository.countByUserId(userId) >= MAX_WATCHLIST_PER_USER) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "watchlist_limit_reached",
                    "Watchlist limit of " + MAX_WATCHLIST_PER_USER + " summoners reached"
            );
        }

        SummonerDashboardResponse dashboard = summonerService.getDashboard(
                region.name(),
                request.gameName(),
                request.tagLine(),
                false
        );

        String puuid = dashboard.profile().puuid();
        if (watchlistRepository.existsByUserIdAndPlatformAndPuuid(userId, region.name(), puuid)) {
            throw new ApiException(HttpStatus.CONFLICT, "already_in_watchlist", "Summoner is already in your watchlist");
        }

        String alias = request.aliasLabel() != null && !request.aliasLabel().isBlank()
                ? request.aliasLabel().trim()
                : null;

        WatchlistEntryEntity entry = new WatchlistEntryEntity(
                UUID.randomUUID(),
                userId,
                region.name(),
                puuid,
                dashboard.profile().gameName(),
                dashboard.profile().tagLine(),
                alias,
                clock.instant()
        );
        watchlistRepository.save(entry);
        return new WatchlistEntryResponse(
                entry.getId(),
                entry.getPlatform(),
                entry.getPuuid(),
                entry.getGameName(),
                entry.getTagLine(),
                entry.getAliasLabel(),
                dashboard.profile().soloTier(),
                dashboard.profile().soloRank(),
                dashboard.profile().soloLp(),
                dashboard.radar().tiltScore(),
                entry.getCreatedAt()
        );
    }

    @Transactional
    public void removeWatchlist(UUID userId, UUID entryId) {
        WatchlistEntryEntity entry = watchlistRepository.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "watchlist_entry_not_found", "Watchlist entry not found"));
        watchlistRepository.delete(entry);
    }

    @Transactional(readOnly = true)
    public List<MatchNoteResponse> listNotes(UUID userId, String matchId) {
        List<MatchNoteEntity> notes = (matchId != null && !matchId.isBlank())
                ? noteRepository.findAllByUserIdAndMatchIdOrderByUpdatedAtDesc(userId, matchId.trim())
                : noteRepository.findAllByUserIdOrderByUpdatedAtDesc(userId);
        return notes.stream().map(MatchNoteResponse::from).toList();
    }

    @Transactional
    public MatchNoteResponse createNote(UUID userId, CreateNoteRequest request) {
        Instant now = clock.instant();
        MatchNoteEntity note = new MatchNoteEntity(
                UUID.randomUUID(),
                userId,
                request.matchId().trim(),
                request.puuid().trim(),
                request.moodTag().trim(),
                request.noteText().trim(),
                now
        );
        noteRepository.save(note);
        return MatchNoteResponse.from(note);
    }

    @Transactional
    public MatchNoteResponse updateNote(UUID userId, UUID noteId, UpdateNoteRequest request) {
        MatchNoteEntity note = noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "match_note_not_found", "Match note not found"));
        note.update(request.moodTag().trim(), request.noteText().trim(), clock.instant());
        noteRepository.save(note);
        return MatchNoteResponse.from(note);
    }

    @Transactional
    public void deleteNote(UUID userId, UUID noteId) {
        MatchNoteEntity note = noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "match_note_not_found", "Match note not found"));
        noteRepository.delete(note);
    }

    private WatchlistEntryResponse enrichEntry(WatchlistEntryEntity entry) {
        SummonerSnapshotEntity snap = snapshotRepository
                .findByPlatformAndPuuid(entry.getPlatform(), entry.getPuuid())
                .orElse(null);

        return new WatchlistEntryResponse(
                entry.getId(),
                entry.getPlatform(),
                entry.getPuuid(),
                entry.getGameName(),
                entry.getTagLine(),
                entry.getAliasLabel(),
                snap != null ? snap.getSoloTier() : null,
                snap != null ? snap.getSoloRank() : null,
                snap != null ? snap.getSoloLp() : null,
                snap != null ? snap.getTiltScore() : null,
                entry.getCreatedAt()
        );
    }
}
