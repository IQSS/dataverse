=======================
Development Environment
=======================

.. contents:: :local:

Requirements
------------

Ubuntu users please visit :doc:`/developers/ubuntu`

Java
~~~~

Dataverse is developed on Java 7. An upgrade to Java 8 is being tracked at https://github.com/IQSS/dataverse/issues/2151

The use of Oracle's version of Java is recommended, which can be downloaded from http://www.oracle.com/technetwork/java/javase/downloads/index.html

The version of OpenJDK available from package managers from common Linux distributions such as Ubuntu and Fedora is probably sufficient for small changes as well as day to day development.

Glassfish
~~~~~~~~~

As a `Java Enterprise Edition <http://en.wikipedia.org/wiki/Java_Platform,_Enterprise_Edition>`_ 7 (Java EE 7) application, Dataverse requires an applications server to run.

Glassfish 4.0+ is required, which can be downloaded from http://glassfish.java.net

PostgreSQL
~~~~~~~~~~

PostgreSQL 9+ is required and can be downloaded from http://postgresql.org

Solr
~~~~

Dataverse depends on `Solr <http://lucene.apache.org/solr/>`_ for browsing and search.

Solr 4.6.0 is the only version that has been tested extensively and is recommended in development. Download and configuration instructions can be found below. An upgrade to newer versions of Solr is being tracked at https://github.com/IQSS/dataverse/issues/456

jq
~~

A command-line tool called ``jq`` ( http://stedolan.github.io/jq/ ) is required by the setup scripts.

If you are already using ``brew``, ``apt-get``, or ``yum``, you can install ``jq`` that way. Otherwise, download the binary for your platform from http://stedolan.github.io/jq/ and make sure it is in your ``$PATH`` (``/usr/bin/jq`` is fine) and executable with ``sudo chmod +x /usr/bin/jq``.

Recommendations
---------------
SSH keys - GitHub
~~~~~~~~~~~~~~~~~

You can use git with passwords over HTTPS but it's much nicer to set up SSH keys. https://github.com/settings/ssh is the place to manage the ssh keys GitHub knows about for you. That page also links to a nice howto: https://help.github.com/articles/generating-ssh-keys

From the terminal, ``ssh-keygen`` will create new ssh keys for you:

private key: ~/.ssh/id_rsa
It is very important to protect your private key. If someone else acquires it, they can access private repositories on GitHub and make commits as you! Ideally, you'll store your ssh keys on an encrypted volume and protect your private key with a password when prompted for one by ssh-keygen. See also "Why do passphrases matter" at https://help.github.com/articles/generating-ssh-keys

public key: ~/.ssh/id_rsa.pub
After you've created your ssh keys, add the public key to your GitHub account.


Mac OS X
~~~~~~~~

The setup of a Dataverse development environment assumes the presence of a Unix shell (i.e. bash) so an operating system with Unix underpinnings such as Mac OS X or Linux is recommended. (The `development team at IQSS <http://datascience.iq.harvard.edu/team>`_ has standardized Mac OS X.) Windows users are encouraged to install `Cygwin <http://cygwin.com>`_.

Netbeans
~~~~~~~~

While developers are welcome to use any editor or IDE they wish, Netbeans 8+ is recommended because it is free of cost, works cross platform, has good support for Java EE projects, and happens to be the IDE that the `development team at IQSS <http://datascience.iq.harvard.edu/team>`_ has standardized on. 

NetBeans can be downloaded from http://netbeans.org. Please make sure that you use an option that contains the Jave EE features when choosing your download bundle. While using the installer you might be prompted about installing JUnit and Glassfish. There is no need to reinstall Glassfish if you already have a 4.0+ version, but it is recommended that you install JUnit.  

This guide will assume you are using Netbeans for development.

Please see also the :doc:`/developers/tools` page, which lists additional tools that very useful but not essential.


Setting up your dev environment
-------------------------------

Clone Project from GitHub
~~~~~~~~~~~~~~~~~~~~~~~~~

Using NetBeans:

Go to Team >> Clone and add the repository info.

Alternative method using Terminal:

Without push access to the project:

Fork the project

- ``git clone git@github.com:[your GitHub user]/dataverse.git``

- ``cd dataverse``

- ``mvn package`` 


With push access to the project:

- ``git clone git@github.com:IQSS/dataverse.git``

- ``cd dataverse``

- ``mvn package`` 


Installing and Running Solr
~~~~~~~~~~~~~~~~~~~~~~~~~~~

A Dataverse-specific ``schema.xml`` configuration file (described below) is required.

Download solr-4.6.0.tgz from http://archive.apache.org/dist/lucene/solr/4.6.0/solr-4.6.0.tgz to any directory you like but in the example below, we have downloaded the tarball to a directory called "solr" in our home directory. For now we are using the "example" template but we are replacing ``schema.xml`` with our own. We will also assume that the clone on the Dataverse repository was retrieved using NetBeans and that it is saved in the path ~/NetBeansProjects.

- ``cd ~/solr``
- ``tar xvfz solr-4.6.0.tgz``
- ``cd solr-4.6.0/example``
- ``cp ~/NetBeansProjects/dataverse/conf/solr/4.6.0/schema.xml solr/collection1/conf/schema.xml``
- ``java -jar start.jar``

Please note: If you prefer, once the proper ``schema.xml`` file is in place, you can simply double-click "start.jar" rather that running ``java -jar start.jar`` from the command line. Figuring out how to stop Solr after double-clicking it is an exercise for the reader.

Once Solr is up and running you should be able to see a "Solr Admin" dashboard at http://localhost:8983/solr

Once some dataverses, datasets, and files have been created and indexed, you can experiment with searches directly from Solr at http://localhost:8983/solr/#/collection1/query and look at the JSON output of searches, such as this wildcard search: http://localhost:8983/solr/collection1/select?q=*%3A*&wt=json&indent=true . You can also get JSON output of static fields Solr knows about: http://localhost:8983/solr/schema/fields

Run installer
~~~~~~~~~~~~~

Once you install Glassfish 4.0+ and PostgreSQL, you need to configure the environment for the Dataverse app - configure the database connection, set some options, etc. We have a new installer script that should do it all for you. Again, assuming that the clone on the Dataverse repository was retrieved using NetBeans and that it is saved in the path ~/NetBeansProjects:

``cd ~/NetBeansProjects/dataverse/scripts/installer``

``./install``

The script will prompt you for some configuration values. It is recommended that you choose "localhost" for your hostname if this is a development environment. For everything else it should be safe to accept the defaults.

The script is a variation of the old installer from DVN 3.x that calls another script that runs ``asadmin`` commands. A serious advantage of this approach is that you should now be able to safely run the installer on an already configured system.

All the future changes to the configuration that are Glassfish-specific and can be done through ``asadmin`` should now go into ``scripts/install/glassfish-setup.sh``.

Shibboleth
----------

If you are working on anything related to users, please keep in mind that your changes will likely affect Shibboleth users. Rather than setting up Shibboleth on your laptop, developers are advised to simply add a value to their database to enable Shibboleth "dev mode" like this:

``curl http://localhost:8080/api/admin/settings/:DebugShibAccountType -X PUT -d RANDOM``

For a list of possible values, please "find usages" on the settings key above and look at the enum.

Now when you go to http://localhost:8080/shib.xhtml you should be prompted to create a Shibboleth account.

Rebuilding your dev environment
-------------------------------

If you have an old copy of the database and old Solr data and want to start fresh, here are the recommended steps: 

- drop your old database
- clear out your existing Solr index: ``scripts/search/clear``
- run the installer script above - it will create the db, deploy the app, populate the db with reference data and run all the scripts that create the domain metadata fields. You no longer need to perform these steps separately.
- confirm you are using the latest Dataverse-specific Solr schema.xml per the "Installing and Running Solr" section of this guide
- confirm http://localhost:8080 is up
- If you want to set some dataset-specific facets, go to the root dataverse (or any dataverse; the selections can be inherited) and click "General Information" and make choices under "Select Facets". There is a ticket to automate this: https://github.com/IQSS/dataverse/issues/619
