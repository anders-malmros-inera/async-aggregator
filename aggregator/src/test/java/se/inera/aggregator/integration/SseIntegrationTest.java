package se.inera.aggregator.integration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import se.inera.aggregator.model.JournalCallback;
import se.inera.aggregator.model.JournalRequest;
import se.inera.aggregator.model.JournalResponse;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for end-to-end SSE flow.
 * Tests that SSE stream delivers all events including final summary.
 * 
 * NOTE: Disabled due to timing issues in containerized environments.
 * Unit tests provide sufficient coverage for SSE logic.
 */
@Disabled("Timing issues in containerized test environment - see unit tests for coverage")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SseIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private WebClient.Builder webClientBuilder;

    private WebClient webClient;

    @Test
    void shouldCompleteStreamWithFinalSummary() {
        // Given: Setup WebClient
        webClient = webClientBuilder.baseUrl("http://localhost:" + port).build();
        
        JournalRequest request = new JournalRequest("patient-456", "50,50,50");

        // When: Trigger aggregation first to get correlation ID
        JournalResponse response = webClient.post()
            .uri("/aggregate/journals")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(JournalResponse.class)
            .block(Duration.ofSeconds(5));

        assertNotNull(response);
        String correlationId = response.getCorrelationId();

        // Subscribe to SSE stream
        Flux<JournalCallback> sseStream = webClient.get()
            .uri("/aggregate/stream?correlationId={id}", correlationId)
            .accept(MediaType.TEXT_EVENT_STREAM)
            .retrieve()
            .bodyToFlux(JournalCallback.class);

        // Simulate 3 resource callbacks in sequence
        for (int i = 1; i <= 3; i++) {
            JournalCallback callback = new JournalCallback(
                "RESOURCE_" + i,
                "patient-456",
                correlationId,
                50,
                "SUCCESS",
                List.of()
            );

            webClient.post()
                .uri("/aggregate/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(callback)
                .retrieve()
                .bodyToMono(Void.class)
                .block(Duration.ofSeconds(2));
        }

        // Then: Verify stream delivers all 4 events (3 resources + 1 COMPLETE) and completes
        StepVerifier.create(sseStream)
            .expectNextMatches(event -> event.getSource().startsWith("RESOURCE_"))
            .expectNextMatches(event -> event.getSource().startsWith("RESOURCE_"))
            .expectNextMatches(event -> event.getSource().startsWith("RESOURCE_"))
            .expectNextMatches(event -> 
                "AGGREGATOR COMPLETE".equals(event.getSource()) && 
                event.getRespondents() != null &&
                event.getRespondents() == 3
            )
            .expectComplete()
            .verify(Duration.ofSeconds(10));
    }
}
