====================================
Prerequisites
====================================

.. _introduction:

Java
----------------------------
Dataverse requires Java 8 (also known as 1.8).

Dataverse should run fine with only the Java Runtime Environment (JRE) installed, but installing the Java Development Kit (JDK) is recommended so that useful tools for troubleshooting production environments are available. We recommend using Oracle JDK or OpenJDK.

The Oracle JDK can be downloaded from http://www.oracle.com/technetwork/java/javase/downloads/index.html

On a Red Hat and similar Linux distributions, install OpenJDK with something like::

	$ yum install java-1.8.0-openjdk-devel

If you have multiple versions of Java installed, Java 8 should be the default when ``java`` is invoked from the command line. You can test this by running ``java -version``.

On Red Hat/CentOS you can make Java 8 the default with the ``alternatives`` command, having it prompt you to select the version of Java from a list::

        $ alternatives --config java

If you don't want to be prompted, here is an example of the non-interactive invocation::

        $ alternatives --set java /usr/lib/jvm/jre-1.8.0-openjdk.x86_64/bin/java

Glassfish
----------------------------

Glassfish Version 4.1 is required. 

**Important**: once Glassfish is installed, a new version of the WELD library (v2.2.10.SP1) must be downloaded and installed. This fixes a serious issue in the library supplied with Glassfish 4.1. 


- Download and install Glassfish (installed in ``/usr/local/glassfish4`` in the example commands below)::

	$ wget http://dlc-cdn.sun.com/glassfish/4.1/release/glassfish-4.1.zip
	$ unzip glassfish-4.1.zip
	$ mv glassfish4 /usr/local

- Remove the stock WELD jar; download WELD v2.2.10.SP1 and install it in the modules folder::

	$ cd /usr/local/glassfish4/glassfish/modules
	$ /bin/rm weld-osgi-bundle.jar
	$ wget http://central.maven.org/maven2/org/jboss/weld/weld-osgi-bundle/2.2.10.SP1/weld-osgi-bundle-2.2.10.SP1-glassfish4.jar
	$ /usr/local/glassfish4/bin/asadmin start-domain domain1

- Verify Weld version::

	$ /usr/local/glassfish4/bin/asadmin osgi lb | grep 'Weld OSGi Bundle'

PostgreSQL
----------------------------

1. Installation
================

Version 9.3 is recommended. 

1A. RedHat and similar systems:
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

We recommend installing Postgres from the EPEL repository::

	$ wget http://yum.postgresql.org/9.3/redhat/rhel-6-x86_64/pgdg-centos93-9.3-1.noarch.rpm
	rpm -ivh pgdg-centos93-9.3-1.noarch.rpm

	$ yum install postgresql93-server.x86_64
	$ chkconfig postgresql-9.3 on
	$ service postgresql-9.3 initdb 
	$ service postgresql-9.3 start

1B. MacOS X:
~~~~~~~~~~~~~

A distribution from `http://www.enterprisedb.com <http://www.enterprisedb.com/products-services-training/pgdownload#osx>`__ is recommended. Fink and MacPorts distributions are also readily available. See `http://www.postgresql.org/download/macosx/ <http://www.postgresql.org/download/macosx/>`__ for more information.

2. Configure access to PostgresQL for the installer script
==========================================================

- The installer script needs to have direct access to the local PostgresQL server via Unix domain sockets. This is configured by the line that starts with "local all all" in the pg_hba.conf file. The location of this file may vary depending on the distribution. But if you followed the suggested installtion instructions above, it will be ``/var/lib/pgsql/9.3/data/pg_hba.conf`` on RedHat (and similar) and ``/Library/PostgreSQL/9.3/data/pg_hba.conf`` on MacOS. Make sure the line looks like this (it will likely be pre-configured like this already)::

	local all all       peer

- If the installer still fails to connect to the databse, we recommend changing this configuration entry to ``trust``::

     	 local all all      trust

  This is a security risk, as it opens your database to anyone with a shell on your server. It does not however compromise remote access to your system. Plus you only need this configuration in place to run the installer. After it's done, you can safely reset it to how it was configured before.

3. Configure database access for the Dataverse application
==========================================================

- The application will be talking to PostgresQL over TCP/IP, using password authentication. If you are running PostgresQL on the same server as Glassfish, we strongly recommend that you use the localhost interface to connect to the databse. Make you sure you accept the default value ``localhost`` when the installer asks you for the PostgresQL server address. Then find the localhost (127.0.0.1) entry that's already in the ``pg_hba.conf`` and modify it to look like this:: 

  	host all all 127.0.0.1/32 password

- If the Dataverse application is running on a different server, you will need to add a new entry to the ``pg_hba.conf`` granting it access by its network address::

        host all all [ADDRESS]      255.255.255.255 password

  (``[ADDRESS]`` should be the numeric IP address of the Glassfish server).

- In some distributions, PostgresQL is pre-configured so that it doesn't accept network connections at all. Check that the ``listen_address`` line in the configuration file ``postgresql.conf`` is not commented-out and looks like this:: 

        listen_addresses='*' 

  The file ``postgresql.conf`` will be located in the same directory as the ``pg_hba.conf`` above.

- **Important: you must restart Postgres** for the configuration changes to take effect! On RedHat and similar (provided you installed Postgres as instructed above)::
        
        $ service postgresql-9.3 restart

  (On MacOS, you may need to restart your system, to be sure).


Solr 
---------------------------

- Download and Install Solr::
	$ wget https://archive.apache.org/dist/lucene/solr/4.6.0/solr-4.6.0.tgz
	$ tar xvzf solr-4.6.0.tgz 
	$ rsync -auv solr-4.6.0 /usr/local/
	$ cd /usr/local/solr-4.6.0/example/solr/collection1/conf/
	$ mv schema.xml schema.xml.backup
	$ wget -q --no-check-certificate https://github.com/IQSS/dataverse/raw/master/conf/solr/4.6.0/schema.xml
	
  In order to start Solr, you will need a customized schema file that is supplied in the Dataverse distribution bundle. 

Start Up Scripts
------------------

- Example of Glassfish Startup file::

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
			
