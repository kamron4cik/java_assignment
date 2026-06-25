package uz.pdp.rentalservice.dto;

import lombok.Builder;
import lombok.Data;
import uz.pdp.rentalservice.entity.Rental.RentalStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class RentalResponse {
    private UUID id;
    private UUID userId;
    private UUID stationId;
    private UUID cardId;
    private UUID slotId;
    private UUID powerBankId;
    private RentalStatus status;
    private String idempotencyKey;
    private String failureReason;
    private BigDecimal ratePerMinute;
    private BigDecimal totalAmount;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private OffsetDateTime createdAt;
}
