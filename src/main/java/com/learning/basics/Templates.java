package com.learning.basics;

import com.learning.utils.AppConfig;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class Templates {

    public static void main(String[] args) {
        ChatModel cmodel = GoogleAiGeminiChatModel.builder()
                .apiKey(AppConfig.getApiKey())
                .modelName(AppConfig.get("model.name"))
                .logRequests(false)
                .logResponses(false)
                .timeout(Duration.ofSeconds(20))
                .maxOutputTokens(256)
                .build();

        String myTemplate = "Please explain {{topic}} to a {{student_type}} using a clear paragraph";
        PromptTemplate promptTemplate = PromptTemplate.from(myTemplate);

        Map<String, Object> variables = new HashMap<>();
        variables.put("topic", "quantum computing");
        variables.put("student_type", "hard rock musician");

        Prompt prompt = promptTemplate.apply(variables);
        String response = cmodel.chat(prompt.text());

        System.out.println(response);
    }
}

