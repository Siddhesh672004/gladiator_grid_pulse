package com.gridpulse.ai.config;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import com.gridpulse.ai.config.properties.GeminiProperties;

@Configuration
public class AiConfig {

    // one spring ai ChatClient per gemini key, in the order of GeminiProperties.keys()
    // GeminiKeys says which one is in use. no key at all = empty list, the chatbot answers from the faq list
    @Bean
    public List<ChatClient> chatClients(GeminiProperties gemini, ChatMemory chatMemory) {
        return gemini.keys().stream()
                .map(key -> chatClient(key, gemini, chatMemory))
                .toList();
    }

    private ChatClient chatClient(String key, GeminiProperties gemini, ChatMemory chatMemory) {
        // one try, no waiting: a key that fails -> GeminiKeys moves to the next key at once
        // (without the empty code list the sdk still sleeps ~1 s after a 429 / 503 before it gives up)
        HttpRetryOptions retry = HttpRetryOptions.builder()
                .attempts(1)
                .httpStatusCodes(List.of())
                .build();
        HttpOptions http = HttpOptions.builder()
                .timeout(gemini.timeoutSeconds() * 1000)
                .retryOptions(retry)
                .build();
        Client connection = Client.builder().apiKey(key).httpOptions(http).build();

        GoogleGenAiChatOptions options = GoogleGenAiChatOptions.builder()
                .model(gemini.generationModel())
                .temperature(0.2)
                .thinkingBudget(0)
                .build();
        // spring ai's own retry is off too
        GoogleGenAiChatModel model = GoogleGenAiChatModel.builder()
                .genAiClient(connection)
                .defaultOptions(options)
                .retryTemplate(RetryTemplate.builder().maxAttempts(1).build())
                .build();

        // the memory advisor sends the earlier messages of the same chat along with every new one
        return ChatClient.builder(model)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}
