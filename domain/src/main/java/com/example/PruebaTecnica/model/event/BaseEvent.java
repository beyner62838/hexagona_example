package com.example.PruebaTecnica.model.event;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaseEvent {
    private String eventId;
    private String occurredAt;
    private EventType eventType;
}
