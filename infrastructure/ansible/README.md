# Ansible and k3s VM

Ansible runs on the workstation and connects over SSH to the Ubuntu VM created by the repository-root `Vagrantfile`. It installs and configures k3s inside that VM; it does not install software on the workstation and does not require workstation sudo.

## Prerequisites

- VirtualBox, Vagrant, and Ansible installed on the workstation.
- Internet access for the Ubuntu box, apt packages, k3s installer, and image builds.
- Run commands from the repository root so the inventory can find Vagrant's SSH key.

## Create and provision the VM

```bash
vagrant up k3s-server
ansible -i infrastructure/ansible/inventories/dev/hosts.yml k3s_server -m ping
ansible-playbook -i infrastructure/ansible/inventories/dev/hosts.yml infrastructure/ansible/playbooks/install-k3s.yml
ansible-playbook -i infrastructure/ansible/inventories/dev/hosts.yml infrastructure/ansible/playbooks/configure-k3s.yml
```

The first command creates a single Ubuntu VM and syncs this repository to `/vagrant`. Ansible connects as the Vagrant user; `become: true` elevates only inside the VM, where the Vagrant account has passwordless sudo. The install playbook installs Docker for image builds and installs k3s. k3s itself uses containerd; Docker is only used to build images that the playbook imports into k3s.

## Deploy and validate

The first deploy run creates the `travel-plan` namespace and stops early if `travel-plan-secrets` is missing. Create that Secret inside the VM with real development values, then rerun deployment. For example, connect with `vagrant ssh k3s-server` and use `sudo k3s kubectl` there. Do not apply the placeholders in `k8s/base/secrets.example.yaml`.

```bash
ansible-playbook -i infrastructure/ansible/inventories/dev/hosts.yml infrastructure/ansible/playbooks/deploy-app.yml
ansible-playbook -i infrastructure/ansible/inventories/dev/hosts.yml infrastructure/ansible/playbooks/validate-cluster.yml
```

`deploy-app.yml` builds the frontend and three active backend images in the VM, imports them into k3s, applies `k8s/overlays/dev`, and waits for the application deployments. The payment service is not included. The app ingress is forwarded to `http://localhost:8088`.

## VM lifecycle

```bash
vagrant halt k3s-server
vagrant up k3s-server
vagrant destroy k3s-server
```

Destroying the VM deletes its cluster and persistent data. The VM uses 4 GiB RAM and 2 CPUs by default; override with `K3S_VM_MEMORY` (MiB) and `K3S_VM_CPUS` when starting Vagrant.
