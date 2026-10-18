package com.learning.basics;

import com.learning.utils.AppConfig;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;

import java.time.Duration;
import java.util.Scanner;

public class ChatWithoutMemory {
    public static void main(String[] args) {
        Scanner userInput;
        String cmdLineInput;

        ChatModel cmodel = GoogleAiGeminiChatModel.builder()
                .apiKey(AppConfig.getApiKey())
                .modelName(AppConfig.get("model.name"))
                .logRequests(false)
                .logResponses(false)
                .timeout(Duration.ofSeconds(20))
                .maxOutputTokens(256)
                .build();

        while (true) {
            System.out.print("Prompt >>");
            userInput = new Scanner(System.in);
            cmdLineInput = userInput.nextLine();
            if (cmdLineInput.isEmpty()) {
                System.out.print("---Please enter a text---");
                continue;
            }
            UserMessage usrmsg = UserMessage.from(cmdLineInput);   // Create the prompt
            ChatResponse answer = cmodel.chat(usrmsg);
            String response = answer.aiMessage().text();
            System.out.println(response);
        }
    }
}
