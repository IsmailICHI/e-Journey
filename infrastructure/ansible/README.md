# Ansible and k3s VM

Ansible runs on the workstation and connects over SSH to the Ubuntu VM created by the repository-root `Vagrantfile`. It installs and configures k3s inside that VM; it does not install software on the workstation and does not require workstation sudo.

## Prerequisites

- VirtualBox, Vagrant, and Ansible installed on the workstation.
- Internet access for the Ubuntu box, apt packages, k3s installer, and image builds.
- Run commands from the repository root so the inventory can find Vagrant's SSH key.

## Exact command sequence to run the full flow

Use these commands in order from the repository root.

### 1) Create the k3s VM

```bash
cd /home/iichi/specialité/Java/e-Journey
vagrant up k3s-server
ansible -i infrastructure/ansible/inventories/dev/hosts.yml k3s_server -m ping
```

### 2) Install k3s inside the VM

```bash
ansible-playbook -i infrastructure/ansible/inventories/dev/hosts.yml \
  infrastructure/ansible/playbooks/install-k3s.yml

ansible-playbook -i infrastructure/ansible/inventories/dev/hosts.yml \
  infrastructure/ansible/playbooks/configure-k3s.yml
```

### 3) Start the host CI platform with Docker Compose

```bash
ANSIBLE_CONFIG=infrastructure/ansible/ansible.cfg \
  ansible-playbook infrastructure/ansible/playbooks/ci-platform.yml
```

This starts Jenkins and SonarQube on the workstation through the existing Compose file, and waits for both endpoints to be reachable.

Open them in the browser:

```bash
http://127.0.0.1:8080
http://127.0.0.1:9000
```

### 4) Manually deploy the app once to the VM

```bash
ansible-playbook -i infrastructure/ansible/inventories/dev/hosts.yml \
  infrastructure/ansible/playbooks/deploy-app.yml

ansible-playbook -i infrastructure/ansible/inventories/dev/hosts.yml \
  infrastructure/ansible/playbooks/validate-cluster.yml
```

### 5) Configure Jenkins for the project

Inside Jenkins, create a secret text credential with the ID:

```text
sonar-token
```

Then set the repository to your branch. The Jenkins pipeline in [Jenkinsfile](../../Jenkinsfile) will run the checks and deploy only when the branch is `main` and all steps succeed.

### 6) Trigger the pipeline from the repo

From the Jenkins UI, click the job and run it manually or push to `main` to let the pipeline trigger automatically.

### 7) Optional cleanup

```bash
vagrant halt k3s-server
# or
vagrant destroy k3s-server
```

## Create and provision the VM

```bash
vagrant up k3s-server
ansible -i infrastructure/ansible/inventories/dev/hosts.yml k3s_server -m ping
ansible-playbook -i infrastructure/ansible/inventories/dev/hosts.yml infrastructure/ansible/playbooks/install-k3s.yml
ansible-playbook -i infrastructure/ansible/inventories/dev/hosts.yml infrastructure/ansible/playbooks/configure-k3s.yml
```

The first command creates a single Ubuntu VM and syncs this repository to `/vagrant`. Ansible connects as the Vagrant user; `become: true` elevates only inside the VM, where the Vagrant account has passwordless sudo. The `k3s` role installs Docker and k3s, then configures kubectl access. k3s itself uses containerd.

## Start Jenkins and SonarQube

After `vagrant up` has created the VM key, start the CI stack on the workstation:

```bash
ANSIBLE_CONFIG=infrastructure/ansible/ansible.cfg ansible-playbook infrastructure/ansible/playbooks/ci-platform.yml
```

This runs the `ci` role, builds and starts the Jenkins/SonarQube Compose project,
and waits until both web endpoints are available. Jenkins and SonarQube are
local-only at `http://127.0.0.1:8080` and `http://127.0.0.1:9000`.

## Deploy and validate

The deploy playbook creates the `travel-plan` namespace, copies `files/travel-plan-secrets.yaml` to the VM, and applies it before deploying the workloads. The manifest contains intentionally simple development-only credentials for this learning environment. Re-running deployment reapplies the same Secret, so you do not need to create it manually after deleting cluster resources. Do not use these credentials outside this local learning setup.

```bash
ansible-playbook -i infrastructure/ansible/inventories/dev/hosts.yml infrastructure/ansible/playbooks/deploy-app.yml
ansible-playbook -i infrastructure/ansible/inventories/dev/hosts.yml infrastructure/ansible/playbooks/validate-cluster.yml
```

`deploy-app.yml` is the manual development path. The Jenkins pipeline instead
uses `deploy-ci.yml` and the `k3s` role to import images built from the tested
workspace, apply that checkout's Kubernetes manifests, use the workspace copy
of `.env` for the application Secret, and wait for application rollouts. The
temporary `.env` copy is removed by Jenkins even when a stage fails. The
payment service is not included. The app ingress is forwarded to
`http://localhost:8088`.

The root Jenkinsfile needs a Jenkins Secret text credential with ID
`sonar-token`. Deployment runs only from `main`; it requires the VM to be
running and reachable at the Vagrant SSH forward on `127.0.0.1:2222`.

## VM lifecycle

```bash
vagrant halt k3s-server
vagrant up k3s-server
vagrant destroy k3s-server
```

Destroying the VM deletes its cluster and persistent data. The VM uses 4 GiB RAM and 2 CPUs by default; override with `K3S_VM_MEMORY` (MiB) and `K3S_VM_CPUS` when starting Vagrant.
