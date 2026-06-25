package uz.pdp.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import uz.pdp.userservice.dto.*;
import uz.pdp.userservice.entity.OtpCode;
import uz.pdp.userservice.entity.User;
import uz.pdp.userservice.exception.InvalidOtpException;
import uz.pdp.userservice.exception.UserNotFoundException;
import uz.pdp.userservice.repository.OtpCodeRepository;
import uz.pdp.userservice.repository.UserRepository;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final OtpCodeRepository otpCodeRepository;
    private final RestTemplate restTemplate;

    @Value("${keycloak.server-url}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    @Value("${otp.expiry-minutes:5}")
    private int otpExpiryMinutes;

    /**
     * Step 1: Generate and send OTP code to the user's phone.
     * In production this would integrate with Telegram Bot API.
     */
    @Transactional
    public void sendOtp(PhoneRequest request) {
        String phone = request.getPhone();
        String code = generateOtpCode();

        OtpCode otp = OtpCode.builder()
                .phone(phone)
                .code(code)
                .expiresAt(OffsetDateTime.now().plusMinutes(otpExpiryMinutes))
                .build();
        otpCodeRepository.save(otp);

        // In production: send via Telegram Bot API
        // TelegramService.sendMessage(phone, "Your code: " + code);
        log.info("OTP for {} is: {} (simulated - not sent in dev mode)", phone, code);
    }

    /**
     * Step 2: Verify OTP and return Keycloak JWT tokens.
     * Creates user in DB if first time.
     */
    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        OtpCode otp = otpCodeRepository
                .findValidOtp(request.getPhone(), request.getCode(), OffsetDateTime.now())
                .orElseThrow(() -> new InvalidOtpException("Invalid or expired OTP code"));

        otp.setIsUsed(true);
        otpCodeRepository.save(otp);

        // Ensure user exists in local DB
        User user = userRepository.findByPhone(request.getPhone())
                .orElseGet(() -> userRepository.save(
                        User.builder().phone(request.getPhone()).build()
                ));

        // Authenticate with Keycloak using Resource Owner Password Credentials Grant
        return authenticateWithKeycloak(request.getPhone(), user.getId().toString());
    }

    /**
     * Refresh tokens via Keycloak
     */
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        return refreshWithKeycloak(request.getRefreshToken());
    }

    /**
     * Get user profile by Keycloak subject claim
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String keycloakId) {
        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return mapToProfileResponse(user);
    }

    // ─── private helpers ─────────────────────────────────────────────────────

    private String generateOtpCode() {
        return String.format("%06d", new Random().nextInt(999999));
    }

    @SuppressWarnings("unchecked")
    private AuthResponse authenticateWithKeycloak(String phone, String userId) {
        String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token",
                keycloakServerUrl, realm);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        // Use phone as username; Keycloak user must be pre-created or we use direct grant
        body.add("username", phone);
        body.add("password", userId); // password = userId for demo purposes

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, entity, Map.class);

        Map<String, Object> tokenData = response.getBody();
        return AuthResponse.builder()
                .accessToken((String) tokenData.get("access_token"))
                .refreshToken((String) tokenData.get("refresh_token"))
                .expiresIn(((Number) tokenData.get("expires_in")).longValue())
                .tokenType("Bearer")
                .build();
    }

    @SuppressWarnings("unchecked")
    private AuthResponse refreshWithKeycloak(String refreshToken) {
        String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token",
                keycloakServerUrl, realm);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, entity, Map.class);

        Map<String, Object> tokenData = response.getBody();
        return AuthResponse.builder()
                .accessToken((String) tokenData.get("access_token"))
                .refreshToken((String) tokenData.get("refresh_token"))
                .expiresIn(((Number) tokenData.get("expires_in")).longValue())
                .tokenType("Bearer")
                .build();
    }

    private UserProfileResponse mapToProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .fullName(user.getFullName())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
