package se.inera.aggregator.resource.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.inera.aggregator.resource.model.JournalNote;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JournalNoteGenerator.
 */
class JournalNoteGeneratorTest {

    private JournalNoteGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new JournalNoteGenerator();
    }

    @Test
    void shouldGenerateDefaultNumberOfNotes() {
        // When: Generating notes with default count
        List<JournalNote> notes = generator.generateSampleNotes("patient-123", "resource-1");

        // Then: Should generate 2 notes
        assertEquals(2, notes.size());
    }

    @Test
    void shouldGenerateSpecifiedNumberOfNotes() {
        // When: Generating 5 notes
        List<JournalNote> notes = generator.generateSampleNotes("patient-456", "resource-2", 5);

        // Then: Should generate exactly 5 notes
        assertEquals(5, notes.size());
    }

    @Test
    void shouldGenerateNotesWithCorrectPatientId() {
        // Given: A specific patient ID
        String patientId = "patient-789";

        // When: Generating notes
        List<JournalNote> notes = generator.generateSampleNotes(patientId, "resource-3");

        // Then: All notes should have correct patient ID
        notes.forEach(note -> 
            assertEquals(patientId, note.getPatientId())
        );
    }

    @Test
    void shouldGenerateNotesWithUniqueIds() {
        // When: Generating multiple notes
        List<JournalNote> notes = generator.generateSampleNotes("patient-999", "resource-4", 10);

        // Then: All note IDs should be unique
        long uniqueIds = notes.stream()
            .map(JournalNote::getId)
            .distinct()
            .count();
        
        assertEquals(notes.size(), uniqueIds);
    }

    @Test
    void shouldGenerateNotesWithResourceIdentification() {
        // Given: A specific resource ID
        String resourceId = "RESOURCE_TEST";

        // When: Generating notes
        List<JournalNote> notes = generator.generateSampleNotes("patient-111", resourceId);

        // Then: Notes should reference the resource
        notes.forEach(note -> {
            assertTrue(note.getCaregiverId().contains(resourceId));
            assertTrue(note.getNote().contains(resourceId));
        });
    }

    @Test
    void shouldGenerateNotesWithSequentialAuthors() {
        // When: Generating notes
        List<JournalNote> notes = generator.generateSampleNotes("patient-222", "resource-5", 3);

        // Then: Authors should be numbered sequentially
        assertEquals("Doctor-1", notes.get(0).getDoctorId());
        assertEquals("Doctor-2", notes.get(1).getDoctorId());
        assertEquals("Doctor-3", notes.get(2).getDoctorId());
    }

    @Test
    void shouldHandleZeroNoteCount() {
        // When: Generating zero notes
        List<JournalNote> notes = generator.generateSampleNotes("patient-333", "resource-6", 0);

        // Then: Should return empty list
        assertNotNull(notes);
        assertTrue(notes.isEmpty());
    }

    @Test
    void shouldGenerateNotesWithDates() {
        // When: Generating notes
        List<JournalNote> notes = generator.generateSampleNotes("patient-444", "resource-7");

        // Then: All notes should have dates
        notes.forEach(note -> {
            assertNotNull(note.getDate());
            assertFalse(note.getDate().isEmpty());
        });
    }
}
