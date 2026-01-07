package com.example.PruebaTecnica.entrypoints.rest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateProductRequest {
    @NotBlank
    private String name;

    @NotNull
    @Min(0)
    private Integer stock;
}
