package se.inera.aggregator.resource.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import se.inera.aggregator.resource.model.JournalCommand;
import se.inera.aggregator.resource.service.ResourceService;

@RestController
@RequestMapping("/journals")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @PostMapping
    public Mono<ResponseEntity<Void>> processRequest(@RequestBody JournalCommand command) {
        if (command.getDelay() == -1) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        return resourceService.processJournalRequest(command)
            .then(Mono.just(ResponseEntity.ok().<Void>build()))
            .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }
}
