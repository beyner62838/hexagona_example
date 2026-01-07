package com.example.PruebaTecnica.drivenadapters.postgres.repository;

import com.example.PruebaTecnica.drivenadapters.postgres.entity.ProductEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProductR2dbcRepository extends ReactiveCrudRepository<ProductEntity, Long> {

    Flux<ProductEntity> findByBranchId(Long branchId);

    @Query("SELECT * FROM products WHERE branch_id = :branchId ORDER BY stock DESC LIMIT 1")
    Mono<ProductEntity> findTopStockByBranchId(Long branchId);
}
