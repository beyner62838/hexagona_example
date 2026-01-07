package com.example.PruebaTecnica.entrypoints.rest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStockRequest {
    @NotNull
    @Min(0)
    private Integer stock;
}
