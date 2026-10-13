# Jenkins and SonarQube

The Ansible `ci` role starts this Compose project: Jenkins, SonarQube, and a
dedicated PostgreSQL database for SonarQube. The app deployment is separate and
uses the Ansible `k3s` role to reach the Vagrant VM.

## Start everything

From the repository root, create and prepare the VM first, then start the CI
platform:

```bash
vagrant up k3s-server
ANSIBLE_CONFIG=infrastructure/ansible/ansible.cfg ansible-playbook infrastructure/ansible/playbooks/install-k3s.yml
ANSIBLE_CONFIG=infrastructure/ansible/ansible.cfg ansible-playbook infrastructure/ansible/playbooks/configure-k3s.yml
ANSIBLE_CONFIG=infrastructure/ansible/ansible.cfg ansible-playbook infrastructure/ansible/playbooks/ci-platform.yml
```

Jenkins is available at `http://127.0.0.1:8080`; SonarQube is at
`http://127.0.0.1:9000`. The Ansible role waits for both services. Persistent
Jenkins and SonarQube data live in named Compose volumes.

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

## Local-only security boundary

Jenkins uses host networking so it can reach Vagrant's SSH port forwarded to
`127.0.0.1:2222` and SonarQube on `127.0.0.1:9000`. Jenkins itself binds only to
host loopback. The container receives the host Docker socket and a read-only
mount of the Vagrant private key; its entrypoint copies that key into a
Jenkins-owned, mode-0600 runtime file. Docker socket access is effectively host
administrator access. Do not expose this setup to untrusted users or use it as
a production CI installation.

The Compose file supplies a learning-only fallback Sonar database password if
`SONAR_DB_PASSWORD` is not present in `.env`. Set a unique value for persistent
use; changing it after the Sonar database volume has been initialized requires
matching the existing database credentials.
