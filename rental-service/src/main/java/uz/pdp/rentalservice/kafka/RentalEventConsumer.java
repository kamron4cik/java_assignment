package uz.pdp.rentalservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import uz.pdp.rentalservice.service.RentalFsmService;

/**
 * Consumes events from station-service and payment-service.
 * Feeds results into the Rental FSM.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RentalEventConsumer {

    private final RentalFsmService fsmService;

    /** Step 2 result: station locked or failed */
    @KafkaListener(
            topics = "acquire-cabinet-lock-result",
            groupId = "${spring.kafka.consumer.group-id:rental-service-group}",
            containerFactory = "lockResultListenerContainerFactory"
    )
    public void handleLockResult(CabinetLockResultEvent event) {
        log.info("Lock result received: rentalId={} success={}", event.getRentalId(), event.isSuccess());
        fsmService.processStationLockResult(event);
    }

    /** Step 3 result: payment succeeded or failed */
    @KafkaListener(
            topics = "payment-events",
            groupId = "${spring.kafka.consumer.group-id:rental-service-group}",
            containerFactory = "paymentResultListenerContainerFactory"
    )
    public void handlePaymentResult(PaymentResultEvent event) {
        if (event.getRentalId() == null) {
            return; // payment not related to a rental
        }
        log.info("Payment result received: rentalId={} status={}", event.getRentalId(), event.getCurrentStatus());
        fsmService.processPaymentResult(event);
    }

    /** Step 4 result: powerbank ejected or failed */
    @KafkaListener(
            topics = "eject-powerbank-result",
            groupId = "${spring.kafka.consumer.group-id:rental-service-group}",
            containerFactory = "ejectResultListenerContainerFactory"
    )
    public void handleEjectResult(EjectPowerBankResultEvent event) {
        log.info("Eject result received: rentalId={} success={}", event.getRentalId(), event.isSuccess());
        fsmService.processEjectResult(event);
    }
}
