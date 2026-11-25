package com.riftradar.watchlist;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchlistEntryRepository extends JpaRepository<WatchlistEntryEntity, UUID> {
    List<WatchlistEntryEntity> findAllByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<WatchlistEntryEntity> findByIdAndUserId(UUID id, UUID userId);
    boolean existsByUserIdAndPlatformAndPuuid(UUID userId, String platform, String puuid);
    long countByUserId(UUID userId);
}
