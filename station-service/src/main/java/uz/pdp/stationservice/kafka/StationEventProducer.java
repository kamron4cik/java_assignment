package uz.pdp.stationservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import uz.pdp.stationservice.entity.OutboxEvent;
import uz.pdp.stationservice.repository.OutboxEventRepository;

/**
 * Publishes station simulation results back to rental-service via Kafka.
 * Uses rentalId as the Kafka key for ordering within a rental flow.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StationEventProducer {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public void publishLockResult(CabinetLockResultEvent event) {
        String key = event.getRentalId().toString();
        saveToOutbox("acquire-cabinet-lock-result", key, event);
        log.info("Saved lock result to outbox: rentalId={} success={}", event.getRentalId(), event.isSuccess());
    }

    public void publishEjectResult(EjectPowerBankResultEvent event) {
        String key = event.getRentalId().toString();
        saveToOutbox("eject-powerbank-result", key, event);
        log.info("Saved eject result to outbox: rentalId={} success={}", event.getRentalId(), event.isSuccess());
    }

    private void saveToOutbox(String topic, String key, Object payload) {
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
        } catch (Exception ex) {
            log.error("Failed to serialize and save outbox event for topic={} key={}: {}", topic, key, ex.getMessage());
            throw new RuntimeException("Could not create outbox event", ex);
        }
    }
}
