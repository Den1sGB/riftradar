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

    public MetaController(RiotGateway riotGateway) {
        this.riotGateway = riotGateway;
    }

    @GetMapping
    public ResponseEntity<MetaResponse> meta() {
        List<String> platforms = Arrays.stream(RiotRegion.values()).map(Enum::name).toList();
        return ResponseEntity.ok(new MetaResponse(
                "RiftRadar",
                riotGateway.modeName(),
                platforms
        ));
    }

    public record MetaResponse(
            String service,
            String riotGatewayMode,
            List<String> supportedPlatforms
    ) {}
}
