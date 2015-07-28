# Vagrant shell provisioning script
# Tested with Vagrant 1.7.2 for Ubuntu 12.04, 14.04 and Centos 6.5
#
# Usage
# -----
#
# $ vagrant up         # to create and run the machine
# $ vagrant provision  # to install new packages or configuration with puppet
#
#
# Environment variables
# ---------------------
#
# OPERATING_SYSTEM
# $ export OPERATING_SYSTEM=centos-6:default|ubuntu-12|ubuntu-14
# 'centos-6' will install Centos 6.5
# 'ubuntu-12' will install Ubuntu 12.04 lts (precise)
# 'ubuntu-14' will install Ubuntu 14.04 lts (trusty)
#
#
# PROVISIONER
# $ export PROVISIONER=dataverse:default:fallback|puppet
# 'puppet' will let the client puppet distribution do the installation. 'dataverse' or an empty or any other value will
# use the available setup scripts that are part of the dataverse code do the installation. With puppet you can
# use hieradata to override the Puppet Dataverse module default settings. A working example is
# /conf/puppet/hieradata/my_environment.json whose
# settings are identical to the defaults. For each environment you can add a new hieradata document. E.g.
# export ENVIRONMENT=my_environment
#
#
# # ENVIRONMENT
# $ export ENVIRONMENT=development:default)|*
# You can set any environment, but only 'my_environment' has hieradata in /conf/puppet. Change the value to try out alternative settings.
#
#
# Post installation setup when provisioning with puppet
# -----------------------------------------------------
# You need to run the final database and api scripts to populate your dataverse instance:
# $ sudo -u dvnApp psql dvndb -f /opt/dataverse/scripts/database/reference_data.sql
# $ /opt/dataverse/scripts/api/setup-all.sh
#
#
# Trouble shooting
# ----------------
# 1. Error: 'A host only network interface... via DHCP....'
# https://github.com/mitchellh/vagrant/issues/3083
# If you get this error it means you have a conflict between two DHCP providers: your host machine and virtualbox which
# runs the virtual box.
# Suggestion: disable the virtualbox DHCP provider on your host:
# $ VBoxManage dhcpserver remove --netname HostInterfaceNetworking-vboxnet0
#
#
# 2. On Ubuntu the error: 'Failed to mount folders in Linux guest...'
# https://github.com/mitchellh/vagrant/issues/3341
#
# The virtual box cannot mount with /vagrant to your host filesystem.
# Suggestion #1: add a symbolic link in the client and reload the VM.
# On your VM:
# $ sudo ln -s /opt/VBoxGuestAdditions-4.3.10/lib/VBoxGuestAdditions /usr/lib/VBoxGuestAdditions
# On our host:
# $ vagrant reload
# Then provision on your host
# $ vagrant provision
#
# If that does not work, try suggestion #2:
# https://www.virtualbox.org/manual/ch04.html
# Install the guest additions on your VM:
# $ sudo apt-get update
# $ sudo apt-get install virtualbox-guest-dkms
# And on your host
# $ vagrant reload
#
#
# 3. Out of memory when installing R packages dataverse::r.
# Suggestion: increase the 'v.customize ["modifyvm", :id, "--memory", "2048"]' setting.
#
#
# 4. When installing you get a 'Connection failed [IP: .......' error.
# Or downloads seem to timeout.
# Suggestion: check if you are not blocked by a firewall. Or sometimes the internet is just busy. If so, try again:
# $ vagrant provision
#
#
# 5. Using puppet provisioning: on Centos the Error: Execution of '/usr/bin/yum -d 0 -e 0 -y install ...' returned 1: Error: Nothing to do
# yum reports an error when the package is in fact installed. Should this happen then repeat the
# provisioning once:
# $ vagrant provision



VAGRANTFILE_API_VERSION = '2'
HOSTNAME = 'standalone'
DEFAULT_ENVIRONMENT = 'development'
DEFAULT_OPERATING_SYSTEM = 'centos-6'
DEFAULT_PROVISIONER='dataverse'

provisioner = if ENV['PROVISIONER'].nil? or ENV['PROVISIONER'] == 'default' then DEFAULT_PROVISIONER else ENV['PROVISIONER'] end
puts "Provisioning by #{provisioner}"

environment = if ENV['ENVIRONMENT'].nil? or ENV['ENVIRONMENT'] == 'default' then DEFAULT_ENVIRONMENT else ENV['ENVIRONMENT'] end
puts "Target environment #{environment}"

operating_system = if ENV['OPERATING_SYSTEM'].nil? or ENV['OPERATING_SYSTEM'] == 'default' then DEFAULT_OPERATING_SYSTEM else ENV['OPERATING_SYSTEM'] end
if operating_system == 'centos-6'
  box = 'puppet-vagrant-boxes.puppetlabs.com-centos-65-x64-virtualbox-puppet.box'
  box_url = 'http://puppet-vagrant-boxes.puppetlabs.com/centos-65-x64-virtualbox-puppet.box'
#elsif operating_system == 'centos-7'
#  box = 'puppetlabs/centos-7.0-64-puppet'
#  box_url = 'https://atlas.hashicorp.com/puppetlabs'
elsif operating_system == 'ubuntu-12'
  box = "puppetlabs/ubuntu-12.04-64-puppet"
  box_url = 'https://atlas.hashicorp.com/puppetlabs'
elsif operating_system == 'ubuntu-14'
  box = "puppetlabs/ubuntu-14.04-64-puppet"
  box_url = 'https://atlas.hashicorp.com/puppetlabs'
else
  puts "Not sure what do to with operating system: #{operating_system}"
  puts 'Use: export OPERATING_SYSTEM=centos-6|ubuntu-12|ubuntu-14'
  exit 1
end
puts "Running on box #{box}"

    mailserver = "localhost"
    if ENV['MAIL_SERVER'].nil?
      puts "MAIL_SERVER environment variable not specified. Using #{mailserver} by default.\nTo specify it in bash: export MAIL_SERVER=localhost"
    else
      mailserver = ENV['MAIL_SERVER']
      puts "MAIL_SERVER environment variable found, using #{mailserver}"
    end

Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|
  config.vm.hostname = HOSTNAME
  config.vm.box = box
  config.vm.box_url = box_url
  config.vm.define HOSTNAME, primary: true do | standalone |

    config.vm.provider "virtualbox" do |v|
      v.customize ["modifyvm", :id, "--cpus", 2]
      v.customize ["modifyvm", :id, "--memory", "4096"]
    end

    standalone.vm.box = box


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


  if provisioner == 'puppet'
    config.vm.provision 'shell', path: 'scripts/puppet/setup.sh', args: [operating_system, environment]
    # Vagrant/Puppet docs:
    #   http://docs.vagrantup.com/v2/provisioning/puppet_apply.html
    config.vm.provision :puppet do |puppet|
      puppet.hiera_config_path = 'scripts/puppet/hiera.yaml'
      puppet.manifest_file = 'default.pp'
      puppet.manifests_path = 'scripts/puppet/manifests'
      puppet.options = "--verbose --debug --environment #{environment} --reports none"
    end
  else
    config.vm.provision "shell", path: "scripts/vagrant/setup.sh"
    config.vm.provision "shell", path: "scripts/vagrant/setup-solr.sh"
    config.vm.provision "shell", path: "scripts/vagrant/install-dataverse.sh", args: mailserver
    # FIXME: get tests working and re-enable them!
    #config.vm.provision "shell", path: "scripts/vagrant/test.sh"
  end


  config.vm.define "solr", autostart: false do |solr|
    config.vm.hostname = "solr"
    solr.vm.box = box
    config.vm.synced_folder ".", "/dataverse"
    config.vm.network "private_network", type: "dhcp"
    config.vm.network "forwarded_port", guest: 8983, host: 9001
  end

  config.vm.define "test", autostart: false do |test|
    config.vm.hostname = "test"
    test.vm.box = box
    config.vm.synced_folder ".", "/dataverse"
    config.vm.network "private_network", type: "dhcp"
  end

end
