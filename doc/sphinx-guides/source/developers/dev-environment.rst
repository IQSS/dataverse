=======================
Development Environment
=======================

These instructions are purposefully opinionated and terse to help you get your development environment up and running as quickly as possible! Please note that familiarity with running commands from the terminal is assumed.

.. contents:: |toctitle|
	:local:

Quick Start
-----------

The quickest way to get Dataverse running is to use Vagrant as described in the :doc:`tools` section, but for day to day development work, we recommended the following setup.

Set Up Dependencies
-------------------

Supported Operating Systems
~~~~~~~~~~~~~~~~~~~~~~~~~~~

Mac OS X or Linux is required because the setup scripts assume the presence of standard Unix utilities.

Windows is not well supported, unfortunately, but Vagrant and Minishift environments are described in the :doc:`windows` section.

Install Java
~~~~~~~~~~~~

Dataverse requires Java 8.

On Mac, we recommend Oracle's version of the JDK, which can be downloaded from http://www.oracle.com/technetwork/java/javase/downloads/index.html

On Linux, you are welcome to use the OpenJDK available from package managers.

Install Netbeans or Maven
~~~~~~~~~~~~~~~~~~~~~~~~~

NetBeans IDE (Java EE bundle) is recommended, and can be downloaded from http://netbeans.org . Developers may use any editor or IDE. We recommend NetBeans because it is free, works cross platform, has good support for Java EE projects, and includes a required build tool, Maven.

Below we describe how to build the Dataverse war file with Netbeans but if you prefer to use only Maven, you can find installation instructions in the :doc:`tools` section.

Install Homebrew (Mac Only)
~~~~~~~~~~~~~~~~~~~~~~~~~~~

On Mac, install Homebrew to simplify the steps below: https://brew.sh

Clone the Dataverse Git Repo
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Fork https://github.com/IQSS/dataverse and then clone your fork like this:

``git clone git@github.com:[YOUR GITHUB USERNAME]/dataverse.git``

Build the Dataverse War File
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Launch Netbeans and click "File" and then "Open Project". Navigate to where you put the Dataverse code and double-click "dataverse" to open the project. Click "Run" in the menu and then "Build Project (dataverse)". The first time you build the war file, it will take a few minutes while dependencies are downloaded from Maven Central. Feel free to move on to other steps but check back for "BUILD SUCCESS" at the end.

If you installed Maven instead of Netbeans, run ``mvn package``.

NOTE: Do you use a locale different than ``en_US.UTF-8`` on your development machine? Are you in a different timezone
than Harvard (Eastern Time)? You might experience issues while running tests that were written with these settings
in mind. The Maven  ``pom.xml`` tries to handle this for you by setting the locale to ``en_US.UTF-8`` and timezone
``UTC``, but more, not yet discovered building or testing problems might lurk in the shadows.

Install jq
~~~~~~~~~~

On Mac, run this command:

``brew install jq``

On Linux, install ``jq`` from your package manager or download a binary from http://stedolan.github.io/jq/

Install Glassfish
~~~~~~~~~~~~~~~~~

Glassfish 4.1 is required.

To install Glassfish, run the following commands:

``cd /usr/local``

``sudo curl -O http://download.oracle.com/glassfish/4.1/release/glassfish-4.1.zip``

``sudo unzip glassfish-4.1.zip``

``sudo chown -R $USER /usr/local/glassfish4``

Test Glassfish Startup Time on Mac
++++++++++++++++++++++++++++++++++

``cd /usr/local/glassfish4/glassfish/bin``

``./asadmin start-domain``

``grep "startup time" /usr/local/glassfish4/glassfish/domains/domain1/logs/server.log``

If you are seeing startup times in the 30 second range (31,584ms for "Felix" for example) please be aware that startup time can be greatly reduced (to less than 1.5 seconds in our testing) if you make a small edit to your ``/etc/hosts`` file as described at https://stackoverflow.com/questions/39636792/jvm-takes-a-long-time-to-resolve-ip-address-for-localhost/39698914#39698914 and https://thoeni.io/post/macos-sierra-java/

Look for a line that says ``127.0.0.1 localhost`` and add a space followed by the output of ``hostname`` which should be something like ``foobar.local`` depending on the name of your Mac. For example, the line would say ``127.0.0.1 localhost foobar.local`` if your Mac's name is "foobar".

Install PostgreSQL
~~~~~~~~~~~~~~~~~~

PostgreSQL 9.6 is recommended to match the version in the Installation Guide.

On Mac, go to https://www.postgresql.org/download/macosx/ and choose "Interactive installer by EnterpriseDB" option. We've tested version 9.6.9. When prompted to set a password for the "database superuser (postgres)" just enter "password".

After installation is complete, make a backup of the ``pg_hba.conf`` file like this:

``sudo cp /Library/PostgreSQL/9.6/data/pg_hba.conf /Library/PostgreSQL/9.6/data/pg_hba.conf.orig``

Then edit ``pg_hba.conf`` with an editor such as vi:

``sudo vi /Library/PostgreSQL/9.6/data/pg_hba.conf``

In the "METHOD" column, change all instances of "md5" to "trust".

In the Finder, click "Applications" then "PostgreSQL 9.6" and launch the "Reload Configuration" app. Click "OK" after you see "server signaled".

Next, launch the "pgAdmin" application from the same folder. Under "Browser", expand "Servers" and double click "PostgreSQL 9.6". When you are prompted for a password, leave it blank and click "OK". If you have successfully edited "pg_hba.conf", you can get in without a password.

On Linux, you should just install PostgreSQL from your package manager without worrying about the version as long as it's 9.x. Find ``pg_hba.conf`` and set the authentication method to "trust" and restart PostgreSQL.

Install Solr
~~~~~~~~~~~~

`Solr <http://lucene.apache.org/solr/>`_ 7.3.0 is required.

To install Solr, execute the following commands:

``sudo mkdir /usr/local/solr``

``sudo chown $USER /usr/local/solr``

``cd /usr/local/solr``

``curl -O http://archive.apache.org/dist/lucene/solr/7.3.0/solr-7.3.0.tgz``

``tar xvfz solr-7.3.0.tgz``

``cd solr-7.3.0/server/solr``

``cp -r configsets/_default collection1``

``curl -O https://raw.githubusercontent.com/IQSS/dataverse/develop/conf/solr/7.3.0/schema.xml``

``mv schema.xml collection1/conf``

``curl -O https://raw.githubusercontent.com/IQSS/dataverse/develop/conf/solr/7.3.0/solrconfig.xml``

``mv solrconfig.xml collection1/conf/solrconfig.xml``

``cd /usr/local/solr/solr-7.3.0``

``bin/solr start``

``bin/solr create_core -c collection1 -d server/solr/collection1/conf``

Run the Dataverse Installer Script
----------------------------------

Navigate to the directory where you cloned the Dataverse git repo and run these commands:

``cd scripts/installer``

``./install``

It's fine to accept the default values.

After a while you will see ``Enter admin user name [Enter to accept default]>`` and you can just hit Enter.

Verify Dataverse is Running
~~~~~~~~~~~~~~~~~~~~~~~~~~~

After the script has finished, you should be able to log into Dataverse with the following credentials:

- http://localhost:8080
- username: dataverseAdmin
- password: admin

Configure Your Development Environment for Publishing
-----------------------------------------------------

Run the following command:

``curl http://localhost:8080/api/admin/settings/:DoiProvider -X PUT -d FAKE``

This will disable DOI registration by using a fake (in-code) DOI provider. Please note that this feature is only available in version >= 4.10 and that at present, the UI will give no indication that the DOIs thus minted are fake.

Next Steps
----------

If you can log in to Dataverse, great! If not, please see the :doc:`troubleshooting` section. For further assitance, please see "Getting Help" in the :doc:`intro` section.

You're almost ready to start hacking on code. Now that the installer script has you up and running, you need to continue on to the :doc:`tips` section to get set up to deploy code from your IDE or the command line.

----

Previous: :doc:`intro` | Next: :doc:`tips`
