package com.gridpulse.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;

import com.gridpulse.ai.feedback.dto.FeedbackLabel;
import com.gridpulse.ai.feedback.dto.Sentiment;
import com.gridpulse.ai.feedback.service.GroqService;
import com.gridpulse.ai.support.AiIntegrationTest;
import com.gridpulse.common.event.EventEnvelope;
import com.gridpulse.common.event.EventJson;
import com.gridpulse.common.event.EventReader;
import com.gridpulse.common.event.FeedbackLabelled;
import com.gridpulse.common.event.FeedbackSubmitted;
import com.gridpulse.common.event.Topics;

// with a real kafka (its own container): FeedbackSubmitted in -> the label (groq faked) -> FeedbackLabelled out,
// once even when the same event comes twice
@Import(FeedbackKafkaIntegrationTest.FakeGroq.class)
class FeedbackKafkaIntegrationTest extends AiIntegrationTest {

    @ServiceConnection
    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:4.0.0");

    static {
        KAFKA.start();
    }

    // the other tests keep the relay and the listeners off
    @DynamicPropertySource
    static void kafkaOn(DynamicPropertyRegistry registry) {
        registry.add("gridpulse.outbox.relay.enabled", () -> "true");
        registry.add("spring.kafka.listener.auto-startup", () -> "true");
        registry.add("spring.kafka.admin.auto-create", () -> "true");
    }

    // groq without a network: every feedback is negative and urgent
    @TestConfiguration(proxyBeanMethods = false)
    static class FakeGroq {

        @Bean
        @Primary
        GroqService fakeGroqService() {
            return new GroqService() {
                @Override
                public boolean isEnabled() {
                    return true;
                }

                @Override
                public String ask(String instructions, String text) {
                    return "";
                }

                @Override
                public <T> T askFor(String instructions, String text, Class<T> type) {
                    return type.cast(new FeedbackLabel(Sentiment.NEGATIVE, true));
                }
            };
        }
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private EventReader eventReader;

    @Test
    void submittedFeedbackComesBackLabelledOnce() throws Exception {
        String submitted = EventJson.MAPPER.writeValueAsString(new EventEnvelope(UUID.randomUUID().toString(),
                "FeedbackSubmitted", Instant.now(), "core",
                EventJson.MAPPER.valueToTree(new FeedbackSubmitted(77L, "The barrier at turn 3 is broken"))));

        // kafka may deliver an event twice
        kafkaTemplate.send(Topics.FEEDBACK, "77", submitted).get();
        kafkaTemplate.send(Topics.FEEDBACK, "77", submitted).get();

        List<FeedbackLabelled> labels = labelsFor(77L, Duration.ofSeconds(40));
        assertThat(labels).hasSize(1);
        assertThat(labels.get(0).sentiment()).isEqualTo("NEGATIVE");
        assertThat(labels.get(0).urgent()).isTrue();
    }

    // reads the feedback topic from the start for a while, keeps the labels of one feedback
    private List<FeedbackLabelled> labelsFor(Long feedbackId, Duration wait) {
        List<FeedbackLabelled> labels = new ArrayList<>();
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"), new StringDeserializer(),
                new StringDeserializer())) {
            consumer.subscribe(List.of(Topics.FEEDBACK));
            Instant until = Instant.now().plus(wait);
            // the first label arrives within seconds; a second (wrong) one would come right after it
            Instant settle = null;
            while (Instant.now().isBefore(settle == null ? until : settle)) {
                consumer.poll(Duration.ofMillis(500)).forEach(record -> {
                    EventEnvelope event = eventReader.read(record.value());
                    if ("FeedbackLabelled".equals(event.type())) {
                        FeedbackLabelled label = eventReader.payload(event, FeedbackLabelled.class);
                        if (feedbackId.equals(label.feedbackId())) {
                            labels.add(label);
                        }
                    }
                });
                if (settle == null && !labels.isEmpty()) {
                    settle = Instant.now().plusSeconds(8);
                }
            }
        }
        return labels;
    }
}
