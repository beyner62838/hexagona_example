package com.example.PruebaTecnica.entrypoints.rest.dto;
import lombok.*;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchResponse {
    private Long id;
    private Long franchiseId;
    private String name;
}
