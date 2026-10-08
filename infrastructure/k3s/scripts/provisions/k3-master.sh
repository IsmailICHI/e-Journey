#!/bin/bash

set -e

echo "========================================="
echo "Installing k3s MASTER node"
echo "========================================="

# Install k3s server with explicit network configuration
curl -sfL https://get.k3s.io | \
INSTALL_K3S_EXEC="\
--node-ip=192.168.56.10 \
--advertise-address=192.168.56.10 \
--flannel-iface=eth1" \
sh -

echo "========================================="
echo "Configuring kubeconfig for vagrant user"
echo "========================================="

mkdir -p /home/vagrant/.kube
cp /etc/rancher/k3s/k3s.yaml /home/vagrant/.kube/config
chown -R vagrant:vagrant /home/vagrant/.kube

# Replace localhost with reachable cluster IP
sed -i 's/127.0.0.1/192.168.56.10/g' /home/vagrant/.kube/config

echo "========================================="
echo "Exporting worker token"
echo "========================================="

TOKEN=$(cat /var/lib/rancher/k3s/server/node-token)

mkdir -p /vagrant
echo "$TOKEN" > /vagrant/token

echo "========================================="
echo "Creating kubeconfig for HOST machine"
echo "========================================="

mkdir -p /vagrant/.kube
cp /etc/rancher/k3s/k3s.yaml /vagrant/.kube/config

# Replace localhost with reachable master IP
sed -i 's/127.0.0.1/192.168.56.10/g' /vagrant/.kube/config

echo "========================================="
echo "Master setup completed"
echo "========================================="


# #!/bin/bash

# curl -sfL https://get.k3s.io | sh -

# sudo mkdir -p /home/vagrant/.kube
# sudo cp /etc/rancher/k3s/k3s.yaml /home/vagrant/.kube/config
# sudo chown vagrant:vagrant /home/vagrant/.kube/config

# TOKEN=$(sudo cat /var/lib/rancher/k3s/server/node-token)

# # Ensure the shared folder exists and store the token for the workers to use
# sudo mkdir -p /vagrant
# echo "$TOKEN" > /vagrant/token

# # Create kubeconfig for host machine access
# sudo mkdir -p /vagrant/.kube
# sudo cp /etc/rancher/k3s/k3s.yaml /vagrant/.kube/config
# sudo sed -i 's/127.0.0.1/localhost/g' /vagrant/.kube/config
# sudo sed -i 's/0.0.0.0/localhost/g' /vagrant/.kube/config