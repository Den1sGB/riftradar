package com.riftradar;

import static org.assertj.core.api.Assertions.assertThat;

import com.riftradar.auth.AuthDtos.AuthResponse;
import com.riftradar.auth.AuthDtos.RegisterRequest;
import com.riftradar.summoner.SummonerDtos.SummonerDashboardResponse;
import com.riftradar.watchlist.WatchlistDtos.AddWatchlistRequest;
import com.riftradar.watchlist.WatchlistDtos.WatchlistEntryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("dev")
class RiftradarIntegrationIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void endToEndFlowRegistersUserSearchesSummonerAndManagesWatchlist() {
        ResponseEntity<AuthResponse> registerRes = restTemplate.postForEntity(
                "/api/v1/auth/register",
                new RegisterRequest("proplayer@riftradar.dev", "ProPlayer", "SecurePass123!"),
                AuthResponse.class
        );
        assertThat(registerRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registerRes.getBody()).isNotNull();
        String accessToken = registerRes.getBody().accessToken();

        ResponseEntity<SummonerDashboardResponse> summonerRes = restTemplate.getForEntity(
                "/api/v1/summoners/BR1/Faker/KR1",
                SummonerDashboardResponse.class
        );
        assertThat(summonerRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(summonerRes.getBody()).isNotNull();
        assertThat(summonerRes.getBody().recentMatches()).isNotEmpty();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        ResponseEntity<WatchlistEntryResponse> addRes = restTemplate.exchange(
                "/api/v1/watchlist",
                HttpMethod.POST,
                new HttpEntity<>(new AddWatchlistRequest("BR1", "Faker", "KR1", "Main mid"), headers),
                WatchlistEntryResponse.class
        );
        assertThat(addRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(addRes.getBody()).isNotNull();
        assertThat(addRes.getBody().aliasLabel()).isEqualTo("Main mid");
    }
}
