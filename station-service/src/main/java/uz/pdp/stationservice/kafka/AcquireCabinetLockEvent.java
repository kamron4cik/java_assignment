package uz.pdp.stationservice.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Consumed from 'acquire-cabinet-lock-event' — triggers cabinet lock simulation */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcquireCabinetLockEvent {
    private UUID rentalId;
    private UUID stationId;
    private String correlationId;
}
