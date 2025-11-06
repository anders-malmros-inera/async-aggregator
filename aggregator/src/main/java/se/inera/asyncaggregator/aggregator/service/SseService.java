package se.inera.asyncaggregator.aggregator.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import se.inera.asyncaggregator.aggregator.model.JournalCallback;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseService {
    
    private final Map<String, Sinks.Many<JournalCallback>> sinks = new ConcurrentHashMap<>();

    public Flux<JournalCallback> subscribe(String correlationId) {
        Sinks.Many<JournalCallback> sink = Sinks.many().multicast().onBackpressureBuffer();
        sinks.put(correlationId, sink);
        return sink.asFlux();
    }

    public void sendEvent(String correlationId, JournalCallback callback) {
        Sinks.Many<JournalCallback> sink = sinks.get(correlationId);
        if (sink != null) {
            sink.tryEmitNext(callback);
        }
    }

    public void complete(String correlationId) {
        Sinks.Many<JournalCallback> sink = sinks.remove(correlationId);
        if (sink != null) {
            sink.tryEmitComplete();
        }
    }
}
