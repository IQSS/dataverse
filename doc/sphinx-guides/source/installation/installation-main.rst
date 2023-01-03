============
Installation
============

Now that the :doc:`prerequisites` are in place, we are ready to execute the Dataverse Software installation script (the "installer") and verify that the installation was successful by logging in with a "superuser" account.

.. contents:: |toctitle|
	:local:

.. _dataverse-installer:

Running the Dataverse Software Installer
----------------------------------------

A scripted, interactive installer is provided. This script will configure your app server environment, create the database, set some required options and start the application. Some configuration tasks will still be required after you run the installer! So make sure to consult the next section. 

As mentioned in the :doc:`prerequisites` section, RHEL or a derivative such as RockyLinux or AlmaLinux is recommended. (The installer is also known to work on Mac OS X for setting up a development environment.)

Generally, the installer has a better chance of succeeding if you run it against a freshly installed Payara node that still has all the default configuration settings. In any event, please make sure that it is still configured to accept http connections on port 8080 - because that's where the installer expects to find the application once it's deployed.

You should have already downloaded the installer from https://github.com/IQSS/dataverse/releases when setting up and starting Solr under the :doc:`prerequisites` section. Again, it's a zip file with "dvinstall" in the name.

Unpack the zip file - this will create the directory ``dvinstall``.

**Important:** The installer will need to use the PostgreSQL command line utility ``psql`` in order to configure the database. If the executable is not in your system PATH, the installer will try to locate it on your system. However, we strongly recommend that you check and make sure it is in the PATH. This is especially important if you have multiple versions of PostgreSQL installed on your system. Make sure the psql that came with the version that you want to use with your Dataverse installation is the first on your path. For example, if the PostgreSQL distribution you are running is installed in  /Library/PostgreSQL/13, add /Library/PostgreSQL/13/bin to the beginning of your $PATH variable. If you are *running* multiple PostgreSQL servers, make sure you know the port number of the one you want to use, as the installer will need it in order to connect to the database (the first PostgreSQL distribution installed on your system is likely using the default port 5432; but the second will likely be on 5433, etc.) Does every word in this paragraph make sense? If it does, great - because you definitely need to be comfortable with basic system tasks in order to install the Dataverse Software. If not - if you don't know how to check where your PostgreSQL is installed, or what port it is running on, or what a $PATH is... it's not too late to stop. Because it will most likely not work. And if you contact us for help, these will be the questions we'll be asking you - so, again, you need to be able to answer them comfortably for it to work. 

**It is no longer necessary to run the installer as root!**

Just make sure the user running the installer has write permission to:

- /usr/local/payara5/glassfish/lib
- /usr/local/payara5/glassfish/domains/domain1
- the current working directory of the installer (it currently writes its logfile there), and
- your jvm-option specified files.dir

The only reason to run Payara as root would be to allow Payara itself to listen on the default HTTP(S) ports 80 and 443, or any other port below 1024. However, it is simpler and more secure to run Payara run on its default port of 8080 and hide it behind an Apache Proxy, via AJP, running on port 80 or 443. This configuration is required if you're going to use Shibboleth authentication. See more discussion on this here: :doc:`shibboleth`.)

Read the installer script directions like this::

        $ cd dvinstall
        $ less README_python.txt

Alternatively you can download :download:`README_python.txt <../../../../scripts/installer/README_python.txt>` from this guides.

Follow the instructions in the text file.

The script will prompt you for some configuration values. If this is a test/evaluation installation, it may be possible to accept the default values provided for most of the settings:

- Internet Address of your host: localhost
- Payara Directory: /usr/local/payara5
- Payara User: current user running the installer script
- Administrator email address for this Dataverse installation: (none)
- SMTP (mail) server to relay notification messages: localhost
- Postgres Server Address: [127.0.0.1]
- Postgres Server Port: 5432
- Postgres ADMIN password: secret
- Name of the Postgres Database: dvndb
- Name of the Postgres User: dvnapp
- Postgres user password: secret
- Remote Solr indexing service: LOCAL
- Rserve Server: localhost
- Rserve Server Port: 6311
- Rserve User Name: rserve
- Rserve User Password: rserve
- Administration Email address for the installation;
- Postgres admin password - We'll need it in order to create the database and user for the Dataverse Software installer to use, without having to run the installer as root. If you don't know your Postgres admin password, you may simply set the authorization level for localhost to "trust" in the PostgreSQL ``pg_hba.conf`` file (See the PostgreSQL section in the Prerequisites). If this is a production environment, you may want to change it back to something more secure, such as "password" or "md5", after the installation is complete.
- Network address of a remote Solr search engine service (if needed) - In most cases, you will be running your Solr server on the same host as the Dataverse Software application (then you will want to leave this set to the default value of ``LOCAL``). But in a serious production environment you may set it up on a dedicated separate server.

If desired, these default values can be configured by creating a ``default.config`` (example :download:`here <../../../../scripts/installer/default.config>`) file in the installer's working directory with new values (if this file isn't present, the above defaults will be used).

This allows the installer to be run in non-interactive mode (with ``./install -y -f > install.out 2> install.err``), which can allow for easier interaction with automated provisioning tools.

All the Payara configuration tasks performed by the installer are isolated in the shell script ``dvinstall/as-setup.sh`` (as ``asadmin`` commands). 

While Postgres can accomodate usernames and database names containing hyphens, it is strongly recommended to use only alphanumeric characters.

**IMPORTANT:** As a security measure, the ``as-setup.sh`` script stores passwords as "aliases" rather than plaintext. If you change your database password, for example, you will need to update the alias with ``asadmin update-password-alias dataverse.db.password``, for example. Here is a list of the password aliases that are set by the installation process and entered into Payara's ``domain.xml`` file:

- ``dataverse.db.password``
- ``doi_password_alias``
- ``rserve_password_alias``

For more information, please see https://docs.payara.fish/documentation/payara-server/password-aliases/password-alias-asadmin-commands.html

.. _importance-of-siteUrl:

**IMPORTANT:** The installer will also ask for an external site URL for the Dataverse installation. It is *imperative* that this value be supplied accurately, or a long list of functions will be inoperable, including:

- email confirmation links
- password reset links
- generating a Private URL
- exporting to Schema.org format (and showing JSON-LD in HTML's <meta/> tag)
- exporting to DDI format
- which Dataverse installation an "external tool" should return to
- URLs embedded in SWORD API responses

The supplied site URL will be saved under the JVM option :ref:`dataverse.siteUrl`.

**IMPORTANT:** Please note, that "out of the box" the installer will configure the Dataverse installation to leave unrestricted access to the administration APIs from (and only from) localhost. Please consider the security implications of this arrangement (anyone with shell access to the server can potentially mess with your Dataverse installation). An alternative solution would be to block open access to these sensitive API endpoints completely; and to only allow requests supplying a pre-defined "unblock token" (password). If you prefer that as a solution, please consult the supplied script ``post-install-api-block.sh`` for examples on how to set it up. See also "Securing Your Installation" under the :doc:`config` section.

The Dataverse Software uses JHOVE_ to help identify the file format (CSV, PNG, etc.) for files that users have uploaded. The installer places files called ``jhove.conf`` and ``jhoveConfig.xsd`` into the directory ``/usr/local/payara5/glassfish/domains/domain1/config`` by default and makes adjustments to the jhove.conf file based on the directory into which you chose to install Payara.

.. _JHOVE: http://jhove.openpreservation.org

Logging In
----------

Out of the box, Payara runs on port 8080 and 8181 rather than 80 and 443, respectively, so visiting http://localhost:8080 (substituting your hostname) should bring up a login page. See the :doc:`shibboleth` page for more on ports, but for now, let's confirm we can log in by using port 8080. Poke a temporary hole in your firewall, if needed. 

Superuser Account
^^^^^^^^^^^^^^^^^

We'll use the superuser account created by the installer to make sure you can log into the Dataverse installation. For more on the difference between being a superuser and having the "Admin" role, read about configuring the root Dataverse collection in the :doc:`config` section.

(The ``dvinstall/setup-all.sh`` script, which is called by the installer sets the password for the superuser account account and the username and email address come from a file it references at ``dvinstall/data/user-admin.json``.)

Use the following credentials to log in:

- URL: http://localhost:8080
- username: dataverseAdmin
- password: admin

Congratulations! You have a working Dataverse installation. Soon you'll be tweeting at `@dataverseorg <https://twitter.com/dataverseorg>`_ asking to be added to the map at http://dataverse.org :)

Trouble? See if you find an answer in the troubleshooting section below.

Next you'll want to check out the :doc:`config` section, especially the section on security which reminds you to change the password above.

Troubleshooting
---------------

If the following doesn't apply, please get in touch as explained in :ref:`support`.

Dataset Cannot Be Published
^^^^^^^^^^^^^^^^^^^^^^^^^^^

Check to make sure you used a fully qualified domain name when installing the Dataverse Software. You can change the ``dataverse.fqdn`` JVM option after the fact per the :doc:`config` section.

Got ERR_ADDRESS_UNREACHABLE While Navigating on Interface or API Calls
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If you are receiving an ``ERR_ADDRESS_UNREACHABLE`` while navigating the GUI or making an API call, make sure the ``siteUrl`` JVM option is defined. For details on how to set ``siteUrl``, please refer to :ref:`dataverse.siteUrl` from the :doc:`config` section. For context on why setting this option is necessary, refer to :ref:`dataverse.fqdn` from the :doc:`config` section.

Problems Sending Email
^^^^^^^^^^^^^^^^^^^^^^

If your Dataverse installation is not sending system emails, you may need to provide authentication for your mail host. First, double check the SMTP server being used with this Payara asadmin command:

``./asadmin get server.resources.mail-resource.mail/notifyMailSession.host``

This should return the DNS of the mail host you configured during or after installation. mail/notifyMailSession is the JavaMail Session that's used to send emails to users. 

If the command returns a host you don't want to use, you can modify your notifyMailSession with the Payara ``asadmin set`` command with necessary options (`click here for the manual page <https://docs.oracle.com/cd/E18930_01/html/821-2433/set-1.html>`_), or via the admin console at http://localhost:4848 with your domain running. 

If your mail host requires a username/password for access, continue to the next section.

Mail Host Configuration & Authentication
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If you need to alter your mail host address, user, or provide a password to connect with, these settings are easily changed in the Payara admin console or via command line. 

For the Payara console, load a browser with your domain online, navigate to http://localhost:4848 and on the side panel find JavaMail Sessions. By default, the Dataverse Software uses a session named mail/notifyMailSession for routing outgoing emails. Click this mail session in the window to modify it.

When fine tuning your JavaMail Session, there are a number of fields you can edit. The most important are:

+ **Mail Host:** Desired mail host’s DNS address (e.g. smtp.gmail.com)
+ **Default User:** Username mail host will recognize (e.g. user\@gmail.com)
+ **Default Sender Address:** Email address that your Dataverse installation will send mail from

Depending on the SMTP server you're using, you may need to add additional properties at the bottom of the page (below "Advanced").

From the "Add Properties" utility at the bottom, use the “Add Property” button for each entry you need, and include the name / corresponding value as needed. Descriptions are optional, but can be used for your own organizational needs. 

**Note:** These properties are just an example. You may need different/more/fewer properties all depending on the SMTP server you’re using.

==============================	==============================
			Name 							Value
==============================	==============================
mail.smtp.auth					true
mail.smtp.password				[Default User password*]
mail.smtp.port					[Port number to route through]
==============================	==============================

**\*WARNING**: Entering a password here will *not* conceal it on-screen. It’s recommended to use an *app password* (for smtp.gmail.com users) or utilize a dedicated/non-personal user account with SMTP server auths so that you do not risk compromising your password.

If your installation’s mail host uses SSL (like smtp.gmail.com) you’ll need these name/value pair properties in place:

======================================	==============================
				Name 								Value
======================================	==============================
mail.smtp.socketFactory.port			465
mail.smtp.port							465
mail.smtp.socketFactory.fallback		false
mail.smtp.socketFactory.class			javax.net.ssl.SSLSocketFactory
======================================	==============================

The mail session can also be set from command line. To use this method, you will need to delete your notifyMailSession and create a new one. See the below example:

- Delete: ``./asadmin delete-javamail-resource mail/notifyMailSession``
- Create (remove brackets and replace the variables inside): ``./asadmin create-javamail-resource --mailhost [smtp.gmail.com] --mailuser [test\@test\.com] --fromaddress [test\@test\.com] --property mail.smtp.auth=[true]:mail.smtp.password=[password]:mail.smtp.port=[465]:mail.smtp.socketFactory.port=[465]:mail.smtp.socketFactory.fallback=[false]:mail.smtp.socketFactory.class=[javax.net.ssl.SSLSocketFactory] mail/notifyMailSession``

Be sure you save the changes made here and then restart your Payara server to test it out.

UnknownHostException While Deploying
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If you are seeing "Caused by: java.net.UnknownHostException: myhost: Name or service not known" in server.log and your hostname is "myhost" the problem is likely that "myhost" doesn't appear in ``/etc/hosts``. See also http://stackoverflow.com/questions/21817809/glassfish-exception-during-deployment-project-with-stateful-ejb/21850873#21850873

.. _fresh-reinstall:

Fresh Reinstall
---------------

Early on when you're installing the Dataverse Software, you may think, "I just want to blow away what I've installed and start over." That's fine. You don't have to uninstall the various components like Payara, PostgreSQL and Solr, but you should be conscious of how to clear out their data. For Payara, a common helpful process is to:

- Stop Payara; 
- Remove the ``generated``, ``lib/databases`` and ``osgi-cache`` directories from the ``domain1`` directory;
- Start Payara

Drop database
^^^^^^^^^^^^^

In order to drop the database, you have to stop Payara, which will have open connections. Before you stop Payara, you may as well undeploy the war file. First, find the name like this:

``./asadmin list-applications``

Then undeploy it like this:

``./asadmin undeploy dataverse-VERSION``

Stop Payara with the init script provided in the :doc:`prerequisites` section or just use:

``./asadmin stop-domain``

With Payara down, you should now be able to drop your database and recreate it:

``psql -U dvnapp -c 'DROP DATABASE "dvndb"' template1``

Clear Solr
^^^^^^^^^^

The database is fresh and new but Solr has stale data it in. Clear it out with this command:

``curl http://localhost:8983/solr/collection1/update/json?commit=true -H "Content-type: application/json" -X POST -d "{\"delete\": { \"query\":\"*:*\"}}"``


Deleting Uploaded Files
^^^^^^^^^^^^^^^^^^^^^^^

The path below will depend on the value for ``dataverse.files.directory`` as described in the :doc:`config` section:

``rm -rf /usr/local/payara5/glassfish/domains/domain1/files``

Rerun Installer
^^^^^^^^^^^^^^^

With all the data cleared out, you should be ready to rerun the installer per above.

Related to all this is a series of scripts at https://github.com/IQSS/dataverse/blob/develop/scripts/deploy/phoenix.dataverse.org/deploy that Dataverse Project Team and Community developers use have the test server http://phoenix.dataverse.org rise from the ashes before integration tests are run against it. For more on this topic, see :ref:`rebuilding-dev-environment` section of the Developer Guide.

Getting Support for Installation Trouble
----------------------------------------

See :ref:`support`.
