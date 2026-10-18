package com.learning.basics;

import com.learning.utils.AppConfig;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;

public class BasicChatModel {
    public static void main(String[] args) {
        ChatModel cmodel = GoogleAiGeminiChatModel.builder()
                .apiKey(AppConfig.getApiKey())
                .modelName(AppConfig.get("model.name"))
                .logRequests(false)
                .logResponses(false)
                .build();
        System.out.println(cmodel.chat("What are the benefits of Java"));
    }
}
