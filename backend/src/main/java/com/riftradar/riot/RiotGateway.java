package com.riftradar.riot;

import com.riftradar.riot.RiotDtos.RiotAccountDto;
import com.riftradar.riot.RiotDtos.RiotLeagueEntryDto;
import com.riftradar.riot.RiotDtos.RiotMatchDto;
import com.riftradar.riot.RiotDtos.RiotSummonerDto;
import java.util.List;

public interface RiotGateway {

    RiotAccountDto findAccountByRiotId(RiotRegion region, String gameName, String tagLine);

    RiotSummonerDto findSummonerByPuuid(RiotRegion region, String puuid);

    List<RiotLeagueEntryDto> findLeagueEntries(RiotRegion region, String summonerId, String puuid);

    List<String> findRecentMatchIds(RiotRegion region, String puuid, int count);

    RiotMatchDto findMatchById(RiotRegion region, String matchId);

    String modeName();
}
