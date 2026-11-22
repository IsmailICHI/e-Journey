# Kubernetes deployment for Travel-Plan

This directory contains a single-node k3s development deployment for the Angular frontend, API gateway, user service, and travel service. It does not deploy the booking or payment services, Kafka, or Elasticsearch; booking, payment, search, and event-driven recommendations are therefore not fully available in this Kubernetes environment. The full implementations are wired in the local Docker Compose stack.

## Architecture

```text
Browser
  │
  ▼
Traefik ingress (k3s)
  ├─ /api/*  → gateway (Spring Cloud Gateway)
  └─ /*      → frontend (Angular static site)
                 │
                 ├─ user-service (ClusterIP)
                 ├─ travel-service (ClusterIP)
                 └─ redis (ClusterIP)
                          │
                          └─ PostgreSQL + Neo4j (private StatefulSets)
```

The frontend runs as a static web application behind Traefik, and the browser uses the same-origin path `/api/...` so the Angular app does not need to know an internal Kubernetes DNS name. The gateway is the single public API entry point and forwards requests to the user and travel microservices via internal service DNS names such as `http://user-service:8081`.

## Directory layout

- `base/` contains the Kubernetes resource set assembled by Kustomize.
- `overlays/dev/` applies environment-specific values for a local k3s development cluster.
- `README.md` describes how the resources fit together and how to validate the rollout.

## Base resources

The base layer creates:

- the `travel-plan` namespace,
- a shared ConfigMap for environment values,
- a `Secret` template with placeholders,
- Redis and the application workloads,
- PostgreSQL and Neo4j StatefulSets with persistent volumes,
- Services for service discovery,
- a Traefik Ingress that routes traffic to the frontend and gateway,
- CPU-based HPAs for the stateless application Deployments.

## Why Kustomize is used

Kustomize is included in the standard Kubernetes tooling and lets us keep a single base definition while applying only the needed differences for dev or staging. Instead of copying almost identical manifests across deployments, we prefer a single base and environment-specific overlays.

## Development prerequisites

The repository's development cluster runs in the VirtualBox VM from the root `Vagrantfile`. Create and provision it using [the Ansible VM instructions](../infrastructure/ansible/README.md). k3s provides the cluster API and Traefik ingress. The Ansible deploy playbook copies and applies the development Secret from `infrastructure/ansible/files/travel-plan-secrets.yaml` before deploying workloads.

## Create the required secrets

For this learning setup, Ansible applies the development Secret automatically. The file uses dummy credentials and must not be used in a production environment. To create an initial admin, set `BOOTSTRAP_ADMIN_EMAIL` and `BOOTSTRAP_ADMIN_PASSWORD` in the CI deployment environment, or add both `bootstrap-admin-email` and `bootstrap-admin-password` keys to the Secret used by the manual playbook. Without both, automatic admin provisioning is disabled. For another cluster, create a Secret with suitable credentials rather than applying the example placeholders:

```bash
cp k8s/base/secrets.example.yaml /tmp/travel-plan-secrets.yaml
# edit /tmp/travel-plan-secrets.yaml
kubectl -n travel-plan apply -f /tmp/travel-plan-secrets.yaml
```

The example manifest intentionally uses obvious placeholders and must never be used in a real environment without replacement.

## Deploy the development overlay

```bash
ansible-playbook -i infrastructure/ansible/inventories/dev/hosts.yml infrastructure/ansible/playbooks/deploy-app.yml
```

The Ansible playbook builds and imports the development images in the VM, then applies this overlay. The dev ingress uses `travel-plan.local` and TLS; use `scripts/https.sh` to prepare a trusted local certificate and map that host to `127.0.0.1`.

## Validate the manifest set

```bash
kubectl kustomize k8s/overlays/dev
ansible-playbook -i infrastructure/ansible/inventories/dev/hosts.yml infrastructure/ansible/playbooks/validate-cluster.yml
```

## Troubleshooting

### Service-to-service connectivity

Use the cluster DNS names, not `localhost` from a Pod. Run cluster commands inside the VM:

```bash
vagrant ssh k3s-server
sudo k3s kubectl -n travel-plan run netcat --rm -it --image=busybox:1.36 -- sh
# inside the diagnostic Pod
nslookup user-service
nslookup travel-service
nslookup redis
```

### Check application health

```bash
sudo k3s kubectl -n travel-plan get pods
sudo k3s kubectl -n travel-plan logs deploy/gateway
sudo k3s kubectl -n travel-plan logs deploy/user-service
sudo k3s kubectl -n travel-plan logs deploy/travel-service
```

### Inspect HPA

```bash
sudo k3s kubectl -n travel-plan get hpa
sudo k3s kubectl -n travel-plan describe hpa gateway
sudo k3s kubectl -n travel-plan describe hpa user-service
sudo k3s kubectl -n travel-plan describe hpa travel-service
```

### Inspect ingress routing

```bash
sudo k3s kubectl -n travel-plan get ingress
sudo k3s kubectl -n travel-plan describe ingress travel-plan
```

## Local Access

The Vagrantfile forwards HTTP port 80 to `localhost:8088` and HTTPS port 443 to `localhost:8443`. The active ingress uses HTTPS, so open `https://travel-plan.local:8443`; the browser calls `/api/...`, which Traefik forwards to the gateway service.

## TLS and production notes

The ingress already declares the `travel-plan-local-tls` Secret, and Ansible provisions it from the certificate files created by `scripts/https.sh`. This is a local development certificate, not a production certificate. Use a valid certificate and managed secret for production, and never commit private keys.

## Safe cleanup

The normal cleanup command is:

```bash
vagrant ssh k3s-server -- sudo k3s kubectl delete -k /vagrant/k8s/overlays/dev
```

This removes the application resources while leaving the persistent database volumes intact unless you explicitly delete the PVCs. In production, delete database PVCs only with a deliberate backup plan because they contain the application data.

## Known limitations

- This is a single-node development deployment, not a multi-node HA control plane.
- Local-path storage is convenient for a dev cluster but is not a substitute for enterprise storage.
- The HPA only scales application replicas; it does not scale the worker nodes themselves.
- Booking, payment, Kafka, and Elasticsearch are not deployed in this k3s setup; deploy those services before using the corresponding product flows here.
- The older scripts under `k3s/` are not used by this Travel-Plan deployment; they refer to a different billing/inventory application layout.
