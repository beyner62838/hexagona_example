package com.example.PruebaTecnica.model;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private Long id;
    private Long branchId;
    private String name;
    private Integer stock;
}
