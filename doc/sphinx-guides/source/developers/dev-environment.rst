=======================
Development Environment
=======================

Here's how to set up a development environment for Dataverse. In a nutshell, you will be installing dependencies (primarily Glassfish, PostgreSQL, and Solr), building a "war" file from Java code in the Dataverse git repo, and running an installation script that deploys that war file into Glassfish and some other setup.

.. contents:: |toctitle|
	:local:

Assumptions
-----------

This guide assumes you are using a Mac and that you want to install the various dependencies (Glassfish, PostgreSQL, Solr, etc.) directly on your Mac. Other options are available such as spinning up Dataverse in Vagrant (see the :doc:`tools` section) or running Dataverse in Docker (see the :doc:`containers` section).

If you are using Windows, you might have the most success using Vagrant, as mentioned above. Please leave a comment at https://github.com/IQSS/dataverse/issues/3927 if you have any suggestions for making Dataverse development on Windows easier.

If you are Linux, you will need to make a few adjustments to the instructions below. We've seen working dev environments on Ubuntu and Fedora.

Requirements
------------

Java
~~~~

Dataverse is developed on and requires Java 8.

On Mac, we recommend Oracle's version of Java, which can be downloaded from http://www.oracle.com/technetwork/java/javase/downloads/index.html

On Linux, you are welcome to use OpenJDK available from package managers on common Linux distributions such as Ubuntu and Fedora.

Glassfish
~~~~~~~~~

As a `Java Enterprise Edition <http://en.wikipedia.org/wiki/Java_Platform,_Enterprise_Edition>`_ 7 (Java EE 7) application, Dataverse requires an applications server to run.

Glassfish 4.1 is required. Never versions of 4.x are known not to work ( https://github.com/IQSS/dataverse/issues/2628 ) and we have made some attempts to use Glassfish 5 ( https://github.com/IQSS/dataverse/issues/4248 ).

To install Glassfish:

- Download http://download.oracle.com/glassfish/4.1/release/glassfish-4.1.zip and place in in ``/usr/local`` or the directory of your choice.
- Run ``unzip glassfish-4.1.zip`` to unzip to ``/usr/local/glassfish4`` or another directory of your choice. Note that if you have installed Homebrew ( https://brew.sh ) on a Mac, which is recommended, ``/usr/local`` will already be writable by your user.

PostgreSQL
~~~~~~~~~~

PostgreSQL 8.4 or higher is required but you are welcome to use a newer version such as 9.x or 10.x. Please let us know if you have any problems with a specific version.

On Linux, you should just use the version of PostgreSQL that comes with your distribution.

On Mac, there are a variety of ways to install PostgreSQL. https://www.postgresql.org/download/macosx/ lists a number of options and the "Interactive installer by EnterpriseDB" is listed first so you should probably try that one and let us know if it doesn't work. Dataverse developers have also successfully used installations of PostgreSQL from Homebrew ( https://brew.sh ).

No matter how you install PostgreSQL, you should adjust the ``local`` and ``host`` lines in ``pg_hba.conf`` to send with ``trust`` and then restart PostgreSQL so that the Dataverse installer can create your database.

Solr
~~~~

Dataverse depends on `Solr <http://lucene.apache.org/solr/>`_ for browsing and search.

Solr 4.6.0 is the only version that has been tested extensively and is recommended in development. Download and configuration instructions can be found below. An upgrade to newer versions of Solr is being tracked at https://github.com/IQSS/dataverse/issues/4158

curl
~~~~

A command-line tool called ``curl`` ( http://curl.haxx.se ) is required by the setup scripts and it is useful to have curl installed when working on APIs. ``curl`` is standard on Mac and Linux so you probably don't need to do anything but it's mentioned here for completeness.

jq
~~

A command-line tool called ``jq`` ( http://stedolan.github.io/jq/ ) is required by the setup scripts.

If you are already using ``brew``, ``apt-get``, or ``yum``, you can install ``jq`` that way. Otherwise, download the binary for your platform from http://stedolan.github.io/jq/ and make sure it is in your ``$PATH`` (``/usr/bin/jq`` is fine) and executable with ``sudo chmod +x /usr/bin/jq``.

Recommendations
---------------

Mac OS X or Linux
~~~~~~~~~~~~~~~~~

The setup of a Dataverse development environment assumes the presence of a Unix shell (i.e. bash) so an operating system with Unix underpinnings such as Mac OS X or Linux is recommended. (The `development team at IQSS <https://dataverse.org/about>`_ has standardized Mac OS X.) Windows users could try installing `Cygwin <http://cygwin.com>`_ or use Vagrant, as mentioned above.

Netbeans
~~~~~~~~

While developers are welcome to use any editor or IDE they wish, Netbeans 8+ is recommended because it is free of cost, works cross platform, has good support for Java EE projects, and happens to be the IDE that the `development team at IQSS <https://dataverse.org/about>`_ has standardized on.

NetBeans can be downloaded from http://netbeans.org. Please make sure that you use an option that contains the Jave EE features when choosing your download bundle. While using the installer you might be prompted about installing JUnit and Glassfish. There is no need to reinstall Glassfish, but it is recommended that you install JUnit.

This guide will assume you are using Netbeans for development.

Please note that if you have downloaded Glassfish as part of a Netbeans bundle, you can manually add the proper (older) version of Glassfish (4.1, as mentioned above) by clicking "Tools", "Servers", "Add Server". You can use the same interface to remove the newer version of Glassfish that's incompatible with Dataverse.

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

Additional Tools
~~~~~~~~~~~~~~~~

Please see also the :doc:`/developers/tools` page, which lists additional tools that very useful but not essential.


Shibboleth and OAuth
--------------------

If you are working on anything related to users, please keep in mind that your changes will likely affect Shibboleth and OAuth users. See :doc:`remote-users` for how to test this code in your local dev environment.

Geoconnect
----------

Geoconnect works as a middle layer, allowing geospatial data files in Dataverse to be visualized with Harvard WorldMap. To set up a Geoconnect development environment, you can follow the steps outlined in the `local_setup.md <https://github.com/IQSS/geoconnect/blob/master/local_setup.md>`_ guide. You will need Python and a few other prerequisites.

As mentioned under "Architecture and Components" in the :doc:`/installation/prep` section of the Installation Guide, Geoconnect is an optional component of Dataverse, so this section is only necessary to follow it you are working on an issue related to this feature.

----

Previous: :doc:`intro` | Next: :doc:`troubleshooting`
