# Jenkins and SonarQube

This Compose project is separate from the application stack. It starts Jenkins,
SonarQube Community Build, and a dedicated PostgreSQL database for SonarQube.
The Angular client remains local and is not built or served by these containers.

## Configure and start

Add a strong, unique `SONAR_DB_PASSWORD` to the repository-root `.env` file.
Do not reuse the application database password. On Linux, set
`DOCKER_SOCKET_GID` to the group ID that owns `/var/run/docker.sock` if it is
not accessible to the Jenkins container's default group. The Docker socket
grants Jenkins control over the host Docker daemon, so only trusted Jenkins
administrators should have access.

From the repository root:

```powershell
docker compose -f infrastructure/ci-platform/docker-compose.yaml up -d --build
```

Jenkins is available at `http://127.0.0.1:8080` and SonarQube at
`http://127.0.0.1:9000`. Jenkins installs the plugins listed in
`jenkins/plugins.txt` when its image is built. The Jenkins home and SonarQube
data are retained in named volumes.

## Jenkins configuration

1. Unlock Jenkins using the initial administrator password shown in the Jenkins
   container logs, then create an administrator account.
2. Under **Manage Jenkins > System**, add a SonarQube installation named
   `SonarQube` with server URL `http://sonarqube:9000` and a SonarQube token
   stored in Jenkins credentials.
3. Under **Manage Jenkins > Tools**, configure a SonarScanner installation
   named `SonarScanner` (install automatically or provide its local path).
4. Create a Pipeline job pointing to this repository and its root `Jenkinsfile`.
   Docker-backed Maven agents join the `travelplan-ci` network to reach
   SonarQube.

The pipeline runs `clean verify` for the gateway, user, and travel services,
then uploads the JaCoCo XML reports to SonarQube. The payment service is not
included. Configure the project key `e-journey-backend` and quality gates in
SonarQube as desired.

## Backend coverage and containers

Each included backend Maven project creates JaCoCo XML at
`services/<service>/target/site/jacoco/jacoco.xml` during `verify`. Run the
application stack separately from the repository root with
`docker compose up -d --build`. It starts PostgreSQL, Redis, Neo4j, and the
gateway, user, and travel services; the Angular client is intentionally not
part of Compose.
