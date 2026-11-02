# GenAI With Java

Hands-on examples for building Generative AI features in plain Java with [LangChain4j](https://docs.langchain4j.dev/) and Google Gemini.

No Spring, no framework magic. Every example is a single runnable class with a `main` method, so you can read it top to bottom and see exactly what goes to the model and what comes back. The examples build on each other, starting from a one-line chat call and ending with a RAG assistant and an agent that calls Java methods as tools.

## What you'll learn

| Module | Package | Concepts |
|---|---|---|
| 1. Chat basics | `basics` | `ChatModel`, single prompts, system vs user messages, prompt templates |
| 2. Conversation memory | `basics` | Why LLMs are stateless, `MessageWindowChatMemory` |
| 3. Context stuffing | `basics` | Putting a whole document in the prompt and its limits |
| 4. AI Services | `chatservices` | Declarative interfaces with `AiServices`, `@SystemMessage`, token streaming |
| 5. Embeddings | `embedding` | Turning text into vectors, cosine similarity from scratch |
| 6. RAG | `rag` | Ingestion, in-memory vector store, retrieval, manual RAG vs `ContentRetriever` |
| 7. Tools / function calling | `tools` | `@Tool` methods the model can decide to invoke |

## Tech stack

- Java 21
- LangChain4j 1.22.0 (`langchain4j`, `langchain4j-google-ai-gemini`)
- Google Gemini for chat and embeddings
- Maven, Lombok, SLF4J

## Getting started

### Prerequisites

- JDK 21 (or newer)
- Maven 3.9+
- A Gemini API key from [Google AI Studio](https://aistudio.google.com/app/apikey)

### Configure

Edit `src/main/resources/application.properties`:

```properties
# Leave empty to read from the API_KEY environment variable instead
api.key=

# Chat model (must support generateContent)
model.name=gemini-2.5-flash

# Embedding model (must support embedContent), used by the embedding and rag packages
embedding-model=gemini-embedding-001

log.requests.responses=true
```

The safer option is to keep `api.key` empty and export the key, so it never lands in Git:

```bash
export API_KEY=your-gemini-api-key        # macOS / Linux
setx API_KEY "your-gemini-api-key"        # Windows
```

`AppConfig` checks the properties file first and falls back to the `API_KEY` environment variable.

> **Chat and embedding models are not interchangeable.** If you point `embedding-model` at a chat model such as a `gemini-*-flash`, the API returns
> `... is not supported for embedContent`. You can list the models available to your key, along with their supported methods, at
> `https://generativelanguage.googleapis.com/v1beta/models?key=YOUR_KEY`.

### Run an example

From your IDE, open any class below and run its `main` method. From the terminal:

```bash
mvn compile exec:java -Dexec.mainClass=com.learning.basics.BasicChatModel
```

Interactive examples read prompts from the console. Press Enter on an empty line to skip; `ChatStreamingService` exits on `exit`, `quit` or `bye`.

## Examples

### 1. Chat basics (`com.learning.basics`)

| Class | What it shows |
|---|---|
| `BasicChatModel` | The smallest possible call: build a `ChatModel`, send one string, print the answer. |
| `DIfferentChatMessages` | Combining a `SystemMessage` (persona and rules) with a `UserMessage` (the question). |
| `Templates` | `PromptTemplate` with `{{placeholders}}` filled from a `Map` for reusable prompts. |

```mermaid
flowchart LR
    U["Your code"] -->|"SystemMessage + UserMessage"| CM["ChatModel"]
    CM -->|"HTTPS"| G[("Gemini API")]
    G -->|"AiMessage"| CM --> U
```

### 2. Conversation memory (`com.learning.basics`)

| Class | What it shows |
|---|---|
| `ChatWithoutMemory` | A REPL where every prompt is independent. Ask "what did I just say?" and the model has no idea. |
| `ChatWithMemory` | The same REPL, but each turn is stored in `MessageWindowChatMemory` and the whole window is sent every time. |

```mermaid
flowchart LR
    IN["Console input"] --> MEM["ChatMemory<br/>last N messages"]
    MEM -->|"full history"| CM["ChatModel"] --> G[("Gemini")]
    G --> OUT["Answer"]
    OUT -->|"stored as AiMessage"| MEM
```

The key takeaway: the model itself remembers nothing. "Memory" is just your application resending the conversation.

### 3. Context stuffing (`com.learning.basics`)

| Class | What it shows |
|---|---|
| `ChatWithFileInContext` | Loads `the-midnight-heap.txt` (a short story about a Java memory leak) and sends the entire text with every question. |

This works for small documents and is the simplest way to ground answers, but cost and latency grow with document size and you eventually hit the context window. That limitation is the motivation for RAG.

### 4. AI Services (`com.learning.chatservices`)

| Class | What it shows |
|---|---|
| `ChatWithAiService` | Define a plain Java interface; `AiServices` generates the implementation and wires in memory. |
| `ChatStreamingService` | Returns a `TokenStream` so text prints token by token, with a `@SystemMessage` persona. |

```mermaid
flowchart LR
    APP["Your code"] -->|"chatBot.sendPrompt"| PROXY["AiServices proxy"]
    PROXY --> MEM["ChatMemory"]
    PROXY --> SM["@SystemMessage"]
    PROXY --> CM["ChatModel / StreamingChatModel"]
    CM --> G[("Gemini")]
    G -->|"full answer or token stream"| APP
```

### 5. Embeddings (`com.learning.embedding`)

| Class | What it shows |
|---|---|
| `EmbeddingBasics` | Embeds one sentence and prints the vector and its dimension. |
| `EmbeddingsComparision` | Embeds two unrelated sentences and computes cosine similarity by hand (dot product and magnitudes). |

```mermaid
flowchart LR
    T1["Text A"] --> EM["EmbeddingModel"] --> V1["Vector A"]
    T2["Text B"] --> EM --> V2["Vector B"]
    V1 --> CS["Cosine similarity"]
    V2 --> CS --> S["Score -1 to 1"]
```

### 6. Retrieval-Augmented Generation (`com.learning.rag`)

Both examples answer questions about a Paris tour guide document (`src/main/resources/paris-tour-guide.txt`).

| Class | What it shows |
|---|---|
| `BasicRAG` | RAG by hand: embed each line, store in `InMemoryEmbeddingStore`, search with `EmbeddingSearchRequest`, then add the matches to the prompt yourself. |
| `RAGUsingAiService` | The same idea the LangChain4j way: `EmbeddingStoreIngestor` with a sentence splitter, `EmbeddingStoreContentRetriever`, plugged into an `AiServices` assistant. |

```mermaid
flowchart TB
    subgraph ING["Ingestion (once at startup)"]
        direction LR
        D["paris-tour-guide.txt"] --> SP["Split into segments"] --> EM1["EmbeddingModel"]
    end
    VS[("InMemoryEmbeddingStore")]
    subgraph QRY["Per question"]
        direction LR
        Q["User question"] --> EM2["EmbeddingModel"]
        P["Prompt = context + question"] --> CM["ChatModel"] --> G[("Gemini")] --> A["Grounded answer"]
    end
    EM1 -->|"store vectors"| VS
    EM2 -->|"query vector"| VS
    VS -->|"top-k matches, minScore 0.7"| P
```

Doing it manually first makes it obvious what `ContentRetriever` is automating.

### 7. Tools / function calling (`com.learning.tools`)

| Class | What it shows |
|---|---|
| `FlightTools` | Three `@Tool` methods (flight status, aircraft type, cost) returning mock data. The tool descriptions are what the model reads to pick a method. |
| `FlightInfo` | A `FlightAssistant` built with `AiServices` that answers multi-turn flight questions, using memory so "it" refers to the flight from the previous turn. |

```mermaid
sequenceDiagram
    participant U as User
    participant A as FlightAssistant
    participant G as Gemini
    participant T as FlightTools
    U->>A: Status of flight UA1011?
    A->>G: prompt + tool specifications
    G-->>A: call getFlightStatus("UA1011")
    A->>T: getFlightStatus("UA1011")
    T-->>A: IN THE AIR, 2 hours from destination
    A->>G: tool result
    G-->>A: final answer
    A-->>U: UA1011 is in the air...
```

## Capstone: On-Call Copilot (`com.learning.realtimeexample`)

One realistic app that uses everything above. It's the night of a big festive sale, `order-pricing-service` pods keep getting OOMKilled, and checkout is timing out. You're on call. The copilot reads the team's runbooks, checks live health, logs and deployments through tools, recognises a similar past incident, proposes a rollback that only you can approve, and writes the stakeholder update.

```mermaid
flowchart LR
    ENG["On-call engineer"] -->|"questions, /update, /approve"| APP["OnCallCopilotApp<br/>console loop"]
    APP --> ASSIST["OnCallAssistant<br/>AiServices + streaming"]
    APP --> WRITER["StatusUpdateWriter<br/>PromptTemplate + ChatModel"]
    ASSIST --> MEM["ChatMemory<br/>one per incident"]
    WRITER -->|"reads transcript"| MEM
    ASSIST -->|"RAG"| RB["RunbookLibrary<br/>runbook vectors"]
    ASSIST -->|"tool calls"| TOOLS["OpsTools<br/>@Tool methods"]
    TOOLS --> GW["OpsGateway<br/>health, logs, deploys"]
    TOOLS --> HIST["IncidentHistory<br/>cosine similarity"]
    ASSIST --> GEM[("Gemini")]
    WRITER --> GEM
```

| Concept from the tutorial | Where it shows up in the capstone |
|---|---|
| ChatModel, system and user messages | `StatusUpdateWriter` calls the model directly with a `SystemMessage` and a `UserMessage` |
| Prompt templates | The status update prompt is a `PromptTemplate` with incident, audience and transcript variables |
| Context stuffing | The whole incident conversation goes into the status update prompt |
| Chat memory | `chatMemoryProvider` with `@MemoryId`, so every incident gets its own memory |
| AI Services and streaming | `OnCallAssistant` is an interface that returns a `TokenStream` |
| Embeddings | `IncidentHistory` embeds past incidents once and ranks them with cosine similarity |
| RAG | `RunbookLibrary` ingests the runbooks and plugs a `ContentRetriever` into the assistant |
| Tools | `OpsTools` exposes health, logs, deployments, similar incidents and rollback proposals |

The capstone also applies a few production habits that the earlier, smaller examples skip:

- **Models are built in one place** (`ModelFactory`), and missing config fails at startup with a clear message.
- **Tools depend on an interface** (`OpsGateway`). The demo uses fake data, and swapping in real Kubernetes or Prometheus clients, or a Mockito mock in a test, doesn't touch the tools.
- **Tools never throw.** Errors come back as text the model can read and recover from.
- **The AI can't take destructive actions on its own.** `proposeRollback` only records a proposal, and a human has to type `/approve`.
- **Ingestion is batched** with `embedAll` and recursive splitting, instead of one API call per line.

### Configure and run

Add these to `application.properties` (all but the first are optional; these are the defaults):

```properties
embedding-model=gemini-embedding-001
embedding.dimensions=768
llm.timeout.seconds=60
realtimeexample.rag.max-results=4
realtimeexample.rag.min-score=0.6
```

```bash
mvn compile exec:java -Dexec.mainClass=com.learning.realtimeexample.OnCallCopilotApp
```

### A session to try

```
oncall> /incident order-pricing-service
INC-4101> Pricing pods keep restarting and checkout is timing out. What's going on?
  [runbooks] jvm-memory-runbook.txt, latency-and-timeouts-runbook.txt
  [tool] getServiceHealth {"service":"order-pricing-service"}
  [tool] getRecentErrors {"service":"order-pricing-service","limit":10}
  [tool] getRecentDeployments {"service":"order-pricing-service"}
  [tool] findSimilarIncidents {"symptoms":"heap growing after cache release, pods OOMKilled"}
Findings: ... (streams in)
INC-4101> Should we roll back?
>> Rollback proposed: order-pricing-service to v2.13.2 (...). Type /approve to execute.
INC-4101> /approve
INC-4101> Is it recovering?
INC-4101> /update leadership
INC-4101> /update customer support
```

The second and third questions only make sense because of memory, since "it" and "roll back" refer back to the first answer. Try `/incident cart-service` afterwards to see that a new incident starts with a clean memory.

## Project structure

```
src/main/java/com/learning
├── basics/          # chat model, messages, templates, memory, context stuffing
├── chatservices/    # AiServices, streaming
├── embedding/       # embeddings and cosine similarity
├── rag/             # manual RAG and AiServices RAG
├── tools/           # @Tool function calling
├── realtimeexample/ # On-Call Copilot capstone, everything combined
│   ├── config/      # ModelFactory
│   ├── knowledge/   # RunbookLibrary (RAG), IncidentHistory (embeddings)
│   ├── ops/         # OpsGateway interface + fake implementation
│   ├── tools/       # OpsTools
│   ├── assistant/   # OnCallAssistant (AI Service)
│   └── report/      # StatusUpdateWriter
└── utils/AppConfig  # loads application.properties, API key fallback
src/main/resources
├── application.properties
├── the-midnight-heap.txt
├── paris-tour-guide.txt
└── realtimeexample/
    ├── runbooks/            # JVM memory, latency, rollback runbooks
    └── past-incidents.txt
docs/diagrams/               # PNG versions of every diagram, for articles
```

## Suggested learning path

`BasicChatModel` → `DIfferentChatMessages` → `Templates` → `ChatWithoutMemory` → `ChatWithMemory` → `ChatWithFileInContext` → `ChatWithAiService` → `ChatStreamingService` → `EmbeddingBasics` → `EmbeddingsComparision` → `BasicRAG` → `RAGUsingAiService` → `FlightInfo` → `OnCallCopilotApp`

## Notes

- `InMemoryEmbeddingStore` is rebuilt on every run, which is fine for learning. For anything real, swap it for a persistent store such as PgVector, Chroma or Qdrant; LangChain4j has integrations for all of them.
- If you change the embedding model, re-ingest your documents. Vectors from different models can't be compared.
- `minScore` and `maxResults` in the RAG examples are worth experimenting with; they decide how much (and how relevant) context the model sees.

## Resources

- [LangChain4j documentation](https://docs.langchain4j.dev/)
- [LangChain4j Google AI Gemini integration](https://docs.langchain4j.dev/integrations/language-models/google-ai-gemini)
- [Gemini API docs](https://ai.google.dev/gemini-api/docs)

## Author

**Jeevottam Prabhu** · [GitHub @Jeev19](https://github.com/Jeev19)
