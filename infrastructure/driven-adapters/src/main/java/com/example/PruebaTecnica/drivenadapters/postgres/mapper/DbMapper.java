package com.example.PruebaTecnica.drivenadapters.postgres.mapper;

import com.example.PruebaTecnica.drivenadapters.postgres.entity.*;
import com.example.PruebaTecnica.model.*;

public final class DbMapper {

    private DbMapper() {}

    public static FranchiseEntity toEntity(Franchise model) {
        return FranchiseEntity.builder()
                .id(model.getId())
                .name(model.getName())
                .build();
    }

    public static Franchise toModel(FranchiseEntity entity) {
        return Franchise.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build();
    }

    public static BranchEntity toEntity(Branch model) {
        return BranchEntity.builder()
                .id(model.getId())
                .franchiseId(model.getFranchiseId())
                .name(model.getName())
                .build();
    }

    public static Branch toModel(BranchEntity entity) {
        return Branch.builder()
                .id(entity.getId())
                .franchiseId(entity.getFranchiseId())
                .name(entity.getName())
                .build();
    }

    public static ProductEntity toEntity(Product model) {
        return ProductEntity.builder()
                .id(model.getId())
                .branchId(model.getBranchId())
                .name(model.getName())
                .stock(model.getStock())
                .build();
    }

    public static Product toModel(ProductEntity entity) {
        return Product.builder()
                .id(entity.getId())
                .branchId(entity.getBranchId())
                .name(entity.getName())
                .stock(entity.getStock())
                .build();
    }
}
