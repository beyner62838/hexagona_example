# 💻 EJEMPLOS PRÁCTICOS ESPECÍFICOS PARA TU PROYECTO

## Ejemplo 1: Endpoint para crear Franquicia completa

**Requisito:** Crear una franquicia, una rama y un producto de forma atómica.

```java
@PostMapping("/setup")
public Mono<FranchiseResponse> createFranchiseSetup(
    @Valid @RequestBody CreateFranchiseSetupRequest request) {
    
    return franchiseUseCase.create(request.getFranchiseName())
        // 1. Crear franquicia
        .flatMap(franchise -> 
            branchUseCase.create(franchise.getId(), request.getBranchName())
                // 2. Crear rama en esa franquicia
                .flatMap(branch -> 
                    productUseCase.create(
                        branch.getId(), 
                        request.getProductName(), 
                        request.getInitialStock()
                    )
                    // 3. Crear producto en esa rama
                    .map(product -> franchise) // Ignorar producto, retornar franquicia
                )
        )
        .map(ApiMapper::toResponse);
}
```

**Explicación paso a paso:**

```
1. franchiseUseCase.create(name)
   → Retorna: Mono<Franchise>
   
2. .flatMap(franchise → ...) 
   → Recibe la franquicia creada
   → Ejecuta siguiente operación async
   
3. branchUseCase.create(franchiseId, name)
   → Retorna: Mono<Branch>
   
4. .flatMap(branch → ...)
   → Recibe la rama creada
   
5. productUseCase.create(branchId, name, stock)
   → Retorna: Mono<Product>
   
6. .map(product → franchise)
   → Transforma el Product en Franchise
   → Sin operación async, solo cambiar tipo
   
7. .map(ApiMapper::toResponse)
   → Convertir a DTO para HTTP
```

---

## Ejemplo 2: Obtener Franquicia con todos sus datos

**Requisito:** Obtener 1 franquicia con TODAS sus ramas Y todos los productos de cada rama.

```java
@GetMapping("/{id}/complete")
public Mono<CompleteFranchiseResponse> getComplete(@PathVariable Long id) {
    return franchiseUseCase.get(id)
        .flatMap(franchise ->
            // Obtener todas las ramas de esta franquicia
            branchUseCase.listByFranchise(franchise.getId())
                .flatMap(branch ->
                    // Para CADA rama, obtener sus productos
                    productUseCase.listByBranch(branch.getId())
                        .collectList()
                        // Agrupar productos por rama
                        .map(products -> new BranchWithProducts(branch, products))
                )
                .collectList()
                // Agrupar ramas con franquicia
                .map(branchesWithProducts -> 
                    new CompleteFranchiseResponse(franchise, branchesWithProducts)
                )
        );
}
```

---

## Ejemplo 3: Actualizar múltiples productos a la vez

**Requisito:** Actualizar stock de varios productos en paralelo.

```java
@PostMapping("/bulk-update-stock")
public Flux<ProductResponse> bulkUpdateStock(
    @Valid @RequestBody BulkUpdateStockRequest request) {
    
    return Flux.fromIterable(request.getUpdates())
        // updates es Map<Long productId, Integer newStock>
        .flatMap(update ->
            productUseCase.updateStock(
                update.getProductId(), 
                update.getNewStock()
            )
        )
        .map(ApiMapper::toResponse);
}
```

---

## Ejemplo 4: Búsqueda con filtros

**Requisito:** Listar productos de una rama con stock > 5 y nombre contiene "X".

```java
public Flux<ProductResponse> searchProducts(
    Long branchId, 
    String nameFilter, 
    Integer minStock) {
    
    return productUseCase.listByBranch(branchId)
        .filter(p -> p.getStock() > minStock)
        .filter(p -> p.getName().toLowerCase()
            .contains(nameFilter.toLowerCase()))
        .map(ApiMapper::toResponse);
}
```

---

## Ejemplo 5: Manejar errores y recuperación

**Requisito:** Si falla una operación, retornar valor por defecto.

```java
@GetMapping("/{id}/safe")
public Mono<FranchiseResponse> getSafe(@PathVariable Long id) {
    return franchiseUseCase.get(id)
        .switchIfEmpty(Mono.just(Franchise.builder()
            .id(id)
            .name("Not Found")
            .build()))
        .onErrorResume(e -> {
            log.error("Error fetching franchise", e);
            return Mono.just(Franchise.builder()
                .id(id)
                .name("Error Occurred")
                .build());
        })
        .map(ApiMapper::toResponse);
}
```

---

## Ejemplo 6: Logs en cadenas reactivas

**Requisito:** Debuggear qué pasa en cada paso.

```java
public Mono<Franchise> createWithLogs(String name) {
    return repository.save(Franchise.builder().name(name).build())
        .doOnNext(saved -> log.info("Guardado: {}", saved.getId()))
        .doOnError(error -> log.error("Error al guardar", error))
        .doOnSubscribe(sub -> log.info("Iniciando guardado..."))
        .doFinally(signal -> log.info("Finalizado: {}", signal))
        .flatMap(saved -> publish(EventType.CREATED, saved)
            .doOnNext(v -> log.info("Evento publicado"))
            .thenReturn(saved)
        );
}
```

---

## Ejemplo 7: Combinar múltiples Monos en paralelo

**Requisito:** Obtener franquicia, rama y productos de una sola vez.

```java
@GetMapping("/{franchiseId}/{branchId}/combined")
public Mono<CombinedResponse> getCombined(
    @PathVariable Long franchiseId,
    @PathVariable Long branchId) {
    
    return Mono.zip(
        franchiseUseCase.get(franchiseId),
        branchUseCase.get(branchId),
        productUseCase.listByBranch(branchId).collectList()
    )
    .map(tuple -> new CombinedResponse(
        tuple.getT1(),
        tuple.getT2(),
        tuple.getT3()
    ));
}
```

---

## Ejemplo 8: Timeout y retry automático

**Requisito:** Si la BD tarda mucho, reintentar. Si sigue tardando, error.

```java
public Mono<Franchise> getWithRetry(Long id) {
    return repository.findById(id)
        .switchIfEmpty(Mono.error(new NotFoundException("...")))
        .timeout(Duration.ofSeconds(5))
        .retry(2)
        .onErrorResume(e -> {
            if (e instanceof TimeoutException) {
                return Mono.error(new ServiceTimeoutException("BD no responde"));
            }
            return Mono.error(e);
        });
}
```

---

## Tabla de decisión rápida

| Necesitas | Usa | Ejemplo |
|-----------|-----|---------|
| 1 elemento | `Mono` | `findById()` |
| Múltiples elementos | `Flux` | `findAll()` |
| Transformación simple | `map()` | `p → p.getName()` |
| Operación async | `flatMap()` | `save()` dentro |
| Si está vacío → error | `switchIfEmpty()` | Validaciones |
| Si hay error → alternativa | `onErrorResume()` | Recuperación |
| Ignorar resultado | `then()` | Validaciones |
| Múltiples Mono en paralelo | `Mono.zip()` | Obtener 3 entidades |
| Aguardar timeout | `timeout()` | `Duration.ofSeconds(5)` |
| Reintentar | `retry()` | `retry(2)` |

---

¡Ahora puedes implementar casi cualquier patrón reactivo! 🚀
