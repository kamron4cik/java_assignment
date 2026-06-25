package uz.pdp.paymentservice.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreatePaymentRequest {

    @NotNull(message = "Card ID is required")
    private UUID cardId;

    @NotNull(message = "Rental ID is required")
    private UUID rentalId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    @Digits(integer = 17, fraction = 2, message = "Invalid amount format")
    private BigDecimal amount;

    @NotBlank(message = "Idempotency key is required")
    @Size(max = 100)
    private String idempotencyKey;
}
