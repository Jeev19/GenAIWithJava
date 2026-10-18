package com.learning.basics;

import com.learning.utils.AppConfig;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;

public class ChatWithFileInContext {

    public static void main(String[] args) {

        Scanner userInput ;
        String cmdLineInput;
        String folder = "src/main/resources/";

        ChatModel cmodel = GoogleAiGeminiChatModel.builder()
                .apiKey(AppConfig.getApiKey())
                .modelName(AppConfig.get("model.name"))
                .logRequests(false)
                .logResponses(false)
                .timeout(Duration.ofSeconds(20))
                .maxOutputTokens(2048)
                .build();

        List<String> myfiles = Arrays.asList(folder + "the-midnight-heap.txt");
        String filesString = filesToStrings(myfiles);

        while (true) {
            List<ChatMessage> usermessages = new ArrayList<>();

            System.out.print("Prompt>> ");
            userInput = new Scanner(System.in);
            cmdLineInput = userInput.nextLine();
            if (cmdLineInput.isEmpty())       // If nothing, do nothing
                continue;
            usermessages.add(UserMessage.from(filesString));
            usermessages.add(UserMessage.from(cmdLineInput));
            ChatResponse answer = cmodel.chat(usermessages);
            System.out.println(answer.aiMessage().text());

        }
    }

    public static String filesToStrings(List<String> filenames) {
        String bigString = "";

        for (String f : filenames) {
            String finfo = null;
            try {
                finfo = new String(Files.readAllBytes(Paths.get(f)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            bigString += finfo;
        }
        return bigString;
    }
}
