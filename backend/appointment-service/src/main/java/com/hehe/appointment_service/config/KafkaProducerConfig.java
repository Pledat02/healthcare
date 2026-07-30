package com.hehe.appointment_service.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Khai bao tuong minh producer Kafka (khong dua vao auto-config de tranh lech generic).
 * Serialize value bang JSON, tat type-header de consumer (khac package) doc duoc.
 */
@Configuration
public class KafkaProducerConfig {

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(
            @Value("${spring.kafka.bootstrap-servers}") String servers) {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        cfg.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        cfg.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        cfg.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        // Kafka chet -> send() fail nhanh, khong treo dat lich (PRD muc 6)
        cfg.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 3000);
        ProducerFactory<String, Object> pf = new DefaultKafkaProducerFactory<>(cfg);
        return new KafkaTemplate<>(pf);
    }
}
