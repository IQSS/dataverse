=======================
Development Environment
=======================

These instructions are purposefully opinionated and terse to help you get your development environment up and running as quickly as possible! Please note that familiarity with running commands from the terminal is assumed.

.. contents:: |toctitle|
	:local:

Quick Start
-----------

The quickest way to get Dataverse running is to install VirtualBox and Vagrant and run ``vagrant up`` as described in the :doc:`tools` section. If you're excited about containers, you might want to take a look at the :doc:`containers` section. All that said, we recommend installing all the dependencies directly on your development machine, as described below, for day to day development of Dataverse.

Supported Operating Systems
---------------------------

The setup of a Dataverse development environment assumes the presence of a Unix shell (i.e. bash) and standard Unix utilities (i.e curl) so an operating system with Unix underpinnings such as Mac OS X or Linux is required.

We regret to say that development of Dataverse on Windows is not well supported. Please provide suggestions at https://github.com/IQSS/dataverse/issues/3927 or on the `"Do you want to develop on Windows?" thread <https://groups.google.com/d/msg/dataverse-community/Hs9j5rIxqPI/-q54751aAgAJ>`_ on our mailing list. Your best option at this point is probably the Vagrant environment for Dataverse described in the :doc:`tools` section. If you can help us describe how Windows 10 users can make use of the new `Windows Subsystem for Linux (WSL) <https://en.wikipedia.org/wiki/Windows_Subsystem_for_Linux>`_ feature, please get in touch.

How to Set Up a Dataverse Dev Environment
-----------------------------------------

Install Java
~~~~~~~~~~~~

Dataverse is developed on and requires Java 8. New versions of Java are not supported.

On Mac, we recommend Oracle's version of Java, which can be downloaded from http://www.oracle.com/technetwork/java/javase/downloads/index.html and note that you should select "JDK".

On Linux, you are welcome to use the OpenJDK available from package managers.

Install Netbeans or Maven
~~~~~~~~~~~~~~~~~~~~~~~~~

While you are welcome to use any editor or IDE you wish, Netbeans is recommended because it is free of cost, works cross platform, has good support for Java EE projects, and has Maven (the build tool we use) built in.

Netbeans can be downloaded from http://netbeans.org and you should select the "Java EE" version.

Below we describe how to build the Dataverse war file with Netbeans but if you prefer to use only Maven, you can find installation instructions in the :doc:`tools` section.

Clone the Dataverse Git Repo
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The Dataverse code is at https://github.com/IQSS/dataverse so you'll want to fork that repo and clone your fork with a command that looks something like this:

``git clone git@github.com:[your GitHub user or organization]/dataverse.git``

Build the Dataverse war File
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Launch Netbeans and click "File" and then "Open Project" and navigate to the directory where you ran ``git clone`` above and double-click "dataverse" to open the project. Then click "Run" in the menu and then "Build Project (dataverse)". The first time you build the war file, it will take a few minutes while dependencies are downloaded from Maven Central. Feel free to move on to other steps but check back for "BUILD SUCCESS" at the end.

If you installed Maven instead of Netbeans, you probably know that the command you want is ``mvn package``.

Install Homebrew (Mac Only)
~~~~~~~~~~~~~~~~~~~~~~~~~~~

On Mac, install Homebrew to simplify the steps below: https://brew.sh

Install jq
~~~~~~~~~~

On Mac, run ``brew install jq``.

On Linux, install ``jq`` from your package manager or download a binary from http://stedolan.github.io/jq/

Install Glassfish
~~~~~~~~~~~~~~~~~

Glassfish 4.1 is required. Newer versions of 4.x are known not to work ( https://github.com/IQSS/dataverse/issues/2628 ) and we have made some attempts to use Glassfish 5 ( https://github.com/IQSS/dataverse/issues/4248 ).

To install Glassfish, run the following commands:

``cd /usr/local``

``sudo curl -O http://download.oracle.com/glassfish/4.1/release/glassfish-4.1.zip``

``sudo unzip glassfish-4.1.zip``

``sudo chown -R $USER /usr/local/glassfish4``

Install and Configure PostgreSQL
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

PostgreSQL 9.4 or older is required because of the drivers we have checked into the code.

On Mac, there are a variety of ways to install PostgreSQL listed at https://www.postgresql.org/download/macosx/ and we recommend the first one called "Interactive installer by EnterpriseDB". We've tested version 9.4.17. When prompted to set a password for the "database superuser (postgres)" just enter "password".

Next, make a backup of the ``pg_hba.conf`` file like this:

``sudo cp /Library/PostgreSQL/9.4/data/pg_hba.conf /Library/PostgreSQL/9.4/data/pg_hba.conf.orig``

Then edit ``pg_hba.conf`` with an editor such as vi:

``sudo vi /Library/PostgreSQL/9.4/data/pg_hba.conf``

In the "METHOD" column, change all instances of "md5" to "trust".

In the Finder, click "Applications" then "PostgreSQL 9.4" and launch the "Reload Configuration" app. Click "OK" after you see "server signaled".

Next, launch the "pgAdmin III" application from the same folder. Under "Servers" double click "PostgreSQL 9.4 (localhost)". When you are prompted for a password, leave it blank and click "OK". If you can get in without a password, your editing of "pg_hba.conf" above worked.

On Linux, you should just install PostgreSQL from your package manager without worring about the version as long as it's 9.x. Find ``pg_hba.conf`` and set the authentication method to "trust" and restart PostgreSQL.

Install and Configure Solr
~~~~~~~~~~~~~~~~~~~~~~~~~~

Dataverse depends on `Solr <http://lucene.apache.org/solr/>`_ for browsing and search.

Solr 4.6.0 is the only version that has been tested extensively by the Dataverse team and is recommended in development. We are aware that this version of Solr is old and upgrading to a newer version is being tracked at https://github.com/IQSS/dataverse/issues/4158 .

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

Once Solr is running you should be able to see a "Solr Admin" dashboard at http://localhost:8983/solr and Dataverse-specific fields (with "dataset" in name, for example) at http://localhost:8983/solr/schema/fields

Run the Dataverse ``install`` Script
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Navigate to the directory where you cloned the Dataverse git repo and run these commands:

``cd scripts/installer``

``./install``

It's fine to accept the default values.

After a while you will see ``Enter admin user name [Enter to accept default]>`` and you can just hit Enter.

Verify Dataverse is Running
---------------------------

After the script has finished, you should be able to log into Dataverse with the following credentials:

- http://localhost:8080
- username: dataverseAdmin
- password: admin

Next Steps
----------

If you can log in to Dataverse, great! You're almost ready to start hacking on code. However, initial deployment of the Dataverse war file was done by the ``install`` script and you need to get set up to deploy the war file from an IDE such as Netbeans or the command line. This is the first topic under :doc:`tips`, where you should go next.

If something has gone terribly wrong with any of the steps above, please see the :doc:`troubleshooting` section and don't be shy about reaching out as explained under "Getting Help" in the :doc:`intro` section.

----

Previous: :doc:`intro` | Next: :doc:`tips`
