package com.example.PruebaTecnica.gateway;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Asegura que el header Authorization se reenvíe al backend cuando el cliente
 * envía Basic Auth (p. ej. desde Insomnia). Algunos proxies pueden no hacerlo por defecto.
 */
@Component
public class ForwardAuthorizationGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String auth = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (auth == null || auth.isBlank()) {
            return chain.filter(exchange);
        }
        ServerHttpRequest mutated = exchange.getRequest().mutate().header("Authorization", auth).build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
