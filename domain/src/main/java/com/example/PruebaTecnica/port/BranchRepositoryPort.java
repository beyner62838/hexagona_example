package com.example.PruebaTecnica.port;

import com.example.PruebaTecnica.model.Branch;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BranchRepositoryPort {
    Mono<Branch> save(Branch branch);
    Mono<Branch> findById(Long id);
    Flux<Branch> findByFranchiseId(Long franchiseId);
    Mono<Void> deleteById(Long id);
}
