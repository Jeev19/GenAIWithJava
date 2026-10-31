package com.learning.rag;

import com.learning.tools.FlightInfo;
import com.learning.utils.AppConfig;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class RAGUsingAiService {

    interface TravelAssistant {
        @SystemMessage("Consider yourself a well versed tour/travel guide")
        String getTourInfo(String message);
    }

    public static void main(String[] args) throws IOException {
        ChatMemory memory = MessageWindowChatMemory.withMaxMessages(10);    // Lab: convert this pgm to AiService

        InMemoryEmbeddingStore<TextSegment> myDB = new InMemoryEmbeddingStore<>();

        List<String> lines = Files.readAllLines(Paths.get("src/main/resources/paris-tour-guide.txt"));

        EmbeddingModel em = GoogleAiEmbeddingModel.builder()
                .apiKey(AppConfig.getApiKey())
                .modelName(AppConfig.get("embedding-model"))
                .build();


        List<Document> documents = convertLinesToDocuments(lines);

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.<TextSegment>builder()
                .documentSplitter(new DocumentBySentenceSplitter(512, 25))
                .embeddingStore(myDB)
                .embeddingModel(em)
                .build();

        ingestor.ingest(documents);

        EmbeddingStoreContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
                .embeddingModel(em)
                .embeddingStore(myDB)
                .maxResults(5)          // At most return this number of results
                .minScore(.7)           // Return results that are relevant (0->1 least->most)
                .build();

        ChatModel cmodel = GoogleAiGeminiChatModel.builder()
                .apiKey(AppConfig.getApiKey())
                .modelName(AppConfig.get("model.name"))
                .logRequests(false)
                .logResponses(false)
                .timeout(Duration.ofSeconds(20))
                .maxOutputTokens(2048)
                .build();

        TravelAssistant travelAssistant = AiServices.builder(TravelAssistant.class)
                .chatModel(cmodel)
                .chatMemory(memory)
                .contentRetriever(retriever)
                .build();

        Scanner scanner = new Scanner(System.in);
        String userInput;

        while (true) {
            userInput = scanner.nextLine();
            if (userInput.isEmpty())
                continue;

            String response = travelAssistant.getTourInfo(userInput);        // Send everything to the LLM
            System.out.println(response);
        }
    }

    public static List<Document> convertLinesToDocuments(List<String> mylines) {
        List<Document> documents = new ArrayList<>();
        for (String line : mylines) {
            String trimmed = line.trim(); // remove leading/trailing whitespace
            if (!trimmed.isEmpty()) {     // skip empty strings
                Document doc = Document.from(trimmed); // convert to Document
                documents.add(doc);
            }
        }
        return documents;
    }
}
