#!/bin/bash

set -e

# =========================================================
# Kubernetes Infrastructure Orchestrator
# =========================================================
#
# This script manages the entire infrastructure lifecycle:
#
#   ./orchestrator.sh create
#   ./orchestrator.sh start
#   ./orchestrator.sh stop
#   ./orchestrator.sh destroy
#
# =========================================================
#
# PROJECT STRUCTURE USED:
#
# scripts/
# ├── apply-manifests.sh
# ├── build_and_push.sh
# ├── install-vagrant.sh
# ├── provisions/
# │   ├── k3-master.sh
# │   └── k3-worker.sh
# └── setup-kubectl.sh
#
# =========================================================

# ---------------------------------------------------------
# COLORS
# ---------------------------------------------------------

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# ---------------------------------------------------------
# HELPERS
# ---------------------------------------------------------

log() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# ---------------------------------------------------------
# PATHS
# ---------------------------------------------------------

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

SCRIPTS_DIR="$ROOT_DIR/scripts"

KUBECTL_SETUP_SCRIPT="$SCRIPTS_DIR/setup-kubectl.sh"
MANIFEST_SCRIPT="$SCRIPTS_DIR/apply-manifests.sh"
INSTALL_VAGRANT_SCRIPT="$SCRIPTS_DIR/install-vagrant.sh"
BUILD_PUSH_SCRIPT="$SCRIPTS_DIR/build_and_push.sh"

# ---------------------------------------------------------
# CHECK DEPENDENCIES
# ---------------------------------------------------------

check_requirements() {

    log "Checking dependencies..."

    if ! command -v vagrant &> /dev/null; then
        warn "Vagrant not found."
        warn "Installing Vagrant..."

        bash "$INSTALL_VAGRANT_SCRIPT"
    fi

    if ! command -v virtualbox &> /dev/null; then
        error "VirtualBox is not installed."
        exit 1
    fi

    log "Dependencies OK."
}

# ---------------------------------------------------------
# WAIT FOR KUBERNETES
# ---------------------------------------------------------

wait_for_cluster() {

    log "Waiting for Kubernetes API..."

    export KUBECONFIG="$ROOT_DIR/.kube/config"

    until kubectl get nodes &>/dev/null
    do
        sleep 5
    done

    log "Waiting for nodes to become Ready..."

    kubectl wait \
        --for=condition=Ready nodes \
        --all \
        --timeout=300s

    log "Cluster is Ready."
}

# ---------------------------------------------------------
# CREATE CLUSTER
# ---------------------------------------------------------

create_cluster() {

    check_requirements

    log "Creating infrastructure..."

    # -----------------------------------------------------
    # Start and provision VMs
    # -----------------------------------------------------

    vagrant up

    log "Virtual machines created."

    # -----------------------------------------------------
    # Configure kubectl on host machine
    # -----------------------------------------------------

    log "Configuring kubectl..."

    bash "$KUBECTL_SETUP_SCRIPT"

    export KUBECONFIG="$ROOT_DIR/.kube/config"

    # -----------------------------------------------------
    # Wait for cluster readiness
    # -----------------------------------------------------

    wait_for_cluster

    # -----------------------------------------------------
    # OPTIONAL:
    # Build and push images before deployment
    # -----------------------------------------------------

    # if [ -f "$BUILD_PUSH_SCRIPT" ]; then

    #     log "Building and pushing Docker images..."

    #     bash "$BUILD_PUSH_SCRIPT"

    #     log "Docker images pushed."
    # fi

    # -----------------------------------------------------
    # Apply Kubernetes manifests
    # -----------------------------------------------------

    log "Applying Kubernetes manifests..."

    bash "$MANIFEST_SCRIPT"

    # -----------------------------------------------------
    # Show final status
    # -----------------------------------------------------

    log "Final cluster state:"

    kubectl get nodes -o wide

    echo ""

    kubectl get pods -o wide

    echo ""

    kubectl get svc

    log "Cluster created successfully."
}

# ---------------------------------------------------------
# START CLUSTER
# ---------------------------------------------------------

start_cluster() {

    log "Starting existing cluster..."

    vagrant up

    export KUBECONFIG="$ROOT_DIR/.kube/config"

    wait_for_cluster

    log "Cluster started successfully."

    kubectl get pods -o wide
}

# ---------------------------------------------------------
# STOP CLUSTER
# ---------------------------------------------------------

stop_cluster() {

    log "Stopping cluster..."

    vagrant halt

    log "Cluster stopped."
}

# ---------------------------------------------------------
# DESTROY CLUSTER
# ---------------------------------------------------------

destroy_cluster() {

    warn "Destroying infrastructure..."

    vagrant destroy -f

    log "Cluster destroyed."
}

# ---------------------------------------------------------
# STATUS
# ---------------------------------------------------------

cluster_status() {

    export KUBECONFIG="$ROOT_DIR/.kube/config"

    log "Cluster status"

    echo ""
    kubectl get nodes -o wide

    echo ""
    kubectl get pods -o wide

    echo ""
    kubectl get svc

    echo ""
    kubectl get pvc
}

# ---------------------------------------------------------
# COMMAND HANDLER
# ---------------------------------------------------------

case "$1" in

    create)
        create_cluster
        ;;

    start)
        start_cluster
        ;;

    stop)
        stop_cluster
        ;;

    destroy)
        destroy_cluster
        ;;

    status)
        cluster_status
        ;;

    *)
        echo ""
        echo "Usage:"
        echo ""
        echo "./orchestrator.sh create"
        echo "./orchestrator.sh start"
        echo "./orchestrator.sh stop"
        echo "./orchestrator.sh destroy"
        echo "./orchestrator.sh status"
        echo ""
        exit 1
        ;;

esac