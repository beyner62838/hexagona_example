package com.example.PruebaTecnica.model.event;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FranchiseEvent {
    private String eventId;
    private String occurredAt;
    private EventType eventType;

    private Long franchiseId;
    private String name;
}
