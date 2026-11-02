package com.learning.realtimeexample.report;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Writes stakeholder updates with a plain ChatModel call (no AI Service), a SystemMessage plus UserMessage,
 * a PromptTemplate, and the whole incident conversation stuffed into the context.
 * Context stuffing is the right tool here: the transcript is small and every line of it matters.
 */
public final class StatusUpdateWriter {

    private static final PromptTemplate TEMPLATE = PromptTemplate.from("""
            Write a status update about incident {{incidentId}} affecting {{service}}.
            Audience: {{audience}}. Adjust the level of technical detail for them.
            Use only facts from the investigation transcript. If something is not known yet, say so.
            Sections: Impact, What we know, What we are doing, Next update at {{nextUpdate}}.
            Keep it under 150 words, plain text.

            Investigation transcript:
            {{transcript}}
            """);

    private final ChatModel chatModel;

    public StatusUpdateWriter(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String write(String incidentId, String service, String audience, List<ChatMessage> conversation) {
        Prompt prompt = TEMPLATE.apply(Map.<String, Object>of(
                "incidentId", incidentId,
                "service", service,
                "audience", audience,
                "nextUpdate", LocalTime.now().plusMinutes(30).format(DateTimeFormatter.ofPattern("HH:mm")),
                "transcript", toTranscript(conversation)));

        return chatModel.chat(
                        SystemMessage.from("You are an incident commander. You write calm, factual, specific updates."),
                        prompt.toUserMessage())
                .aiMessage()
                .text();
    }

    private static String toTranscript(List<ChatMessage> conversation) {
        return conversation.stream()
                .map(message -> switch (message) {
                    case UserMessage u -> "Engineer: " + withoutRagContext(u.singleText());
                    case AiMessage a when a.text() != null && !a.text().isBlank() -> "Copilot: " + a.text();
                    case ToolExecutionResultMessage t -> "Tool " + t.toolName() + ": " + t.text();
                    default -> null;   // system messages and tool-call requests add nothing for the reader
                })
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n"));
    }

    /**
     * With RAG, memory stores the augmented question (question + retrieved runbook chunks).
     * The update only needs what the engineer asked, so the injected runbook text is cut off here.
     */
    private static String withoutRagContext(String userText) {
        int idx = userText.indexOf(RAG_MARKER);
        return idx >= 0 ? userText.substring(0, idx).trim() : userText;
    }

    // Matches LangChain4j's DefaultContentInjector template.
    private static final String RAG_MARKER = "\n\nAnswer using the following information:";
}
