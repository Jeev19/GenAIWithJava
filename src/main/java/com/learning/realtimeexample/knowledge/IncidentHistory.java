package com.learning.realtimeexample.knowledge;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.CosineSimilarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Past incidents are embedded once, and new symptoms are compared
 * with cosine similarity. "Heap keeps growing after a cache change" matches an old
 * incident even when none of the words are the same, which keyword search would miss.
 */
public final class IncidentHistory {

    private static final Logger log = LoggerFactory.getLogger(IncidentHistory.class);

    public record PastIncident(String id, String service, String symptoms, String rootCause, String fix) {
        String embeddingText() {
            // Embed what an engineer would describe while the incident is happening: service and symptoms.
            return service + ": " + symptoms;
        }
    }

    public record Match(PastIncident incident, double similarity) {
    }

    private final EmbeddingModel embeddingModel;
    private final List<PastIncident> incidents;
    private final List<Embedding> embeddings;

    private IncidentHistory(EmbeddingModel embeddingModel, List<PastIncident> incidents, List<Embedding> embeddings) {
        this.embeddingModel = embeddingModel;
        this.incidents = List.copyOf(incidents);
        this.embeddings = List.copyOf(embeddings);
    }

    public static IncidentHistory load(String classpathResource, EmbeddingModel embeddingModel) {
        List<PastIncident> incidents = parse(classpathResource);
        List<TextSegment> texts = incidents.stream().map(i -> TextSegment.from(i.embeddingText())).toList();

        // One batched call instead of N round trips.
        List<Embedding> embeddings = embeddingModel.embedAll(texts).content();

        log.info("Embedded {} past incidents", incidents.size());
        return new IncidentHistory(embeddingModel, incidents, embeddings);
    }

    public List<Match> findSimilar(String symptoms, int topK, double minSimilarity) {
        Embedding query = embeddingModel.embed(symptoms).content();
        return IntStream.range(0, incidents.size())
                .mapToObj(i -> new Match(incidents.get(i), CosineSimilarity.between(query, embeddings.get(i))))
                .filter(m -> m.similarity() >= minSimilarity)
                .sorted(Comparator.comparingDouble(Match::similarity).reversed())
                .limit(topK)
                .toList();
    }

    public int size() {
        return incidents.size();
    }

    /** Blocks of "KEY: value" lines separated by blank lines. */
    private static List<PastIncident> parse(String classpathResource) {
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(classpathResource);
        if (in == null) {
            throw new IllegalStateException("Incident history not found on classpath: " + classpathResource);
        }
        List<PastIncident> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            Map<String, String> block = new HashMap<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    addIfComplete(block, result);
                    block.clear();
                } else if (line.contains(":")) {
                    int idx = line.indexOf(':');
                    block.put(line.substring(0, idx).trim().toUpperCase(), line.substring(idx + 1).trim());
                }
            }
            addIfComplete(block, result);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return result;
    }

    private static void addIfComplete(Map<String, String> block, List<PastIncident> sink) {
        if (block.keySet().containsAll(List.of("ID", "SERVICE", "SYMPTOMS", "ROOT CAUSE", "FIX"))) {
            sink.add(new PastIncident(block.get("ID"), block.get("SERVICE"), block.get("SYMPTOMS"),
                    block.get("ROOT CAUSE"), block.get("FIX")));
        } else if (!block.isEmpty()) {
            log.warn("Skipping incomplete incident block: {}", block.keySet());
        }
    }
}
