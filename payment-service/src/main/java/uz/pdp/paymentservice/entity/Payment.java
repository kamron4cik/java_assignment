package uz.pdp.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payments_user_id", columnList = "user_id"),
        @Index(name = "idx_payments_rental_id", columnList = "rental_id"),
        @Index(name = "idx_payments_status", columnList = "status"),
        @Index(name = "idx_payments_idempotency_key", columnList = "idempotency_key", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "card_id", nullable = false)
    private UUID cardId;

    @Column(name = "rental_id")
    private UUID rentalId;

    // NUMERIC — not DOUBLE (floating point errors with money)
    @Column(nullable = false, precision = 19, scale = 2,
            columnDefinition = "NUMERIC(19, 2)")
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    // idempotency_key is UNIQUE — prevents duplicate payments on retries
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "failure_reason")
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime updatedAt;

    public enum PaymentStatus {
        PENDING, SUCCESS, FAILED, CANCELLED
    }
}
