# 🔍 ANÁLISIS COMPLETO DE TU CÓDIGO

## Estructura del Proyecto

```
Arquitectura Hexagonal (Limpia)
├── domain/                    ← Lógica de negocio (sin Spring)
│   ├── model/                 ← Entidades de dominio
│   ├── port/                  ← Interfaces de contrato
│   ├── usecase/               ← Casos de uso (lógica)
│   └── exception/             ← Excepciones de negocio
├── infrastructure/            ← Implementación técnica
│   ├── driven-adapters/       ← BD, Kafka, etc.
│   └── entry-points/          ← REST API, Gateways
└── application/               ← Configuración y arranque
```

---

## 1. CAPA DOMAIN - Los UseCase

### FranchiseUseCase (Manejo de Franquicias)

```java
public Mono<Franchise> create(String name) {
    return repository.save(Franchise.builder().name(name).build())
        .flatMap(saved → publish(EventType.CREATED, saved).thenReturn(saved));
}
```

**Desglose paso a paso:**

1. **`Franchise.builder().name(name).build()`**
   - Crea objeto Franchise nueva
   - Sin ID (BD lo asigna)

2. **`repository.save(...)`**
   - Envía a BD (R2DBC - NO BLOQUEA)
   - Retorna: `Mono<Franchise>` con ID asignado

3. **`.flatMap(saved → ...)`**
   - Recibe la franquicia guardada
   - Ejecuta siguiente operación async (publicar evento)

4. **`publish(EventType.CREATED, saved)`**
   - Publica evento en Kafka
   - Retorna: `Mono<Void>` (sin resultado)

5. **`.thenReturn(saved)`**
   - Completa la publicación
   - Retorna la franquicia guardada (no el void)

**Resultado final:** `Mono<Franchise>` con los datos guardados

---

### UpdateName en todos los useCase

```java
public Mono<Franchise> updateName(Long id, String name) {
    return repository.findById(id)                    // ← Paso 1
        .switchIfEmpty(Mono.error(...))              // ← Paso 2
        .map(f → { f.setName(name); return f; })     // ← Paso 3
        .flatMap(repository::save)                   // ← Paso 4
        .flatMap(saved → publish(...).thenReturn(saved)); // ← Paso 5
}
```

**Paso 1: Buscar** 
```
findById(id) → Mono<Franchise> (si existe) o Mono.empty()
```

**Paso 2: Validar**
```
Si está empty (no existe) → Lanza NotFoundException
Si existe → Continúa con la Franchise
```

**Paso 3: Modificar datos (SÍNCRONO)**
```
map() porque es transformación simple, sin BD ni I/O
Cambia el nombre en memoria (muy rápido)
```

**Paso 4: Guardar (ASINCRÓNICO)**
```
flatMap porque save() es async (espera respuesta BD)
Solo guardamos si tenemos los datos modificados
```

**Paso 5: Publicar evento (ASINCRÓNICO)**
```
flatMap porque publish() es async
El publish depende de que save() haya finalizado
thenReturn() porque necesitamos la franquicia, no el Void del publish
```

---

### Delete en todos los useCase

```java
public Mono<Void> delete(Long id) {
    return repository.findById(id)
        .switchIfEmpty(Mono.error(new NotFoundException(...)))
        .flatMap(entity → 
            repository.deleteById(id)          // Eliminar
                .then(publish(EventType.DELETED, entity)) // Publicar
        );
}
```

**¿Qué pasa aquí?**

1. Valida que existe (findById)
2. Si existe, elimina (deleteById - retorna `Mono<Void>`)
3. Usa `then()` porque deleteById retorna `Mono<Void>` (no hay datos)
4. `then(publish(...))` ejecuta el publish después del delete
5. Retorna `Mono<Void>` (nada que retornar al usuario)

---

## 2. CAPA INFRASTRUCTURE - Los Repositorios

### R2DBC (Reactive Database)

```java
public interface FranchiseR2dbcRepository extends ReactiveCrudRepository<FranchiseEntity, Long> {
}
```

**¿Qué es ReactiveCrudRepository?**
- Spring proporciona métodos async:
  - `save(T)` → `Mono<T>`
  - `findById(ID)` → `Mono<T>`
  - `findAll()` → `Flux<T>`
  - `deleteById(ID)` → `Mono<Void>`

**Ejemplo de query personalizada:**

```java
public interface ProductR2dbcRepository extends ReactiveCrudRepository<ProductEntity, Long> {
    Flux<ProductEntity> findByBranchId(Long branchId);
    
    @Query("SELECT * FROM products WHERE branch_id = :branchId ORDER BY stock DESC LIMIT 1")
    Mono<ProductEntity> findTopStockByBranchId(Long branchId);
}
```

- Spring genera el SQL automáticamente (findByBranchId)
- O puedes usar `@Query` para SQL custom
- Retorna Mono/Flux automáticamente

---

### Adapters (Convertidores)

```java
@Component
public class FranchiseRepositoryAdapter implements FranchiseRepositoryPort {
    
    private final FranchiseR2dbcRepository repository;
    
    public Mono<Franchise> save(Franchise franchise) {
        return repository.save(DbMapper.toEntity(franchise))
            .map(DbMapper::toModel);
    }
}
```

**¿Por qué el Adapter?**

```
UseCase (dominio)
   ↓ usa port
FranchiseRepositoryPort (interfaz, sin Spring)
   ↓ implementado por
FranchiseRepositoryAdapter (Spring, con BD)
   ↓ usa
FranchiseR2dbcRepository (Spring Data R2DBC)
   ↓ accede a
Base de datos (SQL)
```

**Patrón Hexagonal = Independencia de la tecnología**

Cambiar de BD: Solo cambias el Adapter, el UseCase no se entera

---

## 3. CAPA ENTRY-POINTS - Los Controllers

### FranchiseController

```java
@RestController
@RequestMapping("/api/franchises")
public class FranchiseController {
    
    @PostMapping
    public Mono<FranchiseResponse> create(@Valid @RequestBody CreateFranchiseRequest request) {
        return franchiseUseCase.create(request.getName()).map(ApiMapper::toResponse);
    }
    
    @GetMapping
    public Flux<FranchiseResponse> list() {
        return franchiseUseCase.list().map(ApiMapper::toResponse);
    }
    
    @DeleteMapping("/{id}")
    public Mono<Void> delete(@PathVariable Long id) {
        return franchiseUseCase.delete(id);
    }
}
```

**Patrón clave:**

```java
Mono/Flux useCase
    .map(ApiMapper::toResponse)  // Convertir a DTO
// Spring maneja retorno automático
```

**¿Qué hace Spring con Mono/Flux?**

```
return Mono<FranchiseResponse>
   ↓ Spring vé que es Publisher
   ↓ No bloquea (async)
   ↓ Espera a que emita
   ↓ Serializa a JSON
   ↓ Retorna HTTP 200 + JSON
```

---

### Endpoint especial: topProductsByBranch

```java
@GetMapping("/{id}/top-products")
public Flux<ProductResponse> topProductsByBranch(@PathVariable Long id) {
    return branchUseCase.listByFranchise(id)
        .flatMap(branch → productUseCase.topStockByBranch(branch.getId()))
        .filter(p → p != null)
        .map(ApiMapper::toResponse);
}
```

**Este es un BUEN ejemplo de flatMap:**

1. **`listByFranchise(id)`** → `Flux<Branch>` (puede ser 0 a N ramas)
2. Para CADA rama, obtener su producto con stock más alto
3. **`flatMap(branch → ...)`** → Ejecuta para cada rama
4. **`topStockByBranch()`** → `Mono<Product>` (1 producto)
5. Retorna `Flux<Product>` (aplana todos los Monos)

**Flujo visual:**

```
Franquicia ID=1
   ↓
Ramas: [Rama1, Rama2, Rama3]
   ↓
Para Rama1 → topStockByBranch(Rama1.ID) → Product{name: "P1", stock: 100}
Para Rama2 → topStockByBranch(Rama2.ID) → Product{name: "P2", stock: 50}
Para Rama3 → topStockByBranch(Rama3.ID) → Product{name: "P3", stock: 80}
   ↓
Flux emite: [P1, P2, P3]
   ↓
Retorna al usuario
```

---

## 4. KAFKA - PUBLICACIÓN DE EVENTOS

### KafkaEventPublisher

El patrón completo:

```
1. UseCase llama:
   publisher.publish(TOPIC, partition_key, event)
   
2. KafkaEventPublisher:
   .map(mapper a Kafka)
   .flatMap(kafkaTemplate.send(...))
   
3. Kafka recibe:
   Topic: franchise.events
   Partition Key: franchise_id
   Message: JSON del evento
   
4. Flujo retorna:
   Mono<Void> (no necesita resultado)
```

**¿Por qué en flatMap en el useCase?**

```java
repository.save(franchise)
    .flatMap(saved → publish(EventType.CREATED, saved).thenReturn(saved))
```

- `save()` es async → Mono<Franchise>
- `publish()` es async → Mono<Void>
- No puedes publicar hasta no guardar
- `flatMap` encadena ambas operaciones

---

## 5. FLUJOS COMPLETOS

### Crear Franquicia (POST /api/franchises)

```
1. User: POST /api/franchises {"name": "Mi Franquicia"}
   
2. Controller.create()
   ↓ Valida el request con @Valid
   ↓ Llama UseCase
   
3. UseCase.create("Mi Franquicia")
   ↓ repository.save(new Franchise("Mi Franquicia"))
   ↓ BD ejecuta: INSERT INTO franchises (name) VALUES (...)
   ↓ Retorna: Mono<Franchise{id:1, name:"Mi Franquicia"}>
   
4. .flatMap(saved → publish(...))
   ↓ publisher.publish(
       "franchise.events", 
       "1",  // partition key
       FranchiseEvent{eventType: CREATED, ...}
     )
   ↓ Kafka recibe el evento
   ↓ Retorna: Mono<Void>
   
5. .thenReturn(saved)
   ↓ Completa el publish
   ↓ Retorna nuevamente: Mono<Franchise{id:1, name:"Mi Franquicia"}>
   
6. .map(ApiMapper::toResponse)
   ↓ Convierte a FranchiseResponse DTO
   ↓ Retorna: Mono<FranchiseResponse>
   
7. Spring maneja el Mono
   ↓ Espera la respuesta
   ↓ Serializa a JSON
   
8. User recibe: HTTP 200
   {
     "id": 1,
     "name": "Mi Franquicia"
   }
```

### Listar Franquicias (GET /api/franchises)

```
1. User: GET /api/franchises
   
2. Controller.list()
   ↓ franchiseUseCase.list()
   ↓ repository.findAll()  ← Reactive!
   ↓ BD ejecuta: SELECT * FROM franchises
   ↓ Retorna: Flux<FranchiseEntity> (stream de resultados)
   
3. .map(ApiMapper::toResponse)
   ↓ Transforma CADA elemento a DTO
   ↓ Retorna: Flux<FranchiseResponse>
   
4. Spring maneja el Flux
   ↓ Serializa CADA elemento a JSON array
   
5. User recibe: HTTP 200
   [
     {"id": 1, "name": "Franquicia 1"},
     {"id": 2, "name": "Franquicia 2"},
     ...
   ]
```

---

## 6. DIFERENCIAS CON CÓDIGO BLOQUEANTE

### Antes (NO REACTIVO)

```java
@PostMapping
public FranchiseResponse create(@RequestBody CreateFranchiseRequest request) {
    // Hilo #47 entra aquí
    Franchise franchise = new Franchise(request.getName());
    
    // Hilo #47 ESPERA a que BD responda (500ms)
    Franchise saved = repository.save(franchise);
    
    // Hilo #47 ESPERA a que Kafka responda (100ms)
    eventPublisher.publish(new FranchiseEvent(...));
    
    // Hilo #47 RETORNA
    return ApiMapper.toResponse(saved);
    // Total: 600ms con hilo ocupado
}
```

**Problema:** Si llegan 1000 requests simultáneos, necesitas 1000 hilos (mucha memoria)

### Ahora (REACTIVO - Tu proyecto)

```java
@PostMapping
public Mono<FranchiseResponse> create(@RequestBody CreateFranchiseRequest request) {
    // Hilo #1 entra, crea Mono (casi instantáneo)
    // Hilo #1 retorna al pool
    
    return franchiseUseCase.create(request.getName())
        // Cuando BD responda:
        //   Hilo #2 continúa (puede ser diferente)
        .flatMap(saved → publish(...).thenReturn(saved))
        // Cuando Kafka responda:
        //   Hilo #3 continúa
        .map(ApiMapper::toResponse);
        // Cuando todo esté listo:
        //   Hilo #4 retorna resultado
}
```

**Ventaja:** 1000 requests = Pocos hilos reutilizados (más eficiente)

---

## 7. ANÁLISIS DE PERFORMANCE

### Operación: Crear Franquicia + Publicar Evento

```
BD (save)           : 500ms
Kafka (publish)     : 100ms

CON BLOQUEO:
Tiempo total = 500ms + 100ms = 600ms
Hilos ocupados = 1

REACTIVO:
BD y Kafka se ejecutan en PARALELO cuando es posible?
NO, porque flatMap encadena:
├─ save()    : 500ms
└─ publish() : 100ms (espera a save)
Tiempo total = 600ms
Hilos ocupados = 0 (devueltos al pool)

DIFERENCIA:
- Con 10 requests bloqueantes: 10 hilos ocupados 600ms = 6000ms total
- Con 10 requests reactivos: 1 hilo ocupado 600ms cada uno, ejecutados en paralelo
  = 600ms total con 10 requests
```

---

## 8. REGLAS DE ORO EN TU CÓDIGO

### Regla 1: flatMap para encadenar operaciones async
```java
✅ Correcto:
.flatMap(saved → publish(...).thenReturn(saved))

❌ Incorrecto:
.map(saved → publish(...))  // Retorna Mono<Mono<...>>
```

### Regla 2: map para transformaciones simples
```java
✅ Correcto:
.map(franchise → franchise.getName())  // Síncrono

❌ Incorrecto:
.map(franchise → repository.save(franchise))  // Retorna Mono, no Mono<Mono>
```

### Regla 3: then() cuando no necesitas el valor anterior
```java
✅ Correcto:
franchiseRepository.findById(id)
    .then(branchRepository.save(...))  // Solo validar franquicia

❌ Menos claro:
franchiseRepository.findById(id)
    .flatMap(franchise → branchRepository.save(...))  // franchise no se usa
```

### Regla 4: switchIfEmpty() para validaciones
```java
✅ Correcto:
repository.findById(id)
    .switchIfEmpty(Mono.error(new NotFoundException(...)))

❌ Incorrecto:
repository.findById(id)  // Si no existe: Mono.empty(), no error
```

### Regla 5: Retornar Mono/Flux, NUNCA block()
```java
✅ Correcto:
return franchiseUseCase.create(...);  // Retorna Mono<...>

❌ INCORRECTO:
return franchiseUseCase.create(...).block();  // ¡NUNCA!
```

---

## 9. RESUMEN EJECUTIVO

| Aspecto | Tu implementación | Patrón |
|--------|-------------------|--------|
| BD | R2DBC (no-bloqueante) | ✅ Correcto |
| Flujo de datos | Mono/Flux con flatMap | ✅ Correcto |
| Publicación de eventos | Kafka async | ✅ Correcto |
| Arquitectura | Hexagonal (limpia) | ✅ Correcto |
| Mapeo de capas | Adapters entre capas | ✅ Correcto |
| Controllers | Retornan Mono/Flux | ✅ Correcto |
| Error handling | switchIfEmpty() | ✅ Correcto |

---

## 10. PRÓXIMAS MEJORAS (OPCIONAL)

```java
// 1. Agregar timeouts
return repository.findById(id)
    .timeout(Duration.ofSeconds(5))
    
// 2. Agregar reintentos
return repository.save(franchise)
    .retry(3)
    
// 3. Agregar cache
return repository.findById(id)
    .cache(Duration.ofMinutes(5))
    
// 4. Agregar logging
return repository.findById(id)
    .doOnNext(f → log.info("Franquicia: {}", f.getName()))
    
// 5. Agregar métricas
return repository.findById(id)
    .name("franchise.findById")
    .metrics()
```

---

¡Tu código está EXCELENTE para aprender programación reactiva! 🎉

Está 100% alineado con los patrones modernos de Spring WebFlux.
