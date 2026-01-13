package com.example.PruebaTecnica.usecase;

import com.example.PruebaTecnica.exception.NotFoundException;
import com.example.PruebaTecnica.model.Franchise;
import com.example.PruebaTecnica.model.event.Enums.EventType;
import com.example.PruebaTecnica.model.event.FranchiseEvent;
import com.example.PruebaTecnica.port.EventPublisherPort;
import com.example.PruebaTecnica.port.FranchiseRepositoryPort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class FranchiseUseCase {

    public static final String TOPIC = "franchise.events";

    private final FranchiseRepositoryPort repository;
    private final EventPublisherPort publisher;

    public FranchiseUseCase(FranchiseRepositoryPort repository, EventPublisherPort publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    public Mono<Franchise> create(String name) {
        return repository.save(Franchise.builder().name(name).build())
                .flatMap(saved -> publish(EventType.CREATED, saved).thenReturn(saved));
    }

    public Mono<Franchise> updateName(Long id, String name) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Franchise not found: " + id)))
                .map(f -> { f.setName(name); return f; })
                .flatMap(repository::save)
                .flatMap(saved -> publish(EventType.UPDATED, saved).thenReturn(saved));
    }

    public Flux<Franchise> list() {
        return repository.findAll();
    }

    public Mono<Franchise> get(Long id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Franchise not found: " + id)));
    }

    public Mono<Void> delete(Long id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Franchise not found: " + id)))
                .flatMap(fr -> repository.deleteById(id).then(publish(EventType.DELETED, fr)));
    }

    private Mono<Void> publish(EventType type, Franchise franchise) {
        FranchiseEvent event = FranchiseEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .occurredAt(java.time.Instant.now().toString())
                .eventType(type)
                .franchiseId(franchise.getId())
                .name(franchise.getName())
                .build();

        return publisher.publish(TOPIC, String.valueOf(franchise.getId()), event);
    }
}
