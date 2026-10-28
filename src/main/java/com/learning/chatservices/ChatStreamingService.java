package com.learning.chatservices;

import com.learning.utils.AppConfig;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;

import java.time.Duration;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ChatStreamingService {

    public interface ChatBot {
        @SystemMessage("Respond as a professional business consultant without using markdown or numbered bullets in your response")
        TokenStream sendChat(String message);
    }

    public static void main(String[] args) {
        Scanner userinput;
        String cmdline;

        StreamingChatModel model = GoogleAiGeminiStreamingChatModel.builder()
                .apiKey(AppConfig.getApiKey())
                .modelName(AppConfig.get("model.name"))
                .logRequests(false)
                .logResponses(false)
                .timeout(Duration.ofSeconds(20))
                .maxOutputTokens(2048)
                .build();

        ChatBot consultant = AiServices.builder(ChatBot.class)
                .streamingChatModel(model)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();

        Set<String> terminate = Set.of("exit", "quit", "bye");
        String pstring = "\nrequest> ";

        while (true) {
            System.out.print(pstring);

            userinput = new Scanner(System.in);
            cmdline = userinput.nextLine();

            if (terminate.contains(cmdline.toLowerCase()))
                break;

            if (cmdline.isEmpty())
                continue;

            CompletableFuture<ChatResponse> future = new CompletableFuture<>();
            TokenStream stream = consultant.sendChat(cmdline);
            stream.onPartialResponse(System.out::print)
                    .onCompleteResponse(future::complete)
                    .onError(future::completeExceptionally)
                    .start();
            future.join();
        }
    }
}

