package com.gridpulse.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.gridpulse.common.event.EventReader;
import com.gridpulse.common.inbox.Inbox;
import com.gridpulse.common.inbox.ProcessedEventRepository;
import com.gridpulse.common.outbox.EventPublisher;
import com.gridpulse.common.outbox.OutboxRelay;
import com.gridpulse.common.outbox.OutboxRepository;

// every service that has this jar gets: outbox (EventPublisher + OutboxRelay), inbox, EventReader
// the package is added to the scanned ones, so the entities and repositories are found without @EntityScan
@AutoConfiguration(before = { HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class })
@AutoConfigurationPackage(basePackages = "com.gridpulse.common")
@EnableScheduling
public class CommonAutoConfiguration {

    @Bean
    public EventReader eventReader() {
        return new EventReader();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(JpaRepository.class)
    static class OutboxInboxConfiguration {

        // source = the service name in every envelope
        @Bean
        public EventPublisher eventPublisher(OutboxRepository outboxRepository,
                @Value("${spring.application.name:unknown}") String source) {
            return new EventPublisher(outboxRepository, source);
        }

        @Bean
        public Inbox inbox(ProcessedEventRepository processedEventRepository) {
            return new Inbox(processedEventRepository);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({ JpaRepository.class, KafkaTemplate.class })
    @ConditionalOnProperty(name = "gridpulse.outbox.relay.enabled", havingValue = "true", matchIfMissing = true)
    static class RelayConfiguration {

        @Bean
        public OutboxRelay outboxRelay(OutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate) {
            return new OutboxRelay(outboxRepository, kafkaTemplate);
        }
    }
}
