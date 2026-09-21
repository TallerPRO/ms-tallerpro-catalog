# ms-tallerpro-catalog

MS-02 de **TallerPro**: catálogo de servicios (tarifas comunes a la red), repuestos con **stock por taller**
y bahías con su ciclo de ocupación. Spring Boot 4.1 · Java 25 · PostgreSQL · Spring Security (JWT Azure AD).

Capas: `model` (JPA) → `repository` → `service` → `controller` (endpoints) · `dto` · `exception` · `config`.

## Endpoints

Todos exigen `Authorization: Bearer <jwt>`; entre paréntesis el rol mínimo para escribir. Lectura: cualquier usuario autenticado.

### Servicios — `/api/v1/servicios`

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| GET | `/api/v1/servicios?busqueda=&categoria=&soloActivos=true&page=0&size=20` | — | Página de servicios (`Page<Servicio>`) |
| GET | `/api/v1/servicios/{id}` | — | Detalle |
| POST | `/api/v1/servicios` | Admin | Crea; genera código `SRV-0001` |
| PUT | `/api/v1/servicios/{id}` | Admin | Actualiza nombre, categoría, tarifa, duración |
| DELETE | `/api/v1/servicios/{id}` | Admin | Baja lógica (`activo=false`) |

Categorías: `MANTENCION, FRENOS, SUSPENSION, MOTOR, ELECTRICO, CARROCERIA, DIAGNOSTICO`.

### Repuestos — `/api/v1/talleres/{tallerId}/repuestos`

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| GET | `.../repuestos?busqueda=&bajoStock=false&page=&size=` | — | Página de repuestos del taller |
| GET | `.../repuestos/{id}` | — | Detalle (incluye `bajoStock`) |
| POST | `.../repuestos` | Admin | Crea (código único por taller) |
| PUT | `.../repuestos/{id}` | Admin | Actualiza precio/stock/umbral |
| DELETE | `.../repuestos/{id}` | Admin | Baja lógica |
| POST | `.../repuestos/{id}/stock/decremento` | Admin, JefeTaller | **RF-08**, lo llama `ms-tallerpro-jobs` al diagnosticar. Body `{cantidad, eventId?, motivo?}`, header opcional `X-Event-Id`. Idempotente por `eventId`. Stock insuficiente → `409` |
| POST | `.../repuestos/{id}/stock/incremento` | Admin | Reposición, idempotente |
| GET | `.../repuestos/{id}/stock/movimientos` | Admin, JefeTaller, Auditor | Kardex |

### Bahías — `/api/v1/talleres/{tallerId}/bahias`

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| GET | `.../bahias?estado=` | — | Lista del taller (`DISPONIBLE`, `RESERVADA`, `OCUPADA`) |
| GET | `.../bahias/resumen` | — | Total, disponibles, reservadas, ocupadas, tasa, próxima liberación |
| GET | `.../bahias/{id}` | — | Detalle |
| GET | `.../bahias/{id}/disponibilidad` | — | **RF-06**, lo llama `ms-tallerpro-jobs`: `{bahiaId, tallerId, disponible}` |
| POST | `.../bahias` | Admin | Crea |
| PUT | `.../bahias/{id}` | Admin | Actualiza |
| DELETE | `.../bahias/{id}` | Admin | Baja lógica (solo si está DISPONIBLE) |
| POST | `.../bahias/{id}/reserva` | Admin, JefeTaller | `{ordenId, inicioProgramado?, terminoProgramado?}` → RESERVADA |
| POST | `.../bahias/{id}/ocupacion` | Admin, JefeTaller | RESERVADA → OCUPADA (ingreso del vehículo) |
| POST | `.../bahias/{id}/liberacion` | Admin, JefeTaller | → DISPONIBLE (cancelar reserva o terminar) |

Errores: `400` validación · `401` sin token · `403` rol insuficiente · `404` no existe (o pertenece a otro taller) · `409` conflicto de negocio.
Formato: `{timestamp, status, error, message, path}` (igual que `ms-tallerpro-jobs`).

Swagger UI: `http://localhost:8082/swagger-ui.html`

## Configuración

| Variable | Default | Descripción |
|---|---|---|
| `SERVER_PORT` | `8082` | |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | `localhost`, `5432`, `tallerpro_catalog`, `tallerpro`, `tallerpro` | PostgreSQL |
| `TALLERPRO_JWT_ENABLED` | `true` | `false` → sin tenant; se inyecta un usuario ficticio con `TALLERPRO_DEV_ROLES` |
| `TALLERPRO_DEV_ROLES` | `Admin,JefeTaller` | Roles del usuario ficticio (solo con JWT deshabilitado) |
| `AZURE_TENANT_ID`, `AZURE_API_CLIENT_ID` | — | Issuer y audience del token |

## Ejecución

```bash
# Local con PostgreSQL
docker run -d --name tp-pg-catalog -e POSTGRES_USER=tallerpro -e POSTGRES_PASSWORD=tallerpro \
  -e POSTGRES_DB=tallerpro_catalog -p 5432:5432 postgres:16-alpine
TALLERPRO_JWT_ENABLED=false ./mvnw spring-boot:run

# Local sin PostgreSQL (H2 en memoria, classpath de test)
./mvnw spring-boot:test-run -Dspring-boot.run.profiles=test \
  -Dspring-boot.run.arguments=--tallerpro.security.jwt-enabled=false

# Tests (H2)
./mvnw test
```

## Tests

`CatalogControllerTest` (MockMvc + `spring-security-test`): 401 sin token, 403 por rol, CRUD de servicios,
flujo completo de bahía (disponibilidad → reserva → ocupación → liberación), aislamiento por taller,
decremento de stock idempotente y `409` por stock insuficiente o código duplicado.
