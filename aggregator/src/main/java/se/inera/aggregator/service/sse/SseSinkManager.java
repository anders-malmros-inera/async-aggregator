package se.inera.aggregator.service.sse;

import reactor.core.publisher.Sinks;
import se.inera.aggregator.model.JournalCallback;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Responsible for creating and managing sinks per correlation id.
 */
public class SseSinkManager {
    private final Map<String, SinkInfo> sinks = new ConcurrentHashMap<>();

    public SinkInfo getOrCreate(String correlationId) {
        return sinks.computeIfAbsent(correlationId, id -> new SinkInfo(Sinks.many().multicast().onBackpressureBuffer()));
    }

    public SinkInfo getIfPresent(String correlationId) {
        return sinks.get(correlationId);
    }

    public SinkInfo remove(String correlationId) {
        return sinks.remove(correlationId);
    }

    public SinkInfo registerExpected(String correlationId, int expected) {
        SinkInfo info = getOrCreate(correlationId);
        info.setExpected(expected);
        return info;
    }
}
