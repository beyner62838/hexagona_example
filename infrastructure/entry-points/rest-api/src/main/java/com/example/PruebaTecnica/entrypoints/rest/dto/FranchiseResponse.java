package com.example.PruebaTecnica.entrypoints.rest.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FranchiseResponse {
    private Long id;
    private String name;
}
