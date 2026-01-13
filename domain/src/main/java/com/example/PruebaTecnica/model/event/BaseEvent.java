package com.example.PruebaTecnica.model.event;
import com.example.PruebaTecnica.model.event.Enums.EventType;
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
