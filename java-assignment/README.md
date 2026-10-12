# Java Code Assignment

A Quarkus 3 warehouse management API with JWT authentication, paginated responses, rate limiting, CSV export, database migrations, and Docker support.

## Requirements

- JDK 17+ (Java 23 is supported — `.mvn/jvm.config` sets Byte Buddy experimental mode automatically)
- Maven wrapper (`./mvnw`) — no local Maven install required
- Docker + Docker Compose — for running the full stack

---

## Quick start with Docker Compose

```sh
# From the project root (one level above java-assignment/)
docker compose up --build
```

The app builds from source, runs database migrations, seeds data, and starts at **http://localhost:8082**.

> PostgreSQL is exposed on host port **5433** to avoid conflicts with local Postgres.

---

## Running locally (without Docker)

```sh
# Tests use H2 in-memory — no Docker needed
./mvnw clean test

# Dev mode — starts a PostgreSQL container automatically via Quarkus Dev Services (requires Docker)
./mvnw quarkus:dev
```

---

## Authentication

All resource endpoints (`/warehouse`, `/store`, `/product`) require a valid **Bearer JWT**.

### 1. Obtain a token

```sh
curl -s -X POST http://localhost:8082/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | python3 -m json.tool
```

Response:
```json
{
  "token": "<JWT>",
  "type": "Bearer",
  "expiresIn": 3600,
  "username": "admin",
  "role": "admin"
}
```

### 2. Use the token

```sh
TOKEN=$(curl -s -X POST http://localhost:8082/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

curl -s http://localhost:8082/warehouse \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

### Test credentials

| Username | Password   | Role    | Permissions             |
|----------|------------|---------|-------------------------|
| `admin`  | `admin123` | `admin` | Full CRUD on all resources |
| `user`   | `user123`  | `user`  | Read-only (GET + export)   |

Tokens expire after **1 hour**.

---

## Swagger UI

Navigate to **http://localhost:8082/swagger-ui/**

1. Click **Authorize** (lock icon)
2. Enter `Bearer <token>` in the `bearerAuth` field
3. All endpoints are now available to try

OpenAPI spec: `GET http://localhost:8082/openapi`

---

## API endpoints

### Auth
| Method | Path          | Auth required | Description                  |
|--------|---------------|---------------|------------------------------|
| POST   | `/auth/login` | No            | Exchange credentials for JWT |

### Warehouse
| Method | Path                                   | Role  | Description                       |
|--------|----------------------------------------|-------|-----------------------------------|
| GET    | `/warehouse?page=0&size=20`            | any   | Paginated list of active warehouses |
| POST   | `/warehouse`                           | admin | Create a warehouse                |
| GET    | `/warehouse/{buCode}`                  | any   | Get by business unit code         |
| DELETE | `/warehouse/{buCode}`                  | admin | Archive a warehouse               |
| POST   | `/warehouse/{buCode}/replacement`      | admin | Replace a warehouse               |
| GET    | `/warehouse/export`                    | any   | Download all active warehouses as CSV |

### Store
| Method | Path                         | Role  | Description              |
|--------|------------------------------|-------|--------------------------|
| GET    | `/store?page=0&size=20`      | any   | Paginated list of stores |
| POST   | `/store`                     | admin | Create a store           |
| GET    | `/store/{id}`                | any   | Get store by ID          |
| PUT    | `/store/{id}`                | admin | Full update              |
| PATCH  | `/store/{id}`                | admin | Partial update           |
| DELETE | `/store/{id}`                | admin | Delete a store           |
| GET    | `/store/export`              | any   | Download stores as CSV   |

### Product
| Method | Path                           | Role  | Description                |
|--------|--------------------------------|-------|----------------------------|
| GET    | `/product?page=0&size=20`      | any   | Paginated list of products |
| POST   | `/product`                     | admin | Create a product           |
| GET    | `/product/{id}`                | any   | Get product by ID          |
| PUT    | `/product/{id}`                | admin | Full update                |
| DELETE | `/product/{id}`                | admin | Delete a product           |
| GET    | `/product/export`              | any   | Download products as CSV   |

### Health
| Method | Path              | Description                          |
|--------|-------------------|--------------------------------------|
| GET    | `/q/health`       | Combined liveness + readiness        |
| GET    | `/q/health/live`  | Liveness                             |
| GET    | `/q/health/ready` | Readiness — includes DB connectivity |

---

## Pagination response shape

All list endpoints return a paginated envelope:

```json
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 3,
  "totalPages": 1
}
```

---

## Rate limiting

All endpoints are protected by a sliding-window rate limiter: **100 requests per minute per IP**.

When exceeded, the server responds:
```
HTTP 429 Too Many Requests
Retry-After: 60
{ "error": "Rate limit exceeded", "retryAfterSeconds": 60 }
```

---

## Database migrations (Flyway)

Migrations run **automatically** on application startup (non-test profiles). Scripts live in:

```
src/main/resources/db/migration/
  V1__initial_schema.sql   — creates store, product, warehouse tables + sequences
  V2__seed_data.sql        — inserts the three seed records for each table
```

To add a new migration, drop a `V3__description.sql` file in that directory.

---

## Running tests

Tests use H2 in-memory — no Docker or external services required.

```sh
./mvnw clean test
```

Expected: **54 tests, 0 failures**.

---

## Coverage report

```sh
./mvnw verify -DskipITs
```

Report generated at `target/site/jacoco/index.html`.

- Line coverage: **94%** (threshold 80% ✅)
- Branch coverage: **86%** (threshold 75% ✅)

---

## Building the Docker image manually

From inside `java-assignment/`:

```sh
# Build image from source (multi-stage)
docker build -t inventorix-app .

# Run with an external Postgres
docker run -p 8080:8080 \
  -e QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://host.docker.internal:5432/quarkus_test \
  -e QUARKUS_DATASOURCE_USERNAME=quarkus_test \
  -e QUARKUS_DATASOURCE_PASSWORD=quarkus_test \
  inventorix-app
```

---

## Architecture

Warehouse module uses **hexagonal architecture**:

```
domain/           — use cases, ports (interfaces), models — no framework imports
adapters/         — database (Panache repository) + REST (OpenAPI-generated interface impl)
location/         — LocationGateway implements LocationResolver (CDI @ApplicationScoped)
common/           — PaginatedResponse<T>, RateLimitFilter, OpenApiConfig
auth/             — AuthResource, JwtService, UserService
```

Store and Product use Panache Active Record. `QUESTIONS.md` documents the trade-offs.

CDI events with `@Observes(during = AFTER_SUCCESS)` ensure the legacy store gateway is only
called after a confirmed database commit.
