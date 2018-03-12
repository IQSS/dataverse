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

Iterating on Code and Redeploying
---------------------------------

Deploy on Save
~~~~~~~~~~~~~~

Out of the box, Netbeans is configured to "Deploy on Save" which means that if you save any changes to project files such as Java classes, XHTML files, or "bundle" files (i.e. Bundle.properties), the project is recompiled and redeployed to Glassfish automatically. This behavior works well for many of us but if you don't like it, you can turn it off by right-clicking "dataverse" under the Projects tab, clicking "Run" and unchecking "Deploy on Save".

Deploying Manually
~~~~~~~~~~~~~~~~~~

For developers not using Netbeans, or deploying to a non-local system for development, code can be deployed manually.
There are four steps to this process:

1. Build the war file: ``mvn package``
2. Undeploy the Dataverse application (if necessary): ``asadmin undeploy dataverse-VERSION``
3. Copy the war file to the development server (if necessary)
4. Deploy the new code: ``asadmin deploy /path/to/dataverse-VERSION.war``

The :doc:`/installation/installation-main` section of the Installation Guide has more information on this topic.

Netbeans Connector Chrome Extension
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

For faster iteration while working on JSF pages, it is highly recommended that you install the Netbeans Connector Chrome Extension listed in the :doc:`tools` section. When you save XHTML or CSS files, you will see the changes immediately.

Troubleshooting
---------------

We've described above the "happy path" of when everything goes right with setting up your Dataverse development environment. Here are some common problems and solutions for when things go wrong.

context-root in glassfish-web.xml Munged by Netbeans
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

For unknown reasons, Netbeans will sometimes change the following line under ``src/main/webapp/WEB-INF/glassfish-web.xml``:

``<context-root>/</context-root>``

Sometimes Netbeans will change ``/`` to ``/dataverse``. Sometimes it will delete the line entirely. Either way, you will see very strange behavior when attempting to click around Dataverse in a browser. The home page will load but icons will be missing. Any other page will fail to load entirely and you'll see a Glassfish error.

The solution is to put the file back to how it was before Netbeans touched it. If anyone knows of an open Netbeans bug about this, please let us know.

Configuring / Troubleshooting Mail Host
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Out of the box, no emails will be sent from your development environment. This is because you have to set the ``:SystemEmail`` setting and make sure you've configured your SMTP correctly.

You can configure ``:SystemEmail`` like this:

``curl -X PUT -d 'Davisverse SWAT Team <davisthedog@harvard.edu>' http://localhost:8080/api/admin/settings/:SystemEmail``

Unfortunately for developers not at Harvard, the installer script gives you by default an SMTP server of ``mail.hmdc.harvard.edu`` but you can specify an alternative SMTP server when you run the installer.

You can check the current SMTP server with the ``asadmin`` command:

``asadmin get server.resources.mail-resource.mail/notifyMailSession.host``

This command helps verify what host your domain is using to send mail. Even if it's the correct hostname, you may still need to adjust settings. If all else fails, there are some free SMTP service options available such as Gmail and MailGun. This can be configured from the GlassFish console or the command line.

1. First, navigate to your Glassfish admin console: http://localhost:4848
2. From the left-side panel, select **JavaMail Sessions**
3. You should see one session named **mail/notifyMailSession** -- click on that.

From this window you can modify certain fields of your Dataverse's notifyMailSession, which is the JavaMail session for outgoing system email (such as on user signup or data publication). Two of the most important fields we need are:

- **Mail Host:** The DNS name of the default mail server (e.g. smtp.gmail.com)
- **Default User:** The username provided to your Mail Host when you connect to it (e.g. johndoe@gmail.com)

Most of the other defaults can safely be left as is. **Default Sender Address** indicates the address that your installation's emails are sent from.

If your user credentials for the SMTP server require a password, you'll need to configure some **Additional Properties** at the bottom.

**IMPORTANT:** Before continuing, it's highly recommended that your Default User account does NOT use a password you share with other accounts, as one of the additional properties includes entering the Default User's password (without concealing it on screen). For smtp.gmail.com you can safely use an `app password <https://support.google.com/accounts/answer/185833?hl=en>`_ or create an extra Gmail account for use with your Dataverse dev environment.

Authenticating yourself to a Mail Host can be tricky. As an example, we'll walk through setting up our JavaMail Session to use smtp.gmail.com as a host by way of SSL on port 465. Use the Add Property button to generate a blank property for each name/value pair.

======================================	==============================
				Name 								Value
======================================	==============================
mail.smtp.auth							true
mail.smtp.password						[user's (*app*) password\*]
mail.smtp.port							465
mail.smtp.socketFactory.port			465
mail.smtp.socketFactory.fallback		false
mail.smtp.socketFactory.class			javax.net.ssl.SSLSocketFactory
======================================	==============================

**\*WARNING**: Entering a password here will *not* conceal it on-screen. It’s recommended to use an *app password* (for smtp.gmail.com users) or utilize a dedicated/non-personal user account with SMTP server auths so that you do not risk compromising your password.

Save these changes at the top of the page and restart your Glassfish server to try it out.

The mail session can also be set from command line. To use this method, you will need to delete your notifyMailSession and create a new one. See the below example:

- Delete: ``asadmin delete-javamail-resource mail/MyMailSession``
- Create (remove brackets and replace the variables inside): ``asadmin create-javamail-resource --mailhost [smtp.gmail.com] --mailuser [test\@test\.com] --fromaddress [test\@test\.com] --property mail.smtp.auth=[true]:mail.smtp.password=[password]:mail.smtp.port=[465]:mail.smtp.socketFactory.port=[465]:mail.smtp.socketFactory.fallback=[false]:mail.smtp.socketFactory.class=[javax.net.ssl.SSLSocketFactory] mail/notifyMailSession``

These properties can be tailored to your own preferred mail service, but if all else fails these settings work fine with Dataverse development environments for your localhost.

+ If you're seeing a "Relay access denied" error in your Glassfish logs when your app attempts to send an email, double check your user/password credentials for the Mail Host you're using.
+ If you're seeing a "Connection refused" / similar error upon email sending, try another port.

Rebuilding Your Dev Environment
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

If you have an old copy of the database and old Solr data and want to start fresh, here are the recommended steps: 

- drop your old database
- clear out your existing Solr index: ``scripts/search/clear``
- run the installer script above - it will create the db, deploy the app, populate the db with reference data and run all the scripts that create the domain metadata fields. You no longer need to perform these steps separately.
- confirm you are using the latest Dataverse-specific Solr schema.xml per the "Installing and Running Solr" section of this guide
- confirm http://localhost:8080 is up
- If you want to set some dataset-specific facets, go to the root dataverse (or any dataverse; the selections can be inherited) and click "General Information" and make choices under "Select Facets". There is a ticket to automate this: https://github.com/IQSS/dataverse/issues/619

You may also find https://github.com/IQSS/dataverse/blob/develop/scripts/deploy/phoenix.dataverse.org/deploy and related scripts interesting because they demonstrate how we have at least partially automated the process of tearing down a Dataverse installation and having it rise again, hence the name "phoenix." See also "Fresh Reinstall" in the :doc:`/installation/installation-main` section of the Installation Guide.

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

DataCite
--------

If you've reconfigured from EZID to DataCite and are seeing ``Response code: 400, [url] domain of URL is not allowed`` it's probably because your ``dataverse.siteUrl`` JVM option is unset or set to localhost (``-Ddataverse.siteUrl=http://localhost:8080``). You can try something like this:

``asadmin delete-jvm-options '-Ddataverse.siteUrl=http\://localhost\:8080'``

``asadmin create-jvm-options '-Ddataverse.siteUrl=http\://demo.dataverse.org'``

----

Previous: :doc:`intro` | Next: :doc:`version-control`
