package com.example.PruebaTecnica.entrypoints.rest.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateNameRequest {
    @NotBlank
    private String name;
}
