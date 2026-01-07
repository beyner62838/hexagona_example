package com.example.PruebaTecnica.drivenadapters.postgres.adapter;

import com.example.PruebaTecnica.drivenadapters.postgres.mapper.DbMapper;
import com.example.PruebaTecnica.drivenadapters.postgres.repository.ProductR2dbcRepository;
import com.example.PruebaTecnica.model.Product;
import com.example.PruebaTecnica.port.ProductRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ProductRepositoryAdapter implements ProductRepositoryPort {

    private final ProductR2dbcRepository repository;

    @Override
    public Mono<Product> save(Product product) {
        return repository.save(DbMapper.toEntity(product)).map(DbMapper::toModel);
    }

    @Override
    public Mono<Product> findById(Long id) {
        return repository.findById(id).map(DbMapper::toModel);
    }

    @Override
    public Flux<Product> findByBranchId(Long branchId) {
        return repository.findByBranchId(branchId).map(DbMapper::toModel);
    }

    @Override
    public Mono<Product> findTopStockByBranchId(Long branchId) {
        return repository.findTopStockByBranchId(branchId).map(DbMapper::toModel);
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        return repository.deleteById(id);
    }
}
