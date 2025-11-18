package se.inera.aggregator.service.sse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Sinks;
import se.inera.aggregator.model.JournalCallback;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Encapsulates emission behavior (with retries) to keep tryEmit logic testable and single-responsibility.
 */
public class SseEmitter {
    private static final Logger logger = LoggerFactory.getLogger(SseEmitter.class);

    public interface Sleeper {
        void sleep(long millis);
    }

    private static class ThreadSleeper implements Sleeper {
        public void sleep(long millis) {
            try { Thread.sleep(millis); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        }
    }

    private final int maxAttempts;
    private final long backoffMillis;
    private final Sleeper sleeper;
    // Use a single-threaded executor to serialize emissions and avoid NON_SERIALIZED failures
    private final ExecutorService emitterExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "sse-emitter");
        t.setDaemon(true);
        return t;
    });

    public SseEmitter() {
        this(5, 50L, new ThreadSleeper());
    }

    public SseEmitter(int maxAttempts, long backoffMillis, Sleeper sleeper) {
        this.maxAttempts = maxAttempts;
        this.backoffMillis = backoffMillis;
        this.sleeper = sleeper;
    }

    public boolean emitWithRetries(Sinks.Many<JournalCallback> sink, JournalCallback payload) {
        try {
            // Submit emission to single-threaded executor and wait for it to finish.
            return emitterExecutor.submit(() -> {
                int attempts = 0;
                while (attempts < maxAttempts) {
                    Sinks.EmitResult result = sink.tryEmitNext(payload);
                    if (result.isSuccess()) return true;
                    attempts++;
                    logger.debug("emit failed (attempt {}) result={}", attempts, result);
                    if (backoffMillis > 0) sleeper.sleep(backoffMillis);
                }
                logger.warn("Failed to emit message after {} attempts", maxAttempts);
                return false;
            }).get(5, TimeUnit.SECONDS);
        } catch (TimeoutException te) {
            logger.warn("Emitter timed out while trying to emit message");
            return false;
        } catch (Exception e) {
            logger.warn("Emitter failed to emit message", e);
            return false;
        }
    }

    public boolean emitSummaryWithRetries(Sinks.Many<JournalCallback> sink, JournalCallback summary) {
        return emitWithRetries(sink, summary);
    }
}
