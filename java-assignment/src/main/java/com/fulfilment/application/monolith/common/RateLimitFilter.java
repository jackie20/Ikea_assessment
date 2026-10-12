package com.fulfilment.application.monolith.common;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Sliding-window rate limiter: 100 requests per 60-second window, keyed by client IP.
 * Returns HTTP 429 with a Retry-After header when the limit is exceeded.
 */
@Provider
@ApplicationScoped
@Priority(Priorities.USER - 100)
public class RateLimitFilter implements ContainerRequestFilter {

  static final int MAX_REQUESTS = 100;
  static final long WINDOW_MS = 60_000L;

  private final ConcurrentHashMap<String, ConcurrentLinkedDeque<Long>> log = new ConcurrentHashMap<>();

  @Override
  public void filter(ContainerRequestContext ctx) {
    String ip = resolveIp(ctx);
    long now = System.currentTimeMillis();
    long cutoff = now - WINDOW_MS;

    ConcurrentLinkedDeque<Long> timestamps = log.computeIfAbsent(ip, k -> new ConcurrentLinkedDeque<>());
    while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoff) {
      timestamps.pollFirst();
    }
    timestamps.addLast(now);

    if (timestamps.size() > MAX_REQUESTS) {
      ctx.abortWith(
          Response.status(429)
              .header("Retry-After", "60")
              .entity(Map.of("error", "Rate limit exceeded", "retryAfterSeconds", 60))
              .type(MediaType.APPLICATION_JSON)
              .build());
    }
  }

  private String resolveIp(ContainerRequestContext ctx) {
    String xff = ctx.getHeaderString("X-Forwarded-For");
    if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
    String xri = ctx.getHeaderString("X-Real-IP");
    if (xri != null && !xri.isBlank()) return xri;
    return "default";
  }
}
