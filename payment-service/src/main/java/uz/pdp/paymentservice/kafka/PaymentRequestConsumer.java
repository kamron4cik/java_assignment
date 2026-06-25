package uz.pdp.paymentservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import uz.pdp.paymentservice.service.PaymentService;

/**
 * Listens for payment requests from rental-service on 'payment-request' topic.
 * Processes payment and publishes result to 'payment-events'.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRequestConsumer {

    private final PaymentService paymentService;

    @KafkaListener(
            topics = "payment-request",
            groupId = "${spring.kafka.consumer.group-id:payment-service-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentRequest(PaymentRequestEvent event) {
        log.info("Received payment request: rentalId={}, amount={}",
                event.getRentalId(), event.getAmount());
        try {
            paymentService.processPaymentRequest(event);
        } catch (Exception e) {
            log.error("Error processing payment request for rentalId={}: {}",
                    event.getRentalId(), e.getMessage(), e);
        }
    }
}
