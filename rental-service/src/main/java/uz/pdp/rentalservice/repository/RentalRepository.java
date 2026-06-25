package uz.pdp.rentalservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uz.pdp.rentalservice.entity.Rental;
import uz.pdp.rentalservice.entity.Rental.RentalStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RentalRepository extends JpaRepository<Rental, UUID> {

    Optional<Rental> findByIdempotencyKey(String idempotencyKey);

    Optional<Rental> findByIdAndUserId(UUID id, UUID userId);

    List<Rental> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Rental> findByStatus(RentalStatus status);

    // For recurring billing: find all active leases
    List<Rental> findByStatusAndUserIdOrderByStartedAtAsc(RentalStatus status, UUID userId);
}
