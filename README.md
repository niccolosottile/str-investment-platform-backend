# STR Platform Backend

Java Spring Boot backend for the STR Investment Platform. Handles data scraping orchestration, market analysis calculations, and provides REST APIs for the frontend.

## What This Does

The backend solves the data aggregation problem. STR market data is scattered across Airbnb, Booking.com, and VRBO with no unified API. This service orchestrates Python scrapers, stores normalized data in PostgreSQL, and exposes clean REST endpoints for investment analysis.

## Architecture

**Modular Monolith**: DDD bounded contexts as Gradle modules. Can extract to microservices later if needed, but starts simple.

**Event-Driven**: RabbitMQ for async scraping jobs. Java backend publishes events, Python workers consume them, results flow back through events.

**Distributed Caching**: Redis with different TTLs per data type. Location searches cached 1hr, analysis results 6hrs, driving times 7d.

**Virtual Threads**: Java 21's virtual threads for handling 1000+ concurrent driving time calculations without thread pool exhaustion.

## Tech Stack

- Java 21 LTS (Virtual Threads, Pattern Matching)
- Spring Boot 3.2.x
- PostgreSQL 16 + Spring Data JPA
- Redis 7 (Caching + Queue)
- RabbitMQ (Event-driven communication with Python scrapers)
- Gradle (Kotlin DSL)
- Docker + Docker Compose
- TestContainers + JUnit 5
- GitHub Actions (CI/CD)
- Railway/Render deployment

## Project Structure

```
modules/
├── location/         # Location domain (coordinates, addresses, distance)
├── analysis/         # Investment analysis domain (ROI, metrics)
├── scraping/         # Scraping orchestration domain (jobs, properties)
└── shared-kernel/    # Common DDD primitives

application/
├── api/              # REST controllers
├── config/           # Spring configuration
└── resources/
    └── db/migration/ # Flyway SQL migrations
```

## Running Locally

```bash
# Start infrastructure (PostgreSQL, Redis, RabbitMQ)
docker-compose up -d

# Build and run the application
./gradlew bootRun

# Or with specific profile
./gradlew bootRun --args='--spring.profiles.active=dev'
```

The API runs on `http://localhost:8080`.

**Swagger UI**: `http://localhost:8080/swagger-ui.html`  
**RabbitMQ Management**: `http://localhost:15672` (str_user/str_password)

## Environment Variables

Create a `.env` file (or export in terminal):

```bash
MAPBOX_SECRET_TOKEN=your_token_here
DATABASE_URL=jdbc:postgresql://localhost:5432/str_platform
REDIS_HOST=localhost
RABBITMQ_HOST=localhost
```

## API Endpoints

```
GET  /api/health                      - Health check
GET  /api/locations/nearby            - Find investment opportunities
POST /api/locations/search            - Search locations
POST /api/analysis                    - Run investment analysis
GET  /actuator/health                 - Spring actuator health
GET  /actuator/prometheus             - Metrics for monitoring
```

## Testing

```bash
# Unit tests
./gradlew test

# Integration tests (with TestContainers)
./gradlew integrationTest

# All tests + coverage
./gradlew build jacocoTestReport
```

TestContainers spins up real PostgreSQL and Redis in Docker for integration tests. No mocks for infrastructure.

## Building for Production

```bash
# Build JAR
./gradlew build

# Build Docker image
docker build -t str-backend:latest .

# Run container
docker run -p 8080:8080 \
  -e DATABASE_URL=$DATABASE_URL \
  -e REDIS_HOST=$REDIS_HOST \
  -e MAPBOX_SECRET_TOKEN=$MAPBOX_SECRET_TOKEN \
  str-backend:latest
```

## Deployment

Configured for Railway/Render. Push to GitHub, they detect the Dockerfile and deploy automatically.
