package uz.pdp.rentalservice.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import uz.pdp.rentalservice.kafka.CabinetLockResultEvent;
import uz.pdp.rentalservice.kafka.EjectPowerBankResultEvent;
import uz.pdp.rentalservice.kafka.PaymentResultEvent;

import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:rental-service-group}")
    private String groupId;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class,
                ProducerConfig.ACKS_CONFIG, "all",
                ProducerConfig.RETRIES_CONFIG, 3,
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true
        );
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Dedicated producer factory for the Outbox pattern.
     * Uses StringSerializer so raw JSON payloads are NOT re-encoded by JsonSerializer.
     */
    @Bean
    public ProducerFactory<String, String> outboxProducerFactory() {
        Map<String, Object> props = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.ACKS_CONFIG, "all",
                ProducerConfig.RETRIES_CONFIG, 3,
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true
        );
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, String> outboxKafkaTemplate() {
        return new KafkaTemplate<>(outboxProducerFactory());
    }

    // ── Per-topic typed consumer factories ────────────────────────────────────

    @SuppressWarnings("unchecked")
    private ConsumerFactory<String, Object> buildConsumerFactory(Class<?> targetType) {
        return new DefaultKafkaConsumerFactory<>(
                Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                        ConsumerConfig.GROUP_ID_CONFIG, groupId
                ),
                new StringDeserializer(),
                new JsonDeserializer<>((Class<Object>) targetType, false)
        );
    }

    /** acquire-cabinet-lock-result → CabinetLockResultEvent */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object>
            lockResultListenerContainerFactory() {
        var f = new ConcurrentKafkaListenerContainerFactory<String, Object>();
        f.setConsumerFactory(buildConsumerFactory(CabinetLockResultEvent.class));
        return f;
    }

    /** payment-events → PaymentResultEvent */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object>
            paymentResultListenerContainerFactory() {
        var f = new ConcurrentKafkaListenerContainerFactory<String, Object>();
        f.setConsumerFactory(buildConsumerFactory(PaymentResultEvent.class));
        return f;
    }

    /** eject-powerbank-result → EjectPowerBankResultEvent */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object>
            ejectResultListenerContainerFactory() {
        var f = new ConcurrentKafkaListenerContainerFactory<String, Object>();
        f.setConsumerFactory(buildConsumerFactory(EjectPowerBankResultEvent.class));
        return f;
    }
}

