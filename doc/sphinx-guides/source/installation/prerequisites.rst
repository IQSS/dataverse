.. role:: fixedwidthplain

=============
Prerequisites
=============

Before running the Dataverse installation script, you must install and configure the following software.

After following all the steps below, you can proceed to the :doc:`installation-main` section.

You **may** find it helpful to look at how the configuration is done automatically by various tools such as Vagrant, Puppet, or Ansible. See the :doc:`prep` section for pointers on diving into these scripts.

.. contents:: |toctitle|
	:local:

Linux
-----

We assume you plan to run Dataverse on Linux and we recommend RHEL/CentOS, which is the Linux distribution tested by the Dataverse development team. Please be aware that while el7 (RHEL/CentOS 7) is the recommended platform, the steps below were orginally written for el6 and may need to be updated (please feel free to make a pull request!).

Java
----

Dataverse requires Java SE 8 (8u74/JDK 1.8.0u74 or higher).

Installing Java
===============

Dataverse should run fine with only the Java Runtime Environment (JRE) installed, but installing the Java Development Kit (JDK) is recommended so that useful tools for troubleshooting production environments are available. We recommend using Oracle JDK or OpenJDK.

The Oracle JDK can be downloaded from http://www.oracle.com/technetwork/java/javase/downloads/index.html

On a RHEL/CentOS, install OpenJDK (devel version) using yum::

	# yum install java-1.8.0-openjdk-devel

If you have multiple versions of Java installed, Java 8 should be the default when ``java`` is invoked from the command line. You can test this by running ``java -version``.

On RHEL/CentOS you can make Java 8 the default with the ``alternatives`` command, having it prompt you to select the version of Java from a list::

        # alternatives --config java

If you don't want to be prompted, here is an example of the non-interactive invocation::

        # alternatives --set java /usr/lib/jvm/jre-1.8.0-openjdk.x86_64/bin/java

Glassfish
---------

Glassfish Version 4.1 is required. There are known issues with newer versions of the Glassfish 4.x series so it should be avoided. For details, see https://github.com/IQSS/dataverse/issues/2628 . The issue we are using the track support for Glassfish 5 is https://github.com/IQSS/dataverse/issues/4248 .

Installing Glassfish
====================

**Note:** The Dataverse installer need not be run as root, and it is recommended that Glassfish not run as root either. We suggest the creation of a glassfish service account for this purpose.

- Download and install Glassfish (installed in ``/usr/local/glassfish4`` in the example commands below)::

	# wget http://dlc-cdn.sun.com/glassfish/4.1/release/glassfish-4.1.zip
	# unzip glassfish-4.1.zip
	# mv glassfish4 /usr/local

If you intend to install and run Glassfish under a service account (and we hope you do), chown -R the Glassfish hierarchy to root to protect it but give the service account access to the below directories:

- Set service account permissions::

	# chown -R root:root /usr/local/glassfish4
	# chown glassfish /usr/local/glassfish4/glassfish/lib
	# chown -R glassfish:glassfish /usr/local/glassfish4/glassfish/domains/domain1

After installation, you may chown the lib/ directory back to root; the installer only needs write access to copy the JDBC driver into that directory.

Once Glassfish is installed, you'll need a newer version of the Weld library (v2.2.10.SP1) to fix a serious issue in the library supplied with Glassfish 4.1 (see https://github.com/IQSS/dataverse/issues/647 for details). If you plan to front Glassfish with Apache you must also patch Grizzly as explained in the :doc:`shibboleth` section.

- Remove the stock Weld jar; download Weld v2.2.10.SP1 and install it in the modules folder::

	# cd /usr/local/glassfish4/glassfish/modules
	# rm weld-osgi-bundle.jar
	# wget http://central.maven.org/maven2/org/jboss/weld/weld-osgi-bundle/2.2.10.SP1/weld-osgi-bundle-2.2.10.SP1-glassfish4.jar

- Change from ``-client`` to ``-server`` under ``<jvm-options>-client</jvm-options>``::

	# vim /usr/local/glassfish4/glassfish/domains/domain1/config/domain.xml

This recommendation comes from http://www.c2b2.co.uk/middleware-blog/glassfish-4-performance-tuning-monitoring-and-troubleshooting.php among other places.

- Start Glassfish and verify the Weld version::

	# /usr/local/glassfish4/bin/asadmin start-domain
	# /usr/local/glassfish4/bin/asadmin osgi lb | grep 'Weld OSGi Bundle'

The Certificate Authority (CA) certificate bundle file from Glassfish contains certs that expired in August 2018, causing problems with ORCID login.

- The actual expiration date is August 22, 2018, which you can see with the following command::

	# keytool -list -v -keystore /usr/local/glassfish4/glassfish/domains/domain1/config/cacerts.jks

- Overwrite Glassfish's CA certs file with the file that ships with the operating system and restart Glassfish::

	# cp /etc/pki/ca-trust/extracted/java/cacerts /usr/local/glassfish4/glassfish/domains/domain1/config/cacerts.jks
	# /usr/local/glassfish4/bin/asadmin stop-domain
	# /usr/local/glassfish4/bin/asadmin start-domain

Launching Glassfish on system boot
==================================

The Dataverse installation script will start Glassfish if necessary, but you may find the following scripts helpful to launch Glassfish start automatically on boot.

- This :download:`Systemd file<../_static/installation/files/etc/systemd/glassfish.service>` may be serve as a reference for systems using Systemd (such as RHEL/CentOS 7 or Ubuntu 16+)
- This :download:`init script<../_static/installation/files/etc/init.d/glassfish.init.service>` may be useful for RHEL/CentOS 6 or Ubuntu >= 14 if you're using a Glassfish service account, or
- This :download:`Glassfish init script <../_static/installation/files/etc/init.d/glassfish.init.root>` may be helpful if you're just going to run Glassfish as root.

It is not necessary for Glassfish to be running before you execute the Dataverse installation script; it will start Glassfish for you.

Please note that you must run Glassfish in an English locale. If you are using something like ``LANG=de_DE.UTF-8``, ingest of tabular data will fail with the message "RoundRoutines:decimal separator no in right place".

Also note that Glassfish may utilize more than the default number of file descriptors, especially when running batch jobs such as harvesting. We have increased ours by adding ulimit -n 32768 to our glassfish init script. On operating systems which use systemd such as RHEL or CentOS 7, file descriptor limits may be increased by adding a line like LimitNOFILE=32768 to the systemd unit file. You may adjust the file descriptor limits on running processes by using the prlimit utility::

	# sudo prlimit --pid pid --nofile=32768:32768

PostgreSQL
----------

Installing PostgreSQL
=======================

Version 9.x is required. Previous versions have not been tested.

Version 9.6 is anticipated as an "LTS" release in RHEL and on other platforms::

	# yum install -y https://download.postgresql.org/pub/repos/yum/9.6/redhat/rhel-7-x86_64/pgdg-centos96-9.6-3.noarch.rpm
	# yum makecache fast
	# yum install -y postgresql96-server
	# /usr/pgsql-9.6/bin/postgresql96-setup initdb
	# /usr/bin/systemctl start postgresql-9.6
	# /usr/bin/systemctl enable postgresql-9.6
	
Note these steps are specific to RHEL/CentOS 7. For RHEL/CentOS 6 use::

	# service postgresql-9.6 initdb
	# service postgresql-9.6 start

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

- **Important: PostgreSQL must be restarted** for the configuration changes to take effect! On RHEL/CentOS 7 and similar (provided you installed Postgres as instructed above)::

        # systemctl restart postgresql-9.6

  or on RHEL/CentOS 6::

        # service postgresql restart

  On MacOS X a "Reload Configuration" icon is usually supplied in the PostgreSQL application folder. Or you could look up the process id of the PostgreSQL postmaster process, and send it the SIGHUP signal:: 

      	kill -1 PROCESS_ID

Solr 
----

The Dataverse search index is powered by Solr.

Installing Solr
===============

You should not run Solr as root. Create a user called ``solr`` and a directory to install Solr into::

        useradd solr
        mkdir /usr/local/solr
        chown solr:solr /usr/local/solr

Become the ``solr`` user and then download and configure Solr::

        su - solr
        cd /usr/local/solr
        wget https://archive.apache.org/dist/lucene/solr/7.3.0/solr-7.3.0.tgz
        tar xvzf solr-7.3.0.tgz
        cd solr-7.3.0
        cp -r server/solr/configsets/_default server/solr/collection1

You should already have a "dvinstall.zip" file that you downloaded from https://github.com/IQSS/dataverse/releases . Unzip it into ``/tmp``. Then copy the files into place::

        cp /tmp/dvinstall/schema.xml /usr/local/solr/solr-7.3.0/server/solr/collection1/conf
        cp /tmp/dvinstall/solrconfig.xml /usr/local/solr/solr-7.3.0/server/solr/collection1/conf

Note: Dataverse has customized Solr to boost results that come from certain indexed elements inside Dataverse, for example prioritizing results from Dataverses over Datasets. If you would like to remove this, edit your ``solrconfig.xml`` and remove the ``<str name="qf">`` element and its contents. If you have ideas about how this boosting could be improved, feel free to contact us through our Google Group https://groups.google.com/forum/#!forum/dataverse-dev .

Dataverse requires a change to the ``jetty.xml`` file that ships with Solr. Edit ``/usr/local/solr/solr-7.3.0/server/etc/jetty.xml`` , increasing ``requestHeaderSize`` from ``8192`` to ``102400``

Solr will warn about needing to increase the number of file descriptors and max processes in a production environment but will still run with defaults. We have increased these values to the recommended levels by adding ulimit -n 65000 to the init script, and the following to ``/etc/security/limits.conf``::

        solr soft nproc 65000
        solr hard nproc 65000
        solr soft nofile 65000
        solr hard nofile 65000

On operating systems which use systemd such as RHEL or CentOS 7, you may then add a line like LimitNOFILE=65000 to the systemd unit file, or adjust the limits on a running process using the prlimit tool::

        # sudo prlimit --pid pid --nofile=65000:65000

Solr launches asynchronously and attempts to use the ``lsof`` binary to watch for its own availability. Installation of this package isn't required but will prevent a warning in the log at startup.

Finally, you may start Solr and create the core that will be used to manage search information::

        cd /usr/local/solr/solr-7.3.0
        bin/solr start
        bin/solr create_core -c collection1 -d server/solr/collection1/conf/
	

Solr Init Script
================

For systems running systemd, as root, download :download:`solr.service<../_static/installation/files/etc/systemd/solr.service>` and place it in ``/tmp``. Then start Solr and configure it to start at boot with the following commands::

        cp /tmp/solr.service /usr/lib/systemd/system
        systemctl start solr.service
        systemctl enable solr.service

For systems using init.d, download this :download:`Solr init script <../_static/installation/files/etc/init.d/solr>` and place it in ``/tmp``. Then start Solr and configure it to start at boot with the following commands::

        cp /tmp/solr /etc/init.d
        service solr start
        chkconfig solr on

Securing Solr
=============

Solr must be firewalled off from all hosts except the server(s) running Dataverse. Otherwise, any host  that can reach the Solr port (8983 by default) can add or delete data, search unpublished data, and even reconfigure Solr. For more information, please see https://lucene.apache.org/solr/guide/7_2/securing-solr.html

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

R
-

Dataverse uses `R <https://https://cran.r-project.org/>`_ to handle
tabular data files. The instructions below describe a **minimal** R
installation. It will allow you to ingest R (.RData) files as tabular
data; to export tabular data as .RData files; and to run `Data
Explorer <https://github.com/scholarsportal/Dataverse-Data-Explorer>`_
(specifically, R is used to generate .prep metadata files that Data
Explorer uses).  R can be considered an optional component, meaning
that if you don't have R installed, you will still be able to run and
use Dataverse - but the functionality specific to tabular data
mentioned above will not be available to your users.  **Note** that if
you choose to also install `TwoRavens
<https://github.com/IQSS/TwoRavens>`_, it will require some extra R
components and libraries.  Please consult the instructions in the
TowRavens section of the Installation Guide.


Installing R
============

Can be installed with :fixedwidthplain:`yum`::

       yum install R-core R-core-devel

EPEL distribution is strongly recommended. The version of R currently available from epel6 and epel7 is 3.5; it has been tested and is known to work on RedHat and CentOS versions 6 and 7.

If :fixedwidthplain:`yum` isn't configured to use EPEL repositories ( https://fedoraproject.org/wiki/EPEL ):

RHEL/CentOS users can install the RPM :fixedwidthplain:`epel-release`. For RHEL/CentOS 7::

       yum install https://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm

RHEL/CentOS users can install the RPM :fixedwidthplain:`epel-release`. For RHEL/CentOS 6::

       yum install https://dl.fedoraproject.org/pub/epel/epel-release-latest-6.noarch.rpm

RHEL users will want to log in to their organization's respective RHN interface, find the particular machine in question and:

• click on "Subscribed Channels: Alter Channel Subscriptions"
• enable EPEL, Server Extras, Server Optional

Installing the required R libraries
===================================

The following R packages (libraries) are required::

    R2HTML
    rjson
    DescTools
    Rserve
    haven

Install them following the normal R package installation procedures. For example, with the following R commands::

	install.packages("R2HTML", repos="https://cloud.r-project.org/", lib="/usr/lib64/R/library" )
	install.packages("rjson", repos="https://cloud.r-project.org/", lib="/usr/lib64/R/library" )
	install.packages("DescTools", repos="https://cloud.r-project.org/", lib="/usr/lib64/R/library" )
	install.packages("Rserve", repos="https://cloud.r-project.org/", lib="/usr/lib64/R/library" )
	install.packages("haven", repos="https://cloud.r-project.org/", lib="/usr/lib64/R/library" )

Rserve
======

Dataverse uses `Rserve <https://rforge.net/Rserve/>`_ to communicate
to R. Rserve is installed as a library package, as described in the
step above. It runs as a daemon process on the server, accepting
network connections on a dedicated port. This requires some extra 
configuration and we provide a  script (:fixedwidthplain:`scripts/r/rserve/rserve-setup.sh`) for setting it up.  
Run the script as follows (as root)::

    cd <DATAVERSE SOURCE TREE>/scripts/r/rserve
    ./rserve-setup.sh

The setup script will create a system user :fixedwidthplain:`rserve`
that will run the daemon process.  It will install the startup script
for the daemon (:fixedwidthplain:`/etc/init.d/rserve`), so that it
gets started automatically when the system boots.  This is an
:fixedwidthplain:`init.d`-style startup file. If this is a
RedHat/CentOS 7 system, you may want to use the
:fixedwidthplain:`systemctl`-style file
:fixedwidthplain:`rserve.service` instead. (Copy it into the
:fixedwidthplain:`/usr/lib/systemd/system/` directory)



Note that the setup will also set the Rserve password to
":fixedwidthplain:`rserve`".  Rserve daemon runs under a
non-privileged user id, so there's not much potential for security
damage through unauthorized access. It is however still a good idea
**to change the password**. The password is specified in
:fixedwidthplain:`/etc/Rserv.pwd`.  You can consult `Rserve
documentation <https://rforge.net/Rserve/doc.html>`_ for more
information on password encryption and access security.

You should already have the following 4 JVM options added to your
:fixedwidthplain:`domain.xml` by the Dataverse installer::

        <jvm-options>-Ddataverse.rserve.host=localhost</jvm-options>
        <jvm-options>-Ddataverse.rserve.port=6311</jvm-options>
        <jvm-options>-Ddataverse.rserve.user=rserve</jvm-options>
        <jvm-options>-Ddataverse.rserve.password=rserve</jvm-options>

If you have changed the password, make sure it is correctly specified
in the :fixedwidthplain:`dataverse.rserve.password` option above.  If
Rserve is running on a host that's different from your Dataverse
server, change the :fixedwidthplain:`dataverse.rserve.host` option
above as well (and make sure the port 6311 on the Rserve host is not
firewalled from your Dataverse host).

Now that you have all the prerequisites in place, you can proceed to the :doc:`installation-main` section.


