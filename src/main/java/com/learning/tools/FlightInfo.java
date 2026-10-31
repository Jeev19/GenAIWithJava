package com.learning.tools;

import com.learning.chatservices.ChatWithAiService;
import com.learning.utils.AppConfig;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;

import java.time.Duration;

public class FlightInfo {

    interface FlightAssistant{
        String getFlightInfo(String prompt);
    }

    public static void main(String[] args) {

        ChatModel cmodel = GoogleAiGeminiChatModel.builder()
                .apiKey(AppConfig.getApiKey())
                .modelName(AppConfig.get("model.name"))
                .logRequests(false)
                .logResponses(false)
                .timeout(Duration.ofSeconds(20))
                .maxOutputTokens(2048)
                .build();

        ChatMemory memory = MessageWindowChatMemory.withMaxMessages(10);
        memory.add(SystemMessage.from("You are a helpful, and informative flight agent.  Only use the methods I have described."));

        FlightAssistant assistant = AiServices.builder(FlightAssistant.class)
                .chatModel(cmodel)
                .chatMemory(memory)
                .build();

        String response;

        response = assistant.getFlightInfo("I need the status of Flight UA1011");
        System.out.println(response);

        response = assistant.getFlightInfo("What type of aircraft is it?");
        System.out.println(response);

        response = assistant.getFlightInfo("What was its cost");
        System.out.println(response);
    }
}
