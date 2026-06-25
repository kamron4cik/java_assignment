package uz.pdp.paymentservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uz.pdp.paymentservice.entity.Card;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CardRepository extends JpaRepository<Card, UUID> {

    List<Card> findByUserIdAndIsActiveTrue(UUID userId);

    Optional<Card> findByIdAndUserIdAndIsActiveTrue(UUID id, UUID userId);

    Optional<Card> findByUserIdAndIsDefaultTrueAndIsActiveTrue(UUID userId);

    /**
     * Atomic debit using optimistic lock - prevents double spending.
     * Uses SELECT ... WHERE balance >= amount to ensure sufficient funds.
     */
    @Modifying
    @Query("UPDATE Card c SET c.balance = c.balance - :amount " +
           "WHERE c.id = :cardId AND c.balance >= :amount AND c.isActive = true")
    int debitBalance(UUID cardId, BigDecimal amount);

    /**
     * Atomic credit - for refunds.
     */
    @Modifying
    @Query("UPDATE Card c SET c.balance = c.balance + :amount WHERE c.id = :cardId")
    int creditBalance(UUID cardId, BigDecimal amount);
}
