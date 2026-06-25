package uz.pdp.stationservice.dto;

import lombok.Builder;
import lombok.Data;
import uz.pdp.stationservice.entity.Slot.SlotStatus;
import uz.pdp.stationservice.entity.PowerBank.PowerBankStatus;

import java.util.UUID;

@Data
@Builder
public class SlotResponse {
    private UUID id;
    private Integer slotNumber;
    private SlotStatus status;
    private PowerBankResponse powerBank;
}
