package uz.pdp.rentalservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import uz.pdp.rentalservice.dto.*;
import uz.pdp.rentalservice.service.RentalService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rentals")
@RequiredArgsConstructor
public class RentalController {

    private final RentalService rentalService;

    /**
     * POST /api/v1/rentals — Create a new rental.
     * Returns 200 OK (idempotent: same key returns same rental).
     */
    @PostMapping
    public ResponseEntity<RentalResponse> createRental(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateRentalRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(rentalService.createRental(userId, request));
    }

    /**
     * GET /api/v1/rentals/{id}/status — Get rental status (for polling).
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<RentalResponse> getRentalStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(rentalService.getRentalStatus(id, userId));
    }

    /**
     * GET /api/v1/rentals/history — Get user's rental history.
     */
    @GetMapping("/history")
    public ResponseEntity<List<RentalResponse>> getRentalHistory(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(rentalService.getRentalHistory(userId));
    }

    /**
     * POST /api/v1/rentals/finish — Finish a rental (return powerbank).
     */
    @PostMapping("/finish")
    public ResponseEntity<RentalResponse> finishRental(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody FinishRentalRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(rentalService.finishRental(userId, request));
    }
}
