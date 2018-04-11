============
Installation
============

Now that the :doc:`prerequisites` are in place, we are ready to execute the Dataverse installation script (the "installer") and verify that the installation was successful by logging in with a "superuser" account.

.. contents:: |toctitle|
	:local:

.. _dataverse-installer:

Running the Dataverse Installer
-------------------------------

A scripted, interactive installer is provided. This script will configure your Glassfish environment, create the database, set some required options and start the application. Some configuration tasks will still be required after you run the installer! So make sure to consult the next section. 

As mentioned in the :doc:`prerequisites` section, RHEL/CentOS is the recommended Linux distribution. (The installer is also known to work on Mac OS X for setting up a development environment.)

Generally, the installer has a better chance of succeeding if you run it against a freshly installed Glassfish node that still has all the default configuration settings. In any event, please make sure that it is still configured to accept http connections on port 8080 - because that's where the installer expects to find the application once it's deployed.

You should have already downloaded the installer from https://github.com/IQSS/dataverse/releases when setting up and starting Solr under the :doc:`prerequisites` section. Again, it's a zip file with "dvinstall" in the name.

Unpack the zip file - this will create the directory ``dvinstall``.

Execute the installer script like this (but first read the note below about not running the installer as root)::

        $ cd dvinstall
        $ ./install

**It is no longer necessary to run the installer as root!**

Just make sure the user running the installer has write permission to:

- /usr/local/glassfish4/glassfish/lib
- /usr/local/glassfish4/glassfish/domains/domain1
- the current working directory of the installer (it currently writes its logfile there), and
- your jvm-option specified files.dir

The only reason to run Glassfish as root would be to allow Glassfish itself to listen on the default HTTP(S) ports 80 and 443, or any other port below 1024. However, it is simpler and more secure to run Glassfish run on its default port of 8080 and hide it behind an Apache Proxy, via AJP, running on port 80 or 443. This configuration is required if you're going to use Shibboleth authentication. See more discussion on this here: :doc:`shibboleth`.)

The script will prompt you for some configuration values. If this is a test/evaluation installation, it may be possible to accept the default values provided for most of the settings:

- Internet Address of your host: localhost
- Glassfish Directory: /usr/local/glassfish4
- Glassfish User: current user running the installer script
- Administrator email address for this Dataverse: (none)
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
- Postgres admin password - We'll need it in order to create the database and user for the Dataverse to use, without having to run the installer as root. If you don't know your Postgres admin password, you may simply set the authorization level for localhost to "trust" in the PostgreSQL ``pg_hba.conf`` file (See the PostgreSQL section in the Prerequisites). If this is a production evnironment, you may want to change it back to something more secure, such as "password" or "md5", after the installation is complete.
- Network address of a remote Solr search engine service (if needed) - In most cases, you will be running your Solr server on the same host as the Dataverse application (then you will want to leave this set to the default value of ``LOCAL``). But in a serious production environment you may set it up on a dedicated separate server.

If desired, these default values can be configured by creating a ``default.config`` (example :download:`here <../_static/util/default.config>`) file in the installer's working directory with new values (if this file isn't present, the above defaults will be used).

This allows the installer to be run in non-interactive mode (with ``./install -y -f > install.out 2> install.err``), which can allow for easier interaction with automated provisioning tools.

All the Glassfish configuration tasks performed by the installer are isolated in the shell script ``dvinstall/glassfish-setup.sh`` (as ``asadmin`` commands). 

**IMPORTANT:** Please note, that "out of the box" the installer will configure the Dataverse to leave unrestricted access to the administration APIs from (and only from) localhost. Please consider the security implications of this arrangement (anyone with shell access to the server can potentially mess with your Dataverse). An alternative solution would be to block open access to these sensitive API endpoints completely; and to only allow requests supplying a pre-defined "unblock token" (password). If you prefer that as a solution, please consult the supplied script ``post-install-api-block.sh`` for examples on how to set it up. See also "Securing Your Installation" under the :doc:`config` section.

The script is to a large degree a derivative of the old installer from DVN 3.x. It is written in Perl. If someone in the community is eager to rewrite it, perhaps in a different language, please get in touch. :)

Logging In
----------

Out of the box, Glassfish runs on port 8080 and 8181 rather than 80 and 443, respectively, so visiting http://localhost:8080 (substituting your hostname) should bring up a login page. See the :doc:`shibboleth` page for more on ports, but for now, let's confirm we can log in by using port 8080. Poke a temporary hole in your firewall, if needed. 

Superuser Account
^^^^^^^^^^^^^^^^^

We'll use the superuser account created by the installer to make sure you can log into Dataverse. For more on the difference between being a superuser and having the "Admin" role, read about configuring the root dataverse in the :doc:`config` section.

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

If the following doesn't apply, please get in touch as explained in the :doc:`intro`. You may be asked to provide ``glassfish4/glassfish/domains/domain1/logs/server.log`` for debugging.

Dataset Cannot Be Published
^^^^^^^^^^^^^^^^^^^^^^^^^^^

Check to make sure you used a fully qualified domain name when installing Dataverse. You can change the ``dataverse.fqdn`` JVM option after the fact per the :doc:`config` section.

Problems Sending Email
^^^^^^^^^^^^^^^^^^^^^^

If your Dataverse installation is not sending system emails, you may need to provide authentication for your mail host. First, double check the SMTP server being used with this Glassfish asadmin command:

``asadmin get server.resources.mail-resource.mail/notifyMailSession.host``

This should return the DNS of the mail host you configured during or after installation. mail/notifyMailSession is the JavaMail Session that's used to send emails to users. 

If the command returns a host you don't want to use, you can modify your notifyMailSession with the Glassfish ``asadmin set`` command with necessary options (`click here for the manual page <https://docs.oracle.com/cd/E18930_01/html/821-2433/set-1.html>`_), or via the admin console at http://localhost:4848 with your domain running. 

If your mail host requires a username/password for access, continue to the next section.

Mail Host Configuration & Authentication
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If you need to alter your mail host address, user, or provide a password to connect with, these settings are easily changed in the Glassfish admin console or via command line. 

For the Glassfish console, load a browser with your domain online, navigate to http://localhost:4848 and on the side panel find JavaMail Sessions. By default, Dataverse uses a session named mail/notifyMailSession for routing outgoing emails. Click this mail session in the window to modify it.

When fine tuning your JavaMail Session, there are a number of fields you can edit. The most important are:

+ **Mail Host:** Desired mail host’s DNS address (e.g. smtp.gmail.com)
+ **Default User:** Username mail host will recognize (e.g. user\@gmail.com)
+ **Default Sender Address:** Email address that your Dataverse will send mail from

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

- Delete: ``asadmin delete-javamail-resource mail/MyMailSession``
- Create (remove brackets and replace the variables inside): ``asadmin create-javamail-resource --mailhost [smtp.gmail.com] --mailuser [test\@test\.com] --fromaddress [test\@test\.com] --property mail.smtp.auth=[true]:mail.smtp.password=[password]:mail.smtp.port=[465]:mail.smtp.socketFactory.port=[465]:mail.smtp.socketFactory.fallback=[false]:mail.smtp.socketFactory.class=[javax.net.ssl.SSLSocketFactory] mail/notifyMailSession``

Be sure you save the changes made here and then restart your Glassfish server to test it out.

UnknownHostException While Deploying
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If you are seeing "Caused by: java.net.UnknownHostException: myhost: Name or service not known" in server.log and your hostname is "myhost" the problem is likely that "myhost" doesn't appear in ``/etc/hosts``. See also http://stackoverflow.com/questions/21817809/glassfish-exception-during-deployment-project-with-stateful-ejb/21850873#21850873

Fresh Reinstall
---------------

Early on when you're installing Dataverse, you may think, "I just want to blow away what I've installed and start over." That's fine. You don't have to uninstall the various components like Glassfish, PostgreSQL and Solr, but you should be conscious of how to clear out their data.

Drop database
^^^^^^^^^^^^^

In order to drop the database, you have to stop Glassfish, which will have open connections. Before you stop Glassfish, you may as well undeploy the war file. First, find the name like this:

``asadmin list-applications``

Then undeploy it like this:

``asadmin undeploy dataverse-VERSION``

Stop Glassfish with the init script provided in the :doc:`prerequisites` section or just use:

``asadmin stop-domain``

With Glassfish down, you should now be able to drop your database and recreate it:

``psql -U dvnapp -c 'DROP DATABASE "dvndb"' template1``

Clear Solr
^^^^^^^^^^

The database is fresh and new but Solr has stale data it in. Clear it out with this command:

``curl http://localhost:8983/solr/update/json?commit=true -H "Content-type: application/json" -X POST -d "{\"delete\": { \"query\":\"*:*\"}}"``


Deleting Uploaded Files
^^^^^^^^^^^^^^^^^^^^^^^

The path below will depend on the value for ``dataverse.files.directory`` as described in the :doc:`config` section:

``rm -rf /usr/local/glassfish4/glassfish/domains/domain1/files``

Rerun Installer
^^^^^^^^^^^^^^^

With all the data cleared out, you should be ready to rerun the installer per above.

Related to all this is a series of scripts at https://github.com/IQSS/dataverse/blob/develop/scripts/deploy/phoenix.dataverse.org/deploy that Dataverse developers use have the test server http://phoenix.dataverse.org rise from the ashes before integration tests are run against it. Your mileage may vary. :) For more on this topic, see "Rebuilding Your Dev Environment" in the :doc:`/developers/dev-environment` section of the Developer Guide.
