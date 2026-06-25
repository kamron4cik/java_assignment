package uz.pdp.stationservice.kafka;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/** Consumed from 'eject-powerbank-event' — triggers powerbank eject simulation */
@Data
@Builder
public class EjectPowerBankEvent {
    private UUID rentalId;
    private UUID stationId;
    private UUID slotId;
    private UUID powerBankId;
    private String correlationId;
}
