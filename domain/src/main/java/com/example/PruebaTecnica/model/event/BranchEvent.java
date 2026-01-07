package com.example.PruebaTecnica.model.event;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchEvent {
    private String eventId;
    private String occurredAt;
    private EventType eventType;

    private Long branchId;
    private Long franchiseId;
    private String name;
}
