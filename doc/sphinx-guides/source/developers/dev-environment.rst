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

Windows is not supported, unfortunately. For the current status of Windows support, see https://github.com/IQSS/dataverse/issues/3927 or our community list thread `"Do you want to develop on Windows?" <https://groups.google.com/d/msg/dataverse-community/Hs9j5rIxqPI/-q54751aAgAJ>`_

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

Install PostgreSQL
~~~~~~~~~~~~~~~~~~

PostgreSQL 9.4 or older is required because of the drivers we have checked into the code.

On Mac, go to https://www.postgresql.org/download/macosx/ and choose "Interactive installer by EnterpriseDB" option. We've tested version 9.4.17. When prompted to set a password for the "database superuser (postgres)" just enter "password".

After installation is complete, make a backup of the ``pg_hba.conf`` file like this:

``sudo cp /Library/PostgreSQL/9.4/data/pg_hba.conf /Library/PostgreSQL/9.4/data/pg_hba.conf.orig``

Then edit ``pg_hba.conf`` with an editor such as vi:

``sudo vi /Library/PostgreSQL/9.4/data/pg_hba.conf``

In the "METHOD" column, change all instances of "md5" to "trust".

In the Finder, click "Applications" then "PostgreSQL 9.4" and launch the "Reload Configuration" app. Click "OK" after you see "server signaled".

Next, launch the "pgAdmin III" application from the same folder. Under "Servers" double click "PostgreSQL 9.4 (localhost)". When you are prompted for a password, leave it blank and click "OK". If you have successfully edited "pg_hba.conf", you can get in without a password.

On Linux, you should just install PostgreSQL from your package manager without worrying about the version as long as it's 9.x. Find ``pg_hba.conf`` and set the authentication method to "trust" and restart PostgreSQL.

Install Solr
~~~~~~~~~~~~

`Solr <http://lucene.apache.org/solr/>`_ Solr 4.6.0 is required.

To install Solr, execute the following commands:

``sudo mkdir /usr/local/solr``

``sudo chown $USER /usr/local/solr``

``cd /usr/local/solr``

``curl -O http://archive.apache.org/dist/lucene/solr/4.6.0/solr-4.6.0.tgz``

``tar xvfz solr-4.6.0.tgz``

A Dataverse-specific ``schema.xml`` configuration file is required, which we download from the "develop" branch on GitHub and use to overwrite the default ``schema.xml`` file:

``cd solr-4.6.0/example``

``curl -O https://raw.githubusercontent.com/IQSS/dataverse/develop/conf/solr/4.6.0/schema.xml``

``mv schema.xml solr/collection1/conf/schema.xml``

Assuming you are still in the ``solr-4.6.0/example`` directory, you can start Solr like this:

``java -jar start.jar``

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

Next Steps
----------

If you can log in to Dataverse, great! If not, please see the :doc:`troubleshooting` section. For further assitance, please see "Getting Help" in the :doc:`intro` section.

You're almost ready to start hacking on code. Now that the installer script has you up and running, you need to continue on to the :doc:`tips` section to get set up to deploy code from your IDE or the command line.

----

Previous: :doc:`intro` | Next: :doc:`tips`
