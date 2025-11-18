package se.inera.aggregator.resource.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.inera.aggregator.resource.service.JournalNoteGenerator;

@Configuration
public class ResourceConfig {

    @Bean
    public JournalNoteGenerator journalNoteGenerator() {
        return new JournalNoteGenerator();
    }
}
