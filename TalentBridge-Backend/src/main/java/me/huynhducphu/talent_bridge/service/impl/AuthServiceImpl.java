package me.huynhducphu.talent_bridge.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import me.huynhducphu.talent_bridge.config.auth.AuthConfiguration;
import me.huynhducphu.talent_bridge.dto.request.auth.LoginRequestDto;
import me.huynhducphu.talent_bridge.dto.request.auth.SessionMetaRequest;
import me.huynhducphu.talent_bridge.dto.response.auth.AuthResult;
import me.huynhducphu.talent_bridge.dto.response.auth.AuthTokenResponseDto;
import me.huynhducphu.talent_bridge.dto.response.auth.SessionMetaResponse;
import me.huynhducphu.talent_bridge.dto.response.user.UserDetailsResponseDto;
import me.huynhducphu.talent_bridge.dto.response.user.UserSessionResponseDto;
import me.huynhducphu.talent_bridge.model.Role;
import me.huynhducphu.talent_bridge.model.User;
import me.huynhducphu.talent_bridge.repository.UserRepository;
import me.huynhducphu.talent_bridge.service.RefreshTokenRedisService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Admin 6/9/2025
 **/
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements me.huynhducphu.talent_bridge.service.AuthService {

    private final AuthenticationManager authenticationManager;

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    private final RefreshTokenRedisService refreshTokenRedisService;
    private final UserRepository userRepository;

    @Value("${jwt.access-token-expiration}")
    public Long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    public Long refreshTokenExpiration;

    @Override
    public AuthResult handleLogin(LoginRequestDto loginRequestDto) {
        // EXTRACT PRINCIPAL AND CREDENTIAL FROM REQUEST INTO TOKEN
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                loginRequestDto.getEmail(),
                loginRequestDto.getPassword()
        );

        // VERIFY TOKEN + SAVE AUTHENTICATION INTO CONTEXT
        Authentication authentication = authenticationManager.authenticate(token);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        return buildAuthResult(email, loginRequestDto.getSessionMetaRequest());
    }

    @Override
    public ResponseCookie handleLogout(String refreshToken) {
        if (refreshToken != null) {
            String email = jwtDecoder.decode(refreshToken).getSubject();

            User user = userRepository
                    .findByEmail(email)
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));

            refreshTokenRedisService.deleteRefreshToken(refreshToken, user.getId().toString());
        }

        return ResponseCookie
                .from("refresh_token", "")
                .httpOnly(true)
                .path("/")
                .sameSite("Strict")
                .maxAge(0)
                .build();
    }

    @Override
    public AuthResult handleRefresh(String refreshToken, SessionMetaRequest sessionMetaRequest) {
        String email = jwtDecoder.decode(refreshToken).getSubject();

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));
        String userId = user.getId().toString();

        if (!refreshTokenRedisService.validateToken(refreshToken, userId))
            throw new BadJwtException(null);


        if (!user.getEmail().equalsIgnoreCase(email))
            throw new BadJwtException(null);

        refreshTokenRedisService.deleteRefreshToken(refreshToken, userId);

        return buildAuthResult(user, sessionMetaRequest);
    }

    @Override
    public List<SessionMetaResponse> getAllSessionMetas(String refreshToken) {
        String email = jwtDecoder.decode(refreshToken).getSubject();

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));
        String userId = user.getId().toString();

        return refreshTokenRedisService.getAllSessionMetas(userId, refreshToken);
    }

    @Override
    public UserSessionResponseDto getCurrentUser() {
        String currentUserEmail = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return mapToUserInformation(currentUserEmail);
    }

    @Override
    public UserDetailsResponseDto getCurrentUserDetails() {
        String currentUserEmail = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        User user = userRepository
                .findByEmail(currentUserEmail)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));

        return new UserDetailsResponseDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getDob(),
                user.getAddress(),
                user.getGender(),
                user.getLogoUrl(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    @Override
    public boolean isCurrentUser(User user) {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        String targetUserEmail = user.getEmail();

        return currentUserEmail.equalsIgnoreCase(targetUserEmail);
    }

    private UserSessionResponseDto mapToUserInformation(User user) {
        if (user == null)
            throw new EntityNotFoundException("Không tìm thấy người dùng");

        List<String> permissions = null;
        if (user.getRole() != null && user.getRole().getPermissions() != null)
            permissions = user
                    .getRole()
                    .getPermissions()
                    .stream()
                    .map(x -> x.getMethod() + " " + x.getApiPath())
                    .toList();

        return new UserSessionResponseDto(
                user.getEmail(),
                user.getName(),
                user.getId(),
                user.getLogoUrl(),
                permissions
        );
    }

    private UserSessionResponseDto mapToUserInformation(String email) {
        if (email == null || email.isBlank())
            throw new EntityNotFoundException("Email không được để trống");

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));


        return mapToUserInformation(user);
    }

    private AuthResult buildAuthResult(String email, SessionMetaRequest sessionMetaRequest) {
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng"));

        return buildAuthResult(user, sessionMetaRequest);
    }

    private AuthResult buildAuthResult(User user, SessionMetaRequest sessionMetaRequest) {

        // ================================================
        // HANDLE REFRESH TOKEN
        String refreshToken = buildJwt(refreshTokenExpiration, user);
        refreshTokenRedisService.saveRefreshToken(
                refreshToken,
                user.getId().toString(),
                sessionMetaRequest,
                Duration.ofSeconds(refreshTokenExpiration));

        ResponseCookie responseCookie = ResponseCookie
                .from("refresh_token", refreshToken)
                .httpOnly(true)
                .path("/")
                .sameSite("Strict")
                .maxAge(refreshTokenExpiration)
                .build();
        // ================================================

        // ================================================
        // HANDLE ACCESS TOKEN
        String accessToken = buildJwt(accessTokenExpiration, user);

        AuthTokenResponseDto authTokenResponseDto = new AuthTokenResponseDto(
                mapToUserInformation(user),
                accessToken
        );
        // ================================================


        return new AuthResult(authTokenResponseDto, responseCookie);
    }

    private String buildJwt(Long expirationRate, User user) {
        Instant now = Instant.now();
        Instant validity = now.plus(expirationRate, ChronoUnit.SECONDS);

        // Header
        JwsHeader jwsHeader = JwsHeader.with(AuthConfiguration.MAC_ALGORITHM).build();

        // Payload
        Role role = user.getRole();
        List<String> permissions = role != null && role.getPermissions() != null
                ? role.getPermissions().stream().map(p -> p.getMethod() + " " + p.getApiPath()).toList()
                : List.of();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(now)
                .expiresAt(validity)
                .subject(user.getEmail())
                .claim("user", mapToUserInformation(user))
                .claim("permissions", permissions)
                .build();

        String res = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();

        return res;
    }


}
