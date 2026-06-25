package uz.pdp.rentalservice.kafka;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/** Published to 'payment-request' topic to trigger payment-service */
@Data
@Builder
public class PaymentRequestEvent {
    private UUID rentalId;
    private UUID userId;
    private UUID cardId;
    private BigDecimal amount;
    private String idempotencyKey;
}
