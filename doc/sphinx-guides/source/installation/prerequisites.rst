====================================
Prerequisites
====================================

.. _introduction:

Java
----------------------------
Oracle JDK 1.7.x. Use the latest available. OpenJDK should also work but we are recommending using the Oracle distribution. ::

	$ yum install java-1.7.0-openjdk-devel


Glassfish
----------------------------

Required Glassfish Versiion 4.1 is with weld v.2.2.4 module.


- Download Glassfish::

	$ wget http://dlc-cdn.sun.com/glassfish/4.1/release/glassfish-4.1.zip
	$ rsync -auv glassfish4 /usr/local
	$ cd /usr/local/glassfish4/glassfish/modules
	$ mv weld-osgi-bundle.jar weld-osgi-bundle.jar.2.2

- Download weld v.2.2.4 and copy in the modules folder::

	$ wget http://central.maven.org/maven2/org/jboss/weld/weld-osgi-bundle/2.2.4.Final/weld-osgi-bundle-2.2.4.Final.jar
	$ cp weld-osgi-bundle-2.2.4.Final.jar /usr/local/glassfish4/glassfish/modules/
	$ service glassfish start

- Verify Weld version::

	$./asadmin osgi lb | grep 'Weld OSGi Bundle'

PostgreSQL
----------------------------

- Install Postgres the EPEL repository. ::

	$ wget http://yum.postgresql.org/9.3/redhat/rhel-6-x86_64/pgdg-centos93-9.3-1.noarch.rpm
	rpm -ivh pgdg-centos93-9.3-1.noarch.rpm

- Install PostgreSQL::

	$ yum install postgresql93-server.x86_64
	$ chkconfig postgresql-9.3 on
	$ service postgresql-9.3 initdb 
	$ service postgresql-9.3 start
	$ cd /etc/init.d; mv postgresql-9.3 postgres; chmod +x postgres


- The installer script needs to have direct access to the local PostgresQL server via Unix domain sockets. So this needs to be set to either “trust” or “ident”. 
I.e., your pg_hba.conf must contain either of the 2 lines below::
	local all all ident sameuser
	or
	local all all trust

Solr 
---------------------------

- Download and Install Solr::

	$ wget https://archive.apache.org/dist/lucene/solr/4.6.0/solr-4.6.0.tgz
	$ tar xvzf solr-4.6.0.tgz 
	$ rsync -auv solr-4.6.0 /usr/local/
	$ cd /usr/local/solr-4.6.0/example/solr/collection1/conf/
	$ mv schema.xml schema.xml.backup
	$ wget -q --no-check-certificate https://github.com/IQSS/dataverse/raw/master/conf/solr/4.6.0/schema.xml
	

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

        	#echo
        	#echo "GLASSFISH IS UNDER MAINTENANCE;"
        	#echo "PLEASE DO NOT USE service init script."
        	#echo
			LANG=en_US.UTF-8; export LANG
        	$ASADMIN start-domain domain1
        	echo "."
        	;;
  		  stop)
        	echo -n "Stopping GlassFish server: glassfish"
        	#echo
        	#echo "GLASSFISH IS UNDER MAINTENANCE;"
        	#echo "PLEASE DO NOT USE service init script."
        	#echo

        	$ASADMIN stop-domain domain1
        	echo "."
        	;;

  		  *)
        	echo "Usage: /etc/init.d/glassfish {start|stop}"
        	exit 1
		esac
	exit 0
			
