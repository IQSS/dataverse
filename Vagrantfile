# -*- mode: ruby -*-
# vi: set ft=ruby :

VAGRANTFILE_API_VERSION = "2"

Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|
  config.vm.box = "bento/centos-8.2"

  config.vm.provider "virtualbox" do |vbox|
    vbox.cpus = 4
    vbox.memory = 4096
  end

  config.vm.provision "shell", path: "scripts/vagrant/setup.sh"
  config.vm.provision "shell", path: "scripts/vagrant/setup-solr.sh"
  config.vm.provision "shell", path: "scripts/vagrant/install-dataverse.sh"

  config.vm.network "private_network", type: "dhcp"
  config.vm.network "forwarded_port", guest: 80, host: 8888
  config.vm.network "forwarded_port", guest: 443, host: 9999
  config.vm.network "forwarded_port", guest: 8983, host: 8993
  config.vm.network "forwarded_port", guest: 8080, host: 8088
  config.vm.network "forwarded_port", guest: 8181, host: 8188

  config.vm.synced_folder ".", "/dataverse"

end
