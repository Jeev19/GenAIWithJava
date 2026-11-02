package com.learning.realtimeexample.ops;

import java.time.Instant;
import java.util.List;

/**
 * The boundary to real infrastructure (Kubernetes, Prometheus, the deploy pipeline...).
 * The tools depend on this interface, not on an implementation, so the demo runs on fake
 * data today and the same tools can be pointed at real APIs later, or mocked in a unit test.
 */
public interface OpsGateway {

    ServiceHealth health(String service);

    List<String> recentErrors(String service, int limit);

    List<Deployment> recentDeployments(String service, int limit);

    String rollback(String service, String toVersion);

    record ServiceHealth(String service, String status, int podsReady, int podsDesired,
                         int heapUsedPercent, int restartsLastHour, int p99LatencyMs, double errorRatePercent) {
    }

    record Deployment(String version, Instant deployedAt, String author, String summary) {
    }

    class UnknownServiceException extends RuntimeException {
        public UnknownServiceException(String service, List<String> known) {
            super("Unknown service '" + service + "'. Known services: " + String.join(", ", known));
        }
    }
}
