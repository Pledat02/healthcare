package com.hehe.security_service.config;

import com.hehe.security_service.dto.SecurityAccessEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {
    @Bean
    ConsumerFactory<String, SecurityAccessEvent> securityEventConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String servers) {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "security-service");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        JacksonJsonDeserializer<SecurityAccessEvent> valueDeserializer =
                new JacksonJsonDeserializer<>(SecurityAccessEvent.class, false);
        valueDeserializer.addTrustedPackages("*");
        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), valueDeserializer);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, SecurityAccessEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, SecurityAccessEvent> securityEventConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, SecurityAccessEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(securityEventConsumerFactory);
        return factory;
    }
}
