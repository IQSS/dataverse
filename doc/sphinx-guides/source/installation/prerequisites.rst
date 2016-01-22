=============
Prerequisites
=============

Before running the Dataverse installation script, you must install and configure the following software, preferably on a distribution of Linux such as RHEL or its derivatives such as CentOS. After following all the steps below (which have been written based on CentOS 6), you can proceed to the :doc:`installation-main` section.

You **may** find it helpful to look at how the configuration is done automatically by various tools such as Vagrant, Puppet, or Ansible. See the :doc:`prep` section for pointers on diving into these scripts.

.. contents:: :local:

Java
----
Dataverse requires Java 8 (also known as 1.8).

Dataverse should run fine with only the Java Runtime Environment (JRE) installed, but installing the Java Development Kit (JDK) is recommended so that useful tools for troubleshooting production environments are available. We recommend using Oracle JDK or OpenJDK.

The Oracle JDK can be downloaded from http://www.oracle.com/technetwork/java/javase/downloads/index.html

On a Red Hat and similar Linux distributions, install OpenJDK with something like::

	# yum install java-1.8.0-openjdk-devel

If you have multiple versions of Java installed, Java 8 should be the default when ``java`` is invoked from the command line. You can test this by running ``java -version``.

On Red Hat/CentOS you can make Java 8 the default with the ``alternatives`` command, having it prompt you to select the version of Java from a list::

        # alternatives --config java

If you don't want to be prompted, here is an example of the non-interactive invocation::

        # alternatives --set java /usr/lib/jvm/jre-1.8.0-openjdk.x86_64/bin/java

Glassfish
---------

Glassfish Version 4.1 is required. There are known issues with Glassfish 4.1.1 as chronicled in https://github.com/IQSS/dataverse/issues/2628 so it should be avoided until that issue is resolved.

**Important**: once Glassfish is installed, a new version of the Weld library (v2.2.10.SP1) must be downloaded and installed. This fixes a serious issue in the library supplied with Glassfish 4.1 ( see https://github.com/IQSS/dataverse/issues/647 for details).


- Download and install Glassfish (installed in ``/usr/local/glassfish4`` in the example commands below)::

	# wget http://dlc-cdn.sun.com/glassfish/4.1/release/glassfish-4.1.zip
	# unzip glassfish-4.1.zip
	# mv glassfish4 /usr/local

- Remove the stock Weld jar; download Weld v2.2.10.SP1 and install it in the modules folder::

	# cd /usr/local/glassfish4/glassfish/modules
	# rm weld-osgi-bundle.jar
	# wget http://central.maven.org/maven2/org/jboss/weld/weld-osgi-bundle/2.2.10.SP1/weld-osgi-bundle-2.2.10.SP1-glassfish4.jar
	# /usr/local/glassfish4/bin/asadmin start-domain domain1

- Verify the Weld version::

	# /usr/local/glassfish4/bin/asadmin osgi lb | grep 'Weld OSGi Bundle'

The Dataverse installation script will start Glassfish if necessary, but while you're configuring Glassfish, you might find the following init script helpful to have Glassfish start on boot::

	set -e
	ASADMIN=/usr/local/glassfish4/bin/asadmin
	case "$1" in
  	start)
        	echo -n "Starting GlassFish server: glassfish"
        	# Increase file descriptor limit:
        	ulimit -n 32768
        	# Allow "memory overcommit":
        	# (basically, this allows to run exec() calls from inside the
        	# app, without the Unix fork() call physically hogging 2X
        	# the amount of memory glassfish is already using)
        	echo 1 > /proc/sys/vm/overcommit_memory

		# Set UTF8 as the default encoding:
		LANG=en_US.UTF-8; export LANG
        	$ASADMIN start-domain domain1
        	echo "."
        	;;
  		  stop)
        	echo -n "Stopping GlassFish server: glassfish"

        	$ASADMIN stop-domain domain1
        	echo "."
        	;;

  		  *)
        	echo "Usage: /etc/init.d/glassfish {start|stop}"
        	exit 1
		esac
	exit 0
			
PostgreSQL
----------

1. Installation
================

Version 9.x is required. Previous versions have not been tested.

1A. RHEL and similar systems:
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The version that ships with RHEL 6 and above is fine::

	# yum install postgresql-server
        # chkconfig postgresql on
        # service postgresql initdb
	# service postgresql start

2. Configure access to PostgreSQL for the installer script
==========================================================

- When using localhost for the database server, the installer script needs to have direct access to the local PostgreSQL server via Unix domain sockets. This is configured by the line that starts with ``local all all`` in the pg_hba.conf file. The location of this file may vary depending on the distribution. But if you followed the suggested installation instructions above, it will be ``/var/lib/pgsql/data/pg_hba.conf`` on RHEL and similar. Make sure the line looks like this (it will likely be pre-configured like this already)::

	local all all       peer

- If the installer still fails to connect to the databse, we recommend changing this configuration entry to ``trust``::

     	 local all all      trust

This is a security risk, as it opens your database to anyone with a shell on your server. It does not however compromise remote access to your system. Plus you only need this configuration in place to run the installer. After it's done, you can safely reset it to how it was configured before.

3. Configure database access for the Dataverse application
==========================================================

- The application will be talking to PostgreSQL over TCP/IP, using password authentication. If you are running PostgreSQL on the same server as Glassfish, we strongly recommend that you use the localhost interface to connect to the database. Make you sure you accept the default value ``localhost`` when the installer asks you for the PostgreSQL server address. Then find the localhost (127.0.0.1) entry that's already in the ``pg_hba.conf`` and modify it to look like this:: 

  	host all all 127.0.0.1/32 password

- If the Dataverse application is running on a different server, you will need to add a new entry to the ``pg_hba.conf`` granting it access by its network address::

        host all all [ADDRESS]      255.255.255.255 password

  (``[ADDRESS]`` should be the numeric IP address of the Glassfish server).

- In some distributions, PostgreSQL is pre-configured so that it doesn't accept network connections at all. Check that the ``listen_address`` line in the configuration file ``postgresql.conf`` is not commented-out and looks like this:: 

        listen_addresses='*' 

  The file ``postgresql.conf`` will be located in the same directory as the ``pg_hba.conf`` above.

- **Important: you must restart Postgres** for the configuration changes to take effect! On RHEL and similar (provided you installed Postgres as instructed above)::
        
        # service postgresql-9.3 restart


Solr 
----

- Download and Install Solr::

	# wget https://archive.apache.org/dist/lucene/solr/4.6.0/solr-4.6.0.tgz
	# tar xvzf solr-4.6.0.tgz 
	# rsync -auv solr-4.6.0 /usr/local/
	# cd /usr/local/solr-4.6.0/example/solr/collection1/conf/
	# cp -a schema.xml schema.xml.orig

The reason for backing up the ``schema.xml`` file is that Dataverse requires a custom Solr schema to operate. This ``schema.xml`` file is contained in the "dvinstall" zip supplied in each Dataverse release at https://github.com/IQSS/dataverse/releases . Download this zip file, extract ``schema.xml`` from it, and put it into place (in the same directory as above)::

	# cp /tmp/schema.xml schema.xml

With the Dataverse-specific schema in place, you can now start Solr::

	# java -jar start.jar

The command above will start Solr in the foreground which is good for a quick sanity check that Solr accepted the schema file, but you'll want to put the process in the background by appending `` &`` or by using an init script. The Vagrant environment uses this init script for Solr but your mileage may vary: https://github.com/IQSS/dataverse/blob/develop/conf/vagrant/etc/init.d/solr

Solr should be running before the installation script is executed.

Securing Solr
=============

Solr must be firewalled off from all hosts except the server(s) running Dataverse. Otherwise, any host  that can reach the Solr port (8983 by default) can add or delete data, search unpublished data, and     even reconfigure Solr. For more information, please see https://wiki.apache.org/solr/SolrSecurity

jq
--

``jq`` is a command line tool for parsing JSON output that is used by the Dataverse installation script. https://stedolan.github.io/jq explains various ways of installing it, but a relatively straightforward method is described below. Please note that you must download the 64- or 32-bit version based on your architecture. In the example below, the 64-bit version is installed. We confirm it's executable and in our ``$PATH`` by checking the version (1.4 or higher should be fine):: 

        # cd /usr/bin
        # wget http://stedolan.github.io/jq/download/linux64/jq
        # chmod +x jq
        # jq --version

Now that you have all the prerequisites in place, you can proceed to the :doc:`installation-main` section.
