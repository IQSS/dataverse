#!/bin/bash
#
# setup.sh
#
# Ensure that the packager manager's latest repository settings are up to date.
# Install the required puppet modules to provision the dataverse components.
#
# Usage: ./setup [operating system] [environment]
#
# This script will set an empty file '/opt/firstrun'
# Once there, future vagrant provisioning skip the update steps.
# Remove the file '/opt/firstrun' to repeat the update.

OPERATING_SYSTEM=$1
if [ -z "$OPERATING_SYSTEM" ] ; then
    echo "Operating system not specified"
    exit 1
fi

ENVIRONMENT=$2
if [ -z "$ENVIRONMENT" ] ; then
    echo "environment not specified."
    exit 1
fi


# puppet_config
# Set the puppet config to avoid warnings about deprecated templates.
function puppet_config {

    echo "[main]
    environment=${ENVIRONMENT}
    logdir=/var/log/puppet
    vardir=/var/lib/puppet
    ssldir=/var/lib/puppet/ssl
    rundir=/var/run/puppet
    factpath=/lib/facter

    [master]
    # This is a masterless puppet agent'," > /etc/puppet/puppet.conf
}


# install_module
# Pull a module from git and install it.
function install_module {
    name=$1
    package=$2
    repo=$3

    m=/etc/puppet/modules/$name
    if [ -d $m ] ; then rm -rf $m ; fi
    wget -O /tmp/$package $repo
    puppet module install /tmp/$package
    rm -f /tmp/$package
}

function main {

    # We will only update and install in the first provisioning step.
    # If ever you need to update again
    FIRSTRUN=/opt/firstrun
    if [ ! -f $FIRSTRUN ] ; then

        # Before we continue let us ensure we run the latests packages at the first run.
        case $OPERATING_SYSTEM in
            centos*)
                yum update
                yum -y install unzip
            ;;
            ubuntu*)
                apt-get update
                apt-get -y install unzip
            ;;
            *)
                echo "Operating system ${OPERATING_SYSTEM} not supported."
                exit 1
            ;;
        esac


        # Get the most recent puppet agent.
        sudo puppet resource package puppet ensure=latest


        # Install the puppet modules we need.
        install_module glassfish "fatmcgav-glassfish-0.6.0.tar.gz" "https://github.com/IISH/fatmcgav-glassfish/archive/dataverse.tar.gz"
        install_module iqss "iish-iqss-0.1.0.tar.gz" "https://github.com/iish/iish-iqss/archive/v0.1.0.tar.gz"


        # The puppet Iqss module provides for utility scripts that came from the dataverse code repository.
        # The code base may be ahead of those in the module, hence we copy the configuration and relevant script files to the iqss puppet module.
        files=/etc/puppet/modules/iqss/files/dataverse
        if [ ! -d $files/scripts ] ; then mkdir -p $files/scripts ; fi
        rsync -av --delete --progress --exclude puppet /dataverse/conf/             $files/conf
        rsync -av --delete --progress                  /dataverse/scripts/api/      $files/scripts/api
        rsync -av --delete --progress                  /dataverse/scripts/database/ $files/scripts/database

        touch $FIRSTRUN
    else
        echo "Repositories are already updated and puppet modules are installed. To update and reinstall, remove the file ${FIRSTRUN}"
    fi
}

main

exit 0