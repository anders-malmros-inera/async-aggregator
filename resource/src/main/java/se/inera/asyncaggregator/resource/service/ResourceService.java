package se.inera.asyncaggregator.resource.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import se.inera.asyncaggregator.resource.model.JournalCallback;
import se.inera.asyncaggregator.resource.model.JournalCommand;
import se.inera.asyncaggregator.resource.model.JournalNote;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ResourceService {

    private final WebClient webClient;
    
    @Value("${resource.id}")
    private String resourceId;

    public ResourceService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public Mono<Void> processJournalRequest(JournalCommand command) {
        if (command.getDelay() < 0) {
            return Mono.error(new IllegalArgumentException("Request rejected - delay is negative"));
        }

        return Mono.delay(Duration.ofMillis(command.getDelay()))
            .then(Mono.defer(() -> sendCallback(command)));
    }

    private Mono<Void> sendCallback(JournalCommand command) {
        List<JournalNote> notes = generateSampleNotes(command.getPatientId());
        
        JournalCallback callback = new JournalCallback(
            resourceId,
            command.getPatientId(),
            command.getCorrelationId(),
            command.getDelay(),
            "ok",
            notes
        );

        return webClient.post()
            .uri(command.getCallbackUrl())
            .bodyValue(callback)
            .retrieve()
            .toBodilessEntity()
            .then();
    }

    private List<JournalNote> generateSampleNotes(String patientId) {
        List<JournalNote> notes = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        
        for (int i = 0; i < 2; i++) {
            String noteId = UUID.randomUUID().toString();
            String date = LocalDateTime.now().minusDays(i).format(formatter);
            
            JournalNote note = new JournalNote(
                noteId,
                date,
                "Caregiver-" + resourceId,
                patientId,
                "Doctor-" + (i + 1),
                "Sample note " + (i + 1) + " from " + resourceId
            );
            notes.add(note);
        }
        
        return notes;
    }
}
