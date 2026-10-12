# Java Code Assignment

This is a short code assignment that explores various aspects of software development, including API implementation, documentation, persistence layer handling, and testing.

## About the assignment

You will find the tasks of this assignment on [CODE_ASSIGNMENT](CODE_ASSIGNMENT.md) file.

The assignment questions are answered in [QUESTIONS](QUESTIONS.md). Case study answers are in [`../case-study/CASE_STUDY.md`](../case-study/CASE_STUDY.md).

## About the code base

This is based on https://github.com/quarkusio/quarkus-quickstarts

### Requirements

- JDK 17+ (tested on Java 17 and Java 23; Java 23 requires Byte Buddy experimental mode — handled automatically via `.mvn/jvm.config`)
- Docker (optional — only needed for production PostgreSQL; tests use H2 in-memory)

## Building the demo

```sh
./mvnw package
```

## Running the demo

### Live coding with Quarkus

```sh
./mvnw quarkus:dev
```

In dev mode Quarkus starts a PostgreSQL container automatically via dev services (Docker required).

### (Optional) JVM mode with PostgreSQL

Start a PostgreSQL container:

```sh
docker run -it --rm=true --name quarkus_test \
  -e POSTGRES_USER=quarkus_test \
  -e POSTGRES_PASSWORD=quarkus_test \
  -e POSTGRES_DB=quarkus_test \
  -p 15432:5432 postgres:13.3
```

Then run:

```sh
java -jar ./target/quarkus-app/quarkus-run.jar
```

Navigate to <http://localhost:8080/index.html>

## Running tests

Tests use an H2 in-memory database and do **not** require Docker.

```sh
./mvnw clean test
```

Expected: **54 tests, 0 failures**.

## Generating the coverage report

```sh
./mvnw verify -DskipITs
```

Generates the JaCoCo HTML report at `target/site/jacoco/index.html`.

**Measured coverage (verified):**
- Line coverage: **94%** (threshold: 80% ✅)
- Branch coverage: **86%** (threshold: 75% ✅)

## Health checks

The application exposes SmallRye Health endpoints:

| Endpoint | Description |
|----------|-------------|
| `GET /q/health` | Combined liveness + readiness |
| `GET /q/health/live` | Liveness (is the process up?) |
| `GET /q/health/ready` | Readiness — includes `warehouse-store` check that queries the database |

## API endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/warehouse` | List all active warehouses |
| POST | `/warehouse` | Create a warehouse (validates location, capacity, BU code uniqueness) |
| GET | `/warehouse/{buCode}` | Get warehouse by business unit code |
| DELETE | `/warehouse/{buCode}` | Archive a warehouse |
| POST | `/warehouse/{buCode}/replacement` | Replace a warehouse (archives old, creates new with same BU code) |
| GET/POST/PUT/PATCH/DELETE | `/store` | CRUD for stores |
| GET/POST/PUT/DELETE | `/product` | CRUD for products |

## Architecture notes

The codebase uses a hexagonal architecture for the Warehouse module:
- **Domain layer** (`domain/`): use cases, ports (interfaces), models — no framework dependencies
- **Adapters** (`adapters/`): database (Panache repository) and REST (OpenAPI-generated interface implementation)
- **Infra** (`location/`): `LocationGateway` is the CDI-injectable implementation of `LocationResolver`

Store and Product use Panache Active Record. The `QUESTIONS.md` explains the trade-offs and the rationale for standardising on the repository pattern.

## Troubleshooting

**IntelliJ**: if generated code under `target/.../jaxrs` is not recognised, add it as "generated sources".

**Java 23**: Quarkus 3.13.x uses Byte Buddy which only officially supports Java 22. The `.mvn/jvm.config` file sets `net.bytebuddy.experimental=true` automatically so builds work on Java 23+.
