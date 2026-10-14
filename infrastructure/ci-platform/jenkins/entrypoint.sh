#!/bin/bash
set -euo pipefail

if [[ -r /run/host-project.env ]]; then
    install -o jenkins -g jenkins -m 0600 /run/host-project.env /run/jenkins-project.env
fi

if [[ -r /run/host-deploy-key ]]; then
    install -o jenkins -g jenkins -m 0600 /run/host-deploy-key /run/jenkins-k3s-key
fi

if [[ -r /run/host-ssh-host-key.pub ]]; then
    install -d -o jenkins -g jenkins -m 0700 /var/jenkins_home/.ssh
    read -r key_type key_data _ < /run/host-ssh-host-key.pub
    printf 'host.docker.internal %s %s\n' "$key_type" "$key_data" \
        > /var/jenkins_home/.ssh/known_hosts
    chown jenkins:jenkins /var/jenkins_home/.ssh/known_hosts
    chmod 0600 /var/jenkins_home/.ssh/known_hosts
fi

docker_socket_gid="${DOCKER_SOCKET_GID:-0}"
docker_socket_path="${DOCKER_SOCKET_PATH:-/var/run/docker.sock}"
if [[ "$docker_socket_gid" != 0 ]] && [[ "$docker_socket_path" != /run/user/* ]]; then
    docker_group="$(getent group "$docker_socket_gid" | cut -d: -f1 || true)"
    if [[ -z "$docker_group" ]]; then
        groupadd --gid "$docker_socket_gid" host-docker
        docker_group=host-docker
    fi
    usermod -aG "$docker_group" jenkins
fi

exec gosu jenkins /usr/bin/tini -- /usr/local/bin/jenkins.sh "$@"
