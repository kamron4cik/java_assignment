package uz.pdp.rentalservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import uz.pdp.rentalservice.entity.OutboxEvent;
import uz.pdp.rentalservice.repository.OutboxEventRepository;

/**
 * Publishes events from rental-service to other services via Kafka.
 * Uses rentalId as key to maintain ordering per rental.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RentalEventProducer {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

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
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType(payload.getClass().getSimpleName())
                    .aggregateId(key)
                    .eventType(topic)
                    .payload(jsonPayload)
                    .status(OutboxEvent.OutboxStatus.PENDING)
                    .build();
            outboxEventRepository.save(outboxEvent);
            log.debug("Saved OutboxEvent for topic={} key={}", topic, key);
        } catch (Exception ex) {
            log.error("Failed to serialize and save outbox event for topic={} key={}: {}", topic, key, ex.getMessage());
            throw new RuntimeException("Could not create outbox event", ex);
        }
    }
}
