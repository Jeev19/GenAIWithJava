package com.learning.chatservices;

import com.learning.utils.AppConfig;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;

public class SimpleChatStreaming {

    public static void main(String[] args) throws InterruptedException {

        CountDownLatch count = new CountDownLatch(1);

        System.out.println("Start streaming......\n");
        StreamingChatModel model = GoogleAiGeminiStreamingChatModel.builder()
                .apiKey(AppConfig.getApiKey())
                .modelName(AppConfig.get("model.name"))
                .logRequests(false)
                .logResponses(false)
                .temperature(0.7)
                .timeout(Duration.ofSeconds(60))
                .maxOutputTokens(2048)
                .build();
        model.chat("Write a simple essay on english literature",
                new StreamingChatResponseHandler() {
                    @Override
                    public void onCompleteResponse(ChatResponse chatResponse) {
                        System.out.println("\n\nDone!");
                    }

                    @Override
                    public void onPartialResponse(String token) {
                        System.out.println("..." + token);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        throwable.printStackTrace();
                    }
                }
        );

        count.await();
    }
}
