package com.example.PruebaTecnica.drivenadapters.postgres.repository;

import com.example.PruebaTecnica.drivenadapters.postgres.entity.BranchEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface BranchR2dbcRepository extends ReactiveCrudRepository<BranchEntity, Long> {
    Flux<BranchEntity> findByFranchiseId(Long franchiseId);
}
