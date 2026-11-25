package com.riftradar.match;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRecordRepository extends JpaRepository<MatchRecordEntity, UUID> {
    List<MatchRecordEntity> findTop20ByPuuidOrderByPlayedAtDesc(String puuid);
    boolean existsByMatchIdAndPuuid(String matchId, String puuid);
}
