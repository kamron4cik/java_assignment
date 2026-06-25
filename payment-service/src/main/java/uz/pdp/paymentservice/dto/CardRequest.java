package uz.pdp.paymentservice.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CardRequest {

    @NotBlank(message = "Card number is required")
    @Size(min = 13, max = 19)
    private String cardNumber;

    @NotBlank(message = "Card holder name is required")
    private String cardHolder;

    @NotNull(message = "Initial balance is required")
    @DecimalMin(value = "0.00")
    private BigDecimal initialBalance;
}
