# e-Journey

e-Journey (also referred to as Travel-Plan in the deployment configuration) is
a travel discovery and administration application. It combines an Angular
client with Spring Boot services for user accounts and travel listings, with a
Spring Cloud Gateway as the API entry point.

## What the application includes

- Public travel listing and an Angular home page.
- User registration, sign-in, and profile pages.
- A role-protected administration area for managing travel listings and users.
- JWT-based authentication, Redis-backed gateway rate limiting, and health and
  Prometheus metrics endpoints.
- Local Docker Compose and a single-node k3s development deployment.

The payment and recommendation paths are configured in the gateway, but those
services are not part of the active local Compose or Kubernetes application
deployment. Payment is intentionally excluded from the current CI/CD pipeline
and k3s manifests.

## Architecture

```text
Angular client
     │
     ▼
Spring Cloud Gateway ───── Redis
     ├── User service ───── PostgreSQL (user_db)
     └── Travel service ─── PostgreSQL (travel_db)

Development infrastructure also provides Neo4j, Jenkins, SonarQube,
and a single-node k3s cluster.
```

The gateway runs on port `8089` and forwards API requests to the user and
travel services. The Angular client runs on port `4200`. For Kubernetes
development, Traefik exposes the frontend and gateway through port `8088`.

## Repository layout

| Path | Purpose |
| --- | --- |
| `services/client/` | Angular client |
| `services/user-service/` | Registration, authentication, profiles, and admin user operations |
| `services/travel-service/` | Public travel listings and admin travel operations |
| `services/gateway/` | API routing, JWT security, CORS, and Redis rate limiting |
| `services/payment-service/` | Additional service code; not included in the active deployment |
| `db/` | PostgreSQL initialization scripts |
| `infrastructure/ansible/` | Provisioning and deployment automation for the development VM |
| `infrastructure/ci-platform/` | Jenkins, SonarQube, and their database Compose configuration |
| `k8s/` | Kubernetes base manifests and the development overlay |
| `scripts/` | Ansible and Vagrant installation helper scripts |
| `docker-compose.yaml` | Local PostgreSQL, Redis, Neo4j, and application stack |
| `Jenkinsfile` | CI checks, image builds, SonarQube quality gate, and deployment |
| `Vagrantfile` | Single-node k3s development VM |

## Prerequisites

For local development:

- Java 21 and Maven
- Node.js and npm
- Docker Desktop with Docker Compose

For the VM-based Kubernetes and CI platform, also install Vagrant, VirtualBox,
and Ansible. See the [Ansible and k3s guide](./infrastructure/ansible/README.md)
for the workstation and VM requirements.

## Configure the environment

Create a local environment file from the example and edit it before starting
the stack:

```powershell
Copy-Item .env.example .env
```

Replace the example credentials with local values. In particular, set a unique
`JWT_SECRET` of at least 32 bytes and strong PostgreSQL and Redis passwords.
Keep `.env` private; it is ignored by Git. Compose requires `JWT_SECRET` when
starting the application services.

The database container creates `user_db` and `travel_db` on first
initialization. Its initialization script runs only when the PostgreSQL data
volume is first created. The default host port for PostgreSQL is `5433`; set
`POSTGRES_FORWARD_PORT` in `.env` to change it.

## Run locally

### Start the local dependencies

From the repository root, start PostgreSQL and Redis:

```powershell
docker compose up -d postgres redis
```

The database and Redis ports are bound to localhost. If Redis has no password,
leave `REDIS_PASSWORD` unset for the services.

### Start the backend services

Open separate PowerShell terminals from the repository root. Use the matching
database credentials and JWT secret from `.env` for the environment variables
below.

**User service**:

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

### Start the Angular client

In another terminal:

```powershell
npm.cmd --prefix services/client ci
npm.cmd --prefix services/client start -- --host 127.0.0.1
```

Open [http://127.0.0.1:4200](http://127.0.0.1:4200). The main routes include
`/home`, `/login`, `/register`, `/profile`, and `/admin`. The administration
route requires an administrator account.

For local development, the user service creates an initial administrator if
one does not already exist:

```text
Email:    admin@admin.com
Password: Admin123456@@
```

This is a development-only account. Change or remove its credentials before
using any non-local environment.

### Run the full application stack in containers

After configuring `.env`, build and start all services defined by the root
Compose file:

```powershell
docker compose up -d --build
```

This starts PostgreSQL, Redis, Neo4j, the gateway, user service, and travel
service. The Angular client can still be run locally using the commands above.
Service and dependency host ports can be changed using the corresponding
`*_FORWARD_PORT` variables in `.env`; service-to-service URLs inside Compose
use Compose service names.

To stop the local Compose stack:

```powershell
docker compose down
```

The named database volumes are retained. To reset database data, deliberately
remove the relevant volumes with `docker compose down --volumes`.

## Build and test

Run backend verification for all three active services from the repository
root:

```powershell
mvn -B -f services/gateway/pom.xml clean verify
mvn -B -f services/user-service/pom.xml clean verify
mvn -B -f services/travel-service/pom.xml clean verify
```

Build and test the Angular client:

```powershell
npm.cmd --prefix services/client ci
npm.cmd --prefix services/client run build
npm.cmd --prefix services/client test -- --watch=false
```

The CI pipeline runs the backend Maven verifications and Angular tests, builds
the application images, checks the SonarQube quality gate, and deploys
successful `main` builds. See the
[CI platform guide](./infrastructure/ci-platform/README.md) for Jenkins setup
and pipeline details.

## Kubernetes development environment

The repository's single-node k3s development cluster runs in the VirtualBox VM
defined by [`Vagrantfile`](./Vagrantfile). Ansible provisions the VM and
deploys the application. The active Kubernetes workload includes the
frontend, gateway, user service, travel service, Redis, PostgreSQL, Neo4j,
Traefik ingress, and CPU-based HPAs. Payment is not deployed.

The high-level workflow is:

```bash
vagrant up k3s-server
ANSIBLE_CONFIG=infrastructure/ansible/ansible.cfg ansible-playbook infrastructure/ansible/playbooks/install-k3s.yml
ANSIBLE_CONFIG=infrastructure/ansible/ansible.cfg ansible-playbook infrastructure/ansible/playbooks/configure-k3s.yml
ANSIBLE_CONFIG=infrastructure/ansible/ansible.cfg ansible-playbook infrastructure/ansible/playbooks/ci-platform.yml
```

On the workstation, Jenkins is available at `http://127.0.0.1:8080`,
SonarQube at `http://127.0.0.1:9000`, and the app at `http://localhost:8088`.
The manual development deployment is also available through
`infrastructure/ansible/playbooks/deploy-app.yml`.

For the complete VM workflow, credentials, deployment commands, and cluster
validation, see the
[Ansible guide](./infrastructure/ansible/README.md) and the
[Kubernetes guide](./k8s/README.md).

## Service endpoints

| Service | Local address |
| --- | --- |
| Angular client | http://127.0.0.1:4200 |
| User service | http://localhost:8081 |
| Travel service | http://localhost:8082 |
| API gateway | http://localhost:8089 |
| Gateway health | http://localhost:8090/actuator/health |
| PostgreSQL | localhost:5433 |
| Redis | localhost:6379 |
| Neo4j browser | http://localhost:7474 |

The gateway and backend services expose health and Prometheus endpoints through
Spring Boot Actuator. The gateway health endpoint is published on port `8090`.

## Security notes

- Treat `.env`, JWT signing secrets, and deployment credentials as secrets; do
  not commit them.
- The default administrator credentials and example environment values are
  only for local development. Replace them before using a shared environment.
- The supplied Vagrant, Kubernetes, and CI setup is a development environment,
  not a production-hardened deployment. Review its credentials, network
  exposure, storage, and TLS configuration before adapting it for production.

## Copyright

Copyright © Louhab Ali && Ismail Ichi.
