package uz.pdp.paymentservice.kafka;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Event consumed from 'payment-request' topic (sent by rental-service).
 * Triggers payment processing in payment-service.
 */
@Data
@Builder
public class PaymentRequestEvent {
    private UUID rentalId;
    private UUID userId;
    private UUID cardId;
    private BigDecimal amount;
    private String idempotencyKey;
}
