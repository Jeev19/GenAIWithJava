package com.learning.rag;

import com.learning.utils.AppConfig;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;

public class BasicRAG {

    public static void main(String[] args) {
        InMemoryEmbeddingStore<TextSegment> myDB = new InMemoryEmbeddingStore<>();

        EmbeddingModel em = GoogleAiEmbeddingModel.builder()
                .apiKey(AppConfig.getApiKey())
                .modelName(AppConfig.get("embedding-model"))
                .build();

        try (Stream<String> lines = Files.lines(Path.of("src/main/resources/paris-tour-guide.txt"))) {
            lines.forEach(line -> {
                if (!line.isEmpty()) {
                    TextSegment ts = TextSegment.from(line);
                    Embedding emb = em.embed(ts).content();
                    myDB.add(emb, ts);
                    //System.out.println("Storing: [" + ts.text() + "]");
                }
            });
            System.out.println("Embedding and Loading completed...");
        } catch (IOException iox) {
            iox.printStackTrace();
        }

        ChatMemory memory = MessageWindowChatMemory.withMaxMessages(10);    // Lab: convert this pgm to AiService

        ChatModel cmodel = GoogleAiGeminiChatModel.builder()
                .apiKey(AppConfig.getApiKey())
                .modelName(AppConfig.get("model.name"))
                .logRequests(false)
                .logResponses(false)
                .timeout(Duration.ofSeconds(20))
                .maxOutputTokens(2048)
                .build();

        Scanner scanner = new Scanner(System.in);
        String userInput;
        while (true) {
            userInput = scanner.nextLine();
            if (userInput.isEmpty())
                continue;
            UserMessage usrmsg = UserMessage.from(userInput);          // create the initial prompt
            List<String> match = getMatches(myDB, em, userInput);      // Get list of matches from the DB
            for (String m : match) {                                   // Add matches to the memory
                memory.add(UserMessage.from(m));
            }
            memory.add(usrmsg);                                         // finally, add the user's prompt
            ChatResponse answer = cmodel.chat(memory.messages());       // Send everything to the LLM
            String response = answer.aiMessage().text();
            System.out.println(response);
            memory.add(UserMessage.from(response));                     // Add response from llm
        }
    }

    public static List<String> getMatches(InMemoryEmbeddingStore<TextSegment> myDB, EmbeddingModel embModel, String input) {
        List<String> myList = new ArrayList<>();
        Embedding queryEmbedding = embModel.embed(input).content();     // create embedding vector from cmdline
        EmbeddingSearchRequest embeddingSearchRequest = EmbeddingSearchRequest.builder()    // Search the local embedding store for related strings
                .queryEmbedding(queryEmbedding)
                .maxResults(10)     // at most, find this number of matches [best in a properties file]
                .minScore(0.7)      // 0-1, so ignore anything below this value [best in a properties file]
                .build();
        List<EmbeddingMatch<TextSegment>> matches = myDB.search(embeddingSearchRequest).matches();
        if (!matches.isEmpty()) {
            for (EmbeddingMatch<TextSegment> match : matches) {
                String s = match.embedded().text();
                myList.add(s);
                //System.out.println("[" + match.score() + "] MATCH: " + s);
            }
        }
        return myList;
    }
}
