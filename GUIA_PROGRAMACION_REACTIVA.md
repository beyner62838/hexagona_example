# 📚 GUÍA COMPLETA: PROGRAMACIÓN REACTIVA EN TU PROYECTO

## 🎯 Introducción

Tu proyecto usa **Spring WebFlux** con **Project Reactor**, que es la forma moderna y no-bloqueante de hacer programación reactiva en Spring Boot. En lugar de hilos que esperan respuestas (bloqueantes), usas **streams de datos** que emiten valores de forma asincrónica.

---

## 🔑 CONCEPTOS FUNDAMENTALES

### 1. **Mono** vs **Flux**

#### **Mono<T>** - Retorna 0 o 1 elemento
```java
// Ejemplo: buscar una franquicia por ID
Mono<Franchise> mono = franchiseRepository.findById(1L);
```
- Ideal para operaciones de CRUD simples (GET, POST, DELETE)
- Retorna exactamente 1 resultado o un error

#### **Flux<T>** - Retorna 0 a N elementos
```java
// Ejemplo: listar todas las franquicias
Flux<Franchise> flux = franchiseRepository.findAll();
```
- Ideal para streams de datos múltiples
- Perfecto para búsquedas que retornan listas

---

## 📌 OPERADORES REACTIVOS PRINCIPALES

### **1. `map()` - Transformar datos**

**Uso:** Cuando quieres cambiar el tipo de dato que fluye

```java
// CASO 1: En FranchiseUseCase.create()
Mono<Franchise> mono = repository.save(franchise)
    .map(saved -> { 
        saved.setName("Nuevo nombre");
        return saved;
    });
```
**Cuando usarlo:**
- Convertir un modelo de DB a modelo de dominio
- Mapear entre DTOs y entidades
- Transformaciones simples sin operaciones async

---

### **2. `flatMap()` - Operaciones en cadena (IMPORTANTE)**

**Uso:** Cuando cada operación depende del resultado anterior y es ASINCRÓNICA

```java
// CASO: En FranchiseUseCase.create()
return repository.save(Franchise.builder().name(name).build())
    .flatMap(saved -> publish(EventType.CREATED, saved).thenReturn(saved));
    //      ^^^^^^^ flatMap porque:
    // 1. Primero guardamos (save)
    // 2. Luego publicamos evento (publish es async)
    // 3. El publish depende del resultado del save
```

**Diferencia entre map y flatMap:**

```java
// ❌ INCORRECTO - map con operación async retorna Mono<Mono<...>>
Mono<Mono<Franchise>> incorrecta = repository.save(franchise)
    .map(saved -> publisher.publish(...)); // Retorna Mono, dentro de Mono = ERROR

// ✅ CORRECTO - flatMap "aplana" el resultado
Mono<Franchise> correcta = repository.save(franchise)
    .flatMap(saved -> publisher.publish(...).thenReturn(saved));
```

**Caso real de tu código - BranchUseCase:**
```java
public Mono<Branch> create(Long franchiseId, String name) {
    // 1. Validar que la franquicia existe
    // 2. Si existe, guardar rama
    // 3. Luego publicar evento
    return franchiseRepository.findById(franchiseId)
        .switchIfEmpty(Mono.error(new NotFoundException(...)))
        .then(branchRepository.save(...))          // Encadenar operación
        .flatMap(saved -> publish(...).thenReturn(saved)); // Publicar evento
}
```

---

### **3. `switchIfEmpty()` - Manejo de errores**

**Uso:** Cuando no hay datos, retornar un error o alternativa

```java
// CASO: En todos los useCase para validaciones
repository.findById(id)
    .switchIfEmpty(Mono.error(new NotFoundException("No encontrado")))
    // Si findById retorna empty, ejecuta el error en su lugar
```

**Casos de uso:**
```java
// CASO 1: Validar existencia antes de actualizar
return repository.findById(id)
    .switchIfEmpty(Mono.error(new NotFoundException("...")))
    .map(franchise -> {
        franchise.setName(newName);
        return franchise;
    })
    .flatMap(repository::save);

// CASO 2: Retornar valor por defecto
return repository.findById(id)
    .switchIfEmpty(Mono.just(defaultFranchise));
```

---

### **4. `then()` - Ignorar el resultado anterior**

**Uso:** Cuando ejecutas operación pero solo te importa que se complete, no el resultado

```java
// CASO: En BranchUseCase.create() 
// Validar franquicia existe, pero ignorar el resultado de findById
return franchiseRepository.findById(franchiseId)
    .switchIfEmpty(Mono.error(...))
    .then(branchRepository.save(...)) // Ignora el resultado del findById
    //      ^^^^^^ Ejecuta save pero no usa la franquicia
```

**Comparación:**

```java
// ❌ INCORRECTO - recibe objeto no usado
return franchiseRepository.findById(franchiseId)
    .flatMap(franchise -> branchRepository.save(...)); // franchise no se usa

// ✅ CORRECTO - usa then() explícitamente
return franchiseRepository.findById(franchiseId)
    .then(branchRepository.save(...));
```

---

### **5. `filter()` - Filtrar datos**

**Uso:** Solo pasar elementos que cumplan condición

```java
// CASO: En FranchiseController.topProductsByBranch()
return branchUseCase.listByFranchise(id)
    .flatMap(branch -> productUseCase.topStockByBranch(branch.getId()))
    .filter(p -> p != null)  // Solo emite productos no nulos
    .map(ApiMapper::toResponse);
```

**Otros usos:**
```java
// Filtrar productos con stock > 10
Flux<Product> filtrado = productUseCase.listByBranch(branchId)
    .filter(p -> p.getStock() > 10);

// Filtrar activos
Flux<Franchise> activos = franchiseUseCase.list()
    .filter(f -> f.isActive());
```

---

### **6. `thenReturn()` - Completar y retornar valor**

**Uso:** Terminar operación async y retornar un valor específico

```java
// CASO: En FranchiseUseCase.create()
repository.save(saved)
    .flatMap(saved -> publish(EventType.CREATED, saved)
        .thenReturn(saved))  // Completa el publish y retorna saved
```

---

### **7. `thenEmpty()` / `thenMany()` - Completar con otro Publisher**

```java
// Ejecutar operación y luego ejecutar otra
deleteAllProducts()
    .thenMany(saveNewProducts(list)) // Luego guardar varios

// O simplemente completar
deleteProduct(id)
    .then() // Solo completar sin retornar nada
```

---

## 🏗️ ARQUITECTURA DE TU PROYECTO

### Flujo completo de una CREACIÓN de Franquicia:

```
1. USER (Cliente HTTP)
   ↓ POST /api/franchises
2. FranchiseController.create()
   ↓ franchiseUseCase.create("Mi Franquicia")
3. FranchiseUseCase.create()
   ├─ repository.save(newFranchise)           → Mono<Franchise>
   │  (R2DBC - reactivo a BD)
   ├─ .flatMap(saved → publish(...))          → Mono<Void>
   │  (Kafka - publica evento)
   └─ .thenReturn(saved)                      → Mono<Franchise>
4. Retorna Mono<Franchise> al Usuario
   ↓ Serializa a JSON
5. USER recibe respuesta HTTP con la franquicia creada
```

---

## 💡 PATRONES COMUNES EN TU CÓDIGO

### Patrón 1: CREAR + PUBLICAR EVENTO
```java
// En todos los useCase: create, updateName, delete
public Mono<Franchise> create(String name) {
    return repository.save(Franchise.builder().name(name).build())
        .flatMap(saved -> publish(EventType.CREATED, saved).thenReturn(saved));
        //      ^^^^^^^ Encadenar: primero guardar, luego publicar
}
```

**Por qué flatMap y no map:**
- `save()` retorna `Mono<Franchise>` (async)
- `publish()` retorna `Mono<Void>` (async)
- Necesitas esperar a que `save()` termine para saber qué publicar
- `flatMap` maneja esta dependencia correctamente

---

### Patrón 2: VALIDAR + ACTUALIZAR + GUARDAR
```java
// En updateName de cualquier useCase
public Mono<Franchise> updateName(Long id, String name) {
    return repository.findById(id)                    // Buscar
        .switchIfEmpty(Mono.error(...))              // Si no existe → Error
        .map(f -> { f.setName(name); return f; })    // Cambiar datos
        .flatMap(repository::save)                   // Guardar (async)
        .flatMap(saved -> publish(...).thenReturn(saved)); // Publicar
}
```

**Flujo:**
1. `findById` → obtiene la franquicia o error
2. `map` → cambia el nombre (síncrono)
3. `flatMap(save)` → guarda (async)
4. `flatMap(publish)` → publica evento

---

### Patrón 3: VALIDAR Y ELIMINAR
```java
// En delete de cualquier useCase
public Mono<Void> delete(Long id) {
    return repository.findById(id)
        .switchIfEmpty(Mono.error(new NotFoundException(...)))
        .flatMap(entity -> repository.deleteById(id)  // Eliminar es async
            .then(publish(EventType.DELETED, entity))); // Luego publicar
}
```

---

### Patrón 4: RELACIONALES (Validar entidad relacionada)
```java
// En BranchUseCase.create()
public Mono<Branch> create(Long franchiseId, String name) {
    return franchiseRepository.findById(franchiseId)   // ¿Existe franquicia?
        .switchIfEmpty(Mono.error(...))               // Si no, error
        .then(branchRepository.save(...))              // Ignorar franquicia, guardar rama
        .flatMap(saved -> publish(...).thenReturn(saved));
}
```

**¿Por qué `then()` y no `flatMap()`?**
- Solo necesitas validar que existe la franquicia
- No necesitas usar los datos de la franquicia
- `then()` "descarta" el resultado del findById

---

## 🔄 ANALISIS DE OPERADORES POR CASO

| Operador | Entrada | Salida | Uso |
|----------|---------|--------|-----|
| **map()** | T | U | Transformaciones síncronas |
| **flatMap()** | T | Mono<U> | Operaciones async encadenadas |
| **then()** | Mono<T> | Mono<Void> | Ignorar resultado anterior |
| **thenReturn(T)** | Mono<Void> | Mono<T> | Completar y retornar valor |
| **switchIfEmpty()** | Mono<T> | Mono<T> | Error si está vacío |
| **filter()** | T | T o empty | Condicional |

---

## 🚀 EJEMPLOS PRÁCTICOS AVANZADOS

### Ejemplo 1: Crear Franquicia + Rama + Productos en CADENA
```java
public Mono<Franchise> createFranchiseWithSetup(String franchiseName, 
                                               String branchName, 
                                               List<String> products) {
    return franchiseUseCase.create(franchiseName)        // Crear franquicia
        .flatMap(franchise -> 
            branchUseCase.create(franchise.getId(), branchName)  // Crear rama
                .flatMap(branch -> 
                    Flux.fromIterable(products)         // Para cada producto
                        .flatMap(productName -> 
                            productUseCase.create(branch.getId(), productName, 0)
                        )
                        .then(Mono.just(franchise))     // Retornar franquicia
                )
        );
}
```

---

### Ejemplo 2: Obtener Franquicia con todos sus Productos
```java
public Mono<FranchiseWithAllProducts> getFranchiseComplete(Long franchiseId) {
    return franchiseUseCase.get(franchiseId)            // Obtener franquicia
        .flatMap(franchise -> 
            branchUseCase.listByFranchise(franchiseId)  // Obtener ramas
                .flatMap(branch -> 
                    productUseCase.listByBranch(branch.getId())  // Obtener productos
                        .collectList()                  // Agregar en lista
                        .map(products -> new BranchWithProducts(branch, products))
                )
                .collectList()                          // Agregar ramas
                .map(branches -> new FranchiseWithAllProducts(franchise, branches))
        );
}
```

---

### Ejemplo 3: Actualizar múltiples productos
```java
public Flux<Product> updateMultipleStock(Map<Long, Integer> updates) {
    return Flux.fromIterable(updates.entrySet())        // Para cada entrada
        .flatMap(entry -> 
            productUseCase.updateStock(entry.getKey(), entry.getValue())
        );
}
```

---

## ⚠️ ERRORES COMUNES Y CÓMO EVITARLOS

### Error 1: Usar `map()` con operación async
```java
// ❌ INCORRECTO
Mono<Mono<Franchise>> mal = repository.save(franchise)
    .map(saved -> repository.save(saved)); // Retorna Mono<Mono<...>>

// ✅ CORRECTO
Mono<Franchise> bien = repository.save(franchise)
    .flatMap(saved -> repository.save(saved));
```

---

### Error 2: Olvidar `switchIfEmpty()` para validaciones
```java
// ❌ INCORRECTO - Si no existe, retorna Mono.empty() (silencioso)
public Mono<Franchise> get(Long id) {
    return repository.findById(id);  // ¿Qué pasa si no existe?
}

// ✅ CORRECTO - Lanzar excepción clara
public Mono<Franchise> get(Long id) {
    return repository.findById(id)
        .switchIfEmpty(Mono.error(new NotFoundException("Franchise not found")));
}
```

---

### Error 3: No usar `then()` cuando no necesitas el valor
```java
// ❌ INCORRECTO - Recibe objeto que no usa
return userRepository.findById(userId)
    .flatMap(user -> productRepository.save(product)); // user no se usa

// ✅ CORRECTO - Explícito que ignoramos el resultado
return userRepository.findById(userId)
    .then(productRepository.save(product));
```

---

### Error 4: Bloquear el reactor con `.block()`
```java
// ❌ NUNCA hagas esto en producción
Franchise franchise = franchiseUseCase.create("Nombre").block(); // MALO

// ✅ Retorna el Mono tal cual al cliente
return franchiseUseCase.create("Nombre");
```

---

## 📊 COMPARACIÓN: Síncrono vs Reactivo

### ANTES (Bloqueante - NO en tu proyecto)
```java
@RestController
public class OldFranchiseController {
    public FranchiseResponse create(CreateFranchiseRequest request) {
        // Hilo ESPERA aquí
        Franchise saved = repository.save(new Franchise(request.getName()));
        // Hilo ESPERA aquí
        eventPublisher.publish(new FranchiseEvent(...));
        // Hilo ESPERA aquí
        return ApiMapper.toResponse(saved);
        // Total: Hilo ocupado ~500ms esperando BD
    }
}
```

### AHORA (No-bloqueante - Tu proyecto)
```java
@RestController
public class FranchiseController {
    public Mono<FranchiseResponse> create(CreateFranchiseRequest request) {
        // Hilo NO espera, se devuelve a pool
        return franchiseUseCase.create(request.getName())
            // Otro hilo del pool ejecuta cuando BD responde
            .map(ApiMapper::toResponse);
            // Total: Pocos hilos sirven muchas requests
    }
}
```

---

## 🎓 RESUMEN RÁPIDO

| Necesitas... | Usa |
|--------------|-----|
| Transformar datos | `map()` |
| Operación async dependiente | `flatMap()` |
| Ignorar resultado | `then()` |
| Retornar valor específico | `thenReturn()` |
| Si no hay dato → error | `switchIfEmpty()` |
| Solo algunos datos | `filter()` |
| Múltiples elementos | `Flux` |
| Un único elemento | `Mono` |

---

## 🔗 FLUJO VISUAL COMPLETO DEL PROYECTO

```
                                    USUARIO HTTP
                                         ↓
                    POST /api/franchises (CreateFranchiseRequest)
                                         ↓
                        FranchiseController.create()
                                         ↓
                    franchiseUseCase.create(name: String)
                                         ↓
                        repository.save(new Franchise())
                        (R2DBC - BD Reactiva)
                                         ↓
                            Mono<Franchise> returned
                                         ↓
                        .flatMap(saved → publish(...))
                        (Kafka Publisher)
                                         ↓
                            Mono<Void> (publicación)
                                         ↓
                        .thenReturn(saved)
                                         ↓
                        Mono<Franchise> (resultado final)
                                         ↓
                        .map(ApiMapper::toResponse)
                                         ↓
                        Mono<FranchiseResponse>
                                         ↓
                Spring serializa a JSON y retorna HTTP 200
                                         ↓
                                    USUARIO recibe JSON
```

---

## 📚 REFERENCIAS EN TU CÓDIGO

- **FranchiseUseCase** → Patrón CREAR + PUBLICAR (línea 26-28)
- **BranchUseCase** → Patrón VALIDAR RELACIONAL (línea 28-31)
- **ProductUseCase** → Patrón VALIDAR + ACTUALIZAR (línea 31-36)
- **FranchiseController.topProductsByBranch()** → Patrón FLUJO MÚLTIPLE (línea 54-59)

---

¡Ahora entiendes cómo funciona la programación reactiva en tu proyecto! 🚀
