# gRPC-Based Order Management System

A **Java 21 and Spring Boot microservices project** demonstrating REST-to-gRPC communication, distributed service orchestration, and fault-tolerance patterns using Resilience4j.

The system coordinates inventory reservation and payment authorization across independent services, with a focus on service boundaries, API contracts, failure handling, and resilient inter-service communication.

## Architecture

```text
                         Client
                           |
                           | HTTP / REST
                           v
                 +---------------------+
                 |    Order Service    |
                 |      Port 8080      |
                 |                     |
                 | Order Orchestration |
                 | Resilience4j         |
                 +---------------------+
                      |           |
                 gRPC |           | gRPC
                      v           v
             +----------------+  +----------------+
             |    Inventory   |  |    Payment     |
             |    Service     |  |    Service     |
             |    Port 9091   |  |    Port 9092   |
             +----------------+  +----------------+
                      |                   |
                      v                   v
               Inventory Data       Payment Data

                    PostgreSQL
                  Docker Compose
```

### Design Overview

- **REST at the system boundary:** Provides a straightforward HTTP API for clients.
- **gRPC between services:** Uses Protocol Buffers contracts for strongly typed service communication.
- **Dedicated contracts module:** Centralizes service definitions and generated gRPC classes.
- **Independent service boundaries:** Separates order orchestration, inventory management, and payment processing.
- **Resilience mechanisms:** Uses Resilience4j patterns to handle downstream failures and limit cascading failures.

## Technology Stack

| Category | Technologies |
|---|---|
| Language | Java 21 |
| Application Framework | Spring Boot 4 |
| Service Communication | gRPC, Protocol Buffers |
| API Layer | Spring MVC, REST |
| Resilience | Resilience4j |
| Database | PostgreSQL |
| Database Environment | Docker Compose |
| Build Tool | Maven Multi-Module |
| Monitoring | Spring Boot Actuator |
| API Testing | Postman |

## Project Structure

```text
grpc-order-management/
├── contracts/
│   └── src/main/proto/
│       ├── inventory/v1/inventory.proto
│       └── payment/v1/payment.proto
├── order-service/
│   └── src/main/
│       ├── java/
│       └── resources/application.yml
├── inventory-service/
│   └── src/main/
│       ├── java/
│       └── resources/application.yml
├── payment-service/
│   └── src/main/
│       ├── java/
│       └── resources/application.yml
├── docker/
│   └── postgres/init/
├── docs/
│   ├── architecture.md
│   └── decisions/
├── docker-compose.yml
├── pom.xml
└── README.md
```

## Order Processing Workflow

The Order Service coordinates the order lifecycle through the following steps:

1. The client submits an order using the REST API.
2. The Order Service checks product availability through the Inventory Service.
3. Inventory is reserved for the requested quantity.
4. The Order Service requests payment authorization from the Payment Service.
5. If payment is authorized, the order is returned as `CONFIRMED`.
6. If payment fails, the Order Service attempts to release the inventory reservation.
7. Downstream failures are handled using configured resilience mechanisms.

**Important design consideration:** Inventory reservation and payment authorization span independent services. They do not form a single atomic database transaction. Compensation, idempotency, and reconciliation are therefore important for production-grade consistency.

## API Reference

### Create Order

`POST /api/v1/orders`

**Base URL**

```text
http://localhost:8080
```

**Request headers**

```http
Content-Type: application/json
```

**Sample request**

```json
{
  "productId": "PROD-001",
  "quantity": 2,
  "amountMinorUnits": 50000,
  "currency": "INR"
}
```

**Illustrative successful response**

```json
{
  "orderId": "generated-order-id",
  "status": "CONFIRMED",
  "productId": "PROD-001",
  "quantity": 2,
  "paymentId": "generated-payment-id",
  "reservationId": "generated-reservation-id"
}
```

The identifiers above are illustrative. A successful response should reflect the actual values returned by the running application.

## Resilience and Failure Handling

The project explores resilience patterns for communication between microservices.

### Circuit Breaker

Prevents repeated calls to a downstream service when its failure rate exceeds a configured threshold. Supports recovery through half-open state checks.

### Retry

Retries selected transient failures where retrying is appropriate. Payment authorization and inventory reservation require particular care because a timeout does not necessarily mean the operation failed.

### Bulkhead

Limits concurrent downstream calls to help prevent an unhealthy dependency from exhausting application resources.

### Fallback

Returns a controlled failure response when a downstream operation cannot be completed, rather than reporting a false successful order.

### gRPC Deadlines

Places a time limit on downstream calls so requests do not wait indefinitely.

### Compensation

Attempts to release reserved inventory when payment authorization fails. This is a best-effort approach; durable compensation and reconciliation are necessary to handle failures that occur during recovery itself.

## Running Locally

### Prerequisites

- JDK 21
- Maven
- Docker Desktop
- Postman

### 1. Clone the repository

```bash
git clone https://github.com/YOUR_USERNAME/grpc-order-management.git
cd grpc-order-management
```

Replace `YOUR_USERNAME` with your GitHub username.

### 2. Start PostgreSQL

```bash
docker compose up -d
```

### 3. Build all modules

```bash
mvn clean install -DskipTests
```

### 4. Start the Inventory Service

Open a terminal at the project root:

```bash
mvn -pl inventory-service spring-boot:run
```

### 5. Start the Payment Service

Open another terminal:

```bash
mvn -pl payment-service spring-boot:run
```

### 6. Start the Order Service

Open another terminal:

```bash
mvn -pl order-service spring-boot:run
```

Confirm that the configured ports are available and that the downstream gRPC addresses match the service configuration.

### 7. Send a test request

Use Postman to send the Create Order request to:

```text
POST http://localhost:8080/api/v1/orders
```

All three services should be running for the successful order flow.

## Monitoring

Spring Boot Actuator provides application health and monitoring endpoints, subject to the application's configuration.

**Health check**

```http
GET http://localhost:8080/actuator/health
```

Resilience4j metrics and event endpoints can also be exposed for troubleshooting circuit breaker transitions, retries, and rejected calls. Ensure that only appropriate endpoints are exposed in production.

## Architecture Decision Records

The `docs/decisions/` directory documents key design choices:

- **ADR-001 — REST and gRPC:** Defines the communication approach.
- **ADR-002 — Contracts Module:** Explains the shared Protocol Buffers module.
- **ADR-003 — Database Ownership:** Documents database ownership boundaries.

## Engineering Challenges Explored

- Designing a Maven multi-module microservices application.
- Defining versioned Protocol Buffers contracts.
- Integrating REST APIs with gRPC clients and servers.
- Coordinating dependent operations across services.
- Handling downstream timeouts and service failures.
- Applying circuit breakers, retries, and bulkheads.
- Managing compensation when a multi-service workflow fails.
- Separating service responsibilities and documenting architecture decisions.

## Future Enhancements

- Persist orders, reservations, and payments using service-owned PostgreSQL schemas.
- Add database migrations and transaction management.
- Implement durable compensation and reconciliation.
- Enforce idempotency for order creation and payment authorization.
- Add integration tests for success, failure, timeout, and recovery scenarios.
- Introduce distributed tracing and centralized observability.
- Containerize application services and deploy them to Kubernetes.

## Project Goals

This project is designed to demonstrate practical backend engineering concepts relevant to Java and Spring Boot microservices development, including **inter-service communication, API contract design, fault tolerance, service orchestration, and distributed-system failure handling**.

## License

No open-source license has been specified yet. Add a `LICENSE` file before distributing the project under a particular license.
