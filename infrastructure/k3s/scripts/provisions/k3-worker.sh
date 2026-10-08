#!/bin/bash

set -e

TOKEN_FILE="/vagrant/token"
MASTER_IP="192.168.56.10"

echo "========================================="
echo "Waiting for k3s token..."
echo "========================================="

# Wait until token exists
while [ ! -f "$TOKEN_FILE" ]; do
  echo "Waiting for token file..."
  sleep 2
done

echo "========================================="
echo "Installing k3s WORKER node"
echo "========================================="

export K3S_URL="https://${MASTER_IP}:6443"
export K3S_TOKEN="$(cat $TOKEN_FILE)"
export INSTALL_K3S_EXEC="\
--node-ip=192.168.56.11 \
--flannel-iface=eth1"

curl -sfL https://get.k3s.io | sh -

echo "========================================="
echo "Worker setup completed"
echo "========================================="


# #!/bin/bash



# MASTER_IP=$1

# TOKEN_FILE=/vagrant/token
# WAIT_SECONDS=120

# # Wait for the token file to appear in the shared folder
# while [ ! -f "$TOKEN_FILE" ] && [ "$WAIT_SECONDS" -gt 0 ]; do
#   echo "Waiting for k3s token file at $TOKEN_FILE... ($WAIT_SECONDS seconds left)"
#   sleep 2
#   WAIT_SECONDS=$((WAIT_SECONDS - 2))
# done

# if [ ! -f "$TOKEN_FILE" ]; then
#   echo "ERROR: k3s token file not found at $TOKEN_FILE"
#   exit 1
# fi

# curl -sfL https://get.k3s.io | K3S_URL=https://$MASTER_IP:6443 K3S_TOKEN_FILE=$TOKEN_FILE sh -