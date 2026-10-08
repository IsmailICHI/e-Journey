#!/bin/bash

set -e

MANIFEST_ROOT="${MANIFEST_ROOT:-manifest}"
ENV_FILE="${1:-.env}"
CONFIGMAP_NAME="orchestrator-config"

# ── Validate ──────────────────────────────────────────────────────────────────

if [ ! -f "$ENV_FILE" ]; then
  echo "❌ Missing env file: ${ENV_FILE}"
  echo "   Copy .env.example and fill in values."
  exit 1
fi

echo "========================================"
echo " Kubernetes Deployment Script"
echo " Env file: ${ENV_FILE}"
echo "========================================"

# ── helper ────────────────────────────────────────────────────────────────────

apply_secret() {
  local secret_name="$1"
  local prefix="$2"
  local tmp

  tmp=$(mktemp)
  grep "^${prefix}" "$ENV_FILE" | sed "s/^${prefix}//" > "$tmp"

  kubectl create secret generic "$secret_name" \
    --from-env-file="$tmp" \
    --dry-run=client -o yaml | kubectl apply -f -

  echo "  ✅ $secret_name"
  rm "$tmp"
}

# ── 1. Secrets ────────────────────────────────────────────────────────────────

echo ""
echo "🔐 Applying Secrets..."

apply_secret "billing-db-secret"    "SECRET_BILLING_DB_"
apply_secret "inventory-db-secret"  "SECRET_INVENTORY_DB_"
apply_secret "rabbitmq-secret"      "SECRET_RABBITMQ_"

# ── 2. ConfigMap ──────────────────────────────────────────────────────────────

echo ""
echo "📦 Applying ConfigMap..."

tmp=$(mktemp)
grep -v "^SECRET_" "$ENV_FILE" | grep -v "^#" | grep -v "^$" > "$tmp"

kubectl create configmap "$CONFIGMAP_NAME" \
  --from-env-file="$tmp" \
  --dry-run=client -o yaml | kubectl apply -f -

echo "  ✅ $CONFIGMAP_NAME"
rm "$tmp"

# ── 3. Deploy ─────────────────────────────────────────────────────────────────

echo ""
echo "🚀 Deploying services..."

kubectl apply -f "${MANIFEST_ROOT}/services/gateway/"
kubectl apply -f "${MANIFEST_ROOT}/services/volumes/"
kubectl apply -f "${MANIFEST_ROOT}/services/rabbitmq/"
kubectl apply -f "${MANIFEST_ROOT}/services/billing/db/"
kubectl apply -f "${MANIFEST_ROOT}/services/inventory/db/"
kubectl apply -f "${MANIFEST_ROOT}/services/billing/app/"
kubectl apply -f "${MANIFEST_ROOT}/services/inventory/app/"
kubectl apply -f "${MANIFEST_ROOT}/services/gateway/hpa/"
kubectl apply -f "${MANIFEST_ROOT}/services/gateway/ingress/"
kubectl apply -f "${MANIFEST_ROOT}/services/inventory/hpa/"

# ── 4. Status ─────────────────────────────────────────────────────────────────

echo ""
echo "========================================"
echo " Deployment Finished"
echo "========================================"
echo ""
kubectl get pods -o wide
echo ""
kubectl get svc
echo ""
kubectl get pvc


# #!/bin/bash

# set -euo pipefail

# MANIFEST_ROOT="${MANIFEST_ROOT:-manifest1}"
# ENV_FILE="${1:-.env}"
# CONFIGMAP_NAME="orchestrator-config"

# TEMP_FILES=()

# cleanup() {
#   if [ "${#TEMP_FILES[@]}" -gt 0 ]; then
#     rm -f "${TEMP_FILES[@]}"
#   fi
# }

# trap cleanup EXIT

# new_temp_file() {
#   local result_var="$1"
#   local temp_file

#   temp_file="$(mktemp)"
#   TEMP_FILES+=("$temp_file")
#   printf -v "$result_var" '%s' "$temp_file"
# }

# secret_prefix_for_name() {
#   local secret_name="$1"
#   local service_name

#   service_name="${secret_name%-secret}"
#   service_name="${service_name//-/_}"
#   service_name="${service_name^^}"

#   printf 'SECRET_%s_' "$service_name"
# }

# secret_name_from_manifest() {
#   local manifest_file="$1"

#   awk '
#     $1 == "metadata:" { in_metadata = 1; next }
#     in_metadata && $1 == "name:" { print $2; exit }
#   ' "$manifest_file"
# }

# write_configmap_env_file() {
#   local output_file="$1"

#   awk '
#     /^[[:space:]]*$/ { next }
#     /^[[:space:]]*#/ { next }
#     /^[[:space:]]*SECRET_[A-Za-z0-9_]*=/ { next }
#     /^[[:space:]]*[A-Za-z_][A-Za-z0-9_]*=/ { print }
#   ' "$ENV_FILE" > "$output_file"
# }

# write_secret_env_file() {
#   local prefix="$1"
#   local output_file="$2"

#   awk -v prefix="$prefix" '
#     /^[[:space:]]*$/ { next }
#     /^[[:space:]]*#/ { next }
#     index($0, prefix) == 1 {
#       line = substr($0, length(prefix) + 1)
#       if (line ~ /^[A-Za-z_][A-Za-z0-9_]*=/) {
#         print line
#       }
#     }
#   ' "$ENV_FILE" > "$output_file"
# }

# apply_secret_from_env_file() {
#   local secret_name="$1"
#   local secret_env_file="$2"

#   if [ ! -s "$secret_env_file" ]; then
#     echo "No values found for ${secret_name}; expected variables prefixed with $(secret_prefix_for_name "$secret_name")"
#     exit 1
#   fi

#   kubectl create secret generic "$secret_name" \
#     --from-env-file="$secret_env_file" \
#     --dry-run=client \
#     -o yaml | kubectl apply -f -
# }

# apply_configmap_from_env_file() {
#   local config_env_file="$1"

#   if [ ! -s "$config_env_file" ]; then
#     echo "No non-secret values found in ${ENV_FILE}; cannot create ${CONFIGMAP_NAME}"
#     exit 1
#   fi

#   kubectl create configmap "$CONFIGMAP_NAME" \
#     --from-env-file="$config_env_file" \
#     --dry-run=client \
#     -o yaml | kubectl apply -f -
# }

# if [ ! -f "$ENV_FILE" ]; then
#   echo "Missing env file: ${ENV_FILE}"
#   echo "Create one from .env.example, or pass a path as the first argument."
#   exit 1
# fi

# echo "========================================"
# echo " Kubernetes Deployment Script"
# echo "========================================"
# echo "Using env file: ${ENV_FILE}"

# # -----------------------------
# # 1. Generate and apply Secrets
# # -----------------------------

# echo ""
# echo "Applying generated secrets..."

# while IFS= read -r secret_manifest; do
#   secret_name="$(secret_name_from_manifest "$secret_manifest")"
#   new_temp_file secret_env_file
#   write_secret_env_file "$(secret_prefix_for_name "$secret_name")" "$secret_env_file"
#   apply_secret_from_env_file "$secret_name" "$secret_env_file"
# done < <(find "${MANIFEST_ROOT}/secrets" -type f \( -name '*.yaml' -o -name '*.yml' \) | sort)

# # -----------------------------
# # 2. Generate and apply ConfigMap
# # -----------------------------

# echo ""
# echo "Applying generated configmap..."
# new_temp_file config_env_file
# write_configmap_env_file "$config_env_file"
# apply_configmap_from_env_file "$config_env_file"

# # -----------------------------
# # 2. Deploy API Gateway
# # -----------------------------

# echo ""
# echo "Deploying API Gateway..."
# kubectl apply -f "${MANIFEST_ROOT}/services/gateway/"
# sleep 2

# # -----------------------------
# # 3. Apply Persistent Volumes / PVCs
# # -----------------------------
# sleep 2
# echo ""
# echo "Applying storage volumes..."
# kubectl apply -f "${MANIFEST_ROOT}/services/volumes/"
# sleep 2

# # -----------------------------
# # 4. Deploy RabbitMQ
# # -----------------------------
# echo ""
# echo "Deploying RabbitMQ..."
# kubectl apply -f "${MANIFEST_ROOT}/services/rabbitmq/"
# sleep 2


# # -----------------------------
# # 5. Deploy Databases
# # -----------------------------

# echo ""
# echo "Deploying Billing DB..."
# kubectl apply -f "${MANIFEST_ROOT}/services/billing/db/"
# sleep 2


# echo ""
# echo "Deploying Inventory DB..."
# kubectl apply -f "${MANIFEST_ROOT}/services/inventory/db/"
# sleep 2


# # -----------------------------
# # 6. Deploy Applications
# # -----------------------------

# echo ""
# echo "Deploying Billing App..."
# kubectl apply -f "${MANIFEST_ROOT}/services/billing/app/"
# sleep 2

# # -----------------------------
# # 7. Deploy Inventory App
# # -----------------------------
# echo ""
# echo "Deploying Inventory App..."
# kubectl apply -f "${MANIFEST_ROOT}/services/inventory/app/"
# sleep 2

# echo ""
# kubectl apply -f "${MANIFEST_ROOT}/services/gateway/hpa/"
# sleep 2

# echo "------------------------------"
# echo "Applying Ingress for API Gateway..."
# kubectl apply -f "${MANIFEST_ROOT}/services/gateway/ingress/"
# sleep 2


# echo ""
# kubectl apply -f "${MANIFEST_ROOT}/services/inventory/hpa/"
# sleep 2





# # -----------------------------
# # . Final Cluster Status
# # -----------------------------

# echo ""
# echo "========================================"
# echo " Deployment Finished"
# echo "========================================"

# echo ""
# sleep 5
# kubectl get pods -o wide
# echo ""
# kubectl get svc

# echo ""
# kubectl get pvc

# echo ""
# echo "All manifests applied successfully."



