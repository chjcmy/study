```bash
#!/bin/bash

# Update and upgrade the system
apt update
apt upgrade -y

# Install chrony
apt install -y chrony

# Configure chrony
cat <<EOF | sudo tee -a /etc/chrony/chrony.conf
# pool ntp.ubuntu.com        iburst maxsources 4
# pool 0.ubuntu.pool.ntp.org iburst maxsources 1
# pool 1.ubuntu.pool.ntp.org iburst maxsources 1
# pool 2.ubuntu.pool.ntp.org iburst maxsources 2
server 203.248.240.140 iburst maxsources 2
EOF

systemctl restart chrony

# Verify time synchronization
chronyc sources
timedatectl set-timezone Asia/Seoul

# Disable swap
swapoff -a

# Comment out swap line in /etc/fstab
sed -i '/\/swap.img/s/^/#/' /etc/fstab

# Load necessary kernel modules
cat <<EOF | sudo tee /etc/modules-load.d/k8s.conf
overlay
br_netfilter
EOF

# Configure sysctl
cat <<EOF | sudo tee /etc/sysctl.d/k8s.conf
net.bridge.bridge-nf-call-iptables  = 1
net.bridge.bridge-nf-call-ip6tables = 1
net.ipv4.ip_forward                 = 1
EOF

sudo sysctl --system

# Install containerd
apt install -y containerd

# Configure containerd
mkdir -p /etc/containerd
containerd config default | sudo tee /etc/containerd/config.toml
sed -i 's/SystemdCgroup = false/SystemdCgroup = true/g' /etc/containerd/config.toml
systemctl restart containerd.service

# Install necessary packages
sudo apt-get update
sudo apt-get install -y apt-transport-https ca-certificates curl gpg

# Disable firewall
ufw disable

# Add Kubernetes repository and import the public key
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://pkgs.k8s.io/core:/stable:/v1.31/deb/Release.key | sudo gpg --dearmor -o /etc/apt/keyrings/kubernetes-apt-keyring.gpg
echo 'deb [signed-by=/etc/apt/keyrings/kubernetes-apt-keyring.gpg] https://pkgs.k8s.io/core:/stable:/v1.31/deb/ /' | sudo tee /etc/apt/sources.list.d/kubernetes.list

# Install Kubernetes components
sudo apt-get update
sudo apt-get install -y kubelet kubeadm kubectl
sudo apt-mark hold kubelet kubeadm kubectl

# Enable kubelet
sudo systemctl enable --now kubelet

reboot

```