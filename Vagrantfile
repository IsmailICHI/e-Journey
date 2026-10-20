Vagrant.configure("2") do |config|
  config.vm.box = "generic/ubuntu2204"
  config.vm.synced_folder ".", "/vagrant", create: true

  config.vm.define "k3s-server" do |server|
    server.vm.hostname = "travel-plan-k3s"
    server.vm.network "forwarded_port", guest: 22, host: 2222,
      id: "ssh", host_ip: "127.0.0.1", auto_correct: false
    server.vm.network "forwarded_port", guest: 80, host: 8088,
      host_ip: "127.0.0.1"
    server.vm.network "forwarded_port", guest: 8080, host: 8080,
      host_ip: "127.0.0.1"
    server.vm.network "forwarded_port", guest: 9000, host: 9000,
      host_ip: "127.0.0.1"
    server.vm.network "forwarded_port", guest: 6443, host: 16443,
      host_ip: "127.0.0.1"
    server.vm.network "forwarded_port", guest: 30030, host: 30030,
      host_ip: "127.0.0.1"
    server.vm.network "forwarded_port", guest: 30090, host: 30090,
      host_ip: "127.0.0.1"
    server.vm.provider "virtualbox" do |vb|
      vb.memory = ENV.fetch("K3S_VM_MEMORY", "4096")
      vb.cpus = ENV.fetch("K3S_VM_CPUS", "2")
      vb.customize ["modifyvm", :id, "--natdnshostresolver1", "on"]
    end
  end
end