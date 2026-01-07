package com.example.PruebaTecnica.config;

import com.example.PruebaTecnica.port.*;
import com.example.PruebaTecnica.usecase.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCasesConfig {

    @Bean
    public FranchiseUseCase franchiseUseCase(FranchiseRepositoryPort franchiseRepositoryPort, EventPublisherPort eventPublisherPort) {
        return new FranchiseUseCase(franchiseRepositoryPort, eventPublisherPort);
    }

    @Bean
    public BranchUseCase branchUseCase(BranchRepositoryPort branchRepositoryPort, FranchiseRepositoryPort franchiseRepositoryPort, EventPublisherPort eventPublisherPort) {
        return new BranchUseCase(branchRepositoryPort, franchiseRepositoryPort, eventPublisherPort);
    }

    @Bean
    public ProductUseCase productUseCase(ProductRepositoryPort productRepositoryPort, BranchRepositoryPort branchRepositoryPort, EventPublisherPort eventPublisherPort) {
        return new ProductUseCase(productRepositoryPort, branchRepositoryPort, eventPublisherPort);
    }
}
