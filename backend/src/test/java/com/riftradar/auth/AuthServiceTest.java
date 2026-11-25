package com.riftradar.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.riftradar.auth.AuthDtos.AuthResponse;
import com.riftradar.auth.AuthDtos.RefreshRequest;
import com.riftradar.auth.AuthDtos.RegisterRequest;
import com.riftradar.common.ApiException;
import com.riftradar.config.RiftRadarProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private JwtTokenService jwtTokenService;

    private AuthService authService;
    private final Instant fixedNow = Instant.parse("2026-11-25T18:00:00Z");

    @BeforeEach
    void setUp() {
        RiftRadarProperties props = new RiftRadarProperties(
                new RiftRadarProperties.Jwt("12345678901234567890123456789012", "riftradar", 900, 604800),
                new RiftRadarProperties.Cors(List.of("http://localhost:5173")),
                new RiftRadarProperties.RateLimit(false, 30, 60, 40),
                new RiftRadarProperties.Riot("mock", "", "BR1", "AMERICAS", 300, 86400, 3000, 5000)
        );
        authService = new AuthService(
                userRepository,
                refreshTokenRepository,
                new BCryptPasswordEncoder(4),
                jwtTokenService,
                props,
                Clock.fixed(fixedNow, ZoneOffset.UTC)
        );
    }

    @Test
    void registerCreatesUserAndIssuesTokens() {
        when(userRepository.existsByEmailIgnoreCase("summoner@rift.dev")).thenReturn(false);
        when(jwtTokenService.issueAccessToken(any(), any())).thenReturn("jwt-access");
        when(jwtTokenService.generateOpaqueRefreshToken()).thenReturn("raw-refresh");
        when(jwtTokenService.hashRefreshToken("raw-refresh")).thenReturn("hashed-refresh");

        AuthResponse response = authService.register(new RegisterRequest("Summoner@rift.dev", "AhriMain", "StrongPass123!"));

        assertThat(response.accessToken()).isEqualTo("jwt-access");
        assertThat(response.refreshToken()).isEqualTo("raw-refresh");
        assertThat(response.user().email()).isEqualTo("summoner@rift.dev");
        verify(userRepository).save(any(UserEntity.class));
        verify(refreshTokenRepository).save(any(RefreshTokenEntity.class));
    }

    @Test
    void refreshReplayRevokesAllActiveSessionsForUser() {
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity(userId, "a@b.com", "Test", "hash", "USER", fixedNow);
        RefreshTokenEntity revokedToken = new RefreshTokenEntity(
                UUID.randomUUID(),
                user,
                "hashed-old",
                fixedNow.plusSeconds(3600),
                fixedNow.minusSeconds(100)
        );
        revokedToken.revoke(fixedNow.minusSeconds(50));

        when(jwtTokenService.hashRefreshToken("reused-token")).thenReturn("hashed-old");
        when(refreshTokenRepository.findByTokenHash("hashed-old")).thenReturn(Optional.of(revokedToken));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("reused-token")))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        verify(refreshTokenRepository).revokeAllActiveForUser(userId, fixedNow);
    }
}
