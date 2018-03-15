=======================
Development Environment
=======================

We'll try to help you get your development environment up and running as quickly as possible!

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

You are welcome to skip around through these installation steps as long as you run the ``install`` script last.

Install Java
~~~~~~~~~~~~

Dataverse is developed on and requires Java 8.

On Mac, we recommend Oracle's version of Java, which can be downloaded from http://www.oracle.com/technetwork/java/javase/downloads/index.html

On Linux, you are welcome to use the OpenJDK available from package managers.

Install Glassfish
~~~~~~~~~~~~~~~~~

Glassfish 4.1 is required. Newer versions of 4.x are known not to work ( https://github.com/IQSS/dataverse/issues/2628 ) and we have made some attempts to use Glassfish 5 ( https://github.com/IQSS/dataverse/issues/4248 ).

To install Glassfish:

- Download http://download.oracle.com/glassfish/4.1/release/glassfish-4.1.zip and place it in ``/usr/local`` or the directory of your choice.
- Run ``unzip glassfish-4.1.zip`` to unzip to ``/usr/local/glassfish4`` or another directory of your choice. Note that if you have installed Homebrew ( https://brew.sh ) on a Mac ``/usr/local`` will already be writable by your user.

There is nothing you need to configure for Glassfish but you should make note of the installation location if it differs from ``/usr/local/glassfish4`` because you will need to supply it to the Dataverse installer, described below.

Install and Configure PostgreSQL
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

PostgreSQL 8.4 or higher is required but you are welcome to use a newer version. In practice, 9.x is run most commonly in production environments. Please let us know if you have any problems with a specific version.

On Linux, you should just use the version of PostgreSQL that comes with your distribution.

On Mac, there are a variety of ways to install PostgreSQL. https://www.postgresql.org/download/macosx/ lists a number of options and the "Interactive installer by EnterpriseDB" is listed first so you should probably try that one and let us know if it doesn't work. Dataverse developers have also successfully used installations of PostgreSQL from Homebrew ( https://brew.sh ).

No matter how you install PostgreSQL, you should adjust the ``local`` and ``host`` lines in ``pg_hba.conf`` to end with ``trust`` and then restart PostgreSQL so that the Dataverse installer can create your database. Dataverse doesn't require ``trust`` in production environments but this topic is out scope for the dev guide.

Install and Configure Solr
~~~~~~~~~~~~~~~~~~~~~~~~~~

Dataverse depends on `Solr <http://lucene.apache.org/solr/>`_ for browsing and search.

Solr 4.6.0 is the only version that has been tested extensively by the Dataverse team and is recommended in development. We are aware that this version of Solr is old and upgrading to a newer version is being tracked at https://github.com/IQSS/dataverse/issues/4158 .

First, decide where you would like to install Solr and create a directory for it. In the example below, we create a directory called "solr" in our home directory (``~``).

``mkdir ~/solr``

Change into the directory you created, download the Solr tarball, and uncompress it:

``cd ~/solr``

``curl -O http://archive.apache.org/dist/lucene/solr/4.6.0/solr-4.6.0.tgz``

``tar xvfz solr-4.6.0.tgz``

A Dataverse-specific ``schema.xml`` configuration file is required, which we download from the "develop" branch on GitHub and use to overwrite the default ``schema.xml`` file:

``cd solr-4.6.0/example``

``curl -O https://raw.githubusercontent.com/IQSS/dataverse/develop/conf/solr/4.6.0/schema.xml``

``mv schema.xml solr/collection1/conf/schema.xml``

Assuming you are still in the ``solr-4.6.0/example`` directory, you can start Solr like this:

``java -jar start.jar``

Once Solr is running you should be able to see a "Solr Admin" dashboard at http://localhost:8983/solr and Dataverse-specific fields (with "dataset" in name, for example) at http://localhost:8983/solr/schema/fields

Install jq
~~~~~~~~~~

A command-line tool called ``jq`` ( http://stedolan.github.io/jq/ ) is required by the setup scripts.

If you are already using ``brew`` ( https://brew.sh ), ``apt-get``, or ``yum``, you can install ``jq`` that way. Otherwise, download the binary for your platform from http://stedolan.github.io/jq/ and make sure it is in your ``$PATH`` (``/usr/bin/jq`` is fine) and executable with ``sudo chmod +x /usr/bin/jq``. After you've set it up, you should be able to open a new terminal window and type ``jq`` and see some output.

Install Netbeans or Maven
~~~~~~~~~~~~~~~~~~~~~~~~~

While you are welcome to use any editor or IDE you wish, Netbeans 8+ is recommended because it is free of cost, works cross platform, has good support for Java EE projects, and has Maven (the build tool we use) built in.

Netbeans can be downloaded from http://netbeans.org. It's a good idea to select an option that contains the Jave EE features when choosing your download bundle but it's possible to add them after installation. Go ahead and install JUnit if you are prompted to do so.

Below we describe how to build the Dataverse war file with Netbeans but if you prefer to use only Maven, you can find installation instructions in the :doc:`tools` section.

Clone the Dataverse Git Repo
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The Dataverse code is at https://github.com/IQSS/dataverse so you'll want to fork the repo and clone your fork with a command that looks something like this:

``git clone git@github.com:[your GitHub user or organization]/dataverse.git``

Build the Dataverse war File
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The first time you build the war file, it may take a few minutes while dependencies are downloaded from Maven Central.

From Netbeans, click "File" and then "Open Project" and navigate to the directory where you ran ``git clone`` above and double-click "dataverse". Then click "Run" and then "Build Project (dataverse)". Look for "BUILD SUCCESS" at the end.

If you installed Maven instead of Netbeans, you probably know that the command you want is ``mvn package``.

Run the Dataverse ``install`` Script
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Navigate to the directory where you cloned the Dataverse git repo and run these commands:

``cd scripts/installer``

``./install``

The script will prompt you for some configuration values.

Verify Dataverse is Running
---------------------------

After the script has finished, you should be able to log into Dataverse with the following credentials:

- http://localhost:8080
- username: dataverseAdmin
- password: admin

Next Steps
----------

If you can log in to Dataverse, great! You're almost ready to start hacking on code. However, initial deployment of the Dataverse war file was does by the ``install`` script and you need to get set up to deploy the war file from an IDE such as Netbeans or the command line. This is the first topic under :doc:`tips`, where you should go next.

If something has gone terribly wrong with any of the steps above, please see the :doc:`troubleshooting` section and don't be shy about reaching out as explained under "Getting Help" in the :doc:`intro` section.

----

Previous: :doc:`intro` | Next: :doc:`tips`
