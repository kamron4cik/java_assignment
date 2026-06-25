package uz.pdp.paymentservice.kafka;

import lombok.Builder;
import lombok.Data;
import uz.pdp.paymentservice.entity.Payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Event published to 'payment-events' topic whenever a payment status changes.
 * Used by Kafka key = paymentId for ordering guarantees within a payment.
 */
@Data
@Builder
public class PaymentStatusChangedEvent {
    private UUID paymentId;
    private UUID rentalId;
    private UUID userId;
    private UUID cardId;
    private BigDecimal amount;
    private PaymentStatus previousStatus;
    private PaymentStatus currentStatus;
    private String failureReason;
    private OffsetDateTime occurredAt;
}
