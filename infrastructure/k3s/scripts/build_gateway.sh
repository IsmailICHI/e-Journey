#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
IMAGE="${GATEWAY_IMAGE:-e-journey/gateway:dev}"

docker build \
  --file "$ROOT_DIR/services/gateway/Dockerfile" \
  --tag "$IMAGE" \
  "$ROOT_DIR"

if [[ "${PUSH_IMAGE:-false}" == "true" ]]; then
  docker push "$IMAGE"
fi

printf 'Built gateway image: %s\n' "$IMAGE"