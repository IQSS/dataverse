=======================
Development Environment
=======================

.. contents:: |toctitle|
	:local:

Assumptions
-----------

This guide assumes you are using a Mac. With some tweaks, it's not hard to get a dev environment set up on Linux. If you are using Windows, you might have the most success using Vagrant, which is listed under the :doc:`tools` section.

Requirements
------------

Java
~~~~

Dataverse is developed on Java 8.

The use of Oracle's version of Java is recommended, which can be downloaded from http://www.oracle.com/technetwork/java/javase/downloads/index.html

The version of OpenJDK available from package managers from common Linux distributions such as Ubuntu and Fedora is probably sufficient for small changes as well as day to day development.

Glassfish
~~~~~~~~~

As a `Java Enterprise Edition <http://en.wikipedia.org/wiki/Java_Platform,_Enterprise_Edition>`_ 7 (Java EE 7) application, Dataverse requires an applications server to run.

Glassfish 4.1 is required (not any earlier or later versions until https://github.com/IQSS/dataverse/issues/2628 is resolved), which can be downloaded from http://download.oracle.com/glassfish/4.1/release/glassfish-4.1.zip . If you have downloaded Glassfish as part of a Netbeans bundle, you can manually add the proper version by clicking "Tools", "Servers", "Add Server".

By default, Glassfish reports analytics information. The administration guide suggests this can be disabled with ``asadmin create-jvm-options -Dcom.sun.enterprise.tools.admingui.NO_NETWORK=true``, should this be found to be undesirable for development purposes.

PostgreSQL
~~~~~~~~~~

PostgreSQL 9.x is required and can be downloaded from http://postgresql.org

Solr
~~~~

Dataverse depends on `Solr <http://lucene.apache.org/solr/>`_ for browsing and search.

Solr 4.6.0 is the only version that has been tested extensively and is recommended in development. Download and configuration instructions can be found below. An upgrade to newer versions of Solr is being tracked at https://github.com/IQSS/dataverse/issues/456

curl
~~~~

A command-line tool called ``curl`` ( http://curl.haxx.se ) is required by the setup scripts and it is useful to have curl installed when working on APIs.

jq
~~

A command-line tool called ``jq`` ( http://stedolan.github.io/jq/ ) is required by the setup scripts.

If you are already using ``brew``, ``apt-get``, or ``yum``, you can install ``jq`` that way. Otherwise, download the binary for your platform from http://stedolan.github.io/jq/ and make sure it is in your ``$PATH`` (``/usr/bin/jq`` is fine) and executable with ``sudo chmod +x /usr/bin/jq``.

Recommendations
---------------

Mac OS X
~~~~~~~~

The setup of a Dataverse development environment assumes the presence of a Unix shell (i.e. bash) so an operating system with Unix underpinnings such as Mac OS X or Linux is recommended. (The `development team at IQSS <https://dataverse.org/about>`_ has standardized Mac OS X.) Windows users are encouraged to install `Cygwin <http://cygwin.com>`_.

Netbeans
~~~~~~~~

While developers are welcome to use any editor or IDE they wish, Netbeans 8+ is recommended because it is free of cost, works cross platform, has good support for Java EE projects, and happens to be the IDE that the `development team at IQSS <https://dataverse.org/about>`_ has standardized on.

NetBeans can be downloaded from http://netbeans.org. Please make sure that you use an option that contains the Jave EE features when choosing your download bundle. While using the installer you might be prompted about installing JUnit and Glassfish. There is no need to reinstall Glassfish, but it is recommended that you install JUnit.

This guide will assume you are using Netbeans for development.

Additional Tools
~~~~~~~~~~~~~~~~

Please see also the :doc:`/developers/tools` page, which lists additional tools that very useful but not essential.

Setting Up Your Dev Environment
-------------------------------

Set Up SSH Keys
~~~~~~~~~~~~~~~

You can use git with passwords over HTTPS, but it's much nicer to set up SSH keys. https://github.com/settings/ssh is the place to manage the ssh keys GitHub knows about for you. That page also links to a nice howto: https://help.github.com/articles/generating-ssh-keys

From the terminal, ``ssh-keygen`` will create new ssh keys for you:

- private key: ``~/.ssh/id_rsa`` - It is very important to protect your private key. If someone else acquires it, they can access private repositories on GitHub and make commits as you! Ideally, you'll store your ssh keys on an encrypted volume and protect your private key with a password when prompted for one by ``ssh-keygen``. See also "Why do passphrases matter" at https://help.github.com/articles/generating-ssh-keys

- public key: ``~/.ssh/id_rsa.pub`` - After you've created your ssh keys, add the public key to your GitHub account.

Clone Project from GitHub
~~~~~~~~~~~~~~~~~~~~~~~~~

Before cloning the repo, you are invited to read about our branching strategy in the :doc:`version-control` section but we'll explain the basics here.

Determine Which Repo To Push To
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Developers who are not part of the `development team at IQSS <https://dataverse.org/about>`_ should first fork https://github.com/IQSS/dataverse per https://help.github.com/articles/fork-a-repo/

Cloning the Project from Netbeans
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

From NetBeans, click "Team" then "Remote" then "Clone". Under "Repository URL", enter the `"ssh clone URL" <https://help.github.com/articles/which-remote-url-should-i-use/#cloning-with-ssh>`_ for your fork (if you do not have push access to the repo under IQSS) or ``git@github.com:IQSS/dataverse.git`` (if you do have push access to the repo under IQSS). See also https://netbeans.org/kb/docs/ide/git.html#github

Cloning the Project from the Terminal
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If you prefer using git from the command line, you can clone the project from a terminal and later open the project in Netbeans.

If you do not have push access to https://github.com/IQSS/dataverse clone your fork:

``git clone git@github.com:[your GitHub user or organization]/dataverse.git``

If you do have push access to https://github.com/IQSS/dataverse clone it:

``git clone git@github.com:IQSS/dataverse.git``

Building the WAR File
~~~~~~~~~~~~~~~~~~~~~

Soon, we'll be running the Dataverse installer, but before we do, we must build the Dataverse application, which is delivered as a "WAR" file. WAR stands for "Web application ARchive" and you can read more about this packaging format at https://en.wikipedia.org/wiki/WAR_(file_format)

The first time you build the war file, it may take a few minutes while dependencies are downloaded from Maven Central.

We'll describe below how to build the WAR file from both Netbean and the terminal, but in both cases, you'll want to see the output "BUILD SUCCESS".

Building the War File from Netbeans
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

From Netbeans, click "Run" and then "Build Project (dataverse)".

Building the War File from the Terminal
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

After cloning the git repo, you need to ``cd`` into ``dataverse`` and run ``mvn package``. If you don't have the ``mvn`` command available to you, you need to install Maven, which is mentioned in the :doc:`tools` section.

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

Run Installer
~~~~~~~~~~~~~

Please note the following:

- If you have trouble with the SMTP server, consider editing the installer script to disable the SMTP check.
- Rather than running the installer in "interactive" mode, it's possible to put the values in a file. See "non-interactive mode" in the :doc:`/installation/installation-main` section of the Installation Guide.

Now that you have all the prerequisites in place, you need to configure the environment for the Dataverse app - configure the database connection, set some options, etc. We have an installer script that should do it all for you. Again, assuming that the clone on the Dataverse repository was retrieved using NetBeans and that it is saved in the path ~/NetBeansProjects:

``cd ~/NetBeansProjects/dataverse/scripts/installer``

``./install``

The script will prompt you for some configuration values. It is recommended that you choose "localhost" for your hostname if this is a development environment. For everything else it should be safe to accept the defaults.

The script is a variation of the old installer from DVN 3.x that calls another script that runs ``asadmin`` commands. A serious advantage of this approach is that you should now be able to safely run the installer on an already configured system.

All the future changes to the configuration that are Glassfish-specific and can be done through ``asadmin`` should now go into ``scripts/install/glassfish-setup.sh``.

FIXME: Add a "dev" mode to the installer to allow REST Assured tests to be run. For now, refer to the steps in the :doc:`testing` section.

Troubleshooting
---------------

We've described above the "happy path" of when everything goes right with setting up your Dataverse development environment. If something has gone terribly wrong, please see the :doc:`troubleshooting` section.

Tips
----

Assuming you have a working dev environment, you might want to check out the :doc:`tips` section for ways to optimize it.

Shibboleth and OAuth
--------------------

If you are working on anything related to users, please keep in mind that your changes will likely affect Shibboleth and OAuth users. For some background on user accounts in Dataverse, see "Auth Modes: Local vs. Remote vs. Both" in the :doc:`/installation/config` section of the Installation Guide.

Rather than setting up Shibboleth on your laptop, developers are advised to simply add a value to their database to enable Shibboleth "dev mode" like this:

``curl http://localhost:8080/api/admin/settings/:DebugShibAccountType -X PUT -d RANDOM``

For a list of possible values, please "find usages" on the settings key above and look at the enum.

Now when you go to http://localhost:8080/shib.xhtml you should be prompted to create a Shibboleth account.

OAuth is much more straightforward to get working on your laptop than Shibboleth. GitHub is a good identity provider to test with because you can easily request a Client ID and Client Secret that works against localhost. Follow the instructions in the :doc:`/installation/oauth2` section of the installation Guide and use "http://localhost:8080/oauth2/callback.xhtml" as the callback URL.

In addition to setting up OAuth on your laptop for real per above, you can also use a dev/debug mode:

``curl http://localhost:8080/api/admin/settings/:DebugOAuthAccountType -X PUT -d RANDOM_EMAIL2``

For a list of possible values, please "find usages" on the settings key above and look at the enum.

Now when you go to http://localhost:8080/oauth2/firstLogin.xhtml you should be prompted to create a Shibboleth account.

Geoconnect
----------

Geoconnect works as a middle layer, allowing geospatial data files in Dataverse to be visualized with Harvard WorldMap. To set up a Geoconnect development environment, you can follow the steps outlined in the `local_setup.md <https://github.com/IQSS/geoconnect/blob/master/local_setup.md>`_ guide. You will need Python and a few other prerequisites.

As mentioned under "Architecture and Components" in the :doc:`/installation/prep` section of the Installation Guide, Geoconnect is an optional component of Dataverse, so this section is only necessary to follow it you are working on an issue related to this feature.

----

Previous: :doc:`intro` | Next: :doc:`troubleshooting`
