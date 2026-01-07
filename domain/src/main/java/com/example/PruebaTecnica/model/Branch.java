package com.example.PruebaTecnica.model;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Branch {
    private Long id;
    private Long franchiseId;
    private String name;
}
