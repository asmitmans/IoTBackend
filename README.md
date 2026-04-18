# IoT Backend

REST API for managing IoT devices and telemetry data.

Built with **Spring Boot 3.5**, **PostgreSQL**, **Flyway**, and **Docker**.

---

## Prerequisites

- Docker & Docker Compose
- Java 21+
- Maven 3.9+

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
Edit `.env` if needed.

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

## Database

Database schema is managed using **Flyway migrations**.

Migrations are located in:

```
src/main/resources/db/migration
```

---

## Security notes

- `.env` must **not** be committed to the repository.
- API keys are stored **hashed** in the database.

---

## Tech stack

- Spring Boot 3.5
- Spring Data JPA
- Spring Security
- PostgreSQL
- Flyway
- Docker Compose

---
