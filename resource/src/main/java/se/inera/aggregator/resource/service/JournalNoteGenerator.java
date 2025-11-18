package se.inera.aggregator.resource.service;

import se.inera.aggregator.resource.model.JournalNote;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates sample journal notes for testing and demo purposes.
 * Extracted to improve testability and separation of concerns.
 */
public class JournalNoteGenerator {

    private static final int DEFAULT_NOTE_COUNT = 2;

    /**
     * Generates a list of sample journal notes for a given patient and resource.
     *
     * @param patientId the patient identifier
     * @param resourceId the resource identifier
     * @return list of generated journal notes
     */
    public List<JournalNote> generateSampleNotes(String patientId, String resourceId) {
        return generateSampleNotes(patientId, resourceId, DEFAULT_NOTE_COUNT);
    }

    /**
     * Generates a specified number of sample journal notes.
     *
     * @param patientId the patient identifier
     * @param resourceId the resource identifier
     * @param count number of notes to generate
     * @return list of generated journal notes
     */
    public List<JournalNote> generateSampleNotes(String patientId, String resourceId, int count) {
        List<JournalNote> notes = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            notes.add(createNote(patientId, resourceId, i + 1));
        }
        
        return notes;
    }

    private JournalNote createNote(String patientId, String resourceId, int noteNumber) {
        String noteId = UUID.randomUUID().toString();
        String date = randomDateInPastYear().toString();
        String caregiverId = "Caregiver-" + resourceId;
        String authorId = "Doctor-" + noteNumber;
        String text = "Sample note " + noteNumber + " from " + resourceId;
        
        return new JournalNote(noteId, date, caregiverId, patientId, authorId, text);
    }

    private LocalDateTime randomDateInPastYear() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusDays(365);
        long secondsBetween = ChronoUnit.SECONDS.between(start, now);
        long randomOffsetSeconds = ThreadLocalRandom.current().nextLong(secondsBetween + 1);
        return start.plusSeconds(randomOffsetSeconds);
    }
}
