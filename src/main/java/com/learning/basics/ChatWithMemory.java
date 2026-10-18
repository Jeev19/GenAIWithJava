package com.learning.basics;

import com.learning.utils.AppConfig;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;

import java.time.Duration;
import java.util.Scanner;

public class ChatWithMemory {

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
        ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(100);

        while(true) {
            System.out.print("Prompt >>");
            userInput = new Scanner(System.in);
            cmdLineInput = userInput.nextLine();
            if(cmdLineInput.isEmpty()) {
                System.out.print("---Please enter a text---");
                continue;
            }
            UserMessage usrmsg = UserMessage.from(cmdLineInput);   // Create the prompt
            chatMemory.add(usrmsg);                                // Add prompt to the memory
            ChatResponse answer = cmodel.chat(chatMemory.messages());
            String response = answer.aiMessage().text();
            System.out.println(response);
            chatMemory.add(UserMessage.from(response));     // Add response from llm to the memory
        }

    }
}
