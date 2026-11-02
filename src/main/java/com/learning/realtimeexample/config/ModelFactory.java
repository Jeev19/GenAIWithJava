package com.learning.realtimeexample.config;

import com.learning.utils.AppConfig;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;

import java.time.Duration;

/**
 * One place that knows how to build models. Every earlier example repeated the same builder;
 * centralising it means one config change (model, timeout, temperature) applies everywhere,
 * and missing config fails fast at startup instead of on the first request.
 */
public final class ModelFactory {

    private static final double OPS_TEMPERATURE = 0.2;   // low: we want facts, not creativity

    private ModelFactory() {
    }

    public static ChatModel chatModel() {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey())
                .modelName(required("model.name"))
                .temperature(OPS_TEMPERATURE)
                .timeout(timeout())
                .maxOutputTokens(AppConfig.getInt("realtimeexample.max-output-tokens", 2048))
                .logRequests(logTraffic())
                .logResponses(logTraffic())
                .build();
    }

    public static StreamingChatModel streamingChatModel() {
        return GoogleAiGeminiStreamingChatModel.builder()
                .apiKey(apiKey())
                .modelName(required("model.name"))
                .temperature(OPS_TEMPERATURE)
                .timeout(timeout())
                .maxOutputTokens(AppConfig.getInt("realtimeexample.max-output-tokens", 2048))
                .logRequests(logTraffic())
                .logResponses(logTraffic())
                .build();
    }

    public static EmbeddingModel embeddingModel() {
        return GoogleAiEmbeddingModel.builder()
                .apiKey(apiKey())
                .modelName(required("embedding-model"))
                .outputDimensionality(AppConfig.getInt("embedding.dimensions", 768))
                .timeout(timeout())
                .build();
    }

    private static Duration timeout() {
        return Duration.ofSeconds(AppConfig.getInt("llm.timeout.seconds", 60));
    }

    private static boolean logTraffic() {
        return AppConfig.getBoolean("log.requests.responses", false);
    }

    private static String apiKey() {
        String key = AppConfig.getApiKey();
        if (key == null || key.isBlank()) {
            throw new IllegalStateException(
                    "No API key. Set api.key in application.properties or export API_KEY.");
        }
        return key;
    }

    private static String required(String property) {
        String value = AppConfig.get(property);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required property '" + property + "' in application.properties");
        }
        return value.trim();
    }
}
