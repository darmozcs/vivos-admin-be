# vivos-api (vivos-be)

Backend de **tienda-admin**: tiendas, clientas, vivos y ventas. Es multi-tienda y usa
**darmoz-auth** para el login.

Usa la misma base que `laryon-be`:

- Spring Boot 4.0.7 y Java 21.
- Paquetes por feature, records como DTO y errores `ProblemDetail`.
- Flyway sobre la base `nexora`. Las tablas llevan el prefijo `viv_` y el historial de Flyway es `viv_flyway_schema_history`.

## Modelo

- **Aplicación Vivos en darmoz-auth.** Tiene el UUID fijo `22222222-2222-2222-2222-222222222222` y estos roles:
  - `SUPER_ADMIN`: gestiona las tiendas desde vivos-be.
  - `APP_ADMIN`: gestiona los usuarios de la app Vivos en darmoz-auth.
  - `ADMIN`: es el admin de una tienda.
- **Tiendas.** Viven en vivos-be, en `viv_store` y `viv_store_user`. La segunda vincula `auth_user_id` (el `sub` del JWT) con `store_id`.
- **Tienda del usuario.** Se resuelve siempre desde el `sub` del token y nunca viene del cliente. Si el usuario no tiene tienda, o la tienda está desactivada, la respuesta es **403**.
- **Alta de tiendas.** vivos-be reenvía a darmoz-auth el `Authorization` del Super Admin, junto con `API_ID: <VIVOS_APPLICATION_ID>`. vivos-be no guarda ninguna credencial propia.

## Variables de entorno

| Variable | Default | Descripción |
|---|---|---|
| `APP_PORT` | `8082` | Puerto HTTP |
| `VIVOS_API_PATH_PREFIX` | `/vivos-api` | Context path |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `5432` / `nexora` | Postgres |
| `DB_USER` / `DB_PASSWORD` | `nexora` / *(vacío)* | Credenciales de Postgres |
| `JWT_PUBLIC_KEY_LOCATION` | `classpath:public_key.pem` | Clave pública RSA de darmoz-auth (la misma que usa laryon) |
| `JWT_ISSUER` | `darmoz-auth` | Issuer esperado en el token |
| `VIVOS_APPLICATION_ID` | `22222222-2222-2222-2222-222222222222` | Solo se aceptan tokens con este `applicationId`. Los demás reciben **401** |
| `DARMOZ_AUTH_URL` | `http://localhost:8080/auth` | URL base de darmoz-auth, con su context path |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Orígenes permitidos, separados por coma |
| `VIVOS_TIMEZONE` | `America/Montevideo` | Zona horaria de la tienda. Los filtros `from`/`to` de `GET /orders` y `GET /lives` usan días de esta zona |

## Correr en local

Servicios y puertos:

| Servicio | Puerto | Notas |
|---|---|---|
| darmoz-auth | 8080 | Context path `/auth`. Levantarlo con `CORS_ALLOWED_ORIGINS=http://localhost:5173` |
| darmoz-mail | 8081 | darmoz-auth lo usa para enviar el código de verificación al admin de cada tienda |
| vivos-be | 8082 | Context path `/vivos-api` |
| tienda-admin | 5173 | Front con Vite |

Pasos:

```bash
# Postgres "nexora" corriendo en localhost:5432
DB_PASSWORD=<password> ./mvnw spring-boot:run
# Healthcheck:
curl http://localhost:8082/vivos-api/actuator/health
```

Tests (requiere Docker, porque usa Testcontainers con `postgres:16-alpine`):

```bash
./mvnw -B test
```

## Setup inicial: Super Admin de Vivos (una sola vez)

Lo hace el usuario `SUPER` global de darmoz-auth (el de bootstrap, de la aplicación de sistema
`11111111-1111-1111-1111-111111111111`). Se puede hacer desde darmoz-dashboard o con curl.

1. Obtener un access token del `SUPER`:

```bash
curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -H "API_ID: 11111111-1111-1111-1111-111111111111" \
  -d '{"email":"<super-email>","password":"<super-password>"}'
# -> copiar "accessToken"
```

2. Crear el Super Admin de Vivos con los roles `SUPER_ADMIN` y `APP_ADMIN`:

```bash
curl -s -X POST http://localhost:8080/auth/admin/api/users \
  -H "Content-Type: application/json" \
  -H "API_ID: 11111111-1111-1111-1111-111111111111" \
  -H "Authorization: Bearer <accessToken-del-SUPER>" \
  -d '{
        "email": "superadmin@vivos.com",
        "password": "<password-segura>",
        "applicationId": "22222222-2222-2222-2222-222222222222",
        "roles": ["SUPER_ADMIN", "APP_ADMIN"]
      }'
```

Después, ese usuario entra en tienda-admin (login con `API_ID` = Vivos) y da de alta las tiendas.
Cada alta crea en darmoz-auth un usuario `ADMIN` sin verificar, y darmoz-auth le envía el código
de verificación por darmoz-mail.

## Endpoints

Todos van bajo `/vivos-api`. Los errores son `ProblemDetail` (`{type, title, status, detail, ...}`), y el
front muestra `detail`. En los errores de validación (400), `detail` es el primer mensaje y además
hay una propiedad por cada campo inválido.

### Super Admin (`SUPER_ADMIN`)

`StoreAdminResponse = {id, name, active, adminUserId, adminEmail, adminEmailVerified, createdAt}`

| Método y ruta | Body | Respuesta |
|---|---|---|
| `GET /admin/stores` | — | `200 [StoreAdminResponse]`, ordenada por `createdAt` desc. `adminEmailVerified` sale de **una** llamada a `GET /apps/api/users`. Si esa llamada falla, se responde con el error (502 o el status de darmoz-auth) en vez de una lista incompleta |
| `POST /admin/stores` | `{name (1-60), adminEmail (email, ≤150), adminPassword (8-100)}` | `201 StoreAdminResponse`. Crea el usuario en darmoz-auth con `roles:["ADMIN"]` y después guarda la tienda. Si la BD falla, borra el usuario en darmoz-auth y responde con el error |
| `PATCH /admin/stores/{id}` | `{name?, active?}` | `200 StoreAdminResponse`. Si cambia `active`, primero llama a `PATCH /apps/api/users/{id} {enabled}`. `adminEmailVerified` es `null` cuando no hubo llamada a darmoz-auth |
| `PUT /admin/stores/{id}/admin-password` | `{password (8-100)}` | `204` |
| `POST /admin/stores/{id}/admin-verification-email` | — | `204`. Si darmoz-auth responde 429 o 409, se propaga ese status |

Los errores de darmoz-auth (`{timestamp,status,error,message}`) se devuelven con el mismo status,
con `detail = message`. Si no se puede contactar a darmoz-auth, la respuesta es **502**.

### Tienda (`ADMIN`, siempre limitado a la tienda del token)

| Método y ruta | Body / query | Respuesta |
|---|---|---|
| `GET /settings` | — | `{storeName}` |
| `PUT /settings` | `{storeName}` (obligatorio, ≤60, se recorta) | `{storeName}` |
| `GET /clients` | Query opcional, todo con AND: `search` (busca en "fullName account phone", sin distinguir mayúsculas), `fullName`, `account`, `phone` (contiene, sin distinguir mayúsculas, se recortan y los vacíos se ignoran; en `account` la `@` inicial es opcional), `department` y `deliveryType` (exactos). Un enum inválido responde `400` | `[Client]` en orden de alta |
| `POST /clients` | `{fullName, account?, phone?, address?}` | `201 Client`. A `account` se le antepone `@` |
| `PATCH /clients/{id}` | `{fullName, account, phone, address}`. Reemplaza todo; los campos ausentes quedan `""` | `Client` |
| `DELETE /clients/{id}` | — | `204`. Responde `409` si la clienta tiene ventas con items o compras/ventas. Sus ventas vacías se borran con ella |
| `GET /sellers` | Los mismos filtros que `GET /clients` | `[Seller]` en orden de alta. `Seller` tiene los mismos campos que `Client` |
| `POST /sellers` | Igual que `POST /clients` | `201 Seller` |
| `PATCH /sellers/{id}` | Igual que `PATCH /clients/{id}` | `Seller` |
| `DELETE /sellers/{id}` | — | `204`. Responde `409` si el vendedor tiene compras/ventas |
| `GET /trades` | Query opcional, todo con AND: `from` y `to` (`yyyy-MM-dd`, inclusive, sobre `createdAt` en la zona `VIVOS_TIMEZONE`; `from` > `to` responde `400`), `search` (la nota contiene, sin distinguir mayúsculas), `type` (`SALE`, `PURCHASE`), `status` (`PUBLISHED`, `SOLD`, `PENDING`, `PURCHASED`), `sellerId`, `clientId`. Un enum o UUID inválido responde `400` | `[Trade]` del más nuevo al más viejo: `{id, type, status, quantity, price, note, sellerId, sellerName, clientId, clientName, createdAt, updatedAt}` |
| `GET /trades/summary` | Los mismos filtros que `GET /trades` | `{salesTotal, purchasesTotal, balance, salesCount, purchasesCount, salesQuantity, purchasesQuantity}` sobre esas compras/ventas, de cualquier estado. `balance` = ventas − compras |
| `POST /trades` | `{type, status, quantity, price, note?, sellerId?, clientId?}`. Una venta (`SALE`) lleva `clientId` y estado `PUBLISHED` o `SOLD`; una compra (`PURCHASE`) lleva `sellerId` y estado `PENDING` o `PURCHASED`. La contraparte que no corresponde se ignora | `201 Trade`. Responde `400` si el estado no corresponde al tipo o falta la contraparte o no es de la tienda |
| `PATCH /trades/{id}` | Igual que `POST /trades`. Reemplaza todo; el `type` no se puede cambiar (`400`) | `Trade` |
| `DELETE /trades/{id}` | — | `204` |
| `GET /balance` | Query opcional, todo con AND: `from` y `to` (`yyyy-MM-dd`, inclusive, sobre `createdAt` en la zona `VIVOS_TIMEZONE`; `from` > `to` responde `400`) y `type` (`PURCHASE`, `SALE`, `LIVE`) | `{entries: [{id, type, quantity, amount, createdAt}], summary: {salesTotal, purchasesTotal, balance, soldQuantity, boughtQuantity, netQuantity}}`. Une los vivos finalizados (`amount` = total recaudado, `quantity` = prendas), las ventas `SOLD` y las compras `PURCHASED`, del más nuevo al más viejo. Vivos y ventas suman, compras restan: `balance` = ventas − compras y `netQuantity` = prendas compradas − vendidas |
| `GET /lives` | Query opcional, todo con AND: `from` y `to` (`yyyy-MM-dd`, inclusive, sobre `createdAt` en la zona `VIVOS_TIMEZONE`), `title` (contiene, sin distinguir mayúsculas, se recorta), `minDurationMinutes`/`maxDurationMinutes` (enteros ≥ 0 sobre la duración efectiva en minutos enteros: `floor((accumulatedMs + (RUNNING ? ahora - runningSince : 0)) / 60000)`), `minBuyers`/`maxBuyers` y `minUnits`/`maxUnits` (enteros ≥ 0), `minTotal`/`maxTotal` (decimales ≥ 0) sobre los totales de `LiveSummary`. Todos los rangos son inclusive. Un valor negativo o no numérico, una fecha inválida, `from` > `to` o un mínimo mayor que su máximo responden `400` | `[LiveSummary]` que cumplen los filtros, del más nuevo al más viejo |
| `GET /lives/summary` | Los mismos filtros que `GET /lives` | `{lives, buyers, units, totalAmount}` sobre esos mismos vivos: cantidad de vivos y suma de los totales de cada uno (una clienta que compró en tres vivos cuenta tres veces en `buyers`). Sin resultados, todo en `0` |
| `GET /lives/current` | — | `Live`: el último creado. Responde `404` si la tienda no tiene vivos |
| `GET /lives/{id}` | — | `LiveSummary` |
| `POST /lives` | `{title}` | `201 Live` (`NOT_STARTED`, plataforma "Instagram Live") |
| `POST /lives/{id}/{start\|pause\|resume\|end}` | — | `Live`. Responde `409` si la acción no está permitida en el estado actual y `404` si la acción no existe |
| `GET /orders` | Query opcional, todo con AND: `from` y `to` (`yyyy-MM-dd`, inclusive, sobre `createdAt` en la zona `VIVOS_TIMEZONE`), `liveTitle` (el título del vivo contiene, sin distinguir mayúsculas), `clientId` (UUID), `status`, `packageStatus`. Fecha, UUID o enum inválidos, o `from` > `to`, responden `400` | `[Order]`: las ventas con items que cumplen los filtros, por `number` desc |
| `GET /orders/summary` | Los mismos filtros que `GET /orders` | `{orders, units, totalAmount}` sobre esas mismas ventas, de cualquier estado (para excluir canceladas, filtrar por `status`): cantidad de ventas, suma de cantidades y suma de precio × cantidad |
| `PATCH /orders/{id}` | `{status?, packageStatus?, number?}`. `number` (entero > 0) cambia el número de paquete | `Order`. Responde `409` si la venta no tiene items o si otra venta del vivo ya usa ese `number`. El contador del vivo sigue después del número puesto a mano |
| `GET /lives/{liveId}/orders` | — | `[Order]`: todas las ventas del vivo (incluidas las vacías), por `number` |
| `PUT /lives/{liveId}/orders/{clientId}` | `{status, packageStatus?, items:[{code?,name?,color?,price,quantity?}]}`. De cada item solo `price` es obligatorio: los textos faltantes se guardan vacíos y `quantity` vale 1 por defecto | `Order`. Hace upsert por vivo + clienta. `number` (número de paquete) se asigna con la primera compra: es el siguiente del contador del vivo, que nunca retrocede. Si la venta queda sin items, `number` vuelve a `null` y ese número no se reusa; si la clienta vuelve a comprar recibe uno nuevo. Responde `409` si el vivo no comenzó |

Los totales de `LiveSummary` (`buyers`, `units` y `totalAmount`) se calculan como en el mock: solo
cuentan las ventas con items que no están canceladas.
