package se.inera.aggregator.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import se.inera.aggregator.model.JournalCallback;
import se.inera.aggregator.service.sse.SinkInfo;
import se.inera.aggregator.service.sse.SseEmitter;
import se.inera.aggregator.service.sse.SseSinkManager;

/**
 * Thin orchestration class that delegates sink lifecycle and emission concerns
 * to dedicated collaborators. Keeps methods small and testable.
 */
@Service
public class SseService {

    private static final Logger logger = LoggerFactory.getLogger(SseService.class);

    private final SseSinkManager sinkManager;
    private final SseEmitter emitter;

    public SseService() {
        this(new SseSinkManager(), new SseEmitter());
    }

    // package-visible constructor for tests
    SseService(SseSinkManager sinkManager, SseEmitter emitter) {
        this.sinkManager = sinkManager;
        this.emitter = emitter;
    }

    public Flux<JournalCallback> subscribe(String correlationId) {
        SinkInfo info = sinkManager.getOrCreate(correlationId);
        return info.getSink().asFlux();
    }

    public void registerExpected(String correlationId, int expected) {
        SinkInfo info = sinkManager.registerExpected(correlationId, expected);
        if (info.getReceived() >= info.getExpected() && info.getExpected() > 0) {
            // Race: already have enough responses -- complete with summary
            completeWithSummary(correlationId, info.getReceived());
        }
    }

    public void sendEvent(String correlationId, JournalCallback callback) {
        SinkInfo info = sinkManager.getIfPresent(correlationId);
        if (info == null) return;
        emitter.emitWithRetries(info.getSink(), callback);
        int received = info.incrementAndGetReceived();
        if (info.getExpected() > 0 && received >= info.getExpected()) {
            completeWithSummary(correlationId, received);
        }
    }

    public void completeWithSummary(String correlationId, int respondents) {
        SinkInfo info = sinkManager.remove(correlationId);
        if (info != null) {
            JournalCallback summary = new JournalCallback("AGGREGATOR", null, correlationId, null, "COMPLETE", null, respondents);
            emitter.emitSummaryWithRetries(info.getSink(), summary);
            info.getSink().tryEmitComplete();
        } else {
            logger.debug("completeWithSummary called but no sink found for {}", correlationId);
        }
    }

    public void complete(String correlationId) {
        SinkInfo info = sinkManager.remove(correlationId);
        if (info != null) info.getSink().tryEmitComplete();
    }
}
