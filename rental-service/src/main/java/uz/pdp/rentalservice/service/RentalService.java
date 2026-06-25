package uz.pdp.rentalservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pdp.rentalservice.dto.*;
import uz.pdp.rentalservice.entity.Rental;
import uz.pdp.rentalservice.repository.RentalRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RentalService {

    private final RentalRepository rentalRepository;
    private final RentalFsmService fsmService;

    /**
     * Create a new rental — idempotent.
     * Returns existing rental (200 OK) if idempotency key already used.
     */
    @Transactional
    public RentalResponse createRental(UUID userId, CreateRentalRequest request) {
        String idempotencyKey = request.getIdempotencyKey() != null
                ? request.getIdempotencyKey()
                : UUID.randomUUID().toString();

        // Idempotency check
        return rentalRepository.findByIdempotencyKey(idempotencyKey)
                .map(existing -> mapToResponse(existing))
                .orElseGet(() -> {
                    try {
                        Rental rental = fsmService.initiateRental(
                                userId, request.getStationId(),
                                request.getCardId(), idempotencyKey
                        );
                        return mapToResponse(rental);
                    } catch (DataIntegrityViolationException e) {
                        // Race condition: return existing
                        return rentalRepository.findByIdempotencyKey(idempotencyKey)
                                .map(this::mapToResponse)
                                .orElseThrow(() -> new RuntimeException("Unexpected error"));
                    }
                });
    }

    @Transactional(readOnly = true)
    public RentalResponse getRentalStatus(UUID rentalId, UUID userId) {
        Rental rental = rentalRepository.findByIdAndUserId(rentalId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Rental not found"));
        return mapToResponse(rental);
    }

    @Transactional(readOnly = true)
    public List<RentalResponse> getRentalHistory(UUID userId) {
        return rentalRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public RentalResponse finishRental(UUID userId, FinishRentalRequest request) {
        return mapToResponse(fsmService.finishRental(request.getRentalId(), userId));
    }

    // ─── mapping ─────────────────────────────────────────────────────────────

    private RentalResponse mapToResponse(Rental rental) {
        return RentalResponse.builder()
                .id(rental.getId())
                .userId(rental.getUserId())
                .stationId(rental.getStationId())
                .cardId(rental.getCardId())
                .slotId(rental.getSlotId())
                .powerBankId(rental.getPowerBankId())
                .status(rental.getStatus())
                .idempotencyKey(rental.getIdempotencyKey())
                .failureReason(rental.getFailureReason())
                .ratePerMinute(rental.getRatePerMinute())
                .totalAmount(rental.getTotalAmount())
                .startedAt(rental.getStartedAt())
                .finishedAt(rental.getFinishedAt())
                .createdAt(rental.getCreatedAt())
                .build();
    }
}
