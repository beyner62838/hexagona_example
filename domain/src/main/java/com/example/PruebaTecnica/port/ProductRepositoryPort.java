package com.example.PruebaTecnica.port;

import com.example.PruebaTecnica.model.Product;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProductRepositoryPort {
    Mono<Product> save(Product product);
    Mono<Product> findById(Long id);
    Flux<Product> findByBranchId(Long branchId);
    Mono<Product> findTopStockByBranchId(Long branchId);
    Mono<Void> deleteById(Long id);
}
