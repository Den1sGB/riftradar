package com.riftradar.meta;

import com.riftradar.riot.RiotGateway;
import com.riftradar.riot.RiotRegion;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/meta")
public class MetaController {

    private final RiotGateway riotGateway;
    private final DataDragonService dataDragonService;

    public MetaController(RiotGateway riotGateway, DataDragonService dataDragonService) {
        this.riotGateway = riotGateway;
        this.dataDragonService = dataDragonService;
    }

    @GetMapping
    public ResponseEntity<MetaResponse> meta() {
        List<String> platforms = Arrays.stream(RiotRegion.values()).map(Enum::name).toList();
        String ddVersion = dataDragonService.getLatestVersion();
        return ResponseEntity.ok(new MetaResponse(
                "RiftRadar",
                riotGateway.modeName(),
                platforms,
                ddVersion,
                "https://ddragon.leagueoflegends.com/cdn/" + ddVersion
        ));
    }

    public record MetaResponse(
            String service,
            String riotGatewayMode,
            List<String> supportedPlatforms,
            String dataDragonVersion,
            String dataDragonBaseUrl
    ) {}
}
