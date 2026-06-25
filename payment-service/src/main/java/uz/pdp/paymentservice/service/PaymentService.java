package uz.pdp.paymentservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pdp.paymentservice.dto.*;
import uz.pdp.paymentservice.entity.Card;
import uz.pdp.paymentservice.entity.Payment;
import uz.pdp.paymentservice.entity.Payment.PaymentStatus;
import uz.pdp.paymentservice.exception.CardNotFoundException;
import uz.pdp.paymentservice.exception.InsufficientFundsException;
import uz.pdp.paymentservice.exception.PaymentNotFoundException;
import uz.pdp.paymentservice.kafka.PaymentEventProducer;
import uz.pdp.paymentservice.kafka.PaymentRequestEvent;
import uz.pdp.paymentservice.repository.CardRepository;
import uz.pdp.paymentservice.repository.PaymentRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final CardRepository cardRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentEventProducer eventProducer;

    // ─── Card Operations ──────────────────────────────────────────────────────

    @Transactional
    public CardResponse addCard(UUID userId, CardRequest request) {
        String masked = maskCardNumber(request.getCardNumber());
        Card card = Card.builder()
                .userId(userId)
                .cardNumber(masked)
                .cardHolder(request.getCardHolder())
                .balance(request.getInitialBalance())
                .build();
        return mapToCardResponse(cardRepository.save(card));
    }

    @Transactional(readOnly = true)
    public List<CardResponse> getCards(UUID userId) {
        return cardRepository.findByUserIdAndIsActiveTrue(userId).stream()
                .map(this::mapToCardResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteCard(UUID cardId, UUID userId) {
        Card card = cardRepository.findByIdAndUserIdAndIsActiveTrue(cardId, userId)
                .orElseThrow(() -> new CardNotFoundException("Card not found"));
        card.setIsActive(false);
        cardRepository.save(card);
    }

    // ─── Payment Operations ───────────────────────────────────────────────────

    /**
     * Create payment via REST API (for direct calls).
     * Handles idempotency: if idempotency_key already exists, return existing payment (200).
     */
    @Transactional
    public PaymentResponse createPayment(UUID userId, CreatePaymentRequest request) {
        // Idempotency check: return existing if key already used
        return paymentRepository.findByIdempotencyKey(request.getIdempotencyKey())
                .map(existing -> {
                    log.info("Duplicate payment request, returning existing: idempotencyKey={}",
                            request.getIdempotencyKey());
                    return mapToPaymentResponse(existing);
                })
                .orElseGet(() -> doCreateAndProcessPayment(userId, request));
    }

    /**
     * Process payment triggered from Kafka (rental-service -> payment-request topic).
     */
    @Transactional
    public void processPaymentRequest(PaymentRequestEvent event) {
        // Check idempotency first
        paymentRepository.findByIdempotencyKey(event.getIdempotencyKey())
                .ifPresent(existing -> {
                    log.info("Duplicate payment request via Kafka: {}", event.getIdempotencyKey());
                    return;
                });

        Payment payment;
        try {
            payment = Payment.builder()
                    .userId(event.getUserId())
                    .cardId(event.getCardId())
                    .rentalId(event.getRentalId())
                    .amount(event.getAmount())
                    .idempotencyKey(event.getIdempotencyKey())
                    .status(PaymentStatus.PENDING)
                    .build();
            payment = paymentRepository.save(payment);
        } catch (DataIntegrityViolationException e) {
            // Race condition: another thread already created this payment
            log.warn("Idempotency key conflict (race condition): {}", event.getIdempotencyKey());
            return;
        }

        // Atomically debit the card using pessimistic locking
        PaymentStatus previousStatus = payment.getStatus();
        
        try {
            Card card = cardRepository.findByIdForUpdate(event.getCardId())
                    .orElseThrow(() -> new CardNotFoundException("Card not found"));
            
            if (!card.getIsActive()) {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("Card is inactive");
                log.warn("Payment FAILED - card inactive: rentalId={}", event.getRentalId());
            } else if (card.getBalance().compareTo(event.getAmount()) < 0) {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("Insufficient funds");
                log.warn("Payment FAILED - insufficient funds: rentalId={}", event.getRentalId());
            } else {
                card.setBalance(card.getBalance().subtract(event.getAmount()));
                cardRepository.save(card);
                payment.setStatus(PaymentStatus.SUCCESS);
                log.info("Payment SUCCESS: rentalId={}, amount={}", event.getRentalId(), event.getAmount());
            }
        } catch (Exception e) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(e.getMessage());
            log.error("Payment FAILED - error: rentalId={}, error={}", event.getRentalId(), e.getMessage());
        }

        payment = paymentRepository.save(payment);
        eventProducer.publishStatusChange(payment, previousStatus);
    }

    @Transactional
    public PaymentResponse cancelPayment(UUID paymentId, UUID userId) {
        Payment payment = paymentRepository.findByIdAndUserId(paymentId, userId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Can only cancel PENDING payments");
        }

        PaymentStatus previousStatus = payment.getStatus();
        payment.setStatus(PaymentStatus.CANCELLED);
        payment = paymentRepository.save(payment);
        eventProducer.publishStatusChange(payment, previousStatus);
        return mapToPaymentResponse(payment);
    }

    // ─── private helpers ──────────────────────────────────────────────────────

    private PaymentResponse doCreateAndProcessPayment(UUID userId, CreatePaymentRequest request) {
        Card card = cardRepository.findByIdAndUserIdAndIsActiveTrue(request.getCardId(), userId)
                .orElseThrow(() -> new CardNotFoundException("Card not found"));

        Payment payment;
        try {
            payment = Payment.builder()
                    .userId(userId)
                    .cardId(card.getId())
                    .rentalId(request.getRentalId())
                    .amount(request.getAmount())
                    .idempotencyKey(request.getIdempotencyKey())
                    .status(PaymentStatus.PENDING)
                    .build();
            payment = paymentRepository.save(payment);
        } catch (DataIntegrityViolationException e) {
            // Race condition — another thread saved with same idempotency key
            return paymentRepository.findByIdempotencyKey(request.getIdempotencyKey())
                    .map(this::mapToPaymentResponse)
                    .orElseThrow(() -> new RuntimeException("Unexpected error"));
        }

        PaymentStatus previousStatus = payment.getStatus();
        
        try {
            Card lockedCard = cardRepository.findByIdForUpdate(card.getId())
                    .orElseThrow(() -> new CardNotFoundException("Card not found"));
                    
            if (!lockedCard.getIsActive()) {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("Card is inactive");
            } else if (lockedCard.getBalance().compareTo(request.getAmount()) < 0) {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("Insufficient funds");
            } else {
                lockedCard.setBalance(lockedCard.getBalance().subtract(request.getAmount()));
                cardRepository.save(lockedCard);
                payment.setStatus(PaymentStatus.SUCCESS);
            }
        } catch (Exception e) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(e.getMessage());
        }

        payment = paymentRepository.save(payment);
        eventProducer.publishStatusChange(payment, previousStatus);
        return mapToPaymentResponse(payment);
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) return cardNumber;
        String last4 = cardNumber.substring(cardNumber.length() - 4);
        return "**** **** **** " + last4;
    }

    private CardResponse mapToCardResponse(Card card) {
        return CardResponse.builder()
                .id(card.getId())
                .userId(card.getUserId())
                .cardNumber(card.getCardNumber())
                .cardHolder(card.getCardHolder())
                .balance(card.getBalance())
                .isDefault(card.getIsDefault())
                .isActive(card.getIsActive())
                .createdAt(card.getCreatedAt())
                .build();
    }

    private PaymentResponse mapToPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .userId(payment.getUserId())
                .cardId(payment.getCardId())
                .rentalId(payment.getRentalId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .idempotencyKey(payment.getIdempotencyKey())
                .failureReason(payment.getFailureReason())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
