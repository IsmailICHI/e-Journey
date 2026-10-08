# Gateway Infrastructure

The gateway is the public API edge. User, travel, and payment services own their data and CRUD behavior; the gateway verifies JWTs, enforces admin access on `/api/v1/admin/**`, and forwards those requests to the owning service.

## Local Compose

From the repository root:

```sh
cp infrastructure/docker/.env.example infrastructure/docker/.env
```

Replace both sample secrets with unique random values, then start the gateway and its private Redis dependency:

```sh
docker compose --env-file infrastructure/docker/.env -f infrastructure/docker/docker-compose.yaml up --build
```

The gateway listens on `http://localhost:8089`; management health checks are on port `8090` inside the Compose network and are not published. The service URLs are configurable through the environment. Until those services exist and are running, their routes will return upstream errors.

## K3s

The K3s manifests under `k3s/manifests/services/gateway` provide two gateway replicas, cluster-internal service discovery, persistent Redis, resource requests, CPU/memory HPA, and a Traefik ingress. Install the `kubernetes.core` Ansible collection and ensure `kubectl` points at the target cluster:

```sh
ansible-galaxy collection install -r infrastructure/ansible/requirements.yml
GATEWAY_IMAGE=registry.example.com/e-journey/gateway:TAG JWT_SECRET='replace-with-32-or-more-random-characters' REDIS_PASSWORD='replace-with-a-unique-password' ansible-playbook -i infrastructure/ansible/inventory/localhost.ini infrastructure/ansible/deploy-gateway.yml
```

Replace the example registry, host, and CORS origin in the deployment manifest before exposing the service. Create the `gateway-tls` Kubernetes Secret with a certificate valid for the ingress host. The current Vagrant cluster has one server and one worker, so it is a development cluster rather than a highly available control plane. Install/configure Kubernetes Metrics Server for HPA to scale replicas.

Build and push an image with `GATEWAY_IMAGE=registry.example.com/e-journey/gateway:TAG infrastructure/k3s/scripts/build_gateway.sh` after authenticating to that registry.

## Jenkins and SonarQube

Configure the Jenkins multibranch pipeline to use `infrastructure/jenkins/jenkinsfile`, install Docker and the SonarQube Jenkins integration on the agent/controller, and define a SonarQube server named `SonarQube`. Add a Jenkins username/password credential named `container-registry` with permission to push to the configured GHCR repository. Replace `IMAGE_REPOSITORY` in the pipeline with that repository. Pull requests run tests, analysis, and an image build; only `main` publishes the image.

The ingress hostname, CORS allow-list, image repository, TLS certificate, JWT signing secret, and Redis password are deployment inputs. Do not commit actual credentials, kubeconfigs, or TLS private keys. Kubernetes Secrets are not encrypted at rest by default; enable cluster encryption or integrate Vault before using real credentials in a production cluster.