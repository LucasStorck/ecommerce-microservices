# E-commerce Microservices

Learning/portfolio e-commerce built as microservices with Java and Spring Boot.
Owner is a developer who wants to **understand** the code: explain decisions and trade-offs, don't just dump code.

## Stack
- Java 25 (LTS), Spring Boot 4.1.1, Spring Cloud 2025.1.3
- Maven multi-module (wrapper included: use `./mvnw`, Maven is not required)
- Docker Compose for infra: MySQL x2, MongoDB, Kafka (KRaft)
- MapStruct 1.6.3 for DTO/entity mapping (version in the parent pom)

## Modules
| Module | Port | Role | Data / messaging |
|---|---|---|---|
| `discovery-server` | 8761 | Eureka server | - |
| `api-gateway` | 8080 | Single entry point (Gateway MVC, not WebFlux) | - |
| `product-service` | 8081 | Product catalog | MongoDB (`localhost:27017`) |
| `order-service` | 8082 | Orders; calls inventory with Resilience4j; publishes Kafka events | MySQL (`localhost:3307`), Kafka |
| `inventory-service` | 8083 | Stock | MySQL (`localhost:3308`) |
| `notification-service` | 8084 | Consumes order events | Kafka (`localhost:9092`) |

Each service is a child module of the root `pom.xml` (packaging `pom`); versions are managed only there.

## Commands
```bash
docker compose up -d                 # start MySQL, MongoDB, Kafka
./mvnw package -DskipTests           # build all modules
./mvnw -pl product-service compile   # build one module
./mvnw -pl product-service spring-boot:run
```

## Architecture decisions
- Database per service (two separate MySQL instances). Product uses MongoDB (flexible attributes, read-heavy); Order/Inventory use MySQL (transactional consistency).
- Order -> Inventory is synchronous (Resilience4j circuit breaker); Order -> Notification is asynchronous via Kafka.
- **No Keycloak / OAuth2** in this project. Do not add security dependencies.
- **Observability is deferred** (Micrometer, Zipkin, Prometheus, Grafana). Actuator is already included.
- `ddl-auto: update` and `root/root` passwords are for local dev only.

## Code conventions
- Layers: `model`, `repository`, `dto`, `mapper`, `service` (interface + `Impl`), `exception`, `config`, `controller`.
- DTOs are records; requests carry bean validation (`@NotBlank`, `@Positive`, ...) and controllers use `@Valid`.
- Never expose entities in the API; clients must not set `id`, `createdAt`, `updatedAt`.
- Constructor injection (no field `@Autowired`).
- Missing resources throw a custom exception (e.g. `ProductNotFoundException`), mapped to 404 by a `@RestControllerAdvice`.
- Mongo: auditing enabled in `MongoConfig`; `BigDecimal` stored as `DECIMAL128`.
- Indentation: 2 spaces in Java files.
- Commits: Conventional Commits, in English (`feat(product-service): ...`, `build: ...`).

## Current state
- Done: multi-module skeleton, Docker Compose, `product-service` model/repository/DTOs/mapper/service/controller/`GlobalExceptionHandler`.
- `product-service` tested at runtime against real MongoDB: POST/GET return 201/200 with `createdAt`/`updatedAt` set and `price` as a proper number (DECIMAL128); 404 (`ProductNotFoundException`) and 400 (bean validation, via `fieldErrors`) confirmed manually with curl.
- Eureka client on `product-service` currently logs connection-refused warnings on startup (retries against `localhost:8761`) since `discovery-server` isn't set up yet; harmless, expected until step 1 below.

## Next steps
1. Discovery Server (Eureka server config), then API Gateway routes.
2. Inventory, then Order (Resilience4j + Kafka producer), then Notification (Kafka consumer).
3. Observability.
