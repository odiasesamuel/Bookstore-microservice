package com.prunny.order_service.config;

import com.bookstore.events.BookOrderedEvent;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.properties.schema.registry.url}")
    private String schemaRegistryUrl;

    @Bean
    public ProducerFactory<String, BookOrderedEvent> producerFactory() {
        Map<String, Object> config = new HashMap<>();

        // Where is Kafka?
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // How to serialize the key (we'll use ISBN as key)
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // How to serialize the value (our Protobuf event)
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaProtobufSerializer.class);

        // Reliability: wait for all replicas
        config.put(ProducerConfig.ACKS_CONFIG, "all");

        // Retry up to 3 times
        config.put(ProducerConfig.RETRIES_CONFIG, 3);

        // Prevent duplicates
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        // Where is Schema Registry?
        config.put(KafkaProtobufSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, BookOrderedEvent> kafkaTemplate() {
        // KafkaTemplate is Spring's high-level API for sending messages
        return new KafkaTemplate<>(producerFactory());
    }
}
