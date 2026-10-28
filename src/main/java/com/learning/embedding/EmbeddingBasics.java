package com.learning.embedding;

import com.learning.utils.AppConfig;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.output.Response;

public class EmbeddingBasics {

    public static void main(String[] args) {

        EmbeddingModel em = GoogleAiEmbeddingModel.builder()
                .apiKey(AppConfig.getApiKey())
                .modelName(AppConfig.get("embedding-model"))
                .build();

        Response<Embedding> response = em.embed("One fish, two fish, red fish, blue fish");
        Embedding embedding = response.content();
        System.out.println(embedding.dimension() + " elements: " + embedding);
    }
}
