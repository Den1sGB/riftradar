package com.riftradar.summoner;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SummonerSnapshotRepository extends JpaRepository<SummonerSnapshotEntity, UUID> {

    Optional<SummonerSnapshotEntity> findByPlatformAndPuuid(String platform, String puuid);

    @Query("""
            SELECT s FROM SummonerSnapshotEntity s
            WHERE s.platform = :platform
              AND LOWER(s.gameName) = LOWER(:gameName)
              AND LOWER(s.tagLine) = LOWER(:tagLine)
            """)
    Optional<SummonerSnapshotEntity> findByRiotId(
            @Param("platform") String platform,
            @Param("gameName") String gameName,
            @Param("tagLine") String tagLine
    );
}
