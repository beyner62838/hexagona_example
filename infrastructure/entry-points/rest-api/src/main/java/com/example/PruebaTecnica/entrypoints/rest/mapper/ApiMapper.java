package com.example.PruebaTecnica.entrypoints.rest.mapper;

import com.example.PruebaTecnica.entrypoints.rest.dto.*;
import com.example.PruebaTecnica.model.*;

public final class ApiMapper {

    private ApiMapper() {}

    public static FranchiseResponse toResponse(Franchise f) {
        return FranchiseResponse.builder().id(f.getId()).name(f.getName()).build();
    }

    public static BranchResponse toResponse(Branch b) {
        return BranchResponse.builder()
                .id(b.getId())
                .franchiseId(b.getFranchiseId())
                .name(b.getName())
                .build();
    }

    public static ProductResponse toResponse(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .branchId(p.getBranchId())
                .name(p.getName())
                .stock(p.getStock())
                .build();
    }
}
