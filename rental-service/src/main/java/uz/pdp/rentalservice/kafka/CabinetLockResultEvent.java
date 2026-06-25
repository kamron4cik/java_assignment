package uz.pdp.rentalservice.kafka;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/** Consumed from 'acquire-cabinet-lock-result' from station-service */
@Data
@Builder
public class CabinetLockResultEvent {
    private UUID rentalId;
    private UUID stationId;
    private UUID slotId;
    private UUID powerBankId;
    private boolean success;
    private String failureReason;
    private String correlationId;
}
