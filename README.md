# ERD Core

This is a Java-based project that uses Spring Boot and Maven. The project is designed to manage diagrams related to a project.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

## Running with Docker

This is the recommended way to run the full stack locally. The `docker-compose.yml` in this repository orchestrates all services: PostgreSQL, MongoDB, the backend (erd-core), and the frontend (erd-client).

1. **Start all services** from the `erd-core/` directory:

```bash
docker compose up --build
```

> The first build takes several minutes as Maven downloads all dependencies. Subsequent runs are faster thanks to the cached Maven volume.

### Useful commands

```bash
# Run in background
docker compose up -d

# View logs for a specific service
docker compose logs -f erd-core

# Stop all services
docker compose down

# Stop and remove all volumes (resets database data)
docker compose down -v

# Rebuild a specific service
docker compose up --build erd-core
```

## Running Locally (without Docker)

### Prerequisites

- Java 17
- Maven
- Spring Boot
- PostgreSQL and MongoDB running locally (use `docker-compose-local.yml` to spin up only the databases)

### Start only the databases

```bash
docker compose -f docker-compose-local.yml up -d
```

Then run the application:

```bash
mvn spring-boot:run
```

## Built With

- [Java](https://www.java.com/) - The programming language used
- [Spring Boot](https://spring.io/projects/spring-boot) - The framework used
- [Maven](https://maven.apache.org/) - Dependency management
