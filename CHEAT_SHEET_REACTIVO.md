# 🎯 CHEAT SHEET: OPERADORES REACTIVOS EN TU PROYECTO

## 1️⃣ MONO (0 o 1 elemento)

### Creación
```java
Mono<T> mono = Mono.just(value);           // Retorna valor
Mono<T> mono = Mono.empty();                // Retorna vacío
Mono<T> mono = Mono.error(exception);       // Retorna error
Mono<T> mono = repository.findById(id);     // De base de datos
```

### Transformación
```java
mono.map(value → transformValue)            // Cambiar tipo (síncrono)
mono.flatMap(value → monoAsync)             // Cambiar a otro Mono (async)
mono.filter(value → condition)              // Pasar si cumple condición
```

### Manejo de errores
```java
mono.switchIfEmpty(Mono.error(...))         // Si vacío → Error
mono.switchIfEmpty(Mono.just(default))      // Si vacío → Default
mono.onErrorResume(e → Mono.just(...))      // Si error → Default
mono.onErrorReturn(value)                   // Si error → Retornar valor
mono.retry(times)                           // Reintentar N veces
```

### Finalización
```java
mono.then()                                 // Ignorar y completar
mono.thenReturn(value)                      // Completar y retornar
mono.thenMany(flux)                         // Luego ejecutar Flux
mono.block()                                // ❌ NUNCA en producción
mono.subscribe(value → {...})               // Suscribirse al resultado
```

### Observación
```java
mono.doOnNext(v → log.info("Valor: {}", v))        // Log en cada emisión
mono.doOnError(e → log.error("Error", e))          // Log si hay error
mono.doOnComplete(() → log.info("Completado"))     // Log al terminar
mono.doOnSubscribe(s → log.info("Suscrito"))       // Log al suscribirse
mono.doFinally(signal → log.info("Fin: {}", signal)) // Log al finalizar
```

---

## 2️⃣ FLUX (0 a N elementos)

### Creación
```java
Flux<T> flux = Flux.just(v1, v2, v3);              // Múltiples valores
Flux<T> flux = Flux.empty();                        // Vacío
Flux<T> flux = Flux.error(exception);               // Error
Flux<T> flux = Flux.range(1, 10);                   // Rango 1-10
Flux<T> flux = Flux.fromIterable(list);             // De lista
Flux<T> flux = repository.findAll();                // De BD
```

### Transformación
```java
flux.map(value → transformValue)            // Transformar cada uno
flux.flatMap(value → monoAsync)             // A Mono async por cada uno
flux.flatMapSequential(value → monoAsync)   // flatMap pero secuencial
flux.filter(value → condition)              // Solo los que cumplan
flux.take(n)                                // Primeros N elementos
flux.skip(n)                                // Saltar N elementos
flux.distinct()                             // Sin duplicados
flux.sort()                                 // Ordenar
flux.sorted(Comparator.comparing(...))      // Ordenar con comparador
```

### Agrupación
```java
flux.collectList()                          // Mono<List<T>>
flux.collectMap(keyMapper)                  // Mono<Map<K, T>>
flux.groupBy(keyMapper)                     // Flux<GroupedFlux<K, T>>
flux.scan((acc, val) → acc+val)             // Acumular valores
flux.reduce((acc, val) → acc+val)           // Reducir a 1 valor
```

### Manejo de errores
```java
flux.onErrorContinue((e, v) → {...})        // Continuar en error
flux.onErrorResume(e → Flux.just(...))      // Si error → Flux alternativa
flux.retry(times)                           // Reintentar
```

### Finalización
```java
flux.then()                                 // Ignorar elementos, completar
flux.then(monoOtro)                         // Luego ejecutar Mono
flux.subscribe(value → {...})               // Suscribirse
```

---

## 3️⃣ PATRONES EN TU CÓDIGO

### Patrón A: CREAR + PUBLICAR
```java
repository.save(entity)
    .flatMap(saved → publisher.publish(...).thenReturn(saved))
    //      ^^^^^^^ flatMap: save es async
    //               ^^^^^^ publisher es async
    //                             ^^^^^^ thenReturn: retorna saved
```

### Patrón B: VALIDAR + ACTUALIZAR
```java
repository.findById(id)
    .switchIfEmpty(Mono.error(...))    // No existe → Error
    .map(entity → {...})               // Cambiar datos (síncrono)
    .flatMap(repository::save)         // Guardar (async)
```

### Patrón C: VALIDAR RELACIONAL
```java
foreignRepository.findById(foreignId)
    .switchIfEmpty(Mono.error(...))    // No existe relación → Error
    .then(mainRepository.save(...))    // Ignorar el objeto relacionado
    //  ^^^^ Porque solo validamos, no usamos el valor
```

### Patrón D: LISTAR TODO
```java
repository.findAll()                   // Flux<T>
    .filter(t → condition)             // Filtrar
    .map(ApiMapper::toResponse)        // Convertir a DTO
```

### Patrón E: OBTENER + LISTAR DEPENDIENTES
```java
mainRepository.findById(id)            // Mono<Main>
    .flatMap(main →
        dependentRepository.findByMainId(main.getId())  // Flux<Dependent>
            .collectList()             // Mono<List<Dependent>>
            .map(list → new DTO(main, list))
    )
```

### Patrón F: MÚLTIPLES MONOS EN PARALELO
```java
Mono.zip(
    repository1.findById(id1),
    repository2.findById(id2),
    repository3.findById(id3)
)
.map(tuple → new DTO(tuple.getT1(), tuple.getT2(), tuple.getT3()))
```

---

## 4️⃣ DECISIONES RÁPIDAS

### ¿Mono o Flux?
```
¿Retorna 1 elemento máximo?      → Mono
¿Retorna múltiples elementos?     → Flux
¿Retorna lista pero es 1 resultado? → Mono<List<T>>
```

### ¿map() o flatMap()?
```
¿Operación síncrona?              → map()
¿Operación asincrónica?           → flatMap()
¿No seguro?                        → Usa flatMap (más seguro)
```

### ¿then() o flatMap()?
```
¿Necesito el valor anterior?       → flatMap()
¿No necesito el valor anterior?    → then()
¿Quiero ignorar explícitamente?    → then()
```

### ¿switchIfEmpty() o onErrorResume()?
```
¿Elemento no encontrado?           → switchIfEmpty()
¿Hay excepción?                    → onErrorResume()
¿No seguro?                        → switchIfEmpty() para validar
```

---

## 5️⃣ ERRORES MÁS COMUNES

### ❌ Error 1: map() con async
```java
// INCORRECTO
Mono<Mono<T>> mal = mono.map(v → otherRepository.save(v));

// CORRECTO
Mono<T> bien = mono.flatMap(v → otherRepository.save(v));
```

### ❌ Error 2: flatMap() para validar sin usar valor
```java
// INCORRECTO (usa flatMap para validar)
return repo1.findById(id)
    .flatMap(unused → repo2.save(...));

// CORRECTO (usa then())
return repo1.findById(id)
    .then(repo2.save(...));
```

### ❌ Error 3: No usar switchIfEmpty()
```java
// INCORRECTO (no valida)
return repository.findById(id);  // Si no existe: mono.empty()

// CORRECTO (valida)
return repository.findById(id)
    .switchIfEmpty(Mono.error(new NotFoundException(...)));
```

### ❌ Error 4: Bloquear con .block()
```java
// ❌ NUNCA en controllers
Franchise f = useCase.create("Name").block();

// ✅ SIEMPRE retornar Mono
return useCase.create("Name");
```

### ❌ Error 5: Olvidar collectList()
```java
// INCORRECTO (Flux<T> en Mono<T>)
Mono<Product> mono = flux;  // Error tipo!

// CORRECTO
Mono<List<Product>> mono = flux.collectList();
```

---

## 6️⃣ FLUJO VISUAL COMPLETO

```
Usuario HTTP → POST /api
    ↓
Controller.create()
    ↓
UseCase.create()
    ↓
repository.save()  [R2DBC]
    ↓ Mono<Entity>
.flatMap()
    ↓
publisher.publish()  [Kafka]
    ↓ Mono<Void>
.thenReturn(saved)
    ↓ Mono<Entity>
.map(ApiMapper::toResponse)
    ↓ Mono<DTO>
Spring serializa a JSON
    ↓
Usuario HTTP ← 200 + JSON
```

---

## 7️⃣ TIEMPO DE EJECUCIÓN

### Secuencial (bad 😞)
```java
return serviceA.doSomething()      // 100ms
    .flatMap(a → serviceB.doSomething(a))  // 100ms
    .flatMap(b → serviceC.doSomething(b))  // 100ms
    
// Total: 300ms
```

### Paralelo (good 😊)
```java
return Mono.zip(
    serviceA.doSomething(),         // 100ms
    serviceB.doSomething(),         // 100ms
    serviceC.doSomething()          // 100ms
)
// Total: 100ms (máximo de los 3)
```

---

## 8️⃣ CASOS DE USO POR OPERADOR

| Operador | Caso | Ejemplo |
|----------|------|---------|
| map() | Transformar tipo | `Product → ProductDTO` |
| flatMap() | Async + dependencia | `save().then(publish())` |
| then() | Async sin usar valor | `validate().then(save())` |
| thenReturn() | Completar + retornar | `publish().thenReturn(saved)` |
| switchIfEmpty() | No encontrado | `findById().switchIfEmpty(error)` |
| filter() | Condicional | `stock > 10` |
| collectList() | Flux → Mono<List> | `findAll().collectList()` |
| timeout() | Máximo tiempo | `Duration.ofSeconds(5)` |
| retry() | Reintentar | `retry(3)` |
| doOnNext() | Log/observar | `log.info()` |

---

## 9️⃣ TEMPLATE PARA NUEVAS FUNCIONES

### Para GET (1 elemento)
```java
return repository.findById(id)
    .switchIfEmpty(Mono.error(new NotFoundException(...)))
    .map(ApiMapper::toResponse);
```

### Para GET (múltiples)
```java
return repository.findAll()
    .map(ApiMapper::toResponse);
```

### Para POST (crear)
```java
return repository.save(entity)
    .flatMap(saved → publisher.publish(...).thenReturn(saved))
    .map(ApiMapper::toResponse);
```

### Para PUT (actualizar)
```java
return repository.findById(id)
    .switchIfEmpty(Mono.error(...))
    .map(entity → {
        entity.setField(newValue);
        return entity;
    })
    .flatMap(repository::save)
    .flatMap(saved → publisher.publish(...).thenReturn(saved))
    .map(ApiMapper::toResponse);
```

### Para DELETE
```java
return repository.findById(id)
    .switchIfEmpty(Mono.error(...))
    .flatMap(entity → repository.deleteById(id)
        .then(publisher.publish(...)));
```

---

¡Con esto tienes todo lo que necesitas! 🚀

Imprime este cheat sheet y tenlo a mano mientras programas. 📋
