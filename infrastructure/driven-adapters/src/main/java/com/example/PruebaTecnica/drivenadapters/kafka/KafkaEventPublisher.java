package com.example.PruebaTecnica.drivenadapters.kafka;

import com.example.PruebaTecnica.port.EventPublisherPort;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class KafkaEventPublisher implements EventPublisherPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public Mono<Void> publish(String topic, String key, Object payload) {
        return Mono.fromFuture(kafkaTemplate.send(topic, key, payload))
                .then();
    }
}
