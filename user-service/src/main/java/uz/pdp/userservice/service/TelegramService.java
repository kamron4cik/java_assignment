package uz.pdp.userservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Sends OTP codes via Telegram Bot API.
 *
 * The bot must have previously received a message from the user (or be in a group
 * with them) so Telegram knows the chat_id. In this implementation the phone number
 * is used as the chat_id lookup key stored in the {@code TELEGRAM_CHAT_IDS} env var
 * (comma-separated "phone:chatId" pairs) for demo purposes.
 *
 * Example env var:
 *   TELEGRAM_CHAT_IDS=+998901234567:123456789,+998907654321:987654321
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
    private String botToken;

    /**
     * Map of phone → Telegram chat_id, populated from env:
     *   TELEGRAM_CHAT_IDS=+998901234567:123456789,...
     */
    @Value("#{${telegram.chat-ids:{}}}")
    private Map<String, String> chatIds;

    /**
     * Send an OTP message to the Telegram chat associated with the phone number.
     *
     * @param phone the user's phone number (key for chat_id lookup)
     * @param code  the 6-digit OTP code
     * @return true if delivered, false if no chat_id mapping exists
     */
    public boolean sendOtp(String phone, String code) {
        String chatId = chatIds.get(phone);
        if (chatId == null) {
            log.warn("No Telegram chat_id mapped for phone={} — OTP not sent via Telegram", phone);
            return false;
        }

        String url = String.format(
                "https://api.telegram.org/bot%s/sendMessage", botToken);

        String text = String.format(
                "🔐 *PowerBank OTP*\n\nYour verification code: `%s`\n\n_Valid for 5 minutes. Do not share this code._",
                code);

        Map<String, Object> body = Map.of(
                "chat_id", chatId,
                "text", text,
                "parse_mode", "Markdown"
        );

        try {
            restTemplate.postForObject(url, body, Map.class);
            log.info("OTP sent via Telegram to phone={} chatId={}", phone, chatId);
            return true;
        } catch (Exception e) {
            log.error("Failed to send Telegram OTP to phone={}: {}", phone, e.getMessage());
            return false;
        }
    }
}
