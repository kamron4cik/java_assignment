package uz.pdp.paymentservice.dto;

import lombok.Builder;
import lombok.Data;
import uz.pdp.paymentservice.entity.Payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class PaymentResponse {
    private UUID id;
    private UUID userId;
    private UUID cardId;
    private UUID rentalId;
    private BigDecimal amount;
    private PaymentStatus status;
    private String idempotencyKey;
    private String failureReason;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
