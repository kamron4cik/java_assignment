package uz.pdp.rentalservice.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Consumed from 'eject-powerbank-result' from station-service */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EjectPowerBankResultEvent {
    private UUID rentalId;
    private UUID stationId;
    private UUID slotId;
    private UUID powerBankId;
    private boolean success;
    private String failureReason;
    private String correlationId;
}
