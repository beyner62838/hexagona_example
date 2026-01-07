package com.example.PruebaTecnica.drivenadapters.postgres.repository;

import com.example.PruebaTecnica.drivenadapters.postgres.entity.FranchiseEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface FranchiseR2dbcRepository extends ReactiveCrudRepository<FranchiseEntity, Long> {
}
