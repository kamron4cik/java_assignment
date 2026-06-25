package uz.pdp.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "cards", indexes = {
        @Index(name = "idx_cards_user_id", columnList = "user_id"),
        @Index(name = "idx_cards_card_number", columnList = "card_number")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    // Storing last 4 digits for display; full number never stored
    @Column(name = "card_number", nullable = false, length = 20)
    private String cardNumber;

    @Column(name = "card_holder", nullable = false)
    private String cardHolder;

    // NUMERIC(19,2) — never use FLOAT/DOUBLE for money
    @Column(nullable = false, precision = 19, scale = 2,
            columnDefinition = "NUMERIC(19, 2)")
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime updatedAt;
}
