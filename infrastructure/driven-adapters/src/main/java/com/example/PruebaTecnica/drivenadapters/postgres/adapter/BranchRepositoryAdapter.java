package com.example.PruebaTecnica.drivenadapters.postgres.adapter;

import com.example.PruebaTecnica.drivenadapters.postgres.mapper.DbMapper;
import com.example.PruebaTecnica.drivenadapters.postgres.repository.BranchR2dbcRepository;
import com.example.PruebaTecnica.model.Branch;
import com.example.PruebaTecnica.port.BranchRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class BranchRepositoryAdapter implements BranchRepositoryPort {

    private final BranchR2dbcRepository repository;

    @Override
    public Mono<Branch> save(Branch branch) {
        return repository.save(DbMapper.toEntity(branch)).map(DbMapper::toModel);
    }

    @Override
    public Mono<Branch> findById(Long id) {
        return repository.findById(id).map(DbMapper::toModel);
    }

    @Override
    public Flux<Branch> findByFranchiseId(Long franchiseId) {
        return repository.findByFranchiseId(franchiseId).map(DbMapper::toModel);
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        return repository.deleteById(id);
    }
}
