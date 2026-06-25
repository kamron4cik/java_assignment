package uz.pdp.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final OtpCodeRepository otpCodeRepository;
    private final RestTemplate restTemplate;

    /** Injected only when telegram.enabled=true — otherwise absent */
    @Autowired(required = false)
    private TelegramService telegramService;

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

        // Try Telegram first; fall back to log-only in dev mode
        boolean sentViaTelegram = telegramService != null && telegramService.sendOtp(phone, code);
        if (!sentViaTelegram) {
            log.info("OTP for {} is: {} (Telegram disabled or no chat_id — dev mode)", phone, code);
        }
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

        // Ensure Keycloak user exists and persist its UUID so GET /v1/me can find the user
        String keycloakUuid = ensureKeycloakUserExists(request.getPhone(), user.getId().toString());
        if (keycloakUuid != null && !keycloakUuid.equals(user.getKeycloakId())) {
            user.setKeycloakId(keycloakUuid);
            userRepository.save(user);
        }

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

    /**
     * Ensures the user exists in Keycloak and returns their Keycloak UUID.
     * This UUID must be stored as keycloakId on the local User so that
     * GET /v1/me (which looks up by JWT subject = Keycloak UUID) works.
     */
    @SuppressWarnings("unchecked")
    private String ensureKeycloakUserExists(String phone, String userId) {
        try {
            // Get admin token from master realm
            String tokenUrl = String.format("%s/realms/master/protocol/openid-connect/token", keycloakServerUrl);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "password");
            body.add("client_id", "admin-cli");
            body.add("username", "admin");
            body.add("password", "admin");

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, entity, Map.class);
            String adminToken = (String) response.getBody().get("access_token");

            HttpHeaders adminHeaders = new HttpHeaders();
            adminHeaders.setBearerAuth(adminToken);

            // URL-encode phone so '+' is sent as '%2B', not interpreted as space
            String encodedPhone = URLEncoder.encode(phone, StandardCharsets.UTF_8);
            String usersUrl = String.format("%s/admin/realms/%s/users?exact=true&username=%s",
                    keycloakServerUrl, realm, encodedPhone);

            // Check if user already exists — use URI.create() so RestTemplate does NOT double-encode %2B
            ResponseEntity<List> usersResponse = restTemplate.exchange(
                    URI.create(usersUrl), HttpMethod.GET, new HttpEntity<>(adminHeaders), List.class);

            if (usersResponse.getBody() != null && !usersResponse.getBody().isEmpty()) {
                // User already exists — return their Keycloak UUID
                Map<String, Object> existing = (Map<String, Object>) usersResponse.getBody().get(0);
                String keycloakId = (String) existing.get("id");
                log.info("Keycloak user {} already exists with id {}", phone, keycloakId);
                return keycloakId;
            }

            // Create user in Keycloak
            String createUserUrl = String.format("%s/admin/realms/%s/users", keycloakServerUrl, realm);
            Map<String, Object> userDef = Map.of(
                "username", phone,
                "enabled", true,
                "email", phone.replace("+", "") + "@powerbank.uz",
                "firstName", "User",
                "lastName", phone,
                "credentials", List.of(
                    Map.of(
                        "type", "password",
                        "value", userId,
                        "temporary", false
                    )
                )
            );
            HttpHeaders createHeaders = new HttpHeaders();
            createHeaders.setContentType(MediaType.APPLICATION_JSON);
            createHeaders.setBearerAuth(adminToken);
            try {
                ResponseEntity<String> createResp = restTemplate.postForEntity(
                        createUserUrl, new HttpEntity<>(userDef, createHeaders), String.class);
                log.info("Created Keycloak user {} — HTTP {}", phone, createResp.getStatusCode());
            } catch (org.springframework.web.client.HttpClientErrorException.Conflict ex) {
                // 409: user was created between our check and create (race) — that's fine, fall through
                log.warn("Keycloak user {} already existed (409), fetching their UUID", phone);
            }

            // Fetch the user UUID (whether just created or pre-existing) — URI.create() prevents double-encoding
            ResponseEntity<List> newUsersResp = restTemplate.exchange(
                    URI.create(usersUrl), HttpMethod.GET, new HttpEntity<>(adminHeaders), List.class);
            if (newUsersResp.getBody() != null && !newUsersResp.getBody().isEmpty()) {
                Map<String, Object> created = (Map<String, Object>) newUsersResp.getBody().get(0);
                return (String) created.get("id");
            }
        } catch (Exception e) {
            log.error("Failed to ensure Keycloak user exists", e);
        }
        return null;
    }

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
