package uz.pdp.paymentservice.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class CardResponse {
    private UUID id;
    private UUID userId;
    private String cardNumber; // masked: **** **** **** 1234
    private String cardHolder;
    private BigDecimal balance;
    private Boolean isDefault;
    private Boolean isActive;
    private OffsetDateTime createdAt;
}
