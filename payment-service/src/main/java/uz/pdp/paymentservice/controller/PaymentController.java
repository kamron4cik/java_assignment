package uz.pdp.paymentservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import uz.pdp.paymentservice.dto.*;
import uz.pdp.paymentservice.service.PaymentService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /** POST /v1/cards — Add/bind a card */
    @PostMapping("/cards")
    public ResponseEntity<CardResponse> addCard(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CardRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.addCard(userId, request));
    }

    /** GET /v1/cards — List user's cards */
    @GetMapping("/cards")
    public ResponseEntity<List<CardResponse>> getCards(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(paymentService.getCards(userId));
    }

    /** DELETE /v1/cards/{id} — Remove a card */
    @DeleteMapping("/cards/{id}")
    public ResponseEntity<Void> deleteCard(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(jwt.getSubject());
        paymentService.deleteCard(id, userId);
        return ResponseEntity.noContent().build();
    }

    /** POST /v1/payments — Create a payment */
    @PostMapping("/payments")
    public ResponseEntity<PaymentResponse> createPayment(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreatePaymentRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        // Returns 200 (not 201) if idempotency key already exists
        return ResponseEntity.ok(paymentService.createPayment(userId, request));
    }

    /** DELETE /v1/payments/{id} — Cancel a payment */
    @DeleteMapping("/payments/{id}")
    public ResponseEntity<PaymentResponse> cancelPayment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(paymentService.cancelPayment(id, userId));
    }
}
