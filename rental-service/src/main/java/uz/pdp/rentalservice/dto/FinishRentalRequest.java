package uz.pdp.rentalservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class FinishRentalRequest {

    @NotNull(message = "Rental ID is required")
    private UUID rentalId;

    // The slot/station where the powerbank is being returned
    private UUID returnStationId;
    private UUID returnSlotId;
}
