package com.example.PruebaTecnica.model.event;
import com.example.PruebaTecnica.model.event.Enums.EventType;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductEvent {
    private String eventId;
    private String occurredAt;
    private EventType eventType;

    private Long productId;
    private Long branchId;
    private String name;
    private Integer stock;
}
