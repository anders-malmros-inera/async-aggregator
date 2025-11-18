package se.inera.aggregator.resource.service;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;
import se.inera.aggregator.resource.model.JournalCommand;
import se.inera.aggregator.resource.model.JournalNote;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ResourceService.
 * Uses MockWebServer to test WebClient interactions without external dependencies.
 */
class ResourceServiceTest {

    private MockWebServer mockWebServer;
    private ResourceService resourceService;
    private JournalNoteGenerator noteGenerator;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        noteGenerator = new JournalNoteGenerator();
        WebClient.Builder webClientBuilder = WebClient.builder();
        resourceService = new ResourceService(webClientBuilder, noteGenerator);
        
        // Use reflection to set the resource ID for testing
        setResourceId("TEST_RESOURCE_1");
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void shouldProcessJournalRequestSuccessfully() {
        // Given: A valid journal command
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));
        
        String callbackUrl = mockWebServer.url("/callback").toString();
        JournalCommand command = new JournalCommand(
            "patient-123",
            50,
            callbackUrl,
            "correlation-456"
        );

        // When: Processing the request
        StepVerifier.create(resourceService.processJournalRequest(command))
            .verifyComplete();

        // Then: Request completes immediately (async processing)
        // The actual callback is sent in background
    }

    @Test
    void shouldRejectNegativeDelay() {
        // Given: A command with negative delay
        JournalCommand command = new JournalCommand(
            "patient-123",
            -1,
            "http://localhost:8080/callback",
            "correlation-456"
        );

        // When & Then: Request should fail
        StepVerifier.create(resourceService.processJournalRequest(command))
            .expectErrorMatches(error -> 
                error instanceof IllegalArgumentException &&
                error.getMessage().contains("negative")
            )
            .verify();
    }

    @Test
    void shouldSendCallbackWithCorrectPayload() throws InterruptedException {
        // Given: Mock server expecting a callback
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));
        
        String callbackUrl = mockWebServer.url("/callback").toString();
        JournalCommand command = new JournalCommand(
            "patient-789",
            10,  // Small delay for test
            callbackUrl,
            "correlation-999"
        );

        // When: Processing request and waiting for callback
        StepVerifier.create(resourceService.processJournalRequest(command))
            .verifyComplete();

        // Wait for async callback to complete
        Thread.sleep(200);

        // Then: Callback should be received with correct structure
        RecordedRequest request = mockWebServer.takeRequest();
        assertNotNull(request);
        assertEquals("/callback", request.getPath());
        assertEquals("POST", request.getMethod());
        
        String body = request.getBody().readUtf8();
        assertTrue(body.contains("patient-789"));
        assertTrue(body.contains("correlation-999"));
        assertTrue(body.contains("TEST_RESOURCE_1"));
        assertTrue(body.contains("\"status\":\"ok\""));
    }

    @Test
    void shouldHandleCallbackFailureGracefully() throws InterruptedException {
        // Given: Mock server that returns error
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        
        String callbackUrl = mockWebServer.url("/callback").toString();
        JournalCommand command = new JournalCommand(
            "patient-456",
            10,
            callbackUrl,
            "correlation-789"
        );

        // When: Processing request
        StepVerifier.create(resourceService.processJournalRequest(command))
            .verifyComplete();

        // Wait for async callback attempt
        Thread.sleep(200);

        // Then: Request was attempted despite error
        RecordedRequest request = mockWebServer.takeRequest();
        assertNotNull(request);
        // Service logs error but doesn't fail (fire-and-forget pattern)
    }

    private void setResourceId(String resourceId) {
        try {
            var field = ResourceService.class.getDeclaredField("resourceId");
            field.setAccessible(true);
            field.set(resourceService, resourceId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set resourceId", e);
        }
    }
}
