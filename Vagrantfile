Vagrant.configure("2") do |config|
  config.vm.box = "generic/ubuntu2204"
  config.vm.synced_folder ".", "/vagrant", create: true

  config.vm.define "k3s-server" do |server|
    server.vm.hostname = "travel-plan-k3s"
    server.vm.network "forwarded_port", guest: 22, host: 2222,
      host_ip: "127.0.0.1", id: "ssh", auto_correct: false
    server.vm.network "forwarded_port", guest: 80, host: 8088,
      host_ip: "127.0.0.1"
    server.vm.network "forwarded_port", guest: 6443, host: 16443,
      host_ip: "127.0.0.1"

    server.vm.provider "virtualbox" do |vb|
      vb.memory = ENV.fetch("K3S_VM_MEMORY", "4096")
      vb.cpus = ENV.fetch("K3S_VM_CPUS", "2")
      vb.customize ["modifyvm", :id, "--natdnshostresolver1", "on"]
    end
  end
end



# Vagrant .configure("2") do |config|
#     config.vm.box = "generic/ubuntu2204"
#     config.vm.synced_folder ".", "/vagrant", create: true

#     # k3s master node
#     config.vm.define "k8-master" do |master|
#         master.vm.hostname = "k3-master"
#         master.vm.network "private_network", ip: "192.168.56.10"
#         master.vm.network "forwarded_port", guest: 6443, host: 6443
#         master.vm.provider "virtualbox" do |vb|
#             vb.memory = "2048"
#             vb.cpus = 2
#         end
#         master.vm.provision "shell", path: "scripts/provisions/k3-master.sh"
#     end

#     # k3s worker node
#     config.vm.define "k3s-worker1" do |worker1|
#       worker1.vm.hostname = "k3s-worker1"
#       worker1.vm.network "private_network", ip: "192.168.56.11"
#       worker1.vm.provider "virtualbox" do |vb|
#         vb.memory = "1024"
#         vb.cpus = 1
#       end
#       worker1.vm.provision "shell", path: "scripts/provisions/k3-worker.sh"
#       # , args: ["192.168.56.10"]
#     end
# end