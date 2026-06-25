package uz.pdp.stationservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes station simulation results back to rental-service via Kafka.
 * Uses rentalId as the Kafka key for ordering within a rental flow.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StationEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishLockResult(CabinetLockResultEvent event) {
        String key = event.getRentalId().toString();
        kafkaTemplate.send("acquire-cabinet-lock-result", key, event)
                .whenComplete((r, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish lock result: rentalId={}", event.getRentalId());
                    } else {
                        log.info("Published lock result: rentalId={} success={}",
                                event.getRentalId(), event.isSuccess());
                    }
                });
    }

    public void publishEjectResult(EjectPowerBankResultEvent event) {
        String key = event.getRentalId().toString();
        kafkaTemplate.send("eject-powerbank-result", key, event)
                .whenComplete((r, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish eject result: rentalId={}", event.getRentalId());
                    } else {
                        log.info("Published eject result: rentalId={} success={}",
                                event.getRentalId(), event.isSuccess());
                    }
                });
    }
}
