package uz.pdp.stationservice.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Published to 'acquire-cabinet-lock-result' after lock simulation completes */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CabinetLockResultEvent {
    private UUID rentalId;
    private UUID stationId;
    private UUID slotId;
    private UUID powerBankId;
    private boolean success;
    private String failureReason;
    private String correlationId;
}
