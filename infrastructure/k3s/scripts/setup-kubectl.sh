#!/bin/bash

# Simple kubectl setup for k3s cluster (no sudo required)

# 1. Install kubectl locally if missing (Linux only)
KUBECTL_LOCAL="$HOME/.local/bin/kubectl"
if ! command -v kubectl &> /dev/null; then
    echo "kubectl not found. Installing kubectl locally..."
    mkdir -p "$HOME/.local/bin"
    curl -Lo "$KUBECTL_LOCAL" "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
    chmod +x "$KUBECTL_LOCAL"
    echo "kubectl installed locally at $KUBECTL_LOCAL."
fi

# 2. Ensure ~/.local/bin is in PATH for this session
case ":$PATH:" in
    *":$HOME/.local/bin:"*) :;;
    *) export PATH="$HOME/.local/bin:$PATH"; echo 'export PATH="$HOME/.local/bin:$PATH"' >> ~/.zshrc;;
esac

# 3. Create kubeconfig if missing
if [ ! -f ".kube/config" ]; then
    vagrant ssh k3-master -- "mkdir -p /vagrant/.kube && cp /etc/rancher/k3s/k3s.yaml /vagrant/.kube/config && sed -i 's/127.0.0.1/localhost/g; s/0.0.0.0/localhost/g' /vagrant/.kube/config" 2>/dev/null
fi

# 4. Add KUBECONFIG to zshrc if not already there
KUBECONFIG_LINE="export KUBECONFIG=\"$(pwd)/.kube/config\""
if ! grep -q "$KUBECONFIG_LINE" ~/.zshrc; then
    echo "$KUBECONFIG_LINE" >> ~/.zshrc
fi

# 5. Set KUBECONFIG for current session
export KUBECONFIG="$(pwd)/.kube/config"
echo "KUBECONFIG set to : $KUBECONFIG for current session."
echo "KUBECONFIG_LINE set to : $KUBECONFIG_LINE for current session."
# 6. Test kubectl config
# echo "Testing kubectl connection..."
sleep 1

# source ~/.zshrc

kubectl version 
