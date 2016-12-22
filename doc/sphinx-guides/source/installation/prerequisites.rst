=============
Prerequisites
=============

Before running the Dataverse installation script, you must install and configure the following software, preferably on a distribution of Linux such as RHEL or its derivatives such as CentOS. After following all the steps below (which have been written based on CentOS 6), you can proceed to the :doc:`installation-main` section.

You **may** find it helpful to look at how the configuration is done automatically by various tools such as Vagrant, Puppet, or Ansible. See the :doc:`prep` section for pointers on diving into these scripts.

.. contents:: :local:

Java
----

Dataverse requires Java 8 (also known as 1.8).

Installing Java
===============

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

Installing Glassfish
====================

**Important**: once Glassfish is installed, a new version of the Weld library (v2.2.10.SP1) must be downloaded and installed. This fixes a serious issue in the library supplied with Glassfish 4.1 ( see https://github.com/IQSS/dataverse/issues/647 for details). Please note that if you plan to front Glassfish with Apache you must also patch Grizzly as explained in the :doc:`shibboleth` section.

- Download and install Glassfish (installed in ``/usr/local/glassfish4`` in the example commands below)::

	# wget http://dlc-cdn.sun.com/glassfish/4.1/release/glassfish-4.1.zip
	# unzip glassfish-4.1.zip
	# mv glassfish4 /usr/local

- Remove the stock Weld jar; download Weld v2.2.10.SP1 and install it in the modules folder::

	# cd /usr/local/glassfish4/glassfish/modules
	# rm weld-osgi-bundle.jar
	# wget http://central.maven.org/maven2/org/jboss/weld/weld-osgi-bundle/2.2.10.SP1/weld-osgi-bundle-2.2.10.SP1-glassfish4.jar
	# /usr/local/glassfish4/bin/asadmin start-domain

- Verify the Weld version::

	# /usr/local/glassfish4/bin/asadmin osgi lb | grep 'Weld OSGi Bundle'

- Stop Glassfish and change from ``-client`` to ``-server`` under ``<jvm-options>-client</jvm-options>``::

	# /usr/local/glassfish4/bin/asadmin stop-domain
	# vim /usr/local/glassfish4/glassfish/domains/domain1/config/domain.xml

This recommendation comes from http://blog.c2b2.co.uk/2013/07/glassfish-4-performance-tuning.html among other places.

Glassfish Init Script
=====================

The Dataverse installation script will start Glassfish if necessary, but while you're configuring Glassfish, you might find the following init script helpful to have Glassfish start on boot.

Adjust this :download:`Glassfish init script <../_static/installation/files/etc/init.d/glassfish>` for your needs or write your own.

It is not necessary to have Glassfish running before you execute the Dataverse installation script because it will start Glassfish for you.

PostgreSQL
----------

Installing PostgreSQL
=======================

Version 9.x is required. Previous versions have not been tested.

The version that ships with RHEL 6 and above is fine::

	# yum install postgresql-server
        # service postgresql initdb
	# service postgresql start

The standard init script that ships RHEL 6 and similar should work fine. Enable it with this command::

        # chkconfig postgresql on



Configuring Database Access for the Dataverse Application (and the Dataverse Installer) 
=======================================================================================

- The application and the installer script will be connecting to PostgreSQL over TCP/IP, using password authentication. In this section we explain how to configure PostgreSQL to accept these connections.


- If PostgreSQL is running on the same server as Glassfish, find the localhost (127.0.0.1) entry that's already in the ``pg_hba.conf`` and modify it to look like this:: 

  	host all all 127.0.0.1/32 md5

  Once you are done with the prerequisites and run the installer script (documented here: :doc:`installation-main`) it will ask you to enter the address of the Postgres server. Simply accept the default value ``127.0.0.1`` there. 


- The Dataverse installer script will need to connect to PostgreSQL **as the admin user**, in order to create and set up the database that the Dataverse will be using. If for whatever reason it is failing to connect (for example, if you don't know/remember what your Postgres admin password is), you may choose to temporarily disable all the access restrictions on localhost connections, by changing the above line to::

  	host all all 127.0.0.1/32 trust

  Note that this rule opens access to the database server **via localhost only**. Still, in a production environment, this may constitute a security risk. So you will likely want to change it back to "md5" once the installer has finished.


- If the Dataverse application is running on a different server, you will need to add a new entry to the ``pg_hba.conf`` granting it access by its network address::

        host all all [ADDRESS]      255.255.255.255 md5

  Where ``[ADDRESS]`` is the numeric IP address of the Glassfish server. Enter this address when the installer asks for the PostgreSQL server address.

- In some distributions, PostgreSQL is pre-configured so that it doesn't accept network connections at all. Check that the ``listen_address`` line in the configuration file ``postgresql.conf`` is not commented out and looks like this:: 

        listen_addresses='*' 

  The file ``postgresql.conf`` will be located in the same directory as the ``pg_hba.conf`` above.

- **Important: PostgreSQL must be restarted** for the configuration changes to take effect! On RHEL and similar (provided you installed Postgres as instructed above)::
        
        # service postgresql restart

  On MacOS X a "Reload Configuration" icon is usually supplied in the PostgreSQL application folder. Or you could look up the process id of the PostgreSQL postmaster process, and send it the SIGHUP signal:: 

      	kill -1 PROCESS_ID

Solr 
----

The Dataverse search index is powered by Solr.

Installing Solr
===============

Download and install Solr with these commands::

	# wget https://archive.apache.org/dist/lucene/solr/4.6.0/solr-4.6.0.tgz
	# tar xvzf solr-4.6.0.tgz 
	# rsync -auv solr-4.6.0 /usr/local/
	# cd /usr/local/solr-4.6.0/example/solr/collection1/conf/
	# cp -a schema.xml schema.xml.orig

The reason for backing up the ``schema.xml`` file is that Dataverse requires a custom Solr schema to operate. This ``schema.xml`` file is contained in the "dvinstall" zip supplied in each Dataverse release at https://github.com/IQSS/dataverse/releases . Download this zip file, extract ``schema.xml`` from it, and put it into place (in the same directory as above)::

	# cp /tmp/schema.xml schema.xml

With the Dataverse-specific schema in place, you can now start Solr::

	# cd /usr/local/solr-4.6.0/example
	# java -jar start.jar

Solr Init Script
================

The command above will start Solr in the foreground which is good for a quick sanity check that Solr accepted the schema file, but starting Solr with an init script is recommended. You can attempt to adjust this :download:`Solr init script <../_static/installation/files/etc/init.d/solr>` for your needs or write your own.

Solr should be running before the installation script is executed.

Securing Solr
=============

Solr must be firewalled off from all hosts except the server(s) running Dataverse. Otherwise, any host  that can reach the Solr port (8983 by default) can add or delete data, search unpublished data, and even reconfigure Solr. For more information, please see https://wiki.apache.org/solr/SolrSecurity

You may want to poke a temporary hole in your firewall to play with the Solr GUI. More information on this can be found in the :doc:`/developers/dev-environment` section of the Developer Guide.

jq
--

Installing jq
=============

``jq`` is a command line tool for parsing JSON output that is used by the Dataverse installation script. https://stedolan.github.io/jq explains various ways of installing it, but a relatively straightforward method is described below. Please note that you must download the 64- or 32-bit version based on your architecture. In the example below, the 64-bit version is installed. We confirm it's executable and in our ``$PATH`` by checking the version (1.4 or higher should be fine):: 

        # cd /usr/bin
        # wget http://stedolan.github.io/jq/download/linux64/jq
        # chmod +x jq
        # jq --version

ImageMagick
-----------

Dataverse uses `ImageMagick <https://www.imagemagick.org>`_ to generate thumbnail previews of PDF files. This is an optional component, meaning that if you don't have ImageMagick installed, there will be no thumbnails for PDF files, in the search results and on the dataset pages; but everything else will be working. (Thumbnail previews for non-PDF image files are generated using standard Java libraries and do not require any special installation steps). 

Installing and configuring ImageMagick
======================================

On a Red Hat and similar Linux distributions, you can install ImageMagick with something like::

	# yum install ImageMagick 

(most RedHat systems will have it pre-installed). 
When installed using standard ``yum`` mechanism, above, the executable for the ImageMagick convert utility will be located at ``/usr/bin/convert``. No further configuration steps will then be required. 

On MacOS you can compile ImageMagick from sources, or use one of the popular installation frameworks, such as brew. 

If the installed location of the convert executable is different from ``/usr/bin/convert``, you will also need to specify it in your Glassfish configuration using the JVM option, below. For example::

   <jvm-options>-Ddataverse.path.imagemagick.convert=/opt/local/bin/convert</jvm-options>

(see the :doc:`config` section for more information on the JVM options)



Now that you have all the prerequisites in place, you can proceed to the :doc:`installation-main` section.


