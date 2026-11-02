package com.learning.realtimeexample.knowledge;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

/**
 * The team's runbooks are split, embedded and stored once at startup.
 * At question time the retriever pulls only the relevant chunks into the prompt,
 * instead of stuffing every runbook into every request.
 */
public final class RunbookLibrary {

    private static final Logger log = LoggerFactory.getLogger(RunbookLibrary.class);

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();

    public RunbookLibrary(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /** Loads every file in a classpath folder. Returns the number of runbooks ingested. */
    public int ingest(String classpathFolder) {
        List<Document> runbooks = FileSystemDocumentLoader.loadDocuments(
                resolveFolder(classpathFolder), new TextDocumentParser());

        EmbeddingStoreIngestor.builder()
                // Recursive splitting keeps paragraphs and steps together where it can,
                // which retrieves better than one-line-per-chunk.
                .documentSplitter(DocumentSplitters.recursive(500, 50))
                .embeddingModel(embeddingModel)
                .embeddingStore(store)
                .build()
                .ingest(runbooks);

        log.info("Ingested {} runbooks from {}", runbooks.size(), classpathFolder);
        return runbooks.size();
    }

    public ContentRetriever retriever(int maxResults, double minScore) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(store)
                .embeddingModel(embeddingModel)
                .maxResults(maxResults)
                .minScore(minScore)
                .build();
    }

    private static Path resolveFolder(String classpathFolder) {
        URL url = Thread.currentThread().getContextClassLoader().getResource(classpathFolder);
        if (url == null) {
            throw new IllegalStateException("Runbook folder not found on classpath: " + classpathFolder);
        }
        try {
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Invalid runbook folder path: " + url, e);
        }
    }
}
