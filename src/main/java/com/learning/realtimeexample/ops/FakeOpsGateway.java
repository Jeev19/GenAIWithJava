package com.learning.realtimeexample.ops;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Canned data for a believable incident: order-pricing-service started getting OOMKilled
 * about an hour after a release that added a coupon cache, in the middle of a festive sale.
 */
public final class FakeOpsGateway implements OpsGateway {

    private final Instant now = Instant.now();
    private final Map<String, ServiceHealth> health = new ConcurrentHashMap<>(Map.of(
            "order-pricing-service", new ServiceHealth("order-pricing-service", "DEGRADED", 3, 6, 97, 14, 4200, 8.5),
            "cart-service", new ServiceHealth("cart-service", "HEALTHY", 4, 4, 52, 0, 180, 0.1),
            "payment-service", new ServiceHealth("payment-service", "HEALTHY", 6, 6, 48, 0, 350, 0.3)));

    private final Map<String, List<String>> errors = Map.of(
            "order-pricing-service", List.of(
                    "ERROR c.k.pricing.CouponCache - cache size=4812330 entries, heap at 97%",
                    "ERROR o.s.web.servlet.DispatcherServlet - java.lang.OutOfMemoryError: Java heap space "
                            + "at java.util.HashMap.resize(HashMap.java:710) at c.k.pricing.CouponCache.put(CouponCache.java:41)",
                    "WARN  k8s - pod order-pricing-service-7c9f-x2kq9 OOMKilled (limit 2Gi), restart #6",
                    "WARN  c.k.pricing.CouponCache - cache hit ratio 0.3% (expected > 90%)",
                    "ERROR c.k.checkout.PricingClient - timeout after 3000 ms calling order-pricing-service"),
            "cart-service", List.of(),
            "payment-service", List.of(
                    "WARN  c.k.payment.GatewayClient - retry 1/3 for txn 88213 (gateway 502)"));

    private final Map<String, List<Deployment>> deployments = Map.of(
            "order-pricing-service", List.of(
                    new Deployment("v2.14.0", now.minus(Duration.ofMinutes(70)), "rahul.k",
                            "Add CouponCache (HashMap keyed by CouponKey) to cut DB lookups during Diwali sale"),
                    new Deployment("v2.13.2", now.minus(Duration.ofDays(3)), "meera.i",
                            "Fix rounding for delivery fee on orders under 199 INR")),
            "cart-service", List.of(
                    new Deployment("v5.2.1", now.minus(Duration.ofDays(6)), "arjun.s", "Dependency upgrades")),
            "payment-service", List.of(
                    new Deployment("v3.8.0", now.minus(Duration.ofDays(12)), "priya.n", "Add UPI autopay")));

    @Override
    public ServiceHealth health(String service) {
        return require(health, service);
    }

    @Override
    public List<String> recentErrors(String service, int limit) {
        return require(errors, service).stream().limit(limit).toList();
    }

    @Override
    public List<Deployment> recentDeployments(String service, int limit) {
        return require(deployments, service).stream().limit(limit).toList();
    }

    @Override
    public String rollback(String service, String toVersion) {
        require(deployments, service);
        health.computeIfPresent(service, (k, h) ->
                new ServiceHealth(k, "RECOVERING", h.podsDesired(), h.podsDesired(), 41, h.restartsLastHour(), 220, 0.4));
        return "Rolled back " + service + " to " + toVersion + ". Pods restarting with the previous image.";
    }

    private <T> T require(Map<String, T> source, String service) {
        T value = source.get(service == null ? "" : service.trim().toLowerCase());
        if (value == null) {
            throw new UnknownServiceException(service, source.keySet().stream().sorted().toList());
        }
        return value;
    }
}
