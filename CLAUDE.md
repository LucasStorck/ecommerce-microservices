# E-commerce Microservices

Learning/portfolio e-commerce built as microservices with Java and Spring Boot.
Owner is a developer who wants to **understand** the code: explain decisions and trade-offs, don't just dump code.

## Stack
- Java 25 (LTS), Spring Boot 4.1.1, Spring Cloud 2025.1.3
- Maven multi-module (wrapper included: use `./mvnw`, Maven is not required)
- Docker Compose for infra: PostgreSQL x2, MongoDB, Kafka (KRaft), Redis
- MapStruct 1.6.3 for DTO/entity mapping (version in the parent pom)

## Modules
| Module                 | Port | Role                                                              | Data / messaging                     |
|------------------------|------|-------------------------------------------------------------------|--------------------------------------|
| `discovery-server`     | 8761 | Eureka server                                                     | -                                    |
| `api-gateway`          | 8080 | Single entry point (Gateway MVC, not WebFlux)                     | -                                    |
| `product-service`      | 8081 | Product catalog                                                   | MongoDB (`localhost:27017`), Redis (`localhost:6379`) |
| `order-service`        | 8082 | Orders; calls inventory with Resilience4j; publishes Kafka events | PostgreSQL (`localhost:5433`), Kafka |
| `inventory-service`    | 8083 | Stock                                                             | PostgreSQL (`localhost:5434`)        |
| `notification-service` | 8084 | Consumes order events                                             | Kafka (`localhost:9092`)             |

Each service is a child module of the root `pom.xml` (packaging `pom`); versions are managed only there.

## Commands
```bash
docker compose up -d                 # start PostgreSQL, MongoDB, Kafka, Redis
./mvnw package -DskipTests           # build all modules
./mvnw -pl product-service compile   # build one module
./mvnw -pl product-service spring-boot:run
```

## Architecture decisions
- Database per service (two separate PostgreSQL instances). Product uses MongoDB (flexible attributes, read-heavy); Order/Inventory use PostgreSQL (transactional consistency).
- Order → Inventory is synchronous (Resilience4j circuit breaker); Order → Notification is asynchronous via Kafka.
- **No Keycloak / OAuth2** in this project. Do not add security dependencies.
- **Observability is deferred** (Micrometer, Zipkin, Prometheus, Grafana). Actuator is already included.
- `product-service` GET-by-id reads are cached in Redis (10 min TTL, JSON serialization); nothing else is cached yet.
- `root/root` passwords are for local dev only. `ddl-auto` is `validate` in `order-service`/`inventory-service` now that Liquibase owns their schema (changelogs under `db/changelog/`); `product-service` has no schema (MongoDB).

## Code conventions
- Layers: `model`, `repository`, `dto`, `mapper`, `service` (interface + `Impl`), `exception`, `config`, `controller`; plus `client` (interface + `Impl`) for calls to other services.
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
- The remote branch is deleted after its PR merges (GitHub's default), so a new feature branches off current `main`, not off an old feature branch — even if a previous session stacked branches (`feat/postgresql` off `feat/liquibase`, `feat/redis-cache` off `feat/postgresql`) while those were still unmerged.
- `README.md` is the public, human-facing doc (minimal prose, English, no emoji, no tables). When a service is implemented, update it in the same change: add its `spring-boot:run` line to "Running locally" and drop its "Not implemented yet" / "Only the JPA model exists so far" note.

## Current state
- Done: multi-module skeleton, Docker Compose, `product-service` and `inventory-service` (model/repository/DTOs/mapper/service/controller/`GlobalExceptionHandler`).
- `product-service` tested at runtime against real MongoDB: POST/GET return 201/200 with `createdAt`/`updatedAt` set and `price` as a proper number (DECIMAL128); 404 (`ProductNotFoundException`) and 400 (bean validation, via `fieldErrors`) confirmed manually with curl.
- `discovery-server` done and verified (in WSL): dashboard at `localhost:8761`, `product-service` registers as `UP`. Instances register with a WSL virtual-interface IP (`10.255.255.254`) instead of `localhost`; routing through it works, so no hostname override is configured.
- `api-gateway`: Gateway MVC routes in `application.yml` (`spring.cloud.gateway.server.webmvc.routes`), resolved via Eureka with `lb://`. Routes: `/api/products/**` → `product-service`, `/api/inventory/**` → `inventory-service`; verified that 200/201/404/400 pass through unchanged and unknown paths return 404. Add a route for each new service as it's implemented.
- `skuCode` is the cross-service product identifier (not the Mongo id): required in `ProductRequest`, stored as `sku_code`. Products created before it was added have no `skuCode`.
- `inventory-service` done and verified through the gateway: `Inventory` (JPA, `sku_code` unique, `quantity`, audited). `POST /api/inventory` (409 on duplicate skuCode), `GET /api/inventory/{skuCode}` (404 if missing), `PUT /api/inventory/{skuCode}` sets an absolute quantity, and `GET /api/inventory?skuCode=A&skuCode=B` is the batch stock check for order-service: one entry per distinct requested code, unknown codes reported as quantity 0. Stock is not yet decremented when an order is placed; that belongs to the order flow.
- `order-service`: `Order`/`OrderItem` JPA models done, with `Status` enum (defaults to `PENDING`, with getter/setter), bidirectional mapping (`Order` mappedBy, cascade ALL + orphanRemoval; `OrderItem` owns the `order_id` FK), money precision, and `JpaConfig`. Repository (`@EntityGraph` on `findAll`/`findById` to avoid N+1), DTOs, `OrderMapper` (MapStruct, `@AfterMapping` links items to their order, `total` computed in the mapper), `OrderServiceImpl` (`placeOrder`/`getAllOrders`/`getOrderById`), `OrderController` (`/api/orders`, routed by the gateway) and `GlobalExceptionHandler` are done.
- `order-service` → `inventory-service`: `placeOrder` calls the batch stock check through `InventoryClient` (`@LoadBalanced` `RestClient`, `http://inventory-service` resolved by Eureka; HTTP timeouts 1s connect / 2s read) wrapped in a Resilience4j circuit breaker `inventory` (configured in `InventoryClientConfig`: window 10, min 5 calls, 50% failure, 10s open, 3s time limiter — Spring Cloud's default time limiter is 1s, which is why it's overridden). Quantities are summed per skuCode before comparing. Insufficient stock → 409 (`InsufficientStockException`); inventory down/slow/circuit open → 503 (`InventoryUnavailableException`, fail closed). `placeOrder` is intentionally not `@Transactional` so the remote call doesn't hold a DB connection. Known limitation: stock is checked, not reserved or decremented, so concurrent orders can oversell. **Verified at runtime (WSL)**: happy path (201), insufficient stock (409), inventory-service stopped (503, fail closed). `price` still comes from the client — to be sourced from `product-service` later. Still missing: status transitions, stock decrement/reservation.
- Liquibase adopted for `order-service` and `inventory-service` (changelogs in `db/changelog/`, `db.changelog-master.yaml` includes each numbered changeset). `ddl-auto` switched from `update` to `validate` in both, since Liquibase now owns schema creation — leaving `update` on caused Hibernate to fight Liquibase's tables on every startup (harmless in `order-service`, fatal in `inventory-service` where a table from a pre-Liquibase run already existed; fixed locally by dropping that dev volume, not by changing the changelog). Merged into `main` via PR (`feat/liquibase`).
- Fixed a real bug found while verifying the above at runtime: `InventoryClientConfig` exposed only one `RestClient.Builder` bean, marked `@LoadBalanced`. Since nothing else provided an unqualified one, Eureka's own internal HTTP client (which also autowires `RestClient.Builder`) picked up the load-balanced version and tried to resolve `localhost` (from its own `serviceUrl`) through the load balancer as if it were a service name — `order-service` registered but could never send a heartbeat. Fix: added a second, `@Primary`, plain `RestClient.Builder` bean so only the explicit `@LoadBalanced` injection point (`InventoryClientImpl`) gets the special one.
- `order-service` and `inventory-service` migrated from MySQL to PostgreSQL (`feat/postgresql` branch, off `feat/liquibase`), to match the owner's main stack: `postgres:17` in `docker-compose.yml` (ports 5433/5434), `mysql-connector-j` → `org.postgresql:postgresql` in both poms, datasource URLs updated. Liquibase changelogs untouched — their column types (`DATETIME`, `DECIMAL`, `VARCHAR`, `INT`) are Liquibase's portable abstract types, translated per-database automatically, so no changeset edits were needed. Merged into `main` via PR (`feat/postgresql`).
- Redis added to `product-service` for read caching (`feat/redis-cache` branch, off `feat/postgresql`; merged into `main`): `GET /api/products/{id}` is `@Cacheable`, `updateProduct` is `@CachePut` (refreshes the entry instead of evicting, since the new value is already at hand), `deleteProduct` is `@CacheEvict`; the product listing is not cached (would need invalidation on every write, not just one key). `RedisConfig` sets a 10 minute TTL and serializes cache values as JSON instead of Java's default binary serialization, since `ProductResponse` carries `Instant`/`BigDecimal`.
- `order-service` → Kafka: `placeOrder` publishes an `OrderPlacedEvent` (own contract in `event/`, decoupled from the `OrderResponse` API DTO) to topic `order-placed-events` after saving, keyed by `orderNumber`. Topic declared explicitly (3 partitions) via a `NewTopic` bean in `KafkaTopicConfig` rather than relying on broker auto-create. `OrderEventProducer.publishOrderPlaced` calls `KafkaTemplate.send()`, which is fire-and-forget (not blocking `placeOrder`); a `whenComplete` callback just logs success/failure.
- `notification-service` implemented (was empty scaffolding before): `OrderEventListener` (`@KafkaListener`, group id from `application.yml`) consumes `order-placed-events`; `NotificationServiceImpl` logs a simulated notification and keeps it in a `CopyOnWriteArrayList` (no database, per its own design — resets on restart); `GET /api/notifications` (routed by the gateway) lists them. Its `event/OrderPlacedEvent` is its own copy of the contract, not shared with `order-service` — each service owns its classpath, same principle as database-per-service. Needed `spring.json.use.type.headers: false` + `spring.json.value.default.type` on the consumer's `JsonDeserializer`, because the producer's `JsonSerializer` stamps messages with its own (different) fully-qualified class name by default, which doesn't exist on `notification-service`'s classpath.
- **Verified at runtime (WSL, real infra)**: the Kafka flow (`POST /api/orders` → `notification-service` log line → `GET /api/notifications` through the gateway), the Redis cache (`GET`/`PUT`/`DELETE` on `/api/products/{id}`), and the PostgreSQL/Liquibase changesets for `order-service`/`inventory-service` (changelog history, translated column types, and persisted rows all checked directly against the databases). Two real bugs were found and fixed along the way (see below); everything now passes with both fixes applied.
- **Bug fixed — Redis cache deserialization** (`fix/redis-cache-deserialization`, PR #7): `RedisConfig` used `GenericJackson2JsonRedisSerializer` with a custom `ObjectMapper`, but that serializer only embeds the `@class` type hint on write when default typing is active for the target class; `ProductResponse` is a record (implicitly final), so no default-typing mode ever emits that hint for it. Every read came back as a raw `LinkedHashMap` and blew up with a `ClassCastException`, silently breaking the cache from the day it was introduced (`feat/redis-cache`) — caught by stopping MongoDB and confirming a second `GET` returned 500 instead of the cached value. Fixed by binding a `Jackson2JsonRedisSerializer<ProductResponse>` to the `products` cache specifically instead of the generic polymorphic serializer.
- **Bug fixed — MongoDB property prefix** (`fix/mongodb-property-prefix`, PR #8): Spring Boot 4.1.1 moved the MongoDB connection properties from `spring.data.mongodb.*` (Spring Boot 3.x) to `spring.mongodb.*`; `spring.data.mongodb.*` now only covers Spring Data Mongo's own repository/mapping options (`field-naming-strategy`, `gridfs.*`), not the connection itself. `application.yml` still used the old `spring.data.mongodb.uri` key; Spring Boot doesn't fail on unrecognized properties, so it was silently ignored and the app fell back to the driver default `mongodb://localhost/test`. Host and port happened to match this project's docker-compose setup, so every request "worked" — but every product ever created through the API landed in Mongo's `test` database instead of `product_service`, since product-service's very first implementation. Caught by seeding `product_service.products` directly and finding none of it showed up through `GET /api/products`. Fixed by moving the property to `spring.mongodb.uri`. **Any future service added to this project that talks to MongoDB must use the `spring.mongodb.*` prefix, not `spring.data.mongodb.*`** — check this project's actual Spring Boot version's property metadata (inside the relevant `spring-boot-*-autoconfigure`/`spring-boot-*` jar's `spring-configuration-metadata.json`) before trusting an older tutorial's property names, since Spring Boot 4 re-split several autoconfigure modules this way.
- `scripts/seed-inventory.sql` and `scripts/seed-products.js` seed 1000 matching SKUs (`SKU-00001`..`SKU-01000`) into `inventory_service.inventory` (Postgres, ~2% at zero stock to exercise the 409 path) and `product_service.products` (Mongo) respectively, for load-testing `order-service`/`inventory-service` beyond hand-picked fixtures. Used to verify: happy path, insufficient stock (zero-quantity SKU), unknown SKU, mixed valid/insufficient items in one order, and 50 concurrent `POST /api/orders` calls (all 201, all matched by a `notification-service` entry, no errors in any log) — this also empirically confirmed the known oversell limitation below, since ordering the same in-stock SKUs twice in a row raised no complaint.

## Known blocker (work PC only)
On the work machine, running any Spring Boot app fails at startup with:
```
java.io.IOException: Unable to establish loopback connection
  at sun.nio.ch.WEPollSelectorImpl...
```
This is the JVM (Java 25) failing to open its internal NIO loopback socket on Windows — happens with plain `mvnw spring-boot:run`, from both Git Bash and PowerShell, so it's not shell-specific. Most likely cause: corporate VPN/EDR/antivirus intercepting loopback sockets. Not a code issue.
Running everything inside WSL (Ubuntu) avoids it: all services start and work there.

## Next steps
1. Observability.
2. Angular frontend, consuming the API through the Gateway (see below), so there's a single base URL and no per-service CORS.
3. Consider reserving/decrementing stock on order placement (known oversell limitation, empirically confirmed again during the 1000-SKU load test).

(The two fix PRs — `fix/redis-cache-deserialization` #7, `fix/mongodb-property-prefix` #8 — and the seed-scripts/docs branch are merged into `main`.)

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
