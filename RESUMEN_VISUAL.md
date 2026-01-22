# 🎓 RESUMEN VISUAL - PROGRAMACIÓN REACTIVA

## ¿QUÉ ES LA PROGRAMACIÓN REACTIVA?

### En palabras simples:

```
ANTES (Tradicional):
────────────────────
Cliente HTTP
    ↓ Solicita datos
Servidor bloquea hilo
    ↓ Espera BD (500ms)
    ↓ Espera Kafka (100ms)
Servidor retorna (600ms después)
Cliente recibe respuesta

PROBLEMA: 1000 clientes = 1000 hilos esperando (mucha RAM)
```

```
AHORA (Reactivo):
────────────────
Cliente HTTP
    ↓ Solicita datos
Servidor recibe, hilo retorna al pool (INMEDIATO)
    ↓ BD responde en background
    ↓ Kafka responde en background
Resultado enviado al cliente (600ms después)
Cliente recibe respuesta

VENTAJA: 1000 clientes = 10 hilos reutilizados (poca RAM)
```

---

## CONCEPTOS CLAVE

### 1. MONO - Un resultado máximo

```
┌─────────────────────┐
│  Mono<Franchise>    │
├─────────────────────┤
│ • 0 elementos       │ ← empty (no encontrado)
│ • 1 elemento        │ ← OK (encontró)
│ • Error             │ ← Exception
└─────────────────────┘

EJEMPLOS EN TU PROYECTO:
• findById(1) → Mono<Franchise>
• create(...) → Mono<Franchise>
• update(...) → Mono<Franchise>
```

### 2. FLUX - Múltiples resultados

```
┌─────────────────────┐
│  Flux<Franchise>    │
├─────────────────────┤
│ • 0 elementos       │ ← empty list
│ • 1 elemento        │ ← 1 item
│ • N elementos       │ ← múltiples items
│ • Infinito          │ ← stream continuo
│ • Error             │ ← Exception
└─────────────────────┘

EJEMPLOS EN TU PROYECTO:
• findAll() → Flux<Franchise>
• listByFranchise(...) → Flux<Branch>
```

---

## OPERADORES PRINCIPALES

### MAP - Transformar (SÍNCRONO)

```
Entrada: Mono<Franchise{id:1, name:"A"}>
                    ↓ map(f → f.getName())
Salida:  Mono<String{"A"}>

Entrada: Flux<Branch{id:1,2,3}>
                    ↓ map(b → b.getId())
Salida:  Flux<Long{1,2,3}>

CUÁNDO USARLO:
✅ Cambiar tipo de dato
✅ Transformación simple (sin BD, sin I/O)
✅ No esperas a otra operación
```

### FLATMAP - Encadenar (ASINCRÓNICO)

```
Entrada: Mono<Franchise>
                    ↓ flatMap(f → BD.save(newData))
                    ↓           (operación async)
Salida:  Mono<Franchise> (con nuevos datos)

FLUJO INTERNO:
1. Recibe Franchise
2. Espera a que BD responda (sin bloquear)
3. Cuando responde, continúa con nuevo Franchise

CUÁNDO USARLO:
✅ Operaciones que dependan entre sí
✅ Operaciones async (BD, HTTP, Kafka)
✅ No seguro cuál usar → usa flatMap
```

### THEN - Ignorar resultado (ASINCRÓNICO)

```
Entrada: Mono<Franchise>
                    ↓ then(BD.save(otherData))
                    ↓ (Franchise se descarta)
Salida:  Mono<OtherData>

EJEMPLO REAL:
repository.findById(1)     // Validar que existe
    .then(save(newData))   // Pero no usar los datos obtenidos

CUÁNDO USARLO:
✅ Solo validar existencia
✅ No necesitas el valor anterior
```

### SWITCHIFEMPTY - Validación (ERROR SI VACÍO)

```
Entrada: Mono<Franchise> (si existe)
                    ↓ switchIfEmpty(error)
Salida:  Mono<Franchise> (sigue igual)

Entrada: Mono.empty() (no existe)
                    ↓ switchIfEmpty(error)
Salida:  Mono<NotFoundException>

CUÁNDO USARLO:
✅ Siempre que hagas findById
✅ Para lanzar error en lugar de returning empty
```

### FILTER - Condicional

```
Entrada: Flux<Product>{stock:100, stock:5, stock:50}
                    ↓ filter(p → p.stock > 10)
Salida:  Flux<Product>{stock:100, stock:50}

CUÁNDO USARLO:
✅ Descartar elementos que no cumplen
✅ Solo pasar ciertos items
```

---

## PATRONES DE COMBINACIÓN

### Patrón 1: CREAR + PUBLICAR

```
.save(entidad)
    └─ Retorna: Mono<Entity>
    
    .flatMap(saved → publish(...))
    └─ publish() es async
    └─ Retorna: Mono<Void>
    
    .thenReturn(saved)
    └─ Completa, retorna Entity original
    
Resultado: Mono<Entity>
```

**Tu código:**
```java
repository.save(franchise)
    .flatMap(saved → publish(EventType.CREATED, saved).thenReturn(saved))
```

---

### Patrón 2: VALIDAR + ACTUALIZAR

```
.findById(id)
    └─ Buscar (Mono<Entity>)
    
    .switchIfEmpty(error)
    └─ Si no existe → Lanzar error
    
    .map(e → { e.setField(value); return e; })
    └─ Cambiar datos (síncrono)
    
    .flatMap(repository::save)
    └─ Guardar (async)
    
    .flatMap(saved → publish(...).thenReturn(saved))
    └─ Publicar evento
    
Resultado: Mono<Entity>
```

**Tu código:**
```java
repository.findById(id)
    .switchIfEmpty(Mono.error(new NotFoundException(...)))
    .map(f → { f.setName(name); return f; })
    .flatMap(repository::save)
    .flatMap(saved → publish(EventType.UPDATED, saved).thenReturn(saved))
```

---

### Patrón 3: VALIDAR RELACIONAL

```
foreignRepository.findById(foreignId)
    └─ ¿Existe la entidad relacionada?
    
    .switchIfEmpty(error)
    └─ Si no existe → Error
    
    .then(mainRepository.save(...))
    └─ No necesito los datos de la relación
    └─ Solo validar que existe
    
Resultado: Mono<Entity>
```

**Tu código:**
```java
franchiseRepository.findById(franchiseId)
    .switchIfEmpty(Mono.error(...))
    .then(branchRepository.save(...))
```

---

## FLUJOS VISUALES

### Crear Franquicia

```
┌─────────────────────────────────────────┐
│ HTTP POST /api/franchises               │
│ {"name": "Mi Franquicia"}               │
└─────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────┐
│ FranchiseController.create()            │
│ franchiseUseCase.create("Mi Franquicia")│
└─────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────┐
│ FranchiseUseCase.create()               │
│ repository.save(new Franchise(...))     │
└─────────────────────────────────────────┘
         ↓ [BD responde 500ms después]
┌─────────────────────────────────────────┐
│ Franchise guardada:                     │
│ {id: 1, name: "Mi Franquicia"}          │
└─────────────────────────────────────────┘
         ↓ .flatMap(saved → publish(...))
┌─────────────────────────────────────────┐
│ Evento publicado en Kafka               │
│ Topic: franchise.events                 │
│ Message: FranchiseEvent{...}            │
└─────────────────────────────────────────┘
         ↓ [Kafka responde 100ms después]
┌─────────────────────────────────────────┐
│ .thenReturn(saved)                      │
│ Retorna Franchise guardada              │
└─────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────┐
│ HTTP 200 OK                             │
│ {"id": 1, "name": "Mi Franquicia"}      │
└─────────────────────────────────────────┘
```

---

### Listar Franquicias

```
┌─────────────────────────────────────┐
│ HTTP GET /api/franchises            │
└─────────────────────────────────────┘
         ↓
┌─────────────────────────────────────┐
│ FranchiseController.list()          │
│ franchiseUseCase.list()             │
└─────────────────────────────────────┘
         ↓
┌─────────────────────────────────────┐
│ repository.findAll()                │
│ [BD trae todos los registros]       │
└─────────────────────────────────────┘
         ↓ Flux<Franchise> empieza a fluir
┌─────────────────────────────────────┐
│ .map(ApiMapper::toResponse)         │
│ Convertir cada Franchise a DTO      │
└─────────────────────────────────────┘
         ↓ Flux<FranchiseResponse> fluye
┌─────────────────────────────────────┐
│ HTTP 200 OK                         │
│ [                                   │
│   {"id": 1, "name": "F1"},          │
│   {"id": 2, "name": "F2"},          │
│   ...                               │
│ ]                                   │
└─────────────────────────────────────┘
```

---

## ERRORES A EVITAR

### ❌ Error 1: map() con async
```
❌ INCORRECTO:
mono.map(v → repository.save(v))
     └─ Retorna: Mono<Mono<...>> ¡INCORRECTO!

✅ CORRECTO:
mono.flatMap(v → repository.save(v))
     └─ Retorna: Mono<...> ✓
```

### ❌ Error 2: Bloquear con block()
```
❌ NUNCA en controllers:
public FranchiseResponse create(...) {
    return useCase.create(...).block();  // ¡NUNCA!
}

✅ SIEMPRE retornar Mono/Flux:
public Mono<FranchiseResponse> create(...) {
    return useCase.create(...);  // ✓
}
```

### ❌ Error 3: Olvidar switchIfEmpty()
```
❌ INCORRECTO (sin validación):
repository.findById(id)  // Si no existe: Mono.empty()
    .map(...)

✅ CORRECTO (con validación):
repository.findById(id)
    .switchIfEmpty(Mono.error(new NotFoundException(...)))
    .map(...)
```

---

## DECISIONES RÁPIDAS

### ¿Mono o Flux?
```
¿Esperas 1 elemento máximo?    → Mono<T>
¿Esperas varios elementos?     → Flux<T>
¿Esperas lista (1 resultado)?  → Mono<List<T>>
```

### ¿map() o flatMap()?
```
¿Operación síncrona?           → map()
¿Operación asincrónica?        → flatMap()
¿No estoy seguro?              → flatMap() (más seguro)
```

### ¿then() o flatMap()?
```
¿Necesito el valor anterior?   → flatMap()
¿Solo valido, no lo uso?       → then()
```

---

## TIMELINE DE EJECUCIÓN

### Secuencial (más lento)
```
Mono A: ████████ 500ms
Mono B:         ████ 100ms
─────────────────────────
Total: ─────────────────── 600ms
```

### Paralelo (más rápido)
```
Mono A: ████████ 500ms
Mono B: ████ 100ms (paralelo)
─────────────────────────
Total: ─────────────────── 500ms (máximo de ambos)
```

**En tu proyecto:**
- Normalmente encadenas operaciones (secuencial): save → publish
- Pero los **hilos no se bloquean** (devueltos al pool)

---

## CHECKLISTA PARA TU CÓDIGO

```
□ ¿Usé map() para transformaciones síncronas?
□ ¿Usé flatMap() para operaciones async?
□ ¿Usé switchIfEmpty() en todas mis validaciones?
□ ¿Usé then() cuando no necesito el valor anterior?
□ ¿Retorno Mono/Flux, nunca block()?
□ ¿Los controllers retornan Publishers?
□ ¿Los useCase retornan Mono/Flux?
□ ¿Los repositories retornan Mono/Flux?
□ ¿Las consultas custom retornan Mono/Flux?
□ ¿Publico eventos en flatMap (async)?
```

---

## TU PROYECTO ESTÁ:

✅ **EXCELENTE** - Implementación correcta de programación reactiva
✅ **ESCALABLE** - Puede manejar muchos requests con pocos hilos
✅ **MODULAR** - Arquitectura hexagonal limpia
✅ **REACTIVA** - BD sin bloqueo (R2DBC), eventos async (Kafka)
✅ **EDUCATIVA** - Perfecto para aprender

---

## PRÓXIMOS PASOS

1. **Lee cada guía en orden:**
   - GUIA_PROGRAMACION_REACTIVA.md (teórico)
   - EJEMPLOS_PRACTICOS.md (casos reales)
   - CHEAT_SHEET_REACTIVO.md (referencia rápida)
   - ANALISIS_CODIGO_COMPLETO.md (detallado)

2. **Practica implementando:**
   - Nuevo endpoint GET con filtros
   - Bulk update de múltiples productos
   - Endpoint que combina múltiples Monos
   - Error handling con retry

3. **Experimenta:**
   - Agrega timeouts a las operaciones
   - Agrega logging con doOnNext()
   - Agrega cache con cache()
   - Agrega métricas

---

¡Ya tienes todo lo que necesitas para dominar la programación reactiva! 🚀

**Imprime este resumen y tenlo a la mano mientras estudias.** 📋
