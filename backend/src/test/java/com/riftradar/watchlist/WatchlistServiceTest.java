package com.riftradar.watchlist;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.riftradar.common.ApiException;
import com.riftradar.summoner.SummonerService;
import com.riftradar.summoner.SummonerSnapshotRepository;
import com.riftradar.watchlist.WatchlistDtos.AddWatchlistRequest;
import com.riftradar.watchlist.WatchlistDtos.UpdateNoteRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class WatchlistServiceTest {

    @Mock
    private WatchlistEntryRepository watchlistRepository;
    @Mock
    private MatchNoteRepository noteRepository;
    @Mock
    private SummonerSnapshotRepository snapshotRepository;
    @Mock
    private SummonerService summonerService;

    private WatchlistService watchlistService;

    @BeforeEach
    void setUp() {
        watchlistService = new WatchlistService(
                watchlistRepository,
                noteRepository,
                snapshotRepository,
                summonerService,
                Clock.fixed(Instant.parse("2026-11-25T18:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void removeWatchlistRejectsAccessToAnotherUsersEntry() {
        UUID callerId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        when(watchlistRepository.findByIdAndUserId(entryId, callerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> watchlistService.removeWatchlist(callerId, entryId))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(watchlistRepository, never()).delete(any());
    }

    @Test
    void updateNoteRejectsAccessToAnotherUsersNote() {
        UUID callerId = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();
        when(noteRepository.findByIdAndUserId(noteId, callerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> watchlistService.updateNote(callerId, noteId, new UpdateNoteRequest("REVIEW", "Should ward river")))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(noteRepository, never()).save(any());
    }

    @Test
    void addWatchlistEnforcesPerUserQuotaLimit() {
        UUID callerId = UUID.randomUUID();
        when(watchlistRepository.countByUserId(callerId)).thenReturn(25L);

        assertThatThrownBy(() -> watchlistService.addWatchlist(
                callerId,
                new AddWatchlistRequest("BR1", "Faker", "KR1", "Mid goat")
        ))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
