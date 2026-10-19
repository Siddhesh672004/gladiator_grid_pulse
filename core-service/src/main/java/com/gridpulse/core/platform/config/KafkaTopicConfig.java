package com.gridpulse.core.platform.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import com.gridpulse.common.event.Topics;

// the core's topics, made at start (the broker does not create topics by itself): 3 partitions, 1 copy (one broker)
@Configuration
public class KafkaTopicConfig {

    private static final int PARTITIONS = 3;

    // codes and invite links: kept one hour, not kafka's default week
    @Bean
    public NewTopic accountTopic() {
        return TopicBuilder.name(Topics.ACCOUNT).partitions(PARTITIONS).replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, "3600000")
                .build();
    }

    @Bean
    public NewTopic bookingTopic() {
        return TopicBuilder.name(Topics.BOOKING).partitions(PARTITIONS).replicas(1).build();
    }

    @Bean
    public NewTopic raceTopic() {
        return TopicBuilder.name(Topics.RACE).partitions(PARTITIONS).replicas(1).build();
    }

    @Bean
    public NewTopic feedbackTopic() {
        return TopicBuilder.name(Topics.FEEDBACK).partitions(PARTITIONS).replicas(1).build();
    }
}
