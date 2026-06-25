package uz.pdp.rentalservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uz.pdp.rentalservice.entity.Rental;
import uz.pdp.rentalservice.entity.Rental.RentalStatus;
import uz.pdp.rentalservice.kafka.PaymentRequestEvent;
import uz.pdp.rentalservice.kafka.RentalEventProducer;
import uz.pdp.rentalservice.repository.RentalRepository;

import java.util.List;

/**
 * Recurring billing scheduler.
 *
 * Every {@code billing.interval-minutes} (default: 1) this job finds all active
 * rentals in {@code IN_THE_LEASE} state and publishes a {@code PaymentRequestEvent}
 * to the payment-service for each one.
 *
 * The idempotency key is constructed as:
 *   "recurring-{rentalId}-{epochMinute}"
 * so re-runs within the same minute are safe (payment-service deduplicates by key).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringBillingService {

    private final RentalRepository    rentalRepository;
    private final RentalEventProducer eventProducer;

    @Value("${billing.interval-minutes:1}")
    private int intervalMinutes;

    /**
     * Runs every minute (fixedDelay ensures no overlapping runs).
     * Configurable via {@code billing.interval-minutes}.
     */
    @Scheduled(fixedDelayString = "${billing.interval-ms:60000}")
    public void chargeActiveRentals() {
        List<Rental> activeRentals = rentalRepository.findByStatus(RentalStatus.IN_THE_LEASE);

        if (activeRentals.isEmpty()) {
            return; // nothing to do — avoid noisy logs
        }

        log.info("Recurring billing: {} active rental(s) to charge", activeRentals.size());

        long epochMinute = System.currentTimeMillis() / 60_000;

        for (Rental rental : activeRentals) {
            try {
                String idempotencyKey = String.format("recurring-%s-%d", rental.getId(), epochMinute);

                eventProducer.publishPaymentRequest(
                        PaymentRequestEvent.builder()
                                .rentalId(rental.getId())
                                .userId(rental.getUserId())
                                .cardId(rental.getCardId())
                                .amount(rental.getRatePerMinute())   // charge per-minute rate
                                .idempotencyKey(idempotencyKey)
                                .build()
                );

                log.info("Billed rental={} user={} amount={}/min key={}",
                        rental.getId(), rental.getUserId(),
                        rental.getRatePerMinute(), idempotencyKey);

            } catch (Exception e) {
                log.error("Failed to bill rental={}: {}", rental.getId(), e.getMessage());
            }
        }
    }
}
