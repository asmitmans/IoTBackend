# IoT Backend

REST API for managing IoT devices and telemetry data.

Built with **Spring Boot 3.5**, **PostgreSQL**, **Flyway**, and **Docker**.

---

## Prerequisites

- Java 21+
- Maven 3.9+
- Docker with Compose V2

> **Docker installation:** Use the official method to ensure Compose V2 is included:
> https://docs.docker.com/engine/install/ubuntu/
>
> Verify with: `docker compose version`
>
> If your system only has Compose V1 (installed via `apt install docker-compose`),
> replace `docker compose` with `docker-compose` in all commands below.

---

## Quickstart

### 1. Clone repository
```bash
git clone <repo-url>
cd IoTBackend
```

### 2. Configure environment
```bash
cp .env.example .env
```

> `.env.example` includes working default credentials for local development.
> Edit `.env` before deploying to any non-local environment.

### 3. Start database
```bash
docker compose up -d
```

### 4. Run application
```bash
./mvnw spring-boot:run
```

---

## Verify service
```bash
curl http://localhost:8080/actuator/health
```
Expected response:
```
{"status":"UP"}
```

---

## Troubleshooting

### Credentials error / database won't connect

If you changed `.env` after the container was already created, PostgreSQL keeps
the original credentials stored in the volume. Drop the volume and recreate:

```bash
docker compose down -v
docker compose up -d
```

> ⚠️ This deletes all local database data.

---

## Database

Database schema is managed using **Flyway migrations**.

Migrations are located in:

```
src/main/resources/db/migration
```

---

## Security notes

- `.env` must **not** be committed to the repository.
- Change default credentials in `.env` before deploying to any non-local environment.
- API keys are stored **hashed** in the database.

---

## Tech stack

- Spring Boot 3.5
- Spring Data JPA
- Spring Security
- PostgreSQL
- Flyway
- Docker Compose
