package me.huynhducphu.talent_bridge.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.huynhducphu.talent_bridge.annotation.ApiMessage;
import me.huynhducphu.talent_bridge.dto.request.auth.UserLoginRequestDto;
import me.huynhducphu.talent_bridge.dto.request.auth.SessionMetaRequest;
import me.huynhducphu.talent_bridge.dto.request.auth.UserRegisterRequestDto;
import me.huynhducphu.talent_bridge.dto.response.auth.AuthResult;
import me.huynhducphu.talent_bridge.dto.response.auth.AuthTokenResponseDto;
import me.huynhducphu.talent_bridge.dto.response.auth.SessionMetaResponse;
import me.huynhducphu.talent_bridge.dto.response.user.UserDetailsResponseDto;
import me.huynhducphu.talent_bridge.dto.response.user.UserSessionResponseDto;
import me.huynhducphu.talent_bridge.service.AuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 6/8/2025
 **/
@Tag(name = "Auth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ApiMessage(value = "Đăng ký thành công")
    @Operation(summary = "Người dùng đăng ký")
    @SecurityRequirements()
    public ResponseEntity<UserSessionResponseDto> register(
            @Valid @RequestBody UserRegisterRequestDto userRegisterRequestDto
    ) {
        return ResponseEntity.ok(authService.handleRegister(userRegisterRequestDto));
    }

    @PostMapping("/login")
    @ApiMessage(value = "Đăng nhập thành công")
    @Operation(summary = "Người dùng đăng nhập")
    @SecurityRequirements()
    public ResponseEntity<AuthTokenResponseDto> login(
            @Valid @RequestBody UserLoginRequestDto userLoginRequestDto
    ) {
        AuthResult authResult = authService.handleLogin(userLoginRequestDto);

        AuthTokenResponseDto authTokenResponseDto = authResult.getAuthTokenResponseDto();
        ResponseCookie responseCookie = authResult.getResponseCookie();

        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
                .body(authTokenResponseDto);
    }

    @PostMapping("/logout")
    @Operation(summary = "Người dùng đăng xuất")
    @SecurityRequirements()
    public ResponseEntity<Void> logout(
            @CookieValue(value = "refresh_token", required = false) String refreshToken
    ) {
        ResponseCookie responseCookie = authService.handleLogout(refreshToken);

        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
                .build();
    }

    @GetMapping("/me")
    @ApiMessage(value = "Trả về thông tin phiên đăng nhập của người dùng hiện tại")
    @Operation(summary = "Lấy thông tin phiên đăng nhập của người dùng hiện tại")
    public ResponseEntity<UserSessionResponseDto> getCurrentUser() {
        return ResponseEntity.ok(authService.getCurrentUser());
    }

    @GetMapping("/me/details")
    @ApiMessage(value = "Trả về thông tin chi tiết của người dùng hiện tại")
    @Operation(summary = "Lấy thông tin chi tiết của người dùng hiện tại")
    public ResponseEntity<UserDetailsResponseDto> getCurrentUserDetails() {
        return ResponseEntity.ok(authService.getCurrentUserDetails());
    }

    @PostMapping("/refresh-token")
    @ApiMessage(value = "Lấy refresh token")
    @Operation(summary = "Cấp lại access token và refresh token mới")
    public ResponseEntity<AuthTokenResponseDto> refreshToken(
            @CookieValue(value = "refresh_token") String refreshToken,
            @RequestBody SessionMetaRequest sessionMetaRequest
    ) {
        AuthResult authResult = authService.handleRefresh(refreshToken, sessionMetaRequest);

        AuthTokenResponseDto authTokenResponseDto = authResult.getAuthTokenResponseDto();
        ResponseCookie responseCookie = authResult.getResponseCookie();

        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
                .body(authTokenResponseDto);
    }

    @GetMapping("/sessions")
    @ApiMessage(value = "Lấy session")
    @Operation(summary = "Lấy tất cả phiên đăng nhập của người dùng hiện tại")
    public ResponseEntity<List<SessionMetaResponse>> getAllSelfSessionMetas(@CookieValue(value = "refresh_token") String refreshToken) {
        return ResponseEntity.ok(authService.getAllSelfSessionMetas(refreshToken));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @ApiMessage(value = "Xóa session")
    @Operation(summary = "Xóa phiên đăng nhập của người dùng theo id phiên")
    public ResponseEntity<Void> removeSelfSession(@PathVariable String sessionId) {
        authService.removeSelfSession(sessionId);

        return ResponseEntity.ok().build();
    }


}
