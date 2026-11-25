package com.riftradar.auth;

import com.riftradar.auth.AuthDtos.AuthResponse;
import com.riftradar.auth.AuthDtos.LoginRequest;
import com.riftradar.auth.AuthDtos.RefreshRequest;
import com.riftradar.auth.AuthDtos.RegisterRequest;
import com.riftradar.auth.AuthDtos.UserProfileResponse;
import com.riftradar.common.ApiException;
import com.riftradar.config.RiftRadarProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final RiftRadarProperties properties;
    private final Clock clock;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            RiftRadarProperties properties,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ApiException(HttpStatus.CONFLICT, "email_already_registered", "An account with this email already exists");
        }

        Instant now = clock.instant();
        UserEntity user = new UserEntity(
                UUID.randomUUID(),
                normalizedEmail,
                request.displayName().trim(),
                passwordEncoder.encode(request.password()),
                "USER",
                now
        );
        userRepository.save(user);
        log.info("Registered new user id={}", user.getId());
        return issueTokenPair(user, now);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        UserEntity user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> invalidCredentials());

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }

        Instant now = clock.instant();
        return issueTokenPair(user, now);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public AuthResponse refresh(RefreshRequest request) {
        Instant now = clock.instant();
        String tokenHash = jwtTokenService.hashRefreshToken(request.refreshToken());

        RefreshTokenEntity existing = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "invalid_refresh_token", "Invalid refresh token"));

        if (existing.isRevoked()) {
            // Replay detected: revoke every active refresh token for this user to contain session theft.
            refreshTokenRepository.revokeAllActiveForUser(existing.getUser().getId(), now);
            log.warn("Refresh token replay detected for user id={}, revoked active sessions", existing.getUser().getId());
            throw new ApiException(HttpStatus.UNAUTHORIZED, "refresh_token_reused", "Refresh token has already been used");
        }

        if (existing.isExpired(now)) {
            existing.revoke(now);
            refreshTokenRepository.save(existing);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "refresh_token_expired", "Refresh token has expired");
        }

        existing.revoke(now);
        refreshTokenRepository.save(existing);
        return issueTokenPair(existing.getUser(), now);
    }

    @Transactional
    public void logout(UUID userId) {
        refreshTokenRepository.revokeAllActiveForUser(userId, clock.instant());
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "user_not_found", "User not found"));
        return UserProfileResponse.from(user);
    }

    private AuthResponse issueTokenPair(UserEntity user, Instant now) {
        String accessToken = jwtTokenService.issueAccessToken(user, now);
        String rawRefreshToken = jwtTokenService.generateOpaqueRefreshToken();
        String refreshHash = jwtTokenService.hashRefreshToken(rawRefreshToken);

        RefreshTokenEntity entity = new RefreshTokenEntity(
                UUID.randomUUID(),
                user,
                refreshHash,
                now.plusSeconds(properties.jwt().refreshTtlSeconds()),
                now
        );
        refreshTokenRepository.save(entity);

        return new AuthResponse(
                accessToken,
                rawRefreshToken,
                "Bearer",
                properties.jwt().accessTtlSeconds(),
                UserProfileResponse.from(user)
        );
    }

    private static ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "invalid_credentials", "Invalid email or password");
    }
}
