package com.example.PruebaTecnica.entrypoints.rest;

import com.example.PruebaTecnica.entrypoints.rest.dto.*;
import com.example.PruebaTecnica.entrypoints.rest.mapper.ApiMapper;
import com.example.PruebaTecnica.usecase.ProductUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductUseCase productUseCase;

    @GetMapping("/{id}")
    public Mono<ProductResponse> get(@PathVariable Long id) {
        return productUseCase.get(id).map(ApiMapper::toResponse);
    }

    @PutMapping("/{id}/name")
    public Mono<ProductResponse> updateName(@PathVariable Long id, @Valid @RequestBody UpdateNameRequest request) {
        return productUseCase.updateName(id, request.getName()).map(ApiMapper::toResponse);
    }

    @PutMapping("/{id}/stock")
    public Mono<ProductResponse> updateStock(@PathVariable Long id, @Valid @RequestBody UpdateStockRequest request) {
        return productUseCase.updateStock(id, request.getStock()).map(ApiMapper::toResponse);
    }

    @DeleteMapping("/{id}")
    public Mono<Void> delete(@PathVariable Long id) {
        return productUseCase.delete(id);
    }
}
