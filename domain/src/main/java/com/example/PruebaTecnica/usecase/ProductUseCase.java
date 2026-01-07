package com.example.PruebaTecnica.usecase;

import com.example.PruebaTecnica.exception.NotFoundException;
import com.example.PruebaTecnica.model.Product;
import com.example.PruebaTecnica.model.event.EventType;
import com.example.PruebaTecnica.model.event.ProductEvent;
import com.example.PruebaTecnica.port.BranchRepositoryPort;
import com.example.PruebaTecnica.port.EventPublisherPort;
import com.example.PruebaTecnica.port.ProductRepositoryPort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class ProductUseCase {

    public static final String TOPIC = "product.events";

    private final ProductRepositoryPort productRepository;
    private final BranchRepositoryPort branchRepository;
    private final EventPublisherPort publisher;

    public ProductUseCase(ProductRepositoryPort productRepository, BranchRepositoryPort branchRepository, EventPublisherPort publisher) {
        this.productRepository = productRepository;
        this.branchRepository = branchRepository;
        this.publisher = publisher;
    }

    public Mono<Product> create(Long branchId, String name, Integer stock) {
        return branchRepository.findById(branchId)
                .switchIfEmpty(Mono.error(new NotFoundException("Branch not found: " + branchId)))
                .then(productRepository.save(Product.builder().branchId(branchId).name(name).stock(stock).build()))
                .flatMap(saved -> publish(EventType.CREATED, saved).thenReturn(saved));
    }

    public Mono<Product> updateName(Long productId, String name) {
        return productRepository.findById(productId)
                .switchIfEmpty(Mono.error(new NotFoundException("Product not found: " + productId)))
                .map(p -> { p.setName(name); return p; })
                .flatMap(productRepository::save)
                .flatMap(saved -> publish(EventType.UPDATED, saved).thenReturn(saved));
    }

    public Mono<Product> updateStock(Long productId, Integer stock) {
        return productRepository.findById(productId)
                .switchIfEmpty(Mono.error(new NotFoundException("Product not found: " + productId)))
                .map(p -> { p.setStock(stock); return p; })
                .flatMap(productRepository::save)
                .flatMap(saved -> publish(EventType.UPDATED, saved).thenReturn(saved));
    }

    public Mono<Void> delete(Long productId) {
        return productRepository.findById(productId)
                .switchIfEmpty(Mono.error(new NotFoundException("Product not found: " + productId)))
                .flatMap(p -> productRepository.deleteById(productId).then(publish(EventType.DELETED, p)));
    }

    public Mono<Product> get(Long id) {
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Product not found: " + id)));
    }

    public Flux<Product> listByBranch(Long branchId) {
        return productRepository.findByBranchId(branchId);
    }

    public Mono<Product> topStockByBranch(Long branchId) {
        return productRepository.findTopStockByBranchId(branchId);
    }

    private Mono<Void> publish(EventType type, Product product) {
        ProductEvent event = ProductEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .occurredAt(java.time.Instant.now().toString())
                .eventType(type)
                .productId(product.getId())
                .branchId(product.getBranchId())
                .name(product.getName())
                .stock(product.getStock())
                .build();

        return publisher.publish(TOPIC, String.valueOf(product.getId()), event);
    }
}
