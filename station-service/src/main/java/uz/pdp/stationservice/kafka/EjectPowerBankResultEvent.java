package uz.pdp.stationservice.kafka;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/** Published to 'eject-powerbank-result' after eject simulation completes */
@Data
@Builder
public class EjectPowerBankResultEvent {
    private UUID rentalId;
    private UUID stationId;
    private UUID slotId;
    private UUID powerBankId;
    private boolean success;
    private String failureReason;
    private String correlationId;
}
