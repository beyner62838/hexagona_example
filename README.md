# Franquicias Prueba — Hexagonal + WebFlux + Kafka + Gateway

Este repo está armado con la misma idea de carpetas de las imágenes: `application`, `domain`, `infrastructure` (con `driven-adapters` y `entry-points`).

## Qué incluye

- Backend **reactivo** (Spring WebFlux) con **Kafka** (eventos JSON).
- Persistencia **reactiva** con **R2DBC + PostgreSQL**.
- **Spring Cloud Gateway** (WebFlux) para enrutar `/api/**` hacia el backend.
- **Kafka UI** (ProvectusLabs) para ver tópicos y mensajes.
- `compose.yml` / `docker-compose.yml` listos para levantar todo.

## Levantar con Docker

```bash
docker compose -f compose.yml up --build
```

Servicios:

- Gateway: http://localhost:8088
- Backend (directo): http://localhost:8082
- Kafka UI: http://localhost:8085
- PostgreSQL: localhost:5433 (db: `franchise_db`, user/pass: `postgres`)

## Endpoints principales (pasan por Gateway)

> Base URL: `http://localhost:8088`

### Franquicias

- **Crear franquicia**
  - `POST /api/franchises`
  - Body:
    ```json
    { "name": "Franquicia 1" }
    ```

- **Listar franquicias**
  - `GET /api/franchises`

- **Actualizar nombre**
  - `PUT /api/franchises/{id}/name`
  - Body:
    ```json
    { "name": "Nuevo nombre" }
    ```

- **Crear sucursal**
  - `POST /api/franchises/{id}/branches`
  - Body:
    ```json
    { "name": "Sucursal 1" }
    ```

- **Top products por sucursal (máximo stock)**
  - `GET /api/franchises/{id}/top-products`

### Sucursales

- **Actualizar nombre**
  - `PUT /api/branches/{id}/name`
  - Body:
    ```json
    { "name": "Sucursal Renombrada" }
    ```

- **Crear producto**
  - `POST /api/branches/{id}/products`
  - Body:
    ```json
    { "name": "Producto A", "stock": 30 }
    ```

- **Listar productos**
  - `GET /api/branches/{id}/products`

### Productos

- **Actualizar nombre**
  - `PUT /api/products/{id}/name`
  - Body:
    ```json
    { "name": "Producto Renombrado" }
    ```

- **Actualizar stock**
  - `PUT /api/products/{id}/stock`
  - Body:
    ```json
    { "stock": 99 }
    ```

- **Eliminar**
  - `DELETE /api/products/{id}`

## Eventos Kafka

Tópicos:

- `franchise.events`
- `branch.events`
- `product.events`

Los eventos se publican en JSON (sin type headers). En **Kafka UI** puedes entrar al tópico y ver los mensajes cuando hagas create/update/delete.

## Estructura (rápida)

- `domain`: modelos, puertos y casos de uso (sin Spring).
- `infrastructure/driven-adapters`: Postgres (R2DBC) + Kafka publisher.
- `infrastructure/entry-points/rest-api`: controladores WebFlux (API).
- `infrastructure/entry-points/api-gateway`: Spring Cloud Gateway.
- `application`: arranque del backend + configuración de beans.

---
Si quieres que el gateway también agregue auth (JWT) o rate limit, se le mete sin tocar el dominio.
