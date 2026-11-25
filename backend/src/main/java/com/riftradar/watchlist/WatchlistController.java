package com.riftradar.watchlist;

import com.riftradar.auth.JwtTokenService;
import com.riftradar.watchlist.WatchlistDtos.AddWatchlistRequest;
import com.riftradar.watchlist.WatchlistDtos.CreateNoteRequest;
import com.riftradar.watchlist.WatchlistDtos.MatchNoteResponse;
import com.riftradar.watchlist.WatchlistDtos.UpdateNoteRequest;
import com.riftradar.watchlist.WatchlistDtos.WatchlistEntryResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class WatchlistController {

    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @GetMapping("/watchlist")
    public ResponseEntity<List<WatchlistEntryResponse>> listWatchlist(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(watchlistService.listWatchlist(JwtTokenService.extractUserId(jwt)));
    }

    @PostMapping("/watchlist")
    public ResponseEntity<WatchlistEntryResponse> addWatchlist(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AddWatchlistRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(watchlistService.addWatchlist(JwtTokenService.extractUserId(jwt), request));
    }

    @DeleteMapping("/watchlist/{entryId}")
    public ResponseEntity<Void> removeWatchlist(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID entryId
    ) {
        watchlistService.removeWatchlist(JwtTokenService.extractUserId(jwt), entryId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/notes")
    public ResponseEntity<List<MatchNoteResponse>> listNotes(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String matchId
    ) {
        return ResponseEntity.ok(watchlistService.listNotes(JwtTokenService.extractUserId(jwt), matchId));
    }

    @PostMapping("/notes")
    public ResponseEntity<MatchNoteResponse> createNote(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateNoteRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(watchlistService.createNote(JwtTokenService.extractUserId(jwt), request));
    }

    @PatchMapping("/notes/{noteId}")
    public ResponseEntity<MatchNoteResponse> updateNote(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID noteId,
            @Valid @RequestBody UpdateNoteRequest request
    ) {
        return ResponseEntity.ok(watchlistService.updateNote(JwtTokenService.extractUserId(jwt), noteId, request));
    }

    @DeleteMapping("/notes/{noteId}")
    public ResponseEntity<Void> deleteNote(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID noteId
    ) {
        watchlistService.deleteNote(JwtTokenService.extractUserId(jwt), noteId);
        return ResponseEntity.noContent().build();
    }
}
