package com.example.PruebaTecnica.drivenadapters.postgres.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("products")
public class ProductEntity {
    @Id
    private Long id;

    @Column("branch_id")
    private Long branchId;

    private String name;
    private Integer stock;
}
