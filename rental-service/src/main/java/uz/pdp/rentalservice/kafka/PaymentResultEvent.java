package uz.pdp.rentalservice.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Consumed from 'payment-events' topic from payment-service */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResultEvent {
    private UUID paymentId;
    private UUID rentalId;
    private UUID userId;
    private UUID cardId;
    private BigDecimal amount;
    private String previousStatus;
    private String currentStatus;
    private String failureReason;
    private OffsetDateTime occurredAt;
}
