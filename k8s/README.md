# Kubernetes deployment for Travel-Plan

This directory contains the Kustomize manifests for the single-node
development deployment on k3s. The deployment includes the Angular frontend,
API gateway, user, travel, booking, and payment services, plus their
development dependencies.

## Architecture

```text
Browser
  │
  ▼
Traefik ingress
  ├─ /api/* → gateway
  └─ /*     → frontend
               │
               └─ gateway → user-service, travel-service,
                             booking-service, payment-service

booking-service → PostgreSQL, Kafka, travel-service
payment-service → PostgreSQL, booking-service, PayPal / Stripe
travel-service  → PostgreSQL, Neo4j, Elasticsearch
                  consumes booking events from Kafka
gateway / user-service → Redis
```

Only the frontend and gateway are exposed through the Ingress. The backend
Services, PostgreSQL, Redis, Neo4j, Kafka, and Elasticsearch are cluster
internal.

## Layout

- `base/` contains the common Kubernetes resources.
- `overlays/dev/` applies the development host, ingress, and image settings.
- `base/monitoring/prometheus-config.yaml` configures Spring Boot metrics
  scraping.

## Resources

The base includes:

- Deployments and internal Services for the frontend, gateway, and all four
  backend services.
- StatefulSets and persistent volume claims for PostgreSQL, Neo4j, Kafka, and
  Elasticsearch.
- Redis, the Traefik Ingress, ConfigMaps, monitoring resources, and HPAs for
  the gateway, user service, travel service, and payment service.

The booking service intentionally has no HPA yet. Its scheduled outbox
publisher currently selects unpublished events without a cross-replica claim
or lock, so multiple replicas could publish the same booking event. Make that
publisher safe for concurrent workers before enabling booking-service
autoscaling. PostgreSQL, Neo4j, Redis, Kafka, and Elasticsearch are stateful
dependencies and are not scaled with these application HPAs.

The HPAs target 70% average CPU utilization, with one to four replicas. They
require a functioning Kubernetes metrics API (provided by metrics-server in
the development k3s cluster).

The deployment uses a single Kafka broker and a single-node Elasticsearch
instance with Elasticsearch security disabled. These settings are for local
development only, not production.

## Secrets and configuration

The application Secret is named `travel-plan-secrets`. The manual development
playbook applies
`infrastructure/ansible/files/travel-plan-secrets.yaml`, which contains
development-only values. For another cluster, create a Secret with real values
instead of applying [`base/secrets.example.yaml`](base/secrets.example.yaml).
Do not commit credentials or private keys.

The Jenkins deployment uses the Jenkins file credential
`travel-plan-env-file` to create the Secret. It must include the application's
existing database, JWT, Redis, and Neo4j values, plus
`BOOKING_PAYMENT_CONFIRMATION_SECRET`. Use the same confirmation secret for
the booking and payment services. Supply PayPal and/or Stripe credentials when
that provider is enabled; empty provider credentials leave that provider
unconfigured. Local Docker Compose continues to use the ignored root `.env`.

Non-secret service URLs, database URLs, Kafka settings, Elasticsearch URL,
provider URLs, pricing conversion, and the payment return base URL are in
`base/configmap.yaml`. The dev payment return URL is
`https://travel-plan.local:8443/payment/...`.

## Provision and deploy the development cluster

The cluster runs in the VirtualBox VM from the root `Vagrantfile`. Follow
[`infrastructure/ansible/README.md`](../infrastructure/ansible/README.md) to
create and provision it. The install playbook sets
`vm.max_map_count=262144`, which Elasticsearch requires. Provisioning also
requires the development TLS certificate and key in the host-mounted certs
directory.

From the repository root, deploy the manual development stack:

```bash
ansible-playbook -i infrastructure/ansible/inventories/dev/hosts.yml \
  infrastructure/ansible/playbooks/deploy-app.yml
```

This path builds and imports the development images, applies the development
Secret and Kustomize overlay, creates any missing PostgreSQL databases, and
waits for the stateful dependencies and application Deployments. The Jenkins
pipeline uses the tested workspace images and the Jenkins-managed `.env`
credential instead.

## Access the application

The dev Ingress accepts HTTPS for `travel-plan.local`. Add
`127.0.0.1 travel-plan.local` to the workstation hosts file and trust the
development certificate as appropriate, then open:

```text
https://travel-plan.local:8443
```

The Vagrantfile forwards the VM's HTTPS port 443 to workstation port 8443 by
default. The browser calls the gateway through the same-origin `/api/...` path.
Prometheus and Grafana are exposed on the forwarded NodePorts
`http://localhost:30090` and `http://localhost:30030`.

## Validate and inspect

Render the manifests and validate the deployed cluster:

```bash
kubectl kustomize k8s/overlays/dev
ansible-playbook -i infrastructure/ansible/inventories/dev/hosts.yml \
  infrastructure/ansible/playbooks/validate-cluster.yml
```

Prometheus scrapes `/actuator/prometheus` from the gateway (management port
8090), user service (8081), travel service (8082), booking service (8085), and
payment service (8083).

Useful cluster checks, run inside the VM:

```bash
vagrant ssh k3s-server
sudo k3s kubectl -n travel-plan get pods,services,pvc
sudo k3s kubectl -n travel-plan get statefulsets
sudo k3s kubectl -n travel-plan get hpa
sudo k3s kubectl -n travel-plan get ingress
```

To inspect logs:

```bash
sudo k3s kubectl -n travel-plan logs deploy/gateway
sudo k3s kubectl -n travel-plan logs deploy/booking-service
sudo k3s kubectl -n travel-plan logs deploy/payment-service
sudo k3s kubectl -n travel-plan logs deploy/travel-service
sudo k3s kubectl -n travel-plan logs kafka-0
sudo k3s kubectl -n travel-plan logs elasticsearch-0
```

## Production considerations and limitations

- This is a single-node development cluster, not a highly available
  deployment. Kafka and Elasticsearch have no production-grade HA or security
  configuration here.
- Use managed or secured Kafka/Elasticsearch and a suitable PostgreSQL and
  storage platform for production. Do not expose internal data services
  publicly.
- Use a managed secret store or equivalent production secret-management
  process; never reuse the development Secret.
- Configure real DNS and TLS certificates for production. Do not commit
  private keys.
- The default Vagrant VM has 4 GiB RAM and 2 CPUs. Increase
  `K3S_VM_MEMORY` if the added services cannot be scheduled or become
  memory-pressured.
- HPAs scale only the gateway, user service, and travel service; they do not
  scale worker nodes.
- The older scripts under `k3s/` refer to another application and are not used
  by this deployment.

## Safe cleanup

```bash
vagrant ssh k3s-server -- sudo k3s kubectl delete -k /vagrant/k8s/overlays/dev
```

Deleting the application resources does not normally delete persistent volume
claims. Delete database or broker claims only with a deliberate data-backup
plan.
