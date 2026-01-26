# Ecommerce Backend (Spring Boot 4, Java 17)

E-commerce backend built with Spring Boot microservices. It includes user, catalog, cart, order, payment, notification, and service discovery services, with shared patterns and config via environment variables.

## Services
- **ServiceDiscovery** (`service-discovery`) - Eureka registry.
- **UserService** (`user-service`) - registration/login, profile, password reset, sessions.
- **ProductCatalogService** (`product-catalog-service`) - products, categories, database search.
- **CartService** (`cart-service`) - cart with MySQL + Redis cache.
- **OrderService** (`order-service`) - order processing, history, tracking.
- **PaymentService** (`payment-service`) - payment processing, receipts.
- **NotificationService** (`notification-service`) - Kafka-driven email notifications.

## Ports

- UserService: 8081
- ProductCatalogService: 8082
- CartService: 8083
- OrderService: 8084
- PaymentService: 8085
- NotificationService: 8086
- ServiceDiscovery (Eureka UI): 8761

## Architecture

- Eureka Service Discovery
- Kafka for async events
- MySQL for structured data, Redis for caching

## Prerequisites

- Java 17
- Maven 3.9+
- Docker (for MySQL/Redis/Kafka)

## Local Dependencies (macOS-friendly)

```bash
docker compose up -d
```

The Docker compose stack uses `docker/mysql/init.sql` to create databases and users.

## Quick Start

```bash
./scripts/start-all.sh
```

Stop everything:

```bash
./scripts/stop-all.sh
```

Logs are written to `./logs`.

## Build

```bash
./mvnw clean package
```

## Tests

```bash
./mvnw test
```

Note: tests are unit-only and do not require Docker.

## Configuration

Configuration is via environment variables (or `.env` loaded by `scripts/start-all.sh`). Defaults live in each
service's `application.properties`. Hibernate manages schemas automatically via `ddl-auto=update`.

Shared environment variables:

- `EUREKA_URL` (default `http://localhost:8761/eureka`)
- `KAFKA_BOOTSTRAP_SERVERS` (default `localhost:9092`)
- `INTERNAL_SHARED_SECRET` (optional)
- `LOG_DIR` (optional, default `./logs`)

UserService:

- `USER_DB_URL`, `USER_DB_USER`, `USER_DB_PASSWORD`
- `JWT_SECRET`
- `PASSWORD_RESET_BASE_URL`
- `KAFKA_BOOTSTRAP_SERVERS`

ProductCatalogService:

- `PRODUCT_DB_URL`, `PRODUCT_DB_USER`, `PRODUCT_DB_PASSWORD`
- `USER_SERVICE_URL`

CartService:

- `CART_DB_URL`, `CART_DB_USER`, `CART_DB_PASSWORD`
- `REDIS_HOST`, `REDIS_PORT`
- `KAFKA_BOOTSTRAP_SERVERS`
- `USER_SERVICE_URL`, `PRODUCT_SERVICE_URL`, `ORDER_SERVICE_URL`

OrderService:

- `ORDER_DB_URL`, `ORDER_DB_USER`, `ORDER_DB_PASSWORD`
- `KAFKA_BOOTSTRAP_SERVERS`
- `USER_SERVICE_URL`, `PRODUCT_SERVICE_URL`

PaymentService:

- `PAYMENT_DB_URL`, `PAYMENT_DB_USER`, `PAYMENT_DB_PASSWORD`
- `KAFKA_BOOTSTRAP_SERVERS`
- `USER_SERVICE_URL`
- `PAYMENT_GATEWAY` (`stripe` or `mock`)
- `STRIPE_SECRET_KEY`
- `STRIPE_WEBHOOK_SECRET`
- `STRIPE_AFTER_COMPLETION_URL`
- `STRIPE_PUBLISHABLE_KEY` (client-side only)

NotificationService:

- `KAFKA_BOOTSTRAP_SERVERS`
- `EMAIL_MODE` (`log` or `smtp`)
- `EMAIL_FROM`
- `SMTP_HOST`, `SMTP_PORT`
- `SMTP_USER`, `SMTP_PASS`

## Grant ADMIN Role (for admin-only APIs)

Create a user and then promote it to `ADMIN` via SQL.


## Stripe Webhooks (required for completion)

Payment remains `PENDING` until Stripe confirms via webhook.

Stripe CLI:

```bash
stripe listen --forward-to http://localhost:8085/api/v1/payments/webhooks/stripe
```

Copy the `whsec_...` secret into `STRIPE_WEBHOOK_SECRET`.

Local dev with ngrok (no hosting):

```bash
ngrok http 8085
```

Use the HTTPS URL from ngrok as your Stripe webhook endpoint:
`https://<ngrok-subdomain>.ngrok-free.app/api/v1/payments/webhooks/stripe`

## Gmail SMTP Setup

1) Enable 2-Step Verification on your Google account.
2) Create an App Password (Google Account -> Security -> App passwords).
3) Set these in `.env`:

```
EMAIL_MODE=smtp
EMAIL_FROM=you@gmail.com
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=you@gmail.com
SMTP_PASS=<app-password>
```

## Postman Collection

- `postman/EcommerceBackend.postman_collection.json`
