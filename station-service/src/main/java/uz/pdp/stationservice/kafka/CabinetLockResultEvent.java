package uz.pdp.stationservice.kafka;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/** Published to 'acquire-cabinet-lock-result' after lock simulation completes */
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
