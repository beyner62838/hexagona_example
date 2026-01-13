package com.example.PruebaTecnica.usecase;

import com.example.PruebaTecnica.exception.NotFoundException;
import com.example.PruebaTecnica.model.Branch;
import com.example.PruebaTecnica.model.event.BranchEvent;
import com.example.PruebaTecnica.model.event.Enums.EventType;
import com.example.PruebaTecnica.port.BranchRepositoryPort;
import com.example.PruebaTecnica.port.EventPublisherPort;
import com.example.PruebaTecnica.port.FranchiseRepositoryPort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class BranchUseCase {

    public static final String TOPIC = "branch.events";

    private final BranchRepositoryPort branchRepository;
    private final FranchiseRepositoryPort franchiseRepository;
    private final EventPublisherPort publisher;

    public BranchUseCase(BranchRepositoryPort branchRepository, FranchiseRepositoryPort franchiseRepository, EventPublisherPort publisher) {
        this.branchRepository = branchRepository;
        this.franchiseRepository = franchiseRepository;
        this.publisher = publisher;
    }

    public Mono<Branch> create(Long franchiseId, String name) {
        return franchiseRepository.findById(franchiseId)
                .switchIfEmpty(Mono.error(new NotFoundException("Franchise not found: " + franchiseId)))
                .then(branchRepository.save(Branch.builder().franchiseId(franchiseId).name(name).build()))
                .flatMap(saved -> publish(EventType.CREATED, saved).thenReturn(saved));
    }

    public Mono<Branch> updateName(Long branchId, String name) {
        return branchRepository.findById(branchId)
                .switchIfEmpty(Mono.error(new NotFoundException("Branch not found: " + branchId)))
                .map(b -> { b.setName(name); return b; })
                .flatMap(branchRepository::save)
                .flatMap(saved -> publish(EventType.UPDATED, saved).thenReturn(saved));
    }

    public Flux<Branch> listByFranchise(Long franchiseId) {
        return branchRepository.findByFranchiseId(franchiseId);
    }

    public Mono<Branch> get(Long id) {
        return branchRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Branch not found: " + id)));
    }

    public Mono<Void> delete(Long id) {
        return branchRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Branch not found: " + id)))
                .flatMap(b -> branchRepository.deleteById(id).then(publish(EventType.DELETED, b)));
    }

    private Mono<Void> publish(EventType type, Branch branch) {
        BranchEvent event = BranchEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .occurredAt(java.time.Instant.now().toString())
                .eventType(type)
                .branchId(branch.getId())
                .franchiseId(branch.getFranchiseId())
                .name(branch.getName())
                .build();

        return publisher.publish(TOPIC, String.valueOf(branch.getId()), event);
    }
}
