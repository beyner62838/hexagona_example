package com.example.PruebaTecnica.port;

import reactor.core.publisher.Mono;

public interface EventPublisherPort {
    Mono<Void> publish(String topic, String key, Object payload);
}
