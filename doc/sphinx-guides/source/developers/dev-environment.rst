=======================
Development Environment
=======================

This section describes how to configure a development environment for Dataverse.

.. contents:: |toctitle|
	:local:

Overview
--------

Dataverse is a `Java EE <http://en.wikipedia.org/wiki/Java_Platform,_Enterprise_Edition>`_ application that depends on Glassfish, PostgreSQL, and Solr.

Here are the high level steps to get Dataverse running on your dev machine:

- Clone the repo (or a fork) from https://github.com/IQSS/dataverse
- Build a WAR file using Netbeans or Maven.
- Install and configure Glassfish, PostgreSQL, Solr, and jq, as described below.
- Run the Dataverse installer.
- Verify that Dataverse is running at http://localhost:8080

Most of the time will be spend downloading dependencies. Let's get started!

Quick Start
-----------

The quickest way to get Dataverse running is to install VirtualBox and Vagrant and run ``vagrant up`` as described in the :doc:`tools` section. If you're into containers, you might want to take a look at the :doc:`containers` section. All that said, most development of Dataverse is done using software installed directly on one's dev machine as described in the overview above.

Requirements
------------

You can download and configure these requirements in almost any order but they must all be in place before you run the Dataverse installation script.

Mac OS X or Linux
~~~~~~~~~~~~~~~~~

The setup of a Dataverse development environment assumes the presence of a Unix shell (i.e. bash) and standard Unix utilities (i.e curl) so an operating system with Unix underpinnings such as Mac OS X or Linux is required.

Developers on Windows are encouraged to provide suggestions for making development easier on Windows at https://github.com/IQSS/dataverse/issues/3927 . We have heard that the Vagrant environment for Dataverse described in the :doc:`tools` section can help Windows developers contribute to Dataverse and that Windows 10 now includes `Windows Subsystem for Linux (WSL) <https://en.wikipedia.org/wiki/Windows_Subsystem_for_Linux>`_  that may help.

Java
~~~~

Dataverse is developed on and requires Java 8.

On Mac, we recommend Oracle's version of Java, which can be downloaded from http://www.oracle.com/technetwork/java/javase/downloads/index.html

On Linux, you are welcome to use the OpenJDK available from package managers on common Linux distributions such as Ubuntu and Fedora.

Glassfish
~~~~~~~~~

Glassfish 4.1 is required. Newer versions of 4.x are known not to work ( https://github.com/IQSS/dataverse/issues/2628 ) and we have made some attempts to use Glassfish 5 ( https://github.com/IQSS/dataverse/issues/4248 ).

To install Glassfish:

- Download http://download.oracle.com/glassfish/4.1/release/glassfish-4.1.zip and place in in ``/usr/local`` or the directory of your choice.
- Run ``unzip glassfish-4.1.zip`` to unzip to ``/usr/local/glassfish4`` or another directory of your choice. Note that if you have installed Homebrew ( https://brew.sh ) on a Mac, which is recommended, ``/usr/local`` will already be writable by your user.

PostgreSQL
~~~~~~~~~~

PostgreSQL 8.4 or higher is required but you are welcome to use a newer version. In practice, 9.x is run most commonly in production environments. Please let us know if you have any problems with a specific version.

On Linux, you should just use the version of PostgreSQL that comes with your distribution.

On Mac, there are a variety of ways to install PostgreSQL. https://www.postgresql.org/download/macosx/ lists a number of options and the "Interactive installer by EnterpriseDB" is listed first so you should probably try that one and let us know if it doesn't work. Dataverse developers have also successfully used installations of PostgreSQL from Homebrew ( https://brew.sh ).

No matter how you install PostgreSQL, you should adjust the ``local`` and ``host`` lines in ``pg_hba.conf`` to send with ``trust`` and then restart PostgreSQL so that the Dataverse installer can create your database. Dataverse doesn't require ``trust`` in production environments but this topic is out scope for the dev guide.

Solr
~~~~

Dataverse depends on `Solr <http://lucene.apache.org/solr/>`_ for browsing and search.

Solr 4.6.0 is the only version that has been tested extensively by the Dataverse team and is recommended in development. We are aware that this version of Solr is old and upgrading to a newer version is being tracked at https://github.com/IQSS/dataverse/issues/4158 .

First, decide where you would like to install Solr and create a directory for it. In the example below, we create a directory called "solr" in our home directory (``~``).

``mkdir ~/solr``

Change into the directory you created, download the Solr tarball, and uncompress it:

``cd ~/solr``

``curl -O http://archive.apache.org/dist/lucene/solr/4.6.0/solr-4.6.0.tgz``

``tar xvfz solr-4.6.0.tgz``

A Dataverse-specific ``schema.xml`` configuration file is required, which we download from the "develop" branch on GitHub and use to overwrite the defaul ``schema.xml`` file:

``cd solr-4.6.0/example``

``curl -O https://raw.githubusercontent.com/IQSS/dataverse/develop/conf/solr/4.6.0/schema.xml``

``mv schema.xml solr/collection1/conf/schema.xml``

Assuming you are still in the ``solr-4.6.0/example`` directory, you can start Solr like this:

``java -jar start.jar``

Once Solr is running you should be able to see a "Solr Admin" dashboard at http://localhost:8983/solr and Dataverse-specific fields (with "dataset" in name, for example) at http://localhost:8983/solr/schema/fields

jq
~~

A command-line tool called ``jq`` ( http://stedolan.github.io/jq/ ) is required by the setup scripts.

If you are already using ``brew``, ``apt-get``, or ``yum``, you can install ``jq`` that way. Otherwise, download the binary for your platform from http://stedolan.github.io/jq/ and make sure it is in your ``$PATH`` (``/usr/bin/jq`` is fine) and executable with ``sudo chmod +x /usr/bin/jq``.

Netbeans or Maven
~~~~~~~~~~~~~~~~~

While developers are welcome to use any editor or IDE they wish, Netbeans 8+ is recommended because it is free of cost, works cross platform, and has good support for Java EE projects.

Netbeans can be downloaded from http://netbeans.org. Please make sure that you use an option that contains the Jave EE features when choosing your download bundle. While using the installer you might be prompted about installing JUnit and Glassfish. It is recommended that you install JUnit. A specific version of Glassfish (4.1) is required by Dataverse so avoid installing Glassfish with Netbeans if you can. You can manually add the proper (older) version of Glassfish (4.1, as mentioned above) by clicking "Tools", "Servers", "Add Server". You can use the same interface to remove any newer version of Glassfish that's incompatible with Dataverse.

Clone the Dataverse Repo
------------------------

The Dataverse code is at https://github.com/IQSS/dataverse so you'll want to fork the repo ( https://help.github.com/articles/fork-a-repo/ if you are unfamiliar) and clone your fork with a command that looks something like this:

``git clone git@github.com:[your GitHub user or organization]/dataverse.git``

On a Mac, you won't have git installed unless you have "Command Line Developer Tools" installed but running ``git clone`` for the first time will prompt you to install them.

Build the Dataverse WAR File
----------------------------

The first time you build the WAR file, it may take a few minutes while dependencies are downloaded from Maven Central. We'll describe below how to build the WAR file from both Netbean and the terminal, but in both cases, you'll want to see the output "BUILD SUCCESS".

From Netbeans, click "File" and then "Open Project" and navigate to the directory where you ran ``git clone`` above and double-click "dataverse". Then click  "Run" and then "Build Project (dataverse)".

If you prefer to build the WAR file from the command line, ``cd`` to the directory where you cloned the Dataverse repo above and run ``mvn package``. If you don't have the ``mvn`` command available to you, you need to install Maven, which is mentioned in the :doc:`tools` section.

Run the Dataverse ``install`` Script
------------------------------------

Please note the following:

- If you have trouble with the SMTP server, consider editing the installer script to disable the SMTP check.
- Rather than running the installer in "interactive" mode, it's possible to put the values in a file. See "non-interactive mode" in the :doc:`/installation/installation-main` section of the Installation Guide.

Now that you have all the prerequisites in place, you need to configure the environment for the Dataverse app - configure the database connection, set some options, etc. We have an installer script that should do it all for you. Again, assuming that the clone on the Dataverse repository was retrieved using Netbeans and that it is saved in the path ~/NetBeansProjects:

``cd ~/NetBeansProjects/dataverse/scripts/installer``

``./install``

The script will prompt you for some configuration values. It is recommended that you choose "localhost" for your hostname if this is a development environment. For everything else it should be safe to accept the defaults.

The script is a variation of the old installer from DVN 3.x that calls another script that runs ``asadmin`` commands. A serious advantage of this approach is that you should now be able to safely run the installer on an already configured system.

All the future changes to the configuration that are Glassfish-specific and can be done through ``asadmin`` should now go into ``scripts/install/glassfish-setup.sh``.

FIXME: Add a "dev" mode to the installer to allow REST Assured tests to be run. For now, refer to the steps in the :doc:`testing` section.

Verify Dataverse is Running
---------------------------

Congratulations! At this point you should be able to log into Dataverse with the following credentials:

- http://localhost:8080
- username: dataverseAdmin
- password: admin

If something has gone terribly wrong, please see the :doc:`troubleshooting` section.

----

Previous: :doc:`intro` | Next: :doc:`troubleshooting`
