package com.hehe.api_gateway.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {
    @Bean
    KafkaTemplate<String, Object> securityKafkaTemplate(
            @Value("${spring.kafka.bootstrap-servers}") String servers) {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        config.put(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        config.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 500);
        ProducerFactory<String, Object> producerFactory = new DefaultKafkaProducerFactory<>(config);
        return new KafkaTemplate<>(producerFactory);
    }
}
