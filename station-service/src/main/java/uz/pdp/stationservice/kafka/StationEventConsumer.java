package uz.pdp.stationservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import uz.pdp.stationservice.service.StationSimulationService;

/**
 * Consumes events from rental-service and simulates IoT station behavior.
 * In real life, these would communicate with physical IoT devices.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StationEventConsumer {

    private final StationSimulationService simulationService;

    /** Simulates locking the cabinet so a powerbank can be dispensed */
    @KafkaListener(
            topics = "acquire-cabinet-lock-event",
            groupId = "${spring.kafka.consumer.group-id:station-service-group}"
    )
    public void handleLockEvent(AcquireCabinetLockEvent event) {
        log.info("Simulating cabinet lock: rentalId={}, stationId={}",
                event.getRentalId(), event.getStationId());
        simulationService.simulateCabinetLock(event);
    }

    /** Simulates physically ejecting a powerbank from its slot */
    @KafkaListener(
            topics = "eject-powerbank-event",
            groupId = "${spring.kafka.consumer.group-id:station-service-group}"
    )
    public void handleEjectEvent(EjectPowerBankEvent event) {
        log.info("Simulating powerbank eject: rentalId={}, slotId={}",
                event.getRentalId(), event.getSlotId());
        simulationService.simulateEjectPowerBank(event);
    }
}
