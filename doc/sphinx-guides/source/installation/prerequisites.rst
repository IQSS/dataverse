.. role:: fixedwidthplain

=============
Prerequisites
=============

Before running the Dataverse Software installation script, you must install and configure Linux, Java, Payara, PostgreSQL, Solr, and jq. The other software listed below is optional but can provide useful features.

After following all the steps below, you can proceed to the :doc:`installation-main` section.

.. contents:: |toctitle|
	:local:

Linux
-----

We assume you plan to run your Dataverse installation on Linux and we recommend RHEL or a derivative such as RockyLinux or AlmaLinux, which is the distribution family tested by the Dataverse Project team. Please be aware that while EL8 (RHEL/derivatives) is the recommended platform, the steps below were orginally written for EL6 and may need to be updated (please feel free to make a pull request!). A number of community members have installed the Dataverse Software in Debian/Ubuntu environments.

Java
----

The Dataverse Software requires Java SE 11 (or higher).

Installing Java
===============

The Dataverse Software should run fine with only the Java Runtime Environment (JRE) installed, but installing the Java Development Kit (JDK) is recommended so that useful tools for troubleshooting production environments are available. We recommend using Oracle JDK or OpenJDK.

The Oracle JDK can be downloaded from http://www.oracle.com/technetwork/java/javase/downloads/index.html

On a RHEL/derivative, install OpenJDK (devel version) using yum::

	# sudo yum install java-11-openjdk

If you have multiple versions of Java installed, Java 11 should be the default when ``java`` is invoked from the command line. You can test this by running ``java -version``.

On RHEL/derivative you can make Java 11 the default with the ``alternatives`` command, having it prompt you to select the version of Java from a list::

        # alternatives --config java


.. _payara:

Payara
------

Payara 5.2022.3 is recommended. Newer versions might work fine, regular updates are recommended.

Installing Payara
=================

**Note:** The Dataverse Software installer need not be run as root, and it is recommended that Payara not run as root either. We suggest the creation of a "dataverse" service account for this purpose::

	# useradd dataverse

- Download and install Payara (installed in ``/usr/local/payara5`` in the example commands below)::

	# wget https://s3-eu-west-1.amazonaws.com/payara.fish/Payara+Downloads/5.2022.3/payara-5.2022.3.zip
	# unzip payara-5.2022.3.zip
	# mv payara5 /usr/local

If you intend to install and run Payara under a service account (and we hope you do), chown -R the Payara hierarchy to root to protect it but give the service account access to the below directories:

- Set service account permissions::

	# chown -R root:root /usr/local/payara5
	# chown dataverse /usr/local/payara5/glassfish/lib
	# chown -R dataverse:dataverse /usr/local/payara5/glassfish/domains/domain1

After installation, you may chown the lib/ directory back to root; the installer only needs write access to copy the JDBC driver into that directory.

- Change from ``-client`` to ``-server`` under ``<jvm-options>-client</jvm-options>``::

	# vim /usr/local/payara5/glassfish/domains/domain1/config/domain.xml

This recommendation comes from http://www.c2b2.co.uk/middleware-blog/glassfish-4-performance-tuning-monitoring-and-troubleshooting.php among other places.

Launching Payara on System Boot
===============================

The Dataverse Software installation script will start Payara if necessary, but you may find the following scripts helpful to launch Payara start automatically on boot. They were originally written for Glassfish but have been adjusted for Payara.

- This :download:`Systemd file<../_static/installation/files/etc/systemd/payara.service>` may be serve as a reference for systems using Systemd (such as RHEL/derivative or Debian 10, Ubuntu 16+)
- This :download:`init script<../_static/installation/files/etc/init.d/payara.init.service>` may be useful for RHEL/derivative or Ubuntu >= 14 if you're using a Payara service account, or
- This :download:`Payara init script <../_static/installation/files/etc/init.d/payara.init.root>` may be helpful if you're just going to run Payara as root (not recommended).

It is not necessary for Payara to be running before you execute the Dataverse Software installation script; it will start Payara for you.

Please note that you must run Payara in an English locale. If you are using something like ``LANG=de_DE.UTF-8``, ingest of tabular data will fail with the message "RoundRoutines:decimal separator no in right place".

Also note that Payara may utilize more than the default number of file descriptors, especially when running batch jobs such as harvesting. We have increased ours by adding ulimit -n 32768 to our Payara init script. On operating systems which use systemd such as RHEL/derivative, file descriptor limits may be increased by adding a line like LimitNOFILE=32768 to the systemd unit file. You may adjust the file descriptor limits on running processes by using the prlimit utility::

	# sudo prlimit --pid pid --nofile=32768:32768

PostgreSQL
----------

Installing PostgreSQL
=====================

The application has been tested with PostgreSQL versions up to 13 and version 10+ is required. We recommend installing the latest version that is available for your OS distribution. *For example*, to install PostgreSQL 13 under RHEL7/derivative::

	# yum install -y https://download.postgresql.org/pub/repos/yum/reporpms/EL-7-x86_64/pgdg-redhat-repo-latest.noarch.rpm
	# yum makecache fast
	# yum install -y postgresql13-server
	# /usr/pgsql-13/bin/postgresql-13-setup initdb
	# /usr/bin/systemctl start postgresql-13
	# /usr/bin/systemctl enable postgresql-13

For RHEL8/derivative the process would be identical, except for the first two commands: you would need to install the "EL-8" yum repository configuration and run ``yum makecache`` instead.

Configuring Database Access for the Dataverse Installation (and the Dataverse Software Installer)
=================================================================================================

- The application and the installer script will be connecting to PostgreSQL over TCP/IP, using password authentication. In this section we explain how to configure PostgreSQL to accept these connections.


- If PostgreSQL is running on the same server as Payara, find the localhost (127.0.0.1) entry that's already in the ``pg_hba.conf`` and modify it to look like this::

  	host all all 127.0.0.1/32 md5

  Once you are done with the prerequisites and run the installer script (documented here: :doc:`installation-main`) it will ask you to enter the address of the Postgres server. Simply accept the default value ``127.0.0.1`` there.


- The Dataverse Software installer script will need to connect to PostgreSQL **as the admin user**, in order to create and set up the database that the Dataverse installation will be using. If for whatever reason it is failing to connect (for example, if you don't know/remember what your Postgres admin password is), you may choose to temporarily disable all the access restrictions on localhost connections, by changing the above line to::

  	host all all 127.0.0.1/32 trust

  Note that this rule opens access to the database server **via localhost only**. Still, in a production environment, this may constitute a security risk. So you will likely want to change it back to "md5" once the installer has finished.


- If the Dataverse installation is running on a different server, you will need to add a new entry to the ``pg_hba.conf`` granting it access by its network address::

        host all all [ADDRESS]      255.255.255.255 md5

  Where ``[ADDRESS]`` is the numeric IP address of the Payara server. Enter this address when the installer asks for the PostgreSQL server address.

- In some distributions, PostgreSQL is pre-configured so that it doesn't accept network connections at all. Check that the ``listen_address`` line in the configuration file ``postgresql.conf`` is not commented out and looks like this::

        listen_addresses='*'

  The file ``postgresql.conf`` will be located in the same directory as the ``pg_hba.conf`` above.

- **Important: PostgreSQL must be restarted** for the configuration changes to take effect! On RHEL7/derivative and similar (provided you installed Postgres as instructed above)::

        # systemctl restart postgresql-13

  On MacOS X a "Reload Configuration" icon is usually supplied in the PostgreSQL application folder. Or you could look up the process id of the PostgreSQL postmaster process, and send it the SIGHUP signal::

      	kill -1 PROCESS_ID

Solr
----

The Dataverse Software search index is powered by Solr.

Supported Versions
==================

The Dataverse Software has been tested with Solr version 8.11.1. Future releases in the 8.x series are likely to be compatible; however, this cannot be confirmed until they are officially tested. Major releases above 8.x (e.g. 9.x) are not supported.

Installing Solr
===============

You should not run Solr as root. Create a user called ``solr`` and a directory to install Solr into::

        useradd solr
        mkdir /usr/local/solr
        chown solr:solr /usr/local/solr

Become the ``solr`` user and then download and configure Solr::

        su - solr
        cd /usr/local/solr
        wget https://archive.apache.org/dist/lucene/solr/8.11.1/solr-8.11.1.tgz
        tar xvzf solr-8.11.1.tgz
        cd solr-8.11.1
        cp -r server/solr/configsets/_default server/solr/collection1

You should already have a "dvinstall.zip" file that you downloaded from https://github.com/IQSS/dataverse/releases . Unzip it into ``/tmp``. Then copy the files into place::

        cp /tmp/dvinstall/schema*.xml /usr/local/solr/solr-8.11.1/server/solr/collection1/conf
        cp /tmp/dvinstall/solrconfig.xml /usr/local/solr/solr-8.11.1/server/solr/collection1/conf

Note: The Dataverse Project team has customized Solr to boost results that come from certain indexed elements inside the Dataverse installation, for example prioritizing results from Dataverse collections over Datasets. If you would like to remove this, edit your ``solrconfig.xml`` and remove the ``<str name="qf">`` element and its contents. If you have ideas about how this boosting could be improved, feel free to contact us through our Google Group https://groups.google.com/forum/#!forum/dataverse-dev .

A Dataverse installation requires a change to the ``jetty.xml`` file that ships with Solr. Edit ``/usr/local/solr/solr-8.11.1/server/etc/jetty.xml`` , increasing ``requestHeaderSize`` from ``8192`` to ``102400``

Solr will warn about needing to increase the number of file descriptors and max processes in a production environment but will still run with defaults. We have increased these values to the recommended levels by adding ulimit -n 65000 to the init script, and the following to ``/etc/security/limits.conf``::

        solr soft nproc 65000
        solr hard nproc 65000
        solr soft nofile 65000
        solr hard nofile 65000

On operating systems which use systemd such as RHEL/derivative, you may then add a line like LimitNOFILE=65000 for the number of open file descriptors and a line with LimitNPROC=65000 for the max processes to the systemd unit file, or adjust the limits on a running process using the prlimit tool::

        # sudo prlimit --pid pid --nofile=65000:65000

Solr launches asynchronously and attempts to use the ``lsof`` binary to watch for its own availability. Installation of this package isn't required but will prevent a warning in the log at startup::

	# yum install lsof

Finally, you need to tell Solr to create the core "collection1" on startup::

        echo "name=collection1" > /usr/local/solr/solr-8.11.1/server/solr/collection1/core.properties

Solr Init Script
================

Please choose the right option for your underlying Linux operating system.
It will not be necessary to execute both!

For systems running systemd (like RedHat or derivatives since 7, Debian since 9, Ubuntu since 15.04), as root, download :download:`solr.service<../_static/installation/files/etc/systemd/solr.service>` and place it in ``/tmp``. Then start Solr and configure it to start at boot with the following commands::

        cp /tmp/solr.service /etc/systemd/system
        systemctl daemon-reload
        systemctl start solr.service
        systemctl enable solr.service

For systems using init.d (like CentOS 6), download this :download:`Solr init script <../_static/installation/files/etc/init.d/solr>` and place it in ``/tmp``. Then start Solr and configure it to start at boot with the following commands::

        cp /tmp/solr /etc/init.d
        service start solr
        chkconfig solr on

Securing Solr
=============

Our sample init script and systemd service file linked above tell Solr to only listen on localhost (127.0.0.1). We strongly recommend that you also use a firewall to block access to the Solr port (8983) from outside networks, for added redundancy.

It is **very important** not to allow direct access to the Solr API from outside networks! Otherwise, any host that can reach the Solr port (8983 by default) can add or delete data, search unpublished data, and even reconfigure Solr. For more information, please see https://lucene.apache.org/solr/guide/7_3/securing-solr.html. A particularly serious security issue that has been identified recently allows a potential intruder to remotely execute arbitrary code on the system. See `RCE in Solr via Velocity Template <https://github.com/veracode-research/solr-injection#7-cve-2019-xxxx-rce-via-velocity-template-by-_s00py>`_ for more information.

If you're running your Dataverse installation across multiple service hosts you'll want to remove the jetty.host argument (``-j jetty.host=127.0.0.1``) from the startup command line, but make sure Solr is behind a firewall and only accessible by the Dataverse installation host(s), by specific ip address(es).

We additionally recommend that the Solr service account's shell be disabled, as it isn't necessary for daily operation::

        # usermod -s /sbin/nologin solr

For Solr upgrades or further configuration you may temporarily re-enable the service account shell::

        # usermod -s /bin/bash solr

or simply prepend each command you would run as the Solr user with "sudo -u solr"::

        # sudo -u solr command

Finally, we would like to reiterate that it is simply never a good idea to run Solr as root! Running the process as a non-privileged user would substantially minimize any potential damage even in the event that the instance is compromised.

jq
--

Installing jq
=============

``jq`` is a command line tool for parsing JSON output that is used by the Dataverse Software installation script. It is available in the EPEL repository::

	# yum install epel-release
	# yum install jq

or you may install it manually::

        # cd /usr/bin
        # wget http://stedolan.github.io/jq/download/linux64/jq
        # chmod +x jq
        # jq --version

.. _install-imagemagick:

ImageMagick
-----------

The Dataverse Software uses `ImageMagick <https://www.imagemagick.org>`_ to generate thumbnail previews of PDF files. This is an optional component, meaning that if you don't have ImageMagick installed, there will be no thumbnails for PDF files, in the search results and on the dataset pages; but everything else will be working. (Thumbnail previews for non-PDF image files are generated using standard Java libraries and do not require any special installation steps).

Installing and configuring ImageMagick
======================================

On a Red Hat or derivative Linux distribution, you can install ImageMagick with something like::

	# yum install ImageMagick

(most RedHat systems will have it pre-installed).
When installed using standard ``yum`` mechanism, above, the executable for the ImageMagick convert utility will be located at ``/usr/bin/convert``. No further configuration steps will then be required.

If the installed location of the convert executable is different from ``/usr/bin/convert``, you will also need to specify it in your Payara configuration using the JVM option, below. For example::

   <jvm-options>-Ddataverse.path.imagemagick.convert=/opt/local/bin/convert</jvm-options>

(see the :doc:`config` section for more information on the JVM options)

R
-

The Dataverse Software uses `R <https://cran.r-project.org>`_ to handle
tabular data files. The instructions below describe a **minimal** R Project
installation. It will allow you to ingest R (.RData) files as tabular
data and to export tabular data as .RData files.  R can be considered an optional component, meaning
that if you don't have R installed, you will still be able to run and
use the Dataverse Software - but the functionality specific to tabular data
mentioned above will not be available to your users.


Installing R
============

For RHEL/derivative, the EPEL distribution is strongly recommended:

If :fixedwidthplain:`yum` isn't configured to use EPEL repositories ( https://fedoraproject.org/wiki/EPEL ):

RHEL8/derivative users can install the epel-release RPM::

       yum install https://dl.fedoraproject.org/pub/epel/epel-release-latest-8.noarch.rpm

RHEL7/derivative users can install the epel-release RPM::

       yum install https://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm

RHEL 8 users will need to enable the CodeReady-Builder repository::

       subscription-manager repos --enable codeready-builder-for-rhel-8-x86_64-rpms

Rocky or AlmaLinux 8.3+ users will need to enable the PowerTools repository::

       dnf config-manager --enable powertools

RHEL 7 users will want to log in to their organization's respective RHN interface, find the particular machine in question and:

• click on "Subscribed Channels: Alter Channel Subscriptions"
• enable EPEL, Server Extras, Server Optional

Finally, install R with :fixedwidthplain:`yum`::

       yum install R-core R-core-devel

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

The Dataverse Software uses `Rserve <https://rforge.net/Rserve/>`_ to communicate
to R. Rserve is installed as a library package, as described in the
step above. It runs as a daemon process on the server, accepting
network connections on a dedicated port. This requires some extra
configuration and we provide a script for setting it up.

You'll want to obtain local copies of the Rserve setup files found in
https://github.com/IQSS/dataverse/tree/master/scripts/r/rserve
either by cloning a local copy of the IQSS repository:
:fixedwidthplain:`git clone -b master https://github.com/IQSS/dataverse.git`
or by downloading the files individually.

Run the script as follows (as root)::

    cd <DATAVERSE SOURCE TREE>/scripts/r/rserve
    ./rserve-setup.sh

The setup script will create a system user :fixedwidthplain:`rserve`
that will run the daemon process.  It will install the startup script
for the daemon (:fixedwidthplain:`/etc/init.d/rserve`), so that it
gets started automatically when the system boots.  This is an
:fixedwidthplain:`init.d`-style startup file. If this is a
RedHat/CentOS 7 system, you may want to use the
:download:`rserve.service<../../../../scripts/r/rserve/rserve.service>`
systemd unit file instead. Copy it into the /usr/lib/systemd/system/ directory, then::

	# systemctl daemon-reload
	# systemctl enable rserve
	# systemctl start rserve

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
installation, change the :fixedwidthplain:`dataverse.rserve.host` option
above as well (and make sure the port 6311 on the Rserve host is not
firewalled from your Dataverse installation host).

Counter Processor
-----------------

Counter Processor is required to enable Make Data Count metrics in a Dataverse installation. See the :doc:`/admin/make-data-count` section of the Admin Guide for a description of this feature. Counter Processor is open source and we will be downloading it from https://github.com/CDLUC3/counter-processor

Installing Counter Processor
============================

A scripted installation using Ansible is mentioned in the :doc:`/developers/make-data-count` section of the Developer Guide.

As root, download and install Counter Processor::

        cd /usr/local
        wget https://github.com/CDLUC3/counter-processor/archive/v0.1.04.tar.gz
        tar xvfz v0.1.04.tar.gz
        cd /usr/local/counter-processor-0.1.04

Installing GeoLite Country Database
===================================

Counter Processor can report per country results if the optional GeoLite Country Database is installed. At present, this database is free but to use it one must signing an agreement (EULA) with MaxMind. 
(The primary concern appears to be that individuals can opt-out of having their location tracked via IP address and, due to various privacy laws, MaxMind needs a way to comply with that for products it has "sold" (for no cost in this case). Their agreement requires you to either configure automatic updates to the GeoLite Country database or be responsible on your own for managing take down notices.)
The process required to sign up, download the database, and to configure automated updating is described at https://blog.maxmind.com/2019/12/18/significant-changes-to-accessing-and-using-geolite2-databases/ and the links from that page.

As root, change to the Counter Processor directory you just created, download the GeoLite2-Country tarball from MaxMind, untar it, and copy the geoip database into place::

        <download or move the GeoLite2-Country.tar.gz to the /usr/local/counter-processor-0.1.04 directory>
        tar xvfz GeoLite2-Country.tar.gz
        cp GeoLite2-Country_*/GeoLite2-Country.mmdb maxmind_geoip

Creating a counter User
=======================

As root, create a "counter" user and change ownership of Counter Processor directory to this new user::

        useradd counter
        chown -R counter:counter /usr/local/counter-processor-0.1.04

Installing Counter Processor Python Requirements
================================================

Counter Processor version 0.1.04 requires Python 3.7 or higher. This version of Python is available in many operating systems, and is purportedly available for RHEL7 or CentOS 7 via Red Hat Software Collections. Alternately, one may compile it from source.

The following commands are intended to be run as root but we are aware that Pythonistas might prefer fancy virtualenv or similar setups. Pull requests are welcome to improve these steps!

Install Python 3.9::

        yum install python39

Install Counter Processor Python requirements::

        python3.9 -m ensurepip
        cd /usr/local/counter-processor-0.1.04
        pip3 install -r requirements.txt

See the :doc:`/admin/make-data-count` section of the Admin Guide for how to configure and run Counter Processor.

Next Steps
----------

Now that you have all the prerequisites in place, you can proceed to the :doc:`installation-main` section.
