package uz.pdp.stationservice.dto;

import lombok.Builder;
import lombok.Data;
import uz.pdp.stationservice.entity.PowerBank.PowerBankStatus;

import java.util.UUID;

@Data
@Builder
public class PowerBankResponse {
    private UUID id;
    private Integer batteryLevel;
    private PowerBankStatus status;
}
