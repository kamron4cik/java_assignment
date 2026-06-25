package uz.pdp.rentalservice.kafka;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/** Published to 'acquire-cabinet-lock-event' to trigger station-service */
@Data
@Builder
public class AcquireCabinetLockEvent {
    private UUID rentalId;
    private UUID stationId;
    private String correlationId;
}
