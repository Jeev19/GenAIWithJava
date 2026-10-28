package com.learning.embedding;

import com.learning.utils.AppConfig;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.output.Response;

import java.util.List;

public class EmbeddingsComparision {

    public static void main(String[] args) {
        EmbeddingModel model = GoogleAiEmbeddingModel.builder()
                .apiKey(AppConfig.getApiKey())
                .modelName(AppConfig.get("embedding-model"))
                .build();

        List<Float> one = getEmbeddingVec(model, "I like the Java programming language.");
        List<Float> two = getEmbeddingVec(model, "Tonight is pizza night at the baseball stadium.");

        double similarity = cosineSimilarity(one, two);
        System.out.println("Cosine Similarity: " + similarity);

    }

    public static List<Float> getEmbeddingVec(EmbeddingModel model, String input) {
        Response<Embedding> response = model.embed(input);
        return response.content().vectorAsList();
    }

    public static double cosineSimilarity(List<Float> vec1, List<Float> vec2) {
        double[] newvec1 = floatList2doubleArray(vec1);
        double[] newvec2 = floatList2doubleArray(vec2);

        double dot = dotProduct(newvec1, newvec2);
        double mag1 = magnitude(newvec1);
        double mag2 = magnitude(newvec2);

        if (mag1 == 0 || mag2 == 0) {
            return 0;
        }
        return dot / (mag1 * mag2);
    }

    public static double[] floatList2doubleArray(List<Float> floatList) {
        double[] result = new double[floatList.size()];
        for (int i = 0; i < floatList.size(); i++) {
            result[i] = floatList.get(i);
        }
        return result;
    }

    public static double dotProduct(double[] vec1, double[] vec2) {
        double dotProduct = 0;
        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
        }
        return dotProduct;
    }

    public static double magnitude(double[] vec) {
        double sum = 0;
        for (double v : vec) {
            sum += v * v;
        }
        return Math.sqrt(sum);
    }
}
