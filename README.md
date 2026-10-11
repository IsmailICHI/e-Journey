# e-Journey

A travel booking application using Spring Boot and Angular.

## Run the admin application locally

For local development, start PostgreSQL and Redis in containers and run the
Spring services and Angular client directly on your machine. Docker Compose
reads the root `.env` file.

```powershell
docker compose up -d postgres redis
```

PostgreSQL is published on `127.0.0.1:5433` by default so it does not collide
with a locally installed PostgreSQL server on port 5432. Override
`POSTGRES_FORWARD_PORT` in the root `.env` file if needed.

On first startup, PostgreSQL creates `user_db` and `travel_db` in addition to
the database named by `POSTGRES_DB`. The application database initialization
script only runs when the PostgreSQL data volume is first created.

Open four PowerShell terminals from the repository root.

**User service** (set these values from `.env` first):

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5433/user_db"
$env:SPRING_DATASOURCE_USERNAME = "<POSTGRES_USER>"
$env:SPRING_DATASOURCE_PASSWORD = "<POSTGRES_PASSWORD>"
$env:REDIS_HOST = "127.0.0.1"
$env:REDIS_PORT = "6379"
$env:REDIS_PASSWORD = "<REDIS_PASSWORD>"
$env:JWT_SECRET = "<JWT_SECRET>"
mvn -f services/user-service/pom.xml spring-boot:run
```

**Travel service**:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5433/travel_db"
$env:SPRING_DATASOURCE_USERNAME = "<POSTGRES_USER>"
$env:SPRING_DATASOURCE_PASSWORD = "<POSTGRES_PASSWORD>"
$env:JWT_SECRET = "<JWT_SECRET>"
mvn -f services/travel-service/pom.xml spring-boot:run
```

**API gateway**:

```powershell
$env:JWT_SECRET = "<JWT_SECRET>"
$env:USER_SERVICE_URL = "http://localhost:8081"
$env:TRAVEL_SERVICE_URL = "http://localhost:8082"
$env:REDIS_HOST = "127.0.0.1"
$env:REDIS_PORT = "6379"
$env:REDIS_PASSWORD = "<REDIS_PASSWORD>"
$env:CORS_ALLOWED_ORIGINS = "http://localhost:4200,http://127.0.0.1:4200,http://localhost:4201,http://127.0.0.1:4201"
mvn -f services/gateway/pom.xml spring-boot:run
```

If Redis runs without a password, leave `REDIS_PASSWORD` unset. Keep Redis and
PostgreSQL bound to localhost for local development.

To run the backend services and their dependencies in containers instead, use:

```powershell
docker compose up -d --build
```

This starts PostgreSQL, Redis, Neo4j, and the gateway, user, and travel
services. The Angular client remains local. Service and dependency ports can
be changed with the corresponding `*_FORWARD_PORT` variables in `.env`; the
database URLs inside Compose use the internal service names.

Jenkins and SonarQube use their own Compose project and data volumes; see
[infrastructure/ci-platform/README.md](./infrastructure/ci-platform/README.md).

**Angular client**:

```powershell
npm.cmd --prefix services/client start -- --host 127.0.0.1
```

Open `http://127.0.0.1:4200/admin`. The route redirects to sign-in until an
administrator logs in. The user service seeds `admin@admin.com` with the
development password `Admin123456@@`; replace the seeded credentials before
using a non-local environment.

| Service | Local address |
| --- | --- |
| Admin dashboard | http://127.0.0.1:4200 |
| User service | http://localhost:8081 |
| Travel service | http://localhost:8082 |
| API gateway | http://localhost:8089 |
| Gateway health | http://localhost:8090/actuator/health |
