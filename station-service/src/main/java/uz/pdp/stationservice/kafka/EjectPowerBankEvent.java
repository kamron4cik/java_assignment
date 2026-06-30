package uz.pdp.stationservice.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Consumed from 'eject-powerbank-event' — triggers powerbank eject simulation */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EjectPowerBankEvent {
    private UUID rentalId;
    private UUID stationId;
    private UUID slotId;
    private UUID powerBankId;
    private String correlationId;
}
