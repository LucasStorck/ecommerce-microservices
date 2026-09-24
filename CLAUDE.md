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
- JPA: auditing enabled in `JpaConfig` (`@EnableJpaAuditing`), but each audited entity also needs `@EntityListeners(AuditingEntityListener.class)` itself — unlike Mongo, the config alone isn't enough.
- Money columns: `@Column(precision = 10, scale = 2)` on `BigDecimal` fields.
- Enums persisted with `@Enumerated(EnumType.STRING)`, never the `ORDINAL` default (breaks on reordering).
- Indentation: 2 spaces in Java files.
- Commits: Conventional Commits, in English (`feat(product-service): ...`, `build: ...`).
- Git workflow: one feature branch per module/feature (e.g. `feat/order-service`), merged into `main` via PR (self-reviewed/self-approved is fine solo). Direct commits to `main` were used before this convention was adopted (up to and including the `order-service` model commits) — not retroactively redone.

## Current state
- Done: multi-module skeleton, Docker Compose, `product-service` model/repository/DTOs/mapper/service/controller/`GlobalExceptionHandler`.
- `product-service` tested at runtime against real MongoDB: POST/GET return 201/200 with `createdAt`/`updatedAt` set and `price` as a proper number (DECIMAL128); 404 (`ProductNotFoundException`) and 400 (bean validation, via `fieldErrors`) confirmed manually with curl.
- `discovery-server` code is done (`@EnableEurekaServer`, self-preservation disabled for local dev) but **not yet run/verified** — see blocker below.
- `order-service`: `Order`/`OrderItem` JPA models done, with `Status` enum (defaults to `PENDING`, with getter/setter), bidirectional mapping (`Order` mappedBy, cascade ALL + orphanRemoval; `OrderItem` owns the `order_id` FK), money precision, and `JpaConfig`. Being developed on branch `feat/order-service`. Repository (`@EntityGraph` on `findAll`/`findById` to avoid N+1), DTOs, `OrderMapper` (MapStruct, `@AfterMapping` links items to their order, `total` computed in the mapper), `OrderServiceImpl` (`placeOrder`/`getAllOrders`/`getOrderById`), `OrderController` (`/api/orders`) and `GlobalExceptionHandler` are done and **compile** (not run, same blocker). `price` still comes from the client — to be sourced from `product-service` later. Still missing: inventory call (Resilience4j), Kafka event, status transitions.

## Known blocker (work PC only)
On the work machine, running any Spring Boot app fails at startup with:
```
java.io.IOException: Unable to establish loopback connection
  at sun.nio.ch.WEPollSelectorImpl...
```
This is the JVM (Java 25) failing to open its internal NIO loopback socket on Windows — happens with plain `mvnw spring-boot:run`, from both Git Bash and PowerShell, so it's not shell-specific. Most likely cause: corporate VPN/EDR/antivirus intercepting loopback sockets. Not a code issue.
Until this is resolved (or tested on a machine without that restriction, e.g. home), builds/compiles are verified but nothing has been run. **Test `discovery-server` at home first**, then re-verify `product-service` still runs correctly there too.

## Next steps
1. At home: run `discovery-server` (`./mvnw -pl discovery-server spring-boot:run`), confirm dashboard at `localhost:8761`, then run `product-service` and confirm it registers (no more connection-refused warnings).
2. API Gateway routes.
3. Inventory, then Order (Resilience4j + Kafka producer), then Notification (Kafka consumer).
4. Observability.
5. Angular frontend, consuming the API through the Gateway (see below) — do this once step 2 is done, so there's a single base URL and no per-service CORS.

## Future: Angular frontend
Planned, not started. Decision: single Git repository, but **not** a Maven monorepo — `frontend/` sits at the root next to the Java modules, outside `<modules>` in the parent `pom.xml`, with its own `package.json` and Angular CLI build. Maven never touches it; CI would run the Java and Node builds as separate steps.
```
ecommerce-microservices/
├── pom.xml            (Java modules, unchanged)
├── discovery-server/
├── api-gateway/
├── ...
└── frontend/           (Angular CLI owns this, not Maven)
```
