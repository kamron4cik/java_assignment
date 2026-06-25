package uz.pdp.rentalservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes events from rental-service to other services via Kafka.
 * Uses rentalId as key to maintain ordering per rental.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RentalEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishLockRequest(AcquireCabinetLockEvent event) {
        send("acquire-cabinet-lock-event", event.getRentalId().toString(), event);
    }

    public void publishPaymentRequest(PaymentRequestEvent event) {
        send("payment-request", event.getRentalId().toString(), event);
    }

    public void publishEjectRequest(EjectPowerBankEvent event) {
        send("eject-powerbank-event", event.getRentalId().toString(), event);
    }

    private void send(String topic, String key, Object payload) {
        kafkaTemplate.send(topic, key, payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish to topic={} key={}: {}", topic, key, ex.getMessage());
                    } else {
                        log.debug("Published to topic={} key={}", topic, key);
                    }
                });
    }
}
