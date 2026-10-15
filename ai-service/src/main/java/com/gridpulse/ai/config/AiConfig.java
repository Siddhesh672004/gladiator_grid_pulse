package com.gridpulse.ai.config;

import java.util.List;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import com.gridpulse.ai.config.properties.GeminiProperties;
import com.gridpulse.ai.tools.ToolErrorProcessor;

@Configuration
public class AiConfig {

    // the Copilot keeps the last 20 messages of each chat (in ai_db)
    private static final int MEMORY_MESSAGES = 20;

    // one spring ai gemini model per key, in the order of GeminiProperties.keys()
    // GeminiChatRunner moves to the next one when a key fails. no key at all = an empty list
    @Bean
    public List<ChatModel> geminiChatModels(GeminiProperties gemini, ToolErrorProcessor toolErrors) {
        // a tool that fails tells the model why in one sentence, instead of ending the chat
        ToolCallingManager tools = ToolCallingManager.builder().toolExecutionExceptionProcessor(toolErrors).build();
        return gemini.keys().stream()
                .<ChatModel>map(key -> chatModel(key, gemini, tools))
                .toList();
    }

    @Bean
    public ChatMemory copilotChatMemory(ChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(MEMORY_MESSAGES)
                .build();
    }

    private GoogleGenAiChatModel chatModel(String key, GeminiProperties gemini, ToolCallingManager tools) {
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
        return GoogleGenAiChatModel.builder()
                .genAiClient(connection)
                .defaultOptions(options)
                .toolCallingManager(tools)
                .retryTemplate(RetryTemplate.builder().maxAttempts(1).build())
                .build();
    }
}
