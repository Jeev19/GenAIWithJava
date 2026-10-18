package com.learning.basics;

import com.learning.utils.AppConfig;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class DIfferentChatMessages {

    public static void main(String[] args) {
        ChatModel cmodel = GoogleAiGeminiChatModel.builder()
                .apiKey(AppConfig.getApiKey())
                .modelName(AppConfig.get("model.name"))
                .logRequests(false)
                .logResponses(false)
                .timeout(Duration.ofSeconds(20))
                .maxOutputTokens(256)
                .build();

        System.out.println("---- Building a simple system ----");
        List<ChatMessage> messages = new ArrayList<>();
        SystemMessage sysmsg = new SystemMessage("You are a polite Java expert explaining concepts to a politician.");
        messages.add(sysmsg);
        UserMessage usrmsg = UserMessage.from("Why should I learn Java lambdas?");
        messages.add(usrmsg);
        ChatResponse answer = cmodel.chat(messages);
        System.out.println(answer.aiMessage().text());


    }


}
