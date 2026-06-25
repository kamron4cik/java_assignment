package uz.pdp.rentalservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pdp.rentalservice.entity.Rental;
import uz.pdp.rentalservice.entity.Rental.RentalStatus;
import uz.pdp.rentalservice.kafka.*;
import uz.pdp.rentalservice.repository.RentalRepository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Rental Finite State Machine (FSM) Service.
 *
 * Manages state transitions as per the sequence diagram:
 *
 * WAITING → [lock request] → ACQUIRING_LOCK
 * ACQUIRING_LOCK → [lock success] → PAYMENT_PROCESSING → [payment request]
 * ACQUIRING_LOCK → [lock failure] → FAILED
 * PAYMENT_PROCESSING → [payment success] → EJECTING → [eject request]
 * PAYMENT_PROCESSING → [payment failure] → FAILED
 * EJECTING → [eject success] → IN_THE_LEASE
 * EJECTING → [eject failure] → FAILED
 * IN_THE_LEASE → [finish] → FINISHED
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RentalFsmService {

    private final RentalRepository rentalRepository;
    private final RentalEventProducer eventProducer;

    // Initial deposit amount for authorization hold
    private static final BigDecimal INITIAL_DEPOSIT = new BigDecimal("5.00");

    /**
     * Creates a new rental and starts the FSM by publishing lock request.
     */
    @Transactional
    public Rental initiateRental(UUID userId, UUID stationId, UUID cardId, String idempotencyKey) {
        Rental rental = Rental.builder()
                .userId(userId)
                .stationId(stationId)
                .cardId(cardId)
                .idempotencyKey(idempotencyKey)
                .status(RentalStatus.ACQUIRING_LOCK)
                .build();
        rental = rentalRepository.save(rental);

        // Step 2: Publish lock request to station-service
        eventProducer.publishLockRequest(
                AcquireCabinetLockEvent.builder()
                        .rentalId(rental.getId())
                        .stationId(stationId)
                        .correlationId(rental.getId().toString())
                        .build()
        );

        log.info("Rental initiated: id={} userId={} stationId={}", rental.getId(), userId, stationId);
        return rental;
    }

    /**
     * Step 2 result: cabinet lock acquired → transition to PAYMENT_PROCESSING.
     */
    @Transactional
    public void processStationLockResult(CabinetLockResultEvent event) {
        Rental rental = getAndValidate(event.getRentalId(), RentalStatus.ACQUIRING_LOCK);
        if (rental == null) return;

        if (!event.isSuccess()) {
            failRental(rental, "Station lock failed: " + event.getFailureReason());
            return;
        }

        rental.setSlotId(event.getSlotId());
        rental.setPowerBankId(event.getPowerBankId());
        rental.setStatus(RentalStatus.PAYMENT_PROCESSING);
        rentalRepository.save(rental);

        // Step 3: Publish payment request to payment-service
        eventProducer.publishPaymentRequest(
                PaymentRequestEvent.builder()
                        .rentalId(rental.getId())
                        .userId(rental.getUserId())
                        .cardId(rental.getCardId())
                        .amount(INITIAL_DEPOSIT)
                        .idempotencyKey("payment-" + rental.getId()) // deterministic key
                        .build()
        );

        log.info("FSM → AcquireStationLockSuccess: rentalId={}", rental.getId());
    }

    /**
     * Step 3 result: payment processed → transition to EJECTING or FAILED.
     */
    @Transactional
    public void processPaymentResult(PaymentResultEvent event) {
        Rental rental = getAndValidate(event.getRentalId(), RentalStatus.PAYMENT_PROCESSING);
        if (rental == null) return;

        if (!"SUCCESS".equals(event.getCurrentStatus())) {
            failRental(rental, "Payment failed: " + event.getFailureReason());
            return;
        }

        rental.setStatus(RentalStatus.EJECTING);
        rentalRepository.save(rental);

        // Step 4: Publish eject request to station-service
        eventProducer.publishEjectRequest(
                EjectPowerBankEvent.builder()
                        .rentalId(rental.getId())
                        .stationId(rental.getStationId())
                        .slotId(rental.getSlotId())
                        .powerBankId(rental.getPowerBankId())
                        .correlationId(rental.getId().toString())
                        .build()
        );

        log.info("FSM → PaymentSuccess: rentalId={}", rental.getId());
    }

    /**
     * Step 4 result: powerbank ejected → transition to IN_THE_LEASE (done!).
     */
    @Transactional
    public void processEjectResult(EjectPowerBankResultEvent event) {
        Rental rental = getAndValidate(event.getRentalId(), RentalStatus.EJECTING);
        if (rental == null) return;

        if (!event.isSuccess()) {
            failRental(rental, "Eject failed: " + event.getFailureReason());
            return;
        }

        rental.setStatus(RentalStatus.IN_THE_LEASE);
        rental.setStartedAt(OffsetDateTime.now());
        rentalRepository.save(rental);

        log.info("FSM → InTheLease: rentalId={} ✅ User has powerbank!", rental.getId());
    }

    /**
     * Finish rental: calculate total amount and transition to FINISHED.
     */
    @Transactional
    public Rental finishRental(UUID rentalId, UUID userId) {
        Rental rental = rentalRepository.findByIdAndUserId(rentalId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Rental not found"));

        if (rental.getStatus() != RentalStatus.IN_THE_LEASE) {
            throw new IllegalStateException("Can only finish rentals in IN_THE_LEASE state");
        }

        OffsetDateTime now = OffsetDateTime.now();
        rental.setFinishedAt(now);
        rental.setStatus(RentalStatus.FINISHED);

        // Calculate total: minutes * rate
        long minutes = java.time.Duration.between(rental.getStartedAt(), now).toMinutes();
        BigDecimal total = rental.getRatePerMinute().multiply(BigDecimal.valueOf(Math.max(minutes, 1)));
        rental.setTotalAmount(total);

        rentalRepository.save(rental);
        log.info("Rental FINISHED: id={} duration={}min total={}", rentalId, minutes, total);
        return rental;
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private Rental getAndValidate(UUID rentalId, RentalStatus expectedStatus) {
        return rentalRepository.findById(rentalId)
                .filter(r -> r.getStatus() == expectedStatus)
                .orElseGet(() -> {
                    log.warn("Rental {} not in expected state {}, ignoring event", rentalId, expectedStatus);
                    return null;
                });
    }

    private void failRental(Rental rental, String reason) {
        rental.setStatus(RentalStatus.FAILED);
        rental.setFailureReason(reason);
        rentalRepository.save(rental);
        log.error("Rental FAILED: id={} reason={}", rental.getId(), reason);
    }
}
