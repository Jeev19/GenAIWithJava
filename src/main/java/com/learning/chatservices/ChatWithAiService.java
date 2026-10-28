package com.learning.chatservices;

import com.learning.utils.AppConfig;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;

import java.time.Duration;
import java.util.Scanner;

public class ChatWithAiService {

    public interface ChatBot {
        public String sendPrompt(String msg);
    }
    public static void main(String[] args) {
        Scanner userInput ;
        String cmdLineInput;

        ChatModel cmodel = GoogleAiGeminiChatModel.builder()
                .apiKey(AppConfig.getApiKey())
                .modelName(AppConfig.get("model.name"))
                .logRequests(false)
                .logResponses(false)
                .timeout(Duration.ofSeconds(20))
                .maxOutputTokens(2048)
                .build();

        ChatMemory memory = MessageWindowChatMemory.withMaxMessages(10);

        ChatBot chatBot = AiServices.builder(ChatBot.class)
                          .chatModel(cmodel)
                          .chatMemory(memory)
                          .build();

        while (true) {
            System.out.println("Prompt>>");
            userInput = new Scanner(System.in);
            cmdLineInput = userInput.nextLine();
            if(cmdLineInput.isEmpty()) continue;
            System.out.println(chatBot.sendPrompt(cmdLineInput));
        }

    }
}
