package com.riftradar.summoner;

import com.riftradar.summoner.SummonerDtos.SummonerDashboardResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/summoners")
public class SummonerController {

    private final SummonerService summonerService;

    public SummonerController(SummonerService summonerService) {
        this.summonerService = summonerService;
    }

    @GetMapping("/{platform}/{gameName}/{tagLine}")
    public ResponseEntity<SummonerDashboardResponse> lookupSummoner(
            @PathVariable String platform,
            @PathVariable String gameName,
            @PathVariable String tagLine,
            @RequestParam(defaultValue = "false") boolean refresh
    ) {
        return ResponseEntity.ok(summonerService.getDashboard(platform, gameName, tagLine, refresh));
    }
}
