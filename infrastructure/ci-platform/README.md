# Jenkins and SonarQube

The Ansible `ci` role starts this Compose project on the k3s VM: Jenkins,
SonarQube, and a dedicated PostgreSQL database. Jenkins and SonarQube share a
private Compose network; the database is isolated on an internal network that
Jenkins cannot join. The app runs in k3s on the same VM.

## Start everything

From the repository root, create and prepare the VM first, then start the CI
platform:

```bash
vagrant up k3s-server
ANSIBLE_CONFIG=infrastructure/ansible/ansible.cfg ansible-playbook infrastructure/ansible/playbooks/install-k3s.yml
ANSIBLE_CONFIG=infrastructure/ansible/ansible.cfg ansible-playbook infrastructure/ansible/playbooks/configure-k3s.yml
ANSIBLE_CONFIG=infrastructure/ansible/ansible.cfg ansible-playbook infrastructure/ansible/playbooks/ci-platform.yml
```

From the workstation, Jenkins is available at `http://127.0.0.1:8080`,
SonarQube at `http://127.0.0.1:9000`, and the app at `http://localhost:8088`.
Vagrant forwards these ports on workstation loopback only. Ansible waits for
Jenkins and additionally checks SonarQube from inside the Jenkins container.
Persistent Jenkins and SonarQube data live in named Compose volumes.

## One-time Jenkins setup

1. Unlock Jenkins using the initial administrator password in its container
   logs and create an administrator account.
2. In SonarQube, create a user token and create/configure the `e-journey-backend`
   project if project auto-provisioning is disabled.
3. In Jenkins, add the token as a **Secret text** credential with ID
   `sonar-token`.
4. Create a Pipeline or Multibranch Pipeline job using the repository root
   `Jenkinsfile`. Deployment runs only for branch `main`.

The pipeline runs Maven `clean verify` for gateway, user-service, and
travel-service, builds the frontend and backend images, submits the root
`sonar-project.properties` analysis, and polls the SonarQube quality gate. Only
after a passing gate does `main` deploy through Ansible to the k3s VM. It copies
the host `.env` into the Jenkins workspace immediately before deployment and
removes that workspace copy in the pipeline's `always` cleanup. The `.env` is
not copied into the Jenkins image or kept in its home volume.

## Communication and security boundary

Jenkins reaches SonarQube at `http://sonarqube:9000` over the Compose network;
PostgreSQL has no published port and is isolated from Jenkins on an internal
network. The pipeline reaches the VM SSH server through Docker's
`host.docker.internal` gateway mapping. Ansible creates a dedicated Ed25519
deploy key, authorizes only its public half for the `vagrant` account, and
mounts the VM's SSH host key so Jenkins can verify the target. The app remains
inside k3s and is published through Traefik.

Vagrant binds all forwarded workstation ports to `127.0.0.1`. The Compose web
ports are published inside the VM to serve those forwards; they are not
published on the workstation's network interfaces. Jenkins still receives the
VM Docker socket, which grants VM-level administrator access. Do not expose
this development setup to untrusted users or use it as production CI.

The Compose file supplies a learning-only fallback Sonar database password if
`SONAR_DB_PASSWORD` is not present in `.env`. Set a unique value for persistent
use; changing it after the Sonar database volume has been initialized requires
matching the existing database credentials.
