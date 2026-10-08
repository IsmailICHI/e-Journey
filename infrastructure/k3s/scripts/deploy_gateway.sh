#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../../.." && pwd)"

: "${GATEWAY_IMAGE:?Set GATEWAY_IMAGE to the pushed gateway image tag}"
: "${JWT_SECRET:?Set JWT_SECRET to a random value of at least 32 characters}"
: "${REDIS_PASSWORD:?Set REDIS_PASSWORD to a unique value of at least 16 characters}"

ansible-playbook \
  --inventory "$ROOT_DIR/infrastructure/ansible/inventory/localhost.ini" \
  "$ROOT_DIR/infrastructure/ansible/deploy-gateway.yml"