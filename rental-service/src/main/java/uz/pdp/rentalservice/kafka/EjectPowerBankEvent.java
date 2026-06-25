package uz.pdp.rentalservice.kafka;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/** Published to 'eject-powerbank-event' after successful payment */
@Data
@Builder
public class EjectPowerBankEvent {
    private UUID rentalId;
    private UUID stationId;
    private UUID slotId;
    private UUID powerBankId;
    private String correlationId;
}
