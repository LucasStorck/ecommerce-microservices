# E-commerce Microservices

A small e-commerce backend built as microservices with Java 25, Spring Boot 4
and Spring Cloud. It is a learning project, so the focus is on how the pieces
fit together rather than on features.

## Running locally

Requires Java 25 and Docker. Maven is not needed; the wrapper is included.

```
docker compose up -d
./mvnw -pl discovery-server spring-boot:run
./mvnw -pl product-service spring-boot:run
./mvnw -pl api-gateway spring-boot:run
```

Start discovery-server first, then the others in any order, each in its own
terminal. The Eureka dashboard is at http://localhost:8761 and the API is
served through the gateway at http://localhost:8080 (for example
`/api/products`).

## Modules

The system is split into six Spring Boot services. Each one runs as its own
process, owns its own data, and is a child module of the root `pom.xml`.

```
client -> api-gateway -> product-service    (MongoDB)
                      -> order-service      (MySQL) -> inventory-service (MySQL)
                                             \
                                              -> Kafka -> notification-service

every service registers with discovery-server
```

### discovery-server

Port 8761. A Eureka server. Services register here on startup with their name
and address, and look each other up by name instead of by host and port. This
lets a service move or run more than once without anyone else changing config.
It holds no business logic and no data beyond the live registry.

### api-gateway

Port 8080. The single entry point for clients. It matches the request path to
a route and forwards the call to the right service, finding it through Eureka.
Clients only need one base URL, and cross-cutting concerns such as CORS can
live in one place. It is built on Gateway MVC (servlet-based), not WebFlux.

### product-service

Port 8081. The product catalog: create, read, update and delete products. It
stores data in MongoDB, since products are read far more often than written
and their attributes vary between categories, which suits a document model.

### order-service

Port 8082. Receives orders and manages their lifecycle. Before accepting an
order it asks inventory-service whether the items are in stock. That call is
synchronous and wrapped in a Resilience4j circuit breaker, so a slow or failing
inventory service does not take orders down with it. Once an order is placed,
it publishes an event to Kafka. Data lives in MySQL, where transactions keep
an order and its items consistent.

Only the JPA model exists so far.

### inventory-service

Port 8083. Tracks stock per product and answers the availability check from
order-service. Uses its own MySQL instance, separate from orders, so neither
service can reach into the other's tables.

Not implemented yet.

### notification-service

Port 8084. Listens for order events on Kafka and notifies the customer. Because
it is asynchronous, order-service does not wait for it, and a notification
failure never blocks an order. It has no database.

Not implemented yet.

## Infrastructure

`docker-compose.yml` starts what the services depend on: two MySQL instances
(orders on 3307, inventory on 3308), MongoDB on 27017 and a single-node Kafka
broker in KRaft mode on 9092.
