package uz.pdp.stationservice.config;

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
import uz.pdp.stationservice.kafka.AcquireCabinetLockEvent;
import uz.pdp.stationservice.kafka.EjectPowerBankEvent;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:station-service-group}")
    private String groupId;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ConsumerFactory<String, Object> lockEventConsumerFactory() {
        return buildConsumerFactory(AcquireCabinetLockEvent.class);
    }

    @Bean
    public ConsumerFactory<String, Object> ejectEventConsumerFactory() {
        return buildConsumerFactory(EjectPowerBankEvent.class);
    }

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

    /** Factory for acquire-cabinet-lock-event topic */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object>
            lockEventListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
        factory.setConsumerFactory(lockEventConsumerFactory());
        return factory;
    }

    /** Factory for eject-powerbank-event topic */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object>
            ejectEventListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
        factory.setConsumerFactory(ejectEventConsumerFactory());
        return factory;
    }
}
