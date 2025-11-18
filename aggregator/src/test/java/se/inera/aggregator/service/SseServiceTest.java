package se.inera.aggregator.service;

import org.junit.jupiter.api.Test;
import se.inera.aggregator.model.JournalCallback;
import se.inera.aggregator.service.sse.SseEmitter;
import se.inera.aggregator.service.sse.SseSinkManager;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SseServiceTest {

    @Test
    public void concurrentEvents_then_summary_and_complete() throws Exception {
        // Use a no-op sleeper to keep test fast
        SseEmitter.Sleeper testSleeper = millis -> { /* no-op */ };
        SseEmitter emitter = new SseEmitter(5, 0L, testSleeper);
        SseSinkManager manager = new SseSinkManager();
        SseService sseService = new SseService(manager, emitter);

        String correlationId = "test-cid";
        int expected = 3;
        sseService.registerExpected(correlationId, expected);

        List<JournalCallback> collected = new CopyOnWriteArrayList<>();
        CountDownLatch completeLatch = new CountDownLatch(1);

        sseService.subscribe(correlationId)
                .doOnNext(collected::add)
                .doOnComplete(completeLatch::countDown)
                .subscribe();

        ExecutorService exec = Executors.newFixedThreadPool(expected);
        for (int i = 0; i < expected; i++) {
            final int idx = i + 1;
            exec.submit(() -> {
                JournalCallback cb = new JournalCallback("resource-" + idx, null, correlationId, null, "OK", null);
                sseService.sendEvent(correlationId, cb);
            });
        }

        exec.shutdown();
        boolean terminated = exec.awaitTermination(2, TimeUnit.SECONDS);
        assertTrue(terminated, "executor did not terminate in time");

        // wait for stream completion
        boolean completed = completeLatch.await(2, TimeUnit.SECONDS);
        assertTrue(completed, "stream did not complete in time");

        // We expect 3 resource callbacks + 1 final summary
        assertEquals(4, collected.size(), "expected 3 callbacks + 1 summary");
        JournalCallback last = collected.get(collected.size() - 1);
        assertEquals("AGGREGATOR", last.getSource());
        assertEquals("COMPLETE", last.getStatus());
        assertEquals(expected, last.getRespondents());
    }
}
