package com.learning.realtimeexample.tools;

import com.learning.realtimeexample.knowledge.IncidentHistory;
import com.learning.realtimeexample.ops.OpsGateway;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * The model decides which of these to call. The @Tool and @P descriptions are
 * effectively part of the prompt, so they say what each tool returns and what valid input looks like.
 *
 * Two production habits worth copying:
 * 1. Tools never throw. They return an error message the model can read and recover from
 *    (for example, retrying with a correct service name).
 * 2. Tools that change things don't act on their own. proposeRollback only records a proposal;
 *    a human runs /approve.
 */
public class OpsTools {

    private static final Logger log = LoggerFactory.getLogger(OpsTools.class);

    public record RollbackProposal(String service, String toVersion, String reason) {
    }

    private final OpsGateway ops;
    private final IncidentHistory history;
    private final AtomicReference<RollbackProposal> pendingRollback = new AtomicReference<>();

    public OpsTools(OpsGateway ops, IncidentHistory history) {
        this.ops = ops;
        this.history = history;
    }

    @Tool("Current health of a service: status, ready vs desired pods, heap usage, restarts in the last hour, "
            + "p99 latency and error rate")
    public String getServiceHealth(@P("Service name, for example order-pricing-service") String service) {
        return safely("getServiceHealth", () -> {
            OpsGateway.ServiceHealth h = ops.health(service);
            return "%s is %s. Pods %d/%d ready, heap %d%%, %d restarts in the last hour, p99 %d ms, error rate %.1f%%"
                    .formatted(h.service(), h.status(), h.podsReady(), h.podsDesired(), h.heapUsedPercent(),
                            h.restartsLastHour(), h.p99LatencyMs(), h.errorRatePercent());
        });
    }

    @Tool("Most recent error and warning log lines for a service, newest first")
    public String getRecentErrors(@P("Service name") String service,
                                  @P("Number of lines to return, between 1 and 20") int limit) {
        return safely("getRecentErrors", () -> {
            List<String> lines = ops.recentErrors(service, Math.clamp(limit, 1, 20));
            return lines.isEmpty() ? "No recent errors for " + service : String.join("\n", lines);
        });
    }

    @Tool("Recent deployments of a service, newest first, with version, how long ago, author and change summary")
    public String getRecentDeployments(@P("Service name") String service) {
        return safely("getRecentDeployments", () -> ops.recentDeployments(service, 5).stream()
                .map(d -> "%s, %s ago by %s: %s".formatted(d.version(), ago(d.deployedAt()), d.author(), d.summary()))
                .collect(Collectors.joining("\n")));
    }

    @Tool("Searches past incidents for ones with similar symptoms and returns their root cause and fix")
    public String findSimilarIncidents(@P("Symptoms in plain words, for example 'heap growing after cache release, pods OOMKilled'")
                                       String symptoms) {
        return safely("findSimilarIncidents", () -> {
            List<IncidentHistory.Match> matches = history.findSimilar(symptoms, 3, 0.6);
            if (matches.isEmpty()) {
                return "No similar past incidents found.";
            }
            return matches.stream()
                    .map(m -> "%s (similarity %.2f) on %s. Symptoms: %s. Root cause: %s. Fix: %s".formatted(
                            m.incident().id(), m.similarity(), m.incident().service(), m.incident().symptoms(),
                            m.incident().rootCause(), m.incident().fix()))
                    .collect(Collectors.joining("\n"));
        });
    }

    @Tool("Proposes rolling a service back to an earlier version. This does NOT perform the rollback; "
            + "an on-call engineer must approve it")
    public String proposeRollback(@P("Service name") String service,
                                  @P("Version to roll back to, taken from getRecentDeployments") String toVersion,
                                  @P("One-sentence reason for the rollback") String reason) {
        return safely("proposeRollback", () -> {
            boolean versionExists = ops.recentDeployments(service, 10).stream()
                    .anyMatch(d -> d.version().equals(toVersion));
            if (!versionExists) {
                return "Version " + toVersion + " is not in the deployment history of " + service + ". Proposal rejected.";
            }
            pendingRollback.set(new RollbackProposal(service, toVersion, reason));
            return "Rollback of " + service + " to " + toVersion
                    + " is proposed and waiting for engineer approval. It has NOT been executed.";
        });
    }

    public Optional<RollbackProposal> pendingRollback() {
        return Optional.ofNullable(pendingRollback.get());
    }

    /** Called only from the app when a human types /approve. */
    public String executePendingRollback() {
        RollbackProposal proposal = pendingRollback.getAndSet(null);
        if (proposal == null) {
            return "Nothing to approve.";
        }
        log.warn("Executing approved rollback: {}", proposal);
        return ops.rollback(proposal.service(), proposal.toVersion());
    }

    private String safely(String toolName, Supplier<String> action) {
        try {
            return action.get();
        } catch (OpsGateway.UnknownServiceException e) {
            return "ERROR: " + e.getMessage();
        } catch (RuntimeException e) {
            log.error("Tool {} failed", toolName, e);
            return "ERROR: " + toolName + " failed: " + e.getMessage();
        }
    }

    private static String ago(Instant at) {
        Duration d = Duration.between(at, Instant.now());
        return d.toDays() > 0 ? d.toDays() + " days" : d.toHours() > 0 ? d.toHours() + " h" : d.toMinutes() + " min";
    }
}
