package uz.pdp.paymentservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import uz.pdp.paymentservice.entity.Payment;
import uz.pdp.paymentservice.entity.Payment.PaymentStatus;

import java.time.OffsetDateTime;

/**
 * Publishes payment status change events to the 'payment-events' Kafka topic.
 *
 * Kafka key = paymentId (UUID string) — this ensures all events for the same
 * payment land on the same partition, guaranteeing ordering per payment.
 *
 * If Kafka is unavailable: the send is fire-and-forget with error logging.
 * For production, use the Outbox pattern to guarantee at-least-once delivery.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private static final String TOPIC = "payment-events";

    private final KafkaTemplate<String, PaymentStatusChangedEvent> kafkaTemplate;

    public void publishStatusChange(Payment payment, PaymentStatus previousStatus) {
        PaymentStatusChangedEvent event = PaymentStatusChangedEvent.builder()
                .paymentId(payment.getId())
                .rentalId(payment.getRentalId())
                .userId(payment.getUserId())
                .cardId(payment.getCardId())
                .amount(payment.getAmount())
                .previousStatus(previousStatus)
                .currentStatus(payment.getStatus())
                .failureReason(payment.getFailureReason())
                .occurredAt(OffsetDateTime.now())
                .build();

        // Use paymentId as Kafka key — all events for the same payment
        // will go to the same partition, guaranteeing ordering.
        String key = payment.getId().toString();

        kafkaTemplate.send(TOPIC, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish payment event for paymentId={}: {}",
                                payment.getId(), ex.getMessage());
                        // TODO: Store in outbox table for guaranteed delivery
                    } else {
                        log.info("Published payment event: paymentId={} status={}",
                                payment.getId(), payment.getStatus());
                    }
                });
    }
}
