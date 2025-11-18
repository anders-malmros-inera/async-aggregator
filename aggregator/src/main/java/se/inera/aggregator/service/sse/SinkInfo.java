package se.inera.aggregator.service.sse;

import reactor.core.publisher.Sinks;
import se.inera.aggregator.model.JournalCallback;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lightweight holder for a sink and its expected/received state.
 */
public class SinkInfo {
    private final Sinks.Many<JournalCallback> sink;
    private final AtomicInteger received = new AtomicInteger(0);
    private volatile int expected = 0;

    public SinkInfo(Sinks.Many<JournalCallback> sink) {
        this.sink = sink;
    }

    public Sinks.Many<JournalCallback> getSink() {
        return sink;
    }

    public int incrementAndGetReceived() {
        return received.incrementAndGet();
    }

    public int getReceived() {
        return received.get();
    }

    public int getExpected() {
        return expected;
    }

    public void setExpected(int expected) {
        this.expected = expected;
    }
}
