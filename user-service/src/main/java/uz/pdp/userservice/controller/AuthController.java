package uz.pdp.userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import uz.pdp.userservice.dto.*;
import uz.pdp.userservice.service.AuthService;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /auth/phone — Request OTP via phone
     */
    @PostMapping("/auth/phone")
    public ResponseEntity<Void> requestOtp(@Valid @RequestBody PhoneRequest request) {
        authService.sendOtp(request);
        return ResponseEntity.ok().build();
    }

    /**
     * POST /auth/verify — Verify OTP, get JWT tokens
     */
    @PostMapping("/auth/verify")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request));
    }

    /**
     * POST /v1/auth/refresh — Refresh access token
     */
    @PostMapping("/v1/auth/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    /**
     * GET /v1/me — Get authenticated user's profile
     */
    @GetMapping("/v1/me")
    public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal Jwt jwt) {
        String keycloakId = jwt.getSubject();
        return ResponseEntity.ok(authService.getProfile(keycloakId));
    }
}
