package com.learning.realtimeexample.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/**
 * A plain interface. LangChain4j generates the implementation and wires in
 * the streaming model, per-incident memory, runbook retrieval and tools.
 */
public interface OnCallAssistant {

    @SystemMessage({
            "You are an on-call SRE copilot for the backend of an online grocery delivery platform.",
            "Investigate with the tools before drawing conclusions. Never invent metrics, log lines or versions.",
            "When a runbook step is relevant, follow it and name the runbook.",
            "Check whether a similar incident has happened before.",
            "You can only PROPOSE a rollback. Never say a rollback has happened unless a tool result says so.",
            "Answer in plain text without markdown, under 200 words, in three parts: Findings, Likely cause, Next steps."
    })
    TokenStream investigate(@MemoryId String incidentId, @UserMessage String message);
}
