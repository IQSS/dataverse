# -*- mode: ruby -*-
# vi: set ft=ruby :

# Vagrantfile API/syntax version. Don't touch unless you know what you're doing!
VAGRANTFILE_API_VERSION = "2"

Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|

  config.vm.define "standalone", primary: true do |standalone|
    config.vm.hostname = "standalone"
    standalone.vm.box = "puppet-vagrant-boxes.puppetlabs.com-centos-65-x64-virtualbox-puppet.box"

    operating_system = "centos"
    if ENV['OPERATING_SYSTEM'].nil?
      puts "OPERATING_SYSTEM environment variable not specified. Using #{operating_system} by default.\nTo specify it in bash: export OPERATING_SYSTEM=debian"
      config.vm.box_url = "http://puppet-vagrant-boxes.puppetlabs.com/centos-65-x64-virtualbox-puppet.box"
      config.vm.box = "puppet-vagrant-boxes.puppetlabs.com-centos-65-x64-virtualbox-puppet.box"
    elsif ENV['OPERATING_SYSTEM'] == 'centos7'
      puts "WARNING: CentOS 7 specified. Newer than what the dev team tests on."
      config.vm.box_url = "https://atlas.hashicorp.com/puppetlabs/boxes/centos-7.2-64-puppet/versions/1.0.1/providers/virtualbox.box"
      config.vm.box = "puppetlabs-centos-7.2-64-puppet-1.0.1-virtualbox.box"
      standalone.vm.box = "puppetlabs-centos-7.2-64-puppet-1.0.1-virtualbox.box"
    elsif ENV['OPERATING_SYSTEM'] == 'debian'
      puts "WARNING: Debian specified. Here be dragons! https://github.com/IQSS/dataverse/issues/1059"
      config.vm.box_url = "http://puppet-vagrant-boxes.puppetlabs.com/debian-73-x64-virtualbox-puppet.box"
      config.vm.box = "puppet-vagrant-boxes.puppetlabs.com-debian-73-x64-virtualbox-puppet.box"
    else
      operating_system = ENV['OPERATING_SYSTEM']
      puts "Not sure what do to with operating system: #{operating_system}"
      exit 1
    end

    mailserver = "localhost"
    if ENV['MAIL_SERVER'].nil?
      puts "MAIL_SERVER environment variable not specified. Using #{mailserver} by default.\nTo specify it in bash: export MAIL_SERVER=localhost"
    else
      mailserver = ENV['MAIL_SERVER']
      puts "MAIL_SERVER environment variable found, using #{mailserver}"
    end

    config.vm.provider "virtualbox" do |v|
      v.memory = 2048
      v.cpus = 1
    end
    config.vm.provision "shell", path: "scripts/vagrant/setup.sh"
    config.vm.provision "shell", path: "scripts/vagrant/setup-solr.sh"
    config.vm.provision "shell", path: "scripts/vagrant/install-dataverse.sh", args: mailserver
    # FIXME: get tests working and re-enable them!
    #config.vm.provision "shell", path: "scripts/vagrant/test.sh"

    config.vm.network "private_network", type: "dhcp"
    config.vm.network "forwarded_port", guest: 80, host: 8888
    config.vm.network "forwarded_port", guest: 443, host: 9999
    config.vm.network "forwarded_port", guest: 8983, host: 8993
    config.vm.network "forwarded_port", guest: 8080, host: 8088
    config.vm.network "forwarded_port", guest: 8181, host: 8188

    # FIXME: use /dataverse/downloads instead
    config.vm.synced_folder "downloads", "/downloads"
    # FIXME: use /dataverse/conf instead
    config.vm.synced_folder "conf", "/conf"
    # FIXME: use /dataverse/scripts instead
    config.vm.synced_folder "scripts", "/scripts"
    config.vm.synced_folder ".", "/dataverse"
  end

  config.vm.define "solr", autostart: false do |solr|
    config.vm.hostname = "solr"
    solr.vm.box = "puppet-vagrant-boxes.puppetlabs.com-centos-65-x64-virtualbox-puppet.box"
    config.vm.synced_folder ".", "/dataverse"
    config.vm.network "private_network", type: "dhcp"
    config.vm.network "forwarded_port", guest: 8983, host: 9001
  end

  config.vm.define "test", autostart: false do |test|
    config.vm.hostname = "test"
    test.vm.box = "puppet-vagrant-boxes.puppetlabs.com-centos-65-x64-virtualbox-puppet.box"
    config.vm.synced_folder ".", "/dataverse"
    config.vm.network "private_network", type: "dhcp"
  end

end
