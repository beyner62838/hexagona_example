package com.example.PruebaTecnica.entrypoints.rest.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private Long id;
    private Long branchId;
    private String name;
    private Integer stock;
}
