package com.gridpulse.payment.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import com.gridpulse.common.event.Topics;

// the topic this service publishes to, made at start: 3 partitions like every gridpulse topic, 1 copy (one broker)
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic paymentTopic() {
        return TopicBuilder.name(Topics.PAYMENT).partitions(3).replicas(1).build();
    }
}
