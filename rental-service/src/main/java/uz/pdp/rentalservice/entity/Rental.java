package uz.pdp.rentalservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Rental entity represents a powerbank rental session.
 *
 * FSM States:
 *   WAITING → ACQUIRING_LOCK → PAYMENT_PROCESSING → EJECTING → IN_THE_LEASE
 *   Any state → FAILED (on error)
 *   IN_THE_LEASE → FINISHED (when user returns powerbank)
 */
@Entity
@Table(name = "rentals", indexes = {
        @Index(name = "idx_rentals_user_id", columnList = "user_id"),
        @Index(name = "idx_rentals_status", columnList = "status"),
        @Index(name = "idx_rentals_idempotency_key", columnList = "idempotency_key", unique = true),
        @Index(name = "idx_rentals_station_id", columnList = "station_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rental {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "station_id", nullable = false)
    private UUID stationId;

    @Column(name = "card_id", nullable = false)
    private UUID cardId;

    @Column(name = "slot_id")
    private UUID slotId;

    @Column(name = "power_bank_id")
    private UUID powerBankId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private RentalStatus status = RentalStatus.WAITING;

    // Idempotency key — client generates UUID per request, UNIQUE in DB
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "failure_reason")
    private String failureReason;

    // Rate per minute for recurring billing
    @Column(name = "rate_per_minute", precision = 10, scale = 4,
            columnDefinition = "NUMERIC(10, 4)")
    @Builder.Default
    private BigDecimal ratePerMinute = new BigDecimal("0.0500");

    @Column(name = "started_at", columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime startedAt;

    @Column(name = "finished_at", columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime finishedAt;

    @Column(name = "total_amount", precision = 19, scale = 2,
            columnDefinition = "NUMERIC(19, 2)")
    private BigDecimal totalAmount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime updatedAt;

    public enum RentalStatus {
        WAITING,            // Created, waiting to acquire station lock
        ACQUIRING_LOCK,     // Sent lock request to station-service
        PAYMENT_PROCESSING, // Lock acquired, processing initial payment
        EJECTING,           // Payment confirmed, ejecting powerbank
        IN_THE_LEASE,       // PowerBank ejected, user has it
        FINISHED,           // PowerBank returned
        CANCELLED,          // Cancelled by user
        FAILED              // Any step failed
    }
}
