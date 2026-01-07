package com.example.PruebaTecnica.entrypoints.rest;

import com.example.PruebaTecnica.entrypoints.rest.dto.*;
import com.example.PruebaTecnica.entrypoints.rest.mapper.ApiMapper;
import com.example.PruebaTecnica.usecase.BranchUseCase;
import com.example.PruebaTecnica.usecase.FranchiseUseCase;
import com.example.PruebaTecnica.usecase.ProductUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/franchises")
@RequiredArgsConstructor
public class FranchiseController {

    private final FranchiseUseCase franchiseUseCase;
    private final BranchUseCase branchUseCase;
    private final ProductUseCase productUseCase;

    @PostMapping
    public Mono<FranchiseResponse> create(@Valid @RequestBody CreateFranchiseRequest request) {
        return franchiseUseCase.create(request.getName()).map(ApiMapper::toResponse);
    }

    @GetMapping
    public Flux<FranchiseResponse> list() {
        return franchiseUseCase.list().map(ApiMapper::toResponse);
    }

    @GetMapping("/{id}")
    public Mono<FranchiseResponse> get(@PathVariable Long id) {
        return franchiseUseCase.get(id).map(ApiMapper::toResponse);
    }

    @PutMapping("/{id}/name")
    public Mono<FranchiseResponse> updateName(@PathVariable Long id, @Valid @RequestBody UpdateNameRequest request) {
        return franchiseUseCase.updateName(id, request.getName()).map(ApiMapper::toResponse);
    }

    @DeleteMapping("/{id}")
    public Mono<Void> delete(@PathVariable Long id) {
        return franchiseUseCase.delete(id);
    }

    @PostMapping("/{id}/branches")
    public Mono<BranchResponse> createBranch(@PathVariable Long id, @Valid @RequestBody CreateBranchRequest request) {
        return branchUseCase.create(id, request.getName()).map(ApiMapper::toResponse);
    }

    @GetMapping("/{id}/branches")
    public Flux<BranchResponse> listBranches(@PathVariable Long id) {
        return branchUseCase.listByFranchise(id).map(ApiMapper::toResponse);
    }

    /**
     * Similar to the original requirement: for each branch, return the product with the highest stock.
     */
    @GetMapping("/{id}/top-products")
    public Flux<ProductResponse> topProductsByBranch(@PathVariable Long id) {
        return branchUseCase.listByFranchise(id)
                .flatMap(branch -> productUseCase.topStockByBranch(branch.getId()))
                .filter(p -> p != null)
                .map(ApiMapper::toResponse);
    }
}
