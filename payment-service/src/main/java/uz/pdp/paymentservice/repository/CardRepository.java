package uz.pdp.paymentservice.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Card c WHERE c.id = :id")
    Optional<Card> findByIdForUpdate(UUID id);

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
