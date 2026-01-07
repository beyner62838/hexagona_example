package com.example.PruebaTecnica.entrypoints.rest;

import com.example.PruebaTecnica.entrypoints.rest.dto.*;
import com.example.PruebaTecnica.entrypoints.rest.mapper.ApiMapper;
import com.example.PruebaTecnica.usecase.BranchUseCase;
import com.example.PruebaTecnica.usecase.ProductUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchUseCase branchUseCase;
    private final ProductUseCase productUseCase;

    @GetMapping("/{id}")
    public Mono<BranchResponse> get(@PathVariable Long id) {
        return branchUseCase.get(id).map(ApiMapper::toResponse);
    }

    @PutMapping("/{id}/name")
    public Mono<BranchResponse> updateName(@PathVariable Long id, @Valid @RequestBody UpdateNameRequest request) {
        return branchUseCase.updateName(id, request.getName()).map(ApiMapper::toResponse);
    }

    @DeleteMapping("/{id}")
    public Mono<Void> delete(@PathVariable Long id) {
        return branchUseCase.delete(id);
    }

    @PostMapping("/{id}/products")
    public Mono<ProductResponse> createProduct(@PathVariable Long id, @Valid @RequestBody CreateProductRequest request) {
        return productUseCase.create(id, request.getName(), request.getStock()).map(ApiMapper::toResponse);
    }

    @GetMapping("/{id}/products")
    public Flux<ProductResponse> listProducts(@PathVariable Long id) {
        return productUseCase.listByBranch(id).map(ApiMapper::toResponse);
    }
}
