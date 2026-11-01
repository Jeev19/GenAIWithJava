package com.learning.basics;

import com.learning.utils.AppConfig;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;

import java.time.Duration;

public class SimplePromptTemplate {

    public static void main(String[] args) {
        String template = "Time: {{ current_date_time }} \n" +
                "Analyze the following restaurant review and provide:\n" +
                "1. Sentiment (positive/negative/neutral)\n" +
                "2. Key points mentioned\n" +
                "3. Suggestions for improvement\n\n" +
                "Review: {{it}}";

        PromptTemplate promptTemplate = PromptTemplate.from(template);

        Prompt prompt = promptTemplate.apply(
                "The food was excellent but "
                        + "the service was very slow. " +
                        "We waited 45 minutes for our main course. " +
                        "The dessert, however, was amazing.");

        ChatModel cmodel = GoogleAiGeminiChatModel.builder()
                .apiKey(AppConfig.getApiKey())
                .modelName(AppConfig.get("model.name"))
                .logRequests(false)
                .logResponses(false)
                .timeout(Duration.ofSeconds(60))
                .maxOutputTokens(256)
                .build();

        UserMessage userMessage = prompt.toUserMessage();
        ChatResponse response = cmodel.chat(userMessage);

        System.out.println("=== Restaurant Review Analysis ===");
        System.out.println("Prompt:\n" + prompt.text());
        System.out.println("\nAnalysis:\n" + response.aiMessage().text());
    }
}
