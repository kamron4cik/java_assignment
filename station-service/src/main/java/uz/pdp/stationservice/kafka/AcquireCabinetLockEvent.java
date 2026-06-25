package uz.pdp.stationservice.kafka;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/** Consumed from 'acquire-cabinet-lock-event' — triggers cabinet lock simulation */
@Data
@Builder
public class AcquireCabinetLockEvent {
    private UUID rentalId;
    private UUID stationId;
    private String correlationId;
}
