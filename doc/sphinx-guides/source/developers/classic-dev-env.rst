=======================
Classic Dev Environment
=======================

These are the old instructions we used for Dataverse 4 and 5. They should still work but these days we favor running Dataverse in Docker as described in :doc:`dev-environment`.

These instructions are purposefully opinionated and terse to help you get your development environment up and running as quickly as possible! Please note that familiarity with running commands from the terminal is assumed.

.. contents:: |toctitle|
	:local:

Quick Start (Docker)
--------------------

The quickest way to get Dataverse running is in Docker as explained in :doc:`../container/dev-usage` section of the Container Guide.


Classic Dev Environment
-----------------------

Since before Docker existed, we have encouraged installing Dataverse and all its dependencies directly on your development machine, as described below. This can be thought of as the "classic" development environment for Dataverse.

However, in 2023 we decided that we'd like to encourage all developers to start using Docker instead and opened https://github.com/IQSS/dataverse/issues/9616 to indicate that we plan to rewrite this page to recommend the use of Docker.

There's nothing wrong with the classic instructions below and we don't plan to simply delete them. They are a valid alternative to running Dataverse in Docker. We will likely move them to another page.

Set Up Dependencies
-------------------

Supported Operating Systems
~~~~~~~~~~~~~~~~~~~~~~~~~~~

Mac OS X or Linux is required because the setup scripts assume the presence of standard Unix utilities.

Windows is gaining support through Docker as described in the :doc:`windows` section.

Install Java
~~~~~~~~~~~~

The Dataverse Software requires Java 17.

We suggest downloading OpenJDK from https://adoptopenjdk.net

On Linux, you are welcome to use the OpenJDK available from package managers.

Install Netbeans or Maven
~~~~~~~~~~~~~~~~~~~~~~~~~

NetBeans IDE is recommended, and can be downloaded from https://netbeans.org . Developers may use any editor or IDE. We recommend NetBeans because it is free, works cross platform, has good support for Jakarta EE projects, and includes a required build tool, Maven.

Below we describe how to build the Dataverse Software war file with Netbeans but if you prefer to use only Maven, you can find installation instructions in the :doc:`tools` section.

Install Homebrew (Mac Only)
~~~~~~~~~~~~~~~~~~~~~~~~~~~

On Mac, install Homebrew to simplify the steps below: https://brew.sh

Clone the Dataverse Software Git Repo
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Fork https://github.com/IQSS/dataverse and then clone your fork like this:

``git clone git@github.com:[YOUR GITHUB USERNAME]/dataverse.git``

Build the Dataverse Software War File
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

If you installed Netbeans, follow these steps:

- Launch Netbeans and click "File" and then "Open Project". Navigate to where you put the Dataverse Software code and double-click "Dataverse" to open the project.
- If you see "resolve project problems," go ahead and let Netbeans try to resolve them. This will probably including downloading dependencies, which can take a while.
- Allow Netbeans to install nb-javac (required for Java 8 and below).
- Select "Dataverse" under Projects and click "Run" in the menu and then "Build Project (Dataverse)". Check back for "BUILD SUCCESS" at the end.

If you installed Maven instead of Netbeans, run ``mvn package``. Check for "BUILD SUCCESS" at the end.

NOTE: Do you use a locale different than ``en_US.UTF-8`` on your development machine? Are you in a different timezone
than Harvard (Eastern Time)? You might experience issues while running tests that were written with these settings
in mind. The Maven  ``pom.xml`` tries to handle this for you by setting the locale to ``en_US.UTF-8`` and timezone
``UTC``, but more, not yet discovered building or testing problems might lurk in the shadows.

Install jq
~~~~~~~~~~

On Mac, run this command:

``brew install jq``

On Linux, install ``jq`` from your package manager or download a binary from https://stedolan.github.io/jq/

.. _install-payara-dev:

Install Payara
~~~~~~~~~~~~~~

Payara 6.2025.2 or higher is required.

To install Payara, run the following commands:

``cd /usr/local``

``sudo curl -O -L https://nexus.payara.fish/repository/payara-community/fish/payara/distributions/payara/6.2025.2/payara-6.2025.2.zip``

``sudo unzip payara-6.2025.2.zip``

``sudo chown -R $USER /usr/local/payara6``

If nexus.payara.fish is ever down for maintenance, Payara distributions are also available from https://repo1.maven.org/maven2/fish/payara/distributions/payara/

Install Service Dependencies Directly on localhost
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Install PostgreSQL
^^^^^^^^^^^^^^^^^^

The Dataverse Software has been tested with PostgreSQL versions up to 17. PostgreSQL version 10+ is required.

On Mac, go to https://www.postgresql.org/download/macosx/ and choose "Interactive installer by EDB" option. Note that version 16 is used in the command line examples below, but the process should be similar for other versions. When prompted to set a password for the "database superuser (postgres)" just enter "password".

After installation is complete, make a backup of the ``pg_hba.conf`` file like this:

``sudo cp /Library/PostgreSQL/16/data/pg_hba.conf /Library/PostgreSQL/16/data/pg_hba.conf.orig``

Then edit ``pg_hba.conf`` with an editor such as vi:

``sudo vi /Library/PostgreSQL/16/data/pg_hba.conf``

In the "METHOD" column, change all instances of "scram-sha-256" (or whatever is in that column) to "trust". This will make it so PostgreSQL doesn't require a password.

In the Finder, click "Applications" then "PostgreSQL 16" and launch the "Reload Configuration" app. Click "OK" after you see "server signaled".

Next, to confirm the edit worked, launch the "pgAdmin" application from the same folder. Under "Browser", expand "Servers" and double click "PostgreSQL 16". When you are prompted for a password, leave it blank and click "OK". If you have successfully edited "pg_hba.conf", you can get in without a password.

On Linux, you should just install PostgreSQL using your favorite package manager, such as ``yum``. (Consult the PostgreSQL section of :doc:`/installation/prerequisites` in the main Installation guide for more info and command line examples). Find ``pg_hba.conf`` and set the authentication method to "trust" and restart PostgreSQL.

Install Solr
^^^^^^^^^^^^

`Solr <https://lucene.apache.org/solr/>`_ 9.8.0 is required.

Follow the instructions in the "Installing Solr" section of :doc:`/installation/prerequisites` in the main Installation guide.

Install Service Dependencies Using Docker Compose
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
To avoid having to install service dependencies like PostgreSQL or Solr directly on your localhost, there is the alternative of using the ``docker-compose-dev.yml`` file available in the repository root. For this option you need to have Docker and Docker Compose installed on your machine.

The ``docker-compose-dev.yml`` can be configured to only run the service dependencies necessary to support a Dataverse installation running directly on localhost. In addition to PostgreSQL and Solr, it also runs a SMTP server.

Before running the Docker Compose file, you need to update the value of the ``DATAVERSE_DB_USER`` environment variable to ``postgres``. The variable can be found inside the ``.env`` file in the repository root. This step is required as the Dataverse installation script expects that database user.

To run the Docker Compose file, go to the Dataverse repository root, then run:

``docker-compose -f docker-compose-dev.yml up -d --scale dev_dataverse=0``

Note that this command omits the Dataverse container defined in the Docker Compose file, since Dataverse is going to be installed directly on localhost in the next section.

The command runs the containers in detached mode, but if you want to run them attached and thus view container logs in real time, remove the ``-d`` option from the command.

Data volumes of each dependency will be persisted inside the ``docker-dev-volumes`` folder, inside the repository root.

If you want to stop the containers, then run (for detached mode only, otherwise use ``Ctrl + C``):

``docker-compose -f docker-compose-dev.yml stop``

If you want to remove the containers, then run:

``docker-compose -f docker-compose-dev.yml down``

If you want to run a single container (the mail server, for example) then run:

``docker-compose -f docker-compose-dev.yml up dev_smtp``

For a fresh installation, and before running the Software Installer Script, it is recommended to delete the docker-dev-env folder to avoid installation problems due to existing data in the containers.

Run the Dataverse Software Installer Script
-------------------------------------------

Navigate to the directory where you cloned the Dataverse Software git repo change directories to the ``scripts/installer`` directory like this:

``cd scripts/installer``

Create a Python virtual environment, activate it, then install dependencies:

``python3 -m venv venv``

``source venv/bin/activate``

``pip install psycopg2-binary``

The installer will try to connect to the SMTP server you tell it to use. If you haven't used the Docker Compose option for setting up the dependencies, or you don't have a mail server handy, you can run ``nc -l 25`` in another terminal and choose "localhost" (the default) to get past this check.

Finally, run the installer (see also :download:`README_python.txt <../../../../scripts/installer/README_python.txt>` if necessary):

``python3 install.py``

Verify the Dataverse Software is Running
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

After the script has finished, you should be able to log into your Dataverse installation with the following credentials:

- http://localhost:8080
- username: dataverseAdmin
- password: admin

Configure Your Development Environment for Publishing
-----------------------------------------------------

Run the following command:

``curl http://localhost:8080/api/admin/settings/:DoiProvider -X PUT -d FAKE``

This will disable DOI registration by using a fake (in-code) DOI provider. Please note that this feature is only available in Dataverse Software 4.10+ and that at present, the UI will give no indication that the DOIs thus minted are fake.

Developers may also wish to consider using :ref:`PermaLinks <permalinks>`

Configure Your Development Environment for GUI Edits
----------------------------------------------------

Out of the box, a JSF setting is configured for production use and prevents edits to the GUI (xhtml files) from being visible unless you do a full deployment.

It is recommended that you run the following command so that simply saving the xhtml file in Netbeans is enough for the change to show up.

``asadmin create-system-properties "dataverse.jsf.refresh-period=1"``

For more on JSF settings like this, see :ref:`jsf-config`.

Next Steps
----------

If you can log in to the Dataverse installation, great! If not, please see the :doc:`troubleshooting` section. For further assistance, please see "Getting Help" in the :doc:`intro` section.

You're almost ready to start hacking on code. Now that the installer script has you up and running, you need to continue on to the :doc:`tips` section to get set up to deploy code from your IDE or the command line.
