# Infrastructure changes needed for booking, payment, and travel search

## Scope and summary

This report compares the current local Docker Compose setup with the active
Kubernetes/Kustomize and Jenkins/Ansible deployment paths. It describes
recommended follow-up work only; no infrastructure resources are changed here.

**Main finding:** Docker Compose already provisions the new backend dependencies
and services. The active k3s deployment does not: it currently deploys only the
frontend, gateway, user service, and travel service. The Kubernetes manifests,
CI image pipeline, Ansible deployment, and deployment documentation need to be
brought into alignment.

## Step 1 decision: payment secret source

Use the existing Jenkins-managed `.env` file credential (`travel-plan-env-file`)
as the source for k3s deployment secrets. The existing CI job installs that
credential as the workspace `.env`, and Ansible creates the Kubernetes Secret
from environment values without committing them to the repository. The ignored
root `.env` remains the source for local Docker Compose development. The
booking/payment confirmation secret is required for k3s deployment; provider
credentials may be empty when that provider is not configured.

## What is already covered

- [`docker-compose.yaml`](./docker-compose.yaml) already defines the booking
  and payment services, Kafka, Elasticsearch, PostgreSQL, Neo4j, and the other
  existing services. Kafka, Elasticsearch, and the databases have persistent
  Compose volumes and health checks.
- [`db/init-databases.sql`](./db/init-databases.sql) already creates
  `booking_db` and `payment_db` as well as the user and travel databases.
- Compose already supplies the payment provider configuration and shared
  booking/payment confirmation secret through environment variables.
- The gateway's [`application.yaml`](./services/gateway/src/main/resources/application.yaml)
  already routes booking and payment API paths to their respective services.
  The existing Kubernetes ingress can continue routing `/api` to the gateway;
  the backing services should remain cluster-internal.

These are not reasons to add duplicate services to Compose. Revisit Compose
only if a future backend change introduces a new dependency or configuration
variable not already represented there.

## Kubernetes changes recommended

### 1. Add the missing workloads and internal Services

Add Deployment and ClusterIP Service manifests for:

- `booking-service` on port `8085`
- `payment-service` on port `8083`

Add Kubernetes resources for the new travel-service dependencies:

- Kafka, reachable as `kafka:9092`
- Elasticsearch, reachable as `http://elasticsearch:9200`

For the existing single-node development cluster, single-node Kafka and
Elasticsearch with persistent storage are sufficient. Keep their Services
internal; do not expose their ports through the public Ingress. For production,
use appropriately secured and operated Kafka and Elasticsearch services rather
than treating single-node development manifests as an HA deployment.
Elasticsearch also requires `vm.max_map_count=262144` on the Kubernetes node;
the k3s install playbook now configures this host setting.

**Files to add:**

- `k8s/base/deployment/booking.deployment.yaml`
- `k8s/base/services/booking.service.yaml`
- `k8s/base/deployment/payment.deployment.yaml`
- `k8s/base/services/payment.service.yaml`
- Kafka and Elasticsearch workload and Service manifests under
  `k8s/base/` (use a StatefulSet or another persistent-storage design where
  appropriate)

**File to update:** `k8s/base/kustomization.yaml`, to include all new
resources.

### 2. Configure all workloads with their backend dependencies

The current Kubernetes gateway manifest sets user and travel service URLs but
does not set `BOOKING_SERVICE_URL` or `PAYMENT_SERVICE_URL`. The travel
deployment does not currently receive its Kafka, Neo4j, or Elasticsearch
configuration. Add the corresponding environment variables to the gateway
and travel deployments, and configure the new booking/payment deployments
with their database and inter-service URLs.

Use the existing in-cluster names and backend ports, including:

- `BOOKING_SERVICE_URL=http://booking-service:8085` in the gateway and
  payment-service
- `PAYMENT_SERVICE_URL=http://payment-service:8083`
- `KAFKA_BOOTSTRAP_SERVERS=kafka:9092`
- `BOOKING_EVENTS_TOPIC=booking.created.v1`
- `NEO4J_URI=bolt://neo4j:7687`
- `ELASTICSEARCH_URL=http://elasticsearch:9200`
- `TRAVEL_SERVICE_URL=http://travel-service:8082` in booking-service

Put non-secret values in `k8s/base/configmap.yaml` and reference them from
the Deployments. Keep credentials and shared secrets in Kubernetes Secrets.
The deployment manifests should have readiness/liveness probes and
resource requests/limits consistent with the existing service deployments.

### 3. Supply payment credentials safely

Payment requires provider credentials and a shared secret used to confirm a
booking after payment. Add Secret keys for PayPal client ID/secret, Stripe
secret key, and the shared booking/payment confirmation secret. Add any
non-secret provider URLs, PayPal MAD-per-USD rate, and frontend return base URL
to the ConfigMap or deployment environment as appropriate.

**Files to update:**

- `k8s/base/secrets.example.yaml` — document the required key names with
  obvious placeholders only.
- `infrastructure/ansible/roles/k3s/tasks/deploy.yml` — create the additional
  Secret entries from the Jenkins-provided environment.
- `infrastructure/ansible/playbooks/deploy-app.yml` and
  `infrastructure/ansible/files/travel-plan-secrets.yaml` — update the manual
  development deployment's Secret inputs as well.

Do not commit real credentials to the example manifest or Ansible files.

### 4. Align database initialization and deployment ordering

The PostgreSQL bootstrap SQL at the repository root already creates all four
service databases. However, the PostgreSQL manifest embeds a separate
`postgres-init` ConfigMap in
`k8s/base/db/deployment/pg.deployment.yaml` that creates only `user_db` and
`travel_db`. The Ansible deployment currently also creates only those two
databases.

Update the Kubernetes PostgreSQL initialization to include `booking_db` and
`payment_db`, preferably keeping it aligned with
[`db/init-databases.sql`](./db/init-databases.sql). Update the Ansible
idempotent database-creation list to include all four databases. This matters
for existing persistent volumes: PostgreSQL's container init scripts run only
when the data directory is first initialized, so retaining the Ansible
idempotent creation step (or replacing it with an explicit migration/job) is
necessary for already-provisioned clusters.

Confirm the workloads are not marked ready until PostgreSQL and their
dependencies are available. The travel service should wait for Kafka,
Neo4j, and Elasticsearch; booking should wait for PostgreSQL and Kafka;
payment should wait for PostgreSQL and booking-service availability as
appropriate.

### 5. Update CI image building, importing, and rollout checks

The Jenkins pipeline currently tests, builds, archives, and deploys only the
frontend, gateway, user service, and travel service. Extend
[`Jenkinsfile`](./Jenkinsfile) to test and build/archive the booking and
payment images. Extend
`infrastructure/ansible/roles/k3s/tasks/deploy.yml` and
`infrastructure/ansible/playbooks/deploy-app.yml` to import those images,
wait for the new Deployments, and create the required databases.

The frontend Dockerfile and package are under `services/client/`, matching the
paths used by Jenkins and the manual Ansible deployment.

### 6. Update monitoring and operational documentation

Add booking and payment to
`k8s/base/monitoring/prometheus-config.yaml` using their Prometheus actuator
endpoints and actual ports. Update `k8s/README.md` to describe the expanded
service topology, new dependencies, configuration/secrets, deployment checks,
and the distinction between local single-node development resources and
production operations.

## Suggested implementation order

1. **Done:** Use the Jenkins-managed `.env` credential for k3s secrets and the
   ignored root `.env` for local Compose. Pass payment credentials and the
   shared confirmation secret into the Kubernetes Secret.
2. **Done:** Add Kafka/Elasticsearch and booking/payment Kubernetes resources,
   register them in Kustomize, and configure the required Elasticsearch host
   sysctl. The dev resources are single-node and cluster-internal.
3. **Done:** Configure service URLs, event/search dependencies, Secrets, and
   database creation in Kubernetes and Ansible.
4. **Done:** Extend Jenkins and Ansible tests, image build/archive/import, and
   rollout steps for booking and payment.
5. **Done:** Add booking/payment metrics targets and update the Kubernetes
   deployment and operations documentation.
6. Validate the rendered manifests with `kubectl kustomize
   k8s/overlays/dev`, then deploy and check pod readiness, service discovery,
   event publishing/consumption, travel search, and the payment return/booking
   confirmation flow.

## Out of scope for this report

This report does not propose changes to the backend application code or
payment-provider behavior. It also does not recommend publishing database,
Kafka, or Elasticsearch endpoints outside the cluster.
