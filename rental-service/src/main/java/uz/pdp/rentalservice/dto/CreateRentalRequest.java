package uz.pdp.rentalservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateRentalRequest {

    @NotNull(message = "Station ID is required")
    private UUID stationId;

    @NotNull(message = "Card ID is required")
    private UUID cardId;

    // Client-generated idempotency key (UUID recommended)
    private String idempotencyKey;
}
