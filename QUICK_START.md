# ⚡ QUICK START - 5 MINUTOS PARA ENTENDER TODO

## El problema que resuelve la programación reactiva

```
ANTES (Bloqueante):
Tu código:    Cliente 1 ─────────→ BD ─────→ Respuesta (600ms)
              Cliente 2 ─────────→ BD ─────→ Respuesta (600ms)
              ...
RAM usada:    1000 clientes × 1 hilo = 1000 hilos esperando

AHORA (Reactivo):
Tu código:    Cliente 1 ┐
              Cliente 2 ├─→ Pocos hilos ─→ BD responde
              ...       │
              Cliente N ┘
RAM usada:    1000 clientes × 0.01 hilos = 10 hilos reutilizados
```

---

## Los 2 tipos de datos

### MONO - Uno o nada

```
Mono<Franchise> = "Te doy 1 franquicia O nada O error"

Examples:
findById(1)    → Mono<Franchise> (1 resultado o nothing)
create(...)    → Mono<Franchise> (1 nuevo registro)
update(...)    → Mono<Franchise> (1 actualizado)
delete(...)    → Mono<Void> (no retorna nada)
```

### FLUX - Cero, uno, muchos O infinito

```
Flux<Franchise> = "Te doy 0 a N franquicias"

Examples:
findAll()      → Flux<Franchise> (todos los registros)
findByCity()   → Flux<Franchise> (múltiples por ciudad)
stream()       → Flux<Franchise> (stream infinito)
```

---

## Los 2 operadores principales

### MAP - Cambiar tipo (RÁPIDO, sin esperar)

```
Mono<Franchise{id:1, name:"A"}>
         ↓ .map(f → f.getName())
Mono<String{"A"}>

Cuándo:  Transformaciones simples, sin BD, sin I/O
```

### FLATMAP - Operación en cadena (ESPERA a la anterior)

```
Mono<Franchise>
  ↓ .flatMap(f → saveAnotherThing(f))
Mono<OtherThing>

Cuándo:  Una operación depende del resultado anterior
```

---

## 3 Patrones = 80% del código

### Patrón 1: GUARDAR + PUBLICAR

```java
repository.save(entity)                              // 1. Guardar
    .flatMap(saved → publish(...).thenReturn(saved)) // 2. Publicar
    //      ^^^^^^ flatMap porque ambas son async
    //             ^^^^^^ .thenReturn() porque publish retorna Void
```

**Cuándo usarlo:** CREATE, UPDATE (siempre que publiques evento)

---

### Patrón 2: VALIDAR + ACTUALIZAR

```java
repository.findById(id)                         // 1. Buscar
    .switchIfEmpty(Mono.error(...))             // 2. Si no existe → Error
    .map(e → { e.setField(value); return e; }) // 3. Cambiar datos
    .flatMap(repository::save)                  // 4. Guardar
```

**Cuándo usarlo:** UPDATE, VALIDAR EXISTENCIA

---

### Patrón 3: VALIDAR RELACIONAL

```java
foreignRepository.findById(foreignId)  // ¿Existe relación?
    .switchIfEmpty(Mono.error(...))    // Si no → Error
    .then(mainRepository.save(...))    // Si sí → Guardar (sin usar)
    //  ^^^^ "then" porque solo validamos, no usamos el dato
```

**Cuándo usarlo:** Cuando creas rama (validar franquicia existe)

---

## Errores que la mayoría comete

### ❌ Error 1: map() con async

```java
// INCORRECTO (Mono<Mono<...>> !)
mono.map(v → repository.save(v))

// CORRECTO
mono.flatMap(v → repository.save(v))
```

**Regla:** Si lo de dentro retorna Mono/Flux → usa flatMap

---

### ❌ Error 2: block() en controllers

```java
// INCORRECTO ❌
public FranchiseResponse create(...) {
    return useCase.create(...).block();  // ¡PROHIBIDO!
}

// CORRECTO ✅
public Mono<FranchiseResponse> create(...) {
    return useCase.create(...);
}
```

**Regla:** Retorna Mono/Flux tal cual, nunca .block()

---

### ❌ Error 3: Olvidar switchIfEmpty()

```java
// INCORRECTO (si no existe, retorna empty en silencio)
repository.findById(id).map(...)

// CORRECTO
repository.findById(id)
    .switchIfEmpty(Mono.error(new NotFoundException(...)))
    .map(...)
```

**Regla:** Siempre valida con switchIfEmpty() después de find

---

## Las 4 preguntas para decidir operador

```
1. ¿Operación síncrona (transformación simple)?
   → map()

2. ¿Operación asincrónica (BD, API, Kafka)?
   → flatMap()

3. ¿Necesito el valor anterior?
   → flatMap()
   NO necesito → then()

4. ¿Está vacío y es error?
   → switchIfEmpty(error)
```

---

## Tu código en 30 segundos

```java
// 1. CREATE (Crear + Publicar)
public Mono<Franchise> create(String name) {
    return repository.save(Franchise.builder().name(name).build())
        .flatMap(saved → publish(EventType.CREATED, saved).thenReturn(saved));
        // ^^^^^^^ flatMap: ambas son async
        //                        ^^^^^^ thenReturn: queremos la franquicia
}

// 2. UPDATE (Validar + Cambiar + Guardar)
public Mono<Franchise> updateName(Long id, String name) {
    return repository.findById(id)
        .switchIfEmpty(Mono.error(new NotFoundException(...)))
        // ^^^^^^^^^^^ Si no existe → Error
        .map(f → { f.setName(name); return f; })
        // ^^^ map() porque es síncrono
        .flatMap(repository::save)
        // ^^^^^^^ flatMap porque save es async
        .flatMap(saved → publish(...).thenReturn(saved));
}

// 3. GET (Buscar)
public Mono<Franchise> get(Long id) {
    return repository.findById(id)
        .switchIfEmpty(Mono.error(new NotFoundException(...)));
}

// 4. LIST (Todos)
public Flux<Franchise> list() {
    return repository.findAll();
}

// 5. DELETE
public Mono<Void> delete(Long id) {
    return repository.findById(id)
        .switchIfEmpty(Mono.error(...))
        .flatMap(f → repository.deleteById(id)
            .then(publish(EventType.DELETED, f))
        );
}
```

---

## Timeline visual

### Crear franquicia (600ms total)

```
0ms          500ms        600ms
│   SAVE BD   │ PUBLISH    │
├─────────────┤ KAFKA
│ BD esperando│ ├────┤
└─────────────┴──────┘

Total: max(500ms, 100ms) = 600ms
Hilos usados: 0 (devueltos al pool)
```

### Listar franquicias (200ms total)

```
0ms                              200ms
│    SELECT * FROM franchises    │
├────────────────────────────────┤
│ BD ejecuta, retorna resultados
└────────────────────────────────┘

Total: 200ms
Hilos usados: 0 (devueltos al pool)
```

---

## Copiar/Pegar: Los 5 templates

### Template 1: GET by ID

```java
public Mono<T> get(Long id) {
    return repository.findById(id)
        .switchIfEmpty(Mono.error(new NotFoundException(...)));
}
```

### Template 2: GET All

```java
public Flux<T> list() {
    return repository.findAll();
}
```

### Template 3: CREATE + PUBLISH

```java
public Mono<T> create(String field) {
    return repository.save(new T(field))
        .flatMap(saved → publisher.publish(...).thenReturn(saved));
}
```

### Template 4: UPDATE + PUBLISH

```java
public Mono<T> update(Long id, String newValue) {
    return repository.findById(id)
        .switchIfEmpty(Mono.error(...))
        .map(t → { t.setField(newValue); return t; })
        .flatMap(repository::save)
        .flatMap(saved → publisher.publish(...).thenReturn(saved));
}
```

### Template 5: DELETE + PUBLISH

```java
public Mono<Void> delete(Long id) {
    return repository.findById(id)
        .switchIfEmpty(Mono.error(...))
        .flatMap(t → repository.deleteById(id)
            .then(publisher.publish(...))
        );
}
```

---

## Una línea importante

```
Si dudas entre map() y flatMap() → SIEMPRE usa flatMap()
```

¿Por qué? flatMap funciona en ambos casos:
- `flatMap(v → map1(v))` = OK, funciona como map
- `flatMap(v → monoAsync(v))` = OK, espera el async

Mientras que map() solo funciona en 1 caso y rompe en el otro.

---

## Checklist antes de hacer commit

```
☑ ¿Todas las funciones retornan Mono/Flux?
☑ ¿Usé flatMap para operaciones async?
☑ ¿Usé map para transformaciones simples?
☑ ¿Validé con switchIfEmpty() después de find?
☑ ¿No tengo .block() en ningún lado?
☑ ¿Los controllers retornan Mono/Flux?
☑ ¿Los DTOs se convierten con map()?
☑ ¿Publico eventos con flatMap?
```

---

## Próximo paso

**LISTO para aprender más:**

1. Si quieres visuals → `RESUMEN_VISUAL.md`
2. Si quieres teoría → `GUIA_PROGRAMACION_REACTIVA.md`
3. Si quieres ejemplos → `EJEMPLOS_PRACTICOS.md`
4. Si quieres referencia → `CHEAT_SHEET_REACTIVO.md`
5. Si quieres análisis → `ANALISIS_CODIGO_COMPLETO.md`

---

## Eso es todo

Con esto tienes:
- ✅ Los 2 tipos (Mono, Flux)
- ✅ Los 2 operadores (map, flatMap)
- ✅ Los 3 patrones (Create, Update, Validate)
- ✅ Los 3 errores (map-async, block, sin-switchIfEmpty)
- ✅ Los 5 templates (listos para copiar)

**Eso es el 80% de todo.** 🎉

¡Ahora abre `RESUMEN_VISUAL.md` para profundizar! 🚀
