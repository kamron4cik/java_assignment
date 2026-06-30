package uz.pdp.stationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.pdp.stationservice.entity.OutboxEvent;
import uz.pdp.stationservice.repository.OutboxEventRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxScheduler {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> outboxKafkaTemplate;

    @Scheduled(fixedDelayString = "${outbox.scheduler.delay:2000}")
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxEvent.OutboxStatus.PENDING);
        
        for (OutboxEvent event : pendingEvents) {
            try {
                // Send raw JSON payload to Kafka
                outboxKafkaTemplate.send(event.getEventType(), event.getAggregateId(), event.getPayload())
                        .get(); // Synchronous wait to ensure delivery order within the same thread

                event.setStatus(OutboxEvent.OutboxStatus.SENT);
                log.debug("Sent OutboxEvent ID={}", event.getId());
            } catch (Exception e) {
                log.error("Failed to send OutboxEvent ID={}: {}", event.getId(), e.getMessage());
            }
        }
    }
}
