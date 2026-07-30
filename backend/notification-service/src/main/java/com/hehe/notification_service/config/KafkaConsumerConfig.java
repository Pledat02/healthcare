package com.hehe.notification_service.config;

import com.hehe.notification_service.dto.event.AppointmentNotificationEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Khai bao tuong minh consumer Kafka + @EnableKafka de kich hoat @KafkaListener.
 * (Spring Boot 4.1 module hoa auto-config nen chi them spring-kafka la chua du.)
 * Deserialize JSON thang thanh AppointmentNotificationEvent (producer da tat type-header).
 */
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, AppointmentNotificationEvent> consumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String servers) {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        cfg.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-service");
        cfg.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        JsonDeserializer<AppointmentNotificationEvent> valueDeser =
                new JsonDeserializer<>(AppointmentNotificationEvent.class, false);
        valueDeser.addTrustedPackages("*");
        return new DefaultKafkaConsumerFactory<>(cfg, new StringDeserializer(), valueDeser);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, AppointmentNotificationEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, AppointmentNotificationEvent> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, AppointmentNotificationEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }
}
