package com.learning.realtimeexample;

import com.learning.realtimeexample.assistant.OnCallAssistant;
import com.learning.realtimeexample.config.ModelFactory;
import com.learning.realtimeexample.knowledge.IncidentHistory;
import com.learning.realtimeexample.knowledge.RunbookLibrary;
import com.learning.realtimeexample.ops.FakeOpsGateway;
import com.learning.realtimeexample.report.StatusUpdateWriter;
import com.learning.realtimeexample.tools.OpsTools;
import com.learning.utils.AppConfig;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolExecution;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * On-Call Copilot: a console assistant that helps an on-call engineer investigate a production incident,
 * propose a rollback for human approval, and write stakeholder updates.
 */
public final class OnCallCopilotApp {

    private static final String HELP = """
            Commands
              /incident <service>   open an incident (each incident has its own memory)
              /update [audience]    write a stakeholder update, e.g. /update leadership
              /approve              execute the rollback the copilot proposed
              /help                 show this help
              /exit                 quit
            Anything else is a question for the copilot.
            """;

    private final OnCallAssistant assistant;
    private final StatusUpdateWriter statusWriter;
    private final OpsTools tools;
    private final Map<String, ChatMemory> memories;   // shared with the assistant so /update can read the history

    private String incidentId;
    private String service;
    private int incidentCounter = 4100;

    OnCallCopilotApp(OnCallAssistant assistant, StatusUpdateWriter statusWriter, OpsTools tools,
                     Map<String, ChatMemory> memories) {
        this.assistant = assistant;
        this.statusWriter = statusWriter;
        this.tools = tools;
        this.memories = memories;
    }

    public static void main(String[] args) {
        System.out.println("Starting On-Call Copilot...");

        EmbeddingModel embeddingModel = ModelFactory.embeddingModel();
        ChatModel chatModel = ModelFactory.chatModel();

        RunbookLibrary runbooks = new RunbookLibrary(embeddingModel);
        int runbookCount = runbooks.ingest("realtimeexample/runbooks");
        IncidentHistory history = IncidentHistory.load("realtimeexample/past-incidents.txt", embeddingModel);

        OpsTools tools = new OpsTools(new FakeOpsGateway(), history);

        Map<String, ChatMemory> memories = new ConcurrentHashMap<>();
        ChatMemoryProvider memoryProvider = id -> memories.computeIfAbsent(String.valueOf(id),
                key -> MessageWindowChatMemory.builder().id(key).maxMessages(40).build());

        OnCallAssistant assistant = AiServices.builder(OnCallAssistant.class)
                .streamingChatModel(ModelFactory.streamingChatModel())
                .chatMemoryProvider(memoryProvider)
                .contentRetriever(runbooks.retriever(
                        AppConfig.getInt("realtimeexample.rag.max-results", 4),
                        AppConfig.getDouble("realtimeexample.rag.min-score", 0.6)))
                .tools(tools)
                .build();

        System.out.printf("Loaded %d runbooks and %d past incidents.%n%n%s%n", runbookCount, history.size(), HELP);

        new OnCallCopilotApp(assistant, new StatusUpdateWriter(chatModel), tools, memories).run();
    }

    void run() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print(incidentId == null ? "\noncall> " : "\n" + incidentId + "> ");
                if (!scanner.hasNextLine()) {
                    return;
                }
                String input = scanner.nextLine().trim();
                if (input.isEmpty()) {
                    continue;
                }
                if (input.equals("/exit")) {
                    return;
                }
                try {
                    handle(input);
                } catch (RuntimeException e) {
                    // One failed LLM call shouldn't end an on-call session.
                    System.out.println("\n[error] " + e.getMessage());
                }
            }
        }
    }

    private void handle(String input) {
        String[] parts = input.split("\\s+", 2);
        String arg = parts.length > 1 ? parts[1].trim() : "";
        switch (parts[0]) {
            case "/help" -> System.out.println(HELP);
            case "/incident" -> openIncident(arg);
            case "/update" -> writeUpdate(arg.isEmpty() ? "engineering and support teams" : arg);
            case "/approve" -> approveRollback();
            default -> {
                if (input.startsWith("/")) {
                    System.out.println("Unknown command. Type /help.");
                } else {
                    ask(input);
                }
            }
        }
    }

    private void openIncident(String serviceName) {
        if (serviceName.isEmpty()) {
            System.out.println("Usage: /incident <service>, e.g. /incident order-pricing-service");
            return;
        }
        incidentId = "INC-" + (++incidentCounter);
        service = serviceName;
        System.out.printf("Opened %s for %s. The copilot starts this incident with an empty memory.%n", incidentId, service);
    }

    private void ask(String question) {
        if (incidentId == null) {
            System.out.println("Open an incident first: /incident <service>");
            return;
        }
        CompletableFuture<Void> done = new CompletableFuture<>();

        assistant.investigate(incidentId, "[Service under investigation: " + service + "] " + question)
                .onRetrieved(this::printSources)
                .onToolExecuted(this::printToolCall)
                .onPartialResponse(System.out::print)
                .onCompleteResponse(response -> done.complete(null))
                .onError(done::completeExceptionally)
                .start();

        await(done);
        tools.pendingRollback().ifPresent(p -> System.out.printf(
                "%n%n>> Rollback proposed: %s to %s (%s). Type /approve to execute.%n",
                p.service(), p.toVersion(), p.reason()));
    }

    private void writeUpdate(String audience) {
        if (incidentId == null || !memories.containsKey(incidentId)) {
            System.out.println("Nothing to report yet. Open an incident and investigate first.");
            return;
        }
        System.out.println("\n----- Status update for " + audience + " -----");
        System.out.println(statusWriter.write(incidentId, service, audience, memories.get(incidentId).messages()));
    }

    private void approveRollback() {
        System.out.println(tools.executePendingRollback());
    }

    private void printSources(List<Content> contents) {
        List<String> files = contents.stream()
                .map(c -> c.textSegment().metadata().getString("file_name"))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (!files.isEmpty()) {
            System.out.println("  [runbooks] " + String.join(", ", files));
        }
    }

    private void printToolCall(ToolExecution execution) {
        System.out.printf("  [tool] %s %s%n", execution.request().name(), execution.request().arguments());
    }

    private static void await(CompletableFuture<Void> done) {
        try {
            done.get(2, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            throw new IllegalStateException("The copilot failed: " + e.getCause().getMessage(), e.getCause());
        } catch (TimeoutException e) {
            throw new IllegalStateException("No complete answer within 2 minutes", e);
        }
    }
}
