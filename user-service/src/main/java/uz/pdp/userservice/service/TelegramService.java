package uz.pdp.userservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Sends OTP codes via Telegram Gateway API.
 *
 * This implementation uses the Telegram Gateway API to send verification codes
 * to users based on their phone number.
 *
 * Activation: set {@code telegram.enabled=true} (or env TELEGRAM_ENABLED=true).
 * When disabled the OTP is only logged (dev mode).
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "telegram.enabled", havingValue = "true")
public class TelegramService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${telegram.bot-token}")
    private String apiToken;

    /**
     * Send an OTP message via Telegram Gateway API.
     *
     * @param phone the user's phone number (E.164 format)
     * @param code  the verification code
     * @return true if successfully processed by the gateway
     */
    public boolean sendOtp(String phone, String code) {
        String url = "https://gatewayapi.telegram.org/sendVerificationMessage";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiToken);

        Map<String, Object> body = Map.of(
                "phone_number", phone,
                "code", code
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            Map response = restTemplate.postForObject(url, entity, Map.class);
            if (response != null && Boolean.TRUE.equals(response.get("ok"))) {
                log.info("OTP sent via Telegram Gateway to phone={}", phone);
                return true;
            } else {
                log.error("Telegram Gateway API returned error: {}", response);
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to send OTP via Telegram Gateway to phone={}: {}", phone, e.getMessage());
            return false;
        }
    }
}
