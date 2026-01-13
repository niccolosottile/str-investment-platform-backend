# STR Platform Backend

Spring Boot backend for the STR Investment Platform. Orchestrates data scraping, handles market analysis, and provides REST APIs.

## Architecture

**Modular Monolith**: DDD bounded contexts organized as Gradle modules (location, analysis, scraping, shared-kernel).

**Event-Driven Communication**: RabbitMQ for async scraping orchestration. Backend publishes scraping job events, Python workers consume and process them, results flow back through completion events.

**Distributed Caching**: Redis for performance optimization and reducing external API calls.

**Bounded Contexts**: Each module encapsulates its own domain logic with clear boundaries. Modules communicate through domain events and well-defined interfaces.

## Tech Stack

- Java 21 LTS
- Spring Boot 3.2.x
- PostgreSQL 16 + Spring Data JPA
- Redis 7
- RabbitMQ
- Gradle (Kotlin DSL)
- Docker + Docker Compose
- TestContainers + JUnit 5

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
# Start infrastructure
docker-compose up -d

# Build and run
./gradlew bootRun

# With specific profile
./gradlew bootRun --args='--spring.profiles.active=dev'
```

API runs on `http://localhost:8080`.

**Swagger UI**: `http://localhost:8080/swagger-ui.html`  
**RabbitMQ Management**: `http://localhost:15672` (str_user/str_password)

## Environment Variables

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
GET  /actuator/prometheus             - Metrics
```

## Testing

```bash
# Unit tests
./gradlew test

# Integration tests (TestContainers)
./gradlew integrationTest

# All tests + coverage
./gradlew build jacocoTestReport
```

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
