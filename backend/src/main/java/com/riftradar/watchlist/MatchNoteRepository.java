package com.riftradar.watchlist;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchNoteRepository extends JpaRepository<MatchNoteEntity, UUID> {
    List<MatchNoteEntity> findAllByUserIdOrderByUpdatedAtDesc(UUID userId);
    List<MatchNoteEntity> findAllByUserIdAndMatchIdOrderByUpdatedAtDesc(UUID userId, String matchId);
    Optional<MatchNoteEntity> findByIdAndUserId(UUID id, UUID userId);
}
