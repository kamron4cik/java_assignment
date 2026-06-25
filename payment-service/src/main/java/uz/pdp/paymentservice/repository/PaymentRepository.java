package uz.pdp.paymentservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.pdp.paymentservice.entity.Payment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    List<Payment> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Payment> findByRentalId(UUID rentalId);

    Optional<Payment> findByIdAndUserId(UUID id, UUID userId);
}
