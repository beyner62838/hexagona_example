package com.example.PruebaTecnica.drivenadapters.postgres.adapter;

import com.example.PruebaTecnica.drivenadapters.postgres.mapper.DbMapper;
import com.example.PruebaTecnica.drivenadapters.postgres.repository.FranchiseR2dbcRepository;
import com.example.PruebaTecnica.model.Franchise;
import com.example.PruebaTecnica.port.FranchiseRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class FranchiseRepositoryAdapter implements FranchiseRepositoryPort {

    private final FranchiseR2dbcRepository repository;

    @Override
    public Mono<Franchise> save(Franchise franchise) {
        return repository.save(DbMapper.toEntity(franchise)).map(DbMapper::toModel);
    }

    @Override
    public Mono<Franchise> findById(Long id) {
        return repository.findById(id).map(DbMapper::toModel);
    }

    @Override
    public Flux<Franchise> findAll() {
        return repository.findAll().map(DbMapper::toModel);
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        return repository.deleteById(id);
    }
}
