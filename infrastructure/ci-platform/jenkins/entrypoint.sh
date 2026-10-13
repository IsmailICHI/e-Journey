#!/bin/bash
set -euo pipefail

if [[ -r /run/host-vagrant-key ]]; then
    install -o jenkins -g jenkins -m 0600 /run/host-vagrant-key /run/jenkins-k3s-key
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
