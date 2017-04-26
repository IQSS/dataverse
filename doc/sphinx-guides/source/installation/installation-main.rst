============
Installation
============

Now that the :doc:`prerequisites` are in place, we are ready to execute the Dataverse installation script (the "installer") and verify that the installation was successful by logging in with a "superuser" account.

.. contents:: :local:

Running the Dataverse Installer
-------------------------------

A scripted, interactive installer is provided. This script will configure your Glassfish environment, create the database, set some required options and start the application. Some configuration tasks will still be required after you run the installer! So make sure to consult the next section. 
At this point the installer only runs on RHEL 6 and similar and MacOS X (recommended as the platform for developers). 

Generally, the installer has a better chance of succeeding if you run it against a freshly installed Glassfish node that still has all the default configuration settings. In any event, please make sure that it is still configured to accept http connections on port 8080 - because that's where the installer expects to find the application once it's deployed. 


You should have already downloaded the installer from https://github.com/IQSS/dataverse/releases when setting up and starting Solr under the :doc:`prerequisites` section. Again, it's a zip file with "dvinstall" in the name.

Unpack the zip file - this will create the directory ``dvinstall``.

Execute the installer script like this::

        # cd dvinstall
        # ./install

**NEW in Dataverse 4.3:** It is no longer necessary to run the installer as root!
Just make sure the user that runs the installer has the write permission in the Glassfish directory. For example, if your Glassfish directory is owned by root, and you try to run the installer as a regular user, it's not going to work. 
(Do note, that you want the Glassfish directory to be owned by the same user that will be running Glassfish. And you most likely won't need to run it as root. The only reason to run Glassfish as root would be to have a convenient way to run the application on the default HTTP(S) ports 80 and 443, instead of 8080 and 8181. However, an easier, and more secure way to achieve that would be to instead keep Glassfish running on a high port, and hide it behind an Apache Proxy, via AJP, running on port 80. This configuration is in fact required if you choose to have your Dataverse support Shibboleth authentication. See more discussion on this here: :doc:`shibboleth`.)


The script will prompt you for some configuration values. If this is a test/evaluation installation, it may be possible to accept the default values provided for most of the settings:

- Internet Address of your host: localhost
- Glassfish Directory: /usr/local/glassfish4
- Administrator email address for this Dataverse: (none)
- SMTP (mail) server to relay notification messages: localhost
- Postgres Server Address: [127.0.0.1]
- Postgres Server Port: 5432
- Postgres ADMIN password: secret
- Name of the Postgres Database: dvndb
- Name of the Postgres User: dvnapp
- Postgres user password: secret
- Remote Solr indexing service: LOCAL
- Will this Dataverse be using TwoRavens application: NOT INSTALLED
- Rserve Server: localhost
- Rserve Server Port: 6311
- Rserve User Name: rserve
- Rserve User Password: rserve

**New, as of 4.3:**

- Administration Email address for the installation;
- Postgres admin password - We'll need it in order to create the database and user for the Dataverse to use, without having to run the installer as root. If you don't know your Postgres admin password, you may simply set the authorization level for localhost to "trust" in the PostgreSQL ``pg_hba.conf`` file (See the PostgreSQL section in the Prerequisites). If this is a production evnironment, you may want to change it back to something more secure, such as "password" or "md5", after the installation is complete.
- Network address of a remote Solr search engine service (if needed) - In most cases, you will be running your Solr server on the same host as the Dataverse application (then you will want to leave this set to the default value of ``LOCAL``). But in a serious production environment you may set it up on a dedicated separate server.
- The URL of the TwoRavens application GUI, if this Dataverse node will be using a companion TwoRavens installation. Otherwise, leave it set to ``NOT INSTALLED``. 

The script is to a large degree a derivative of the old installer from DVN 3.x. It is written in Perl. If someone in the community is eager to rewrite it, perhaps in a different language, please get in touch. :)

All the Glassfish configuration tasks performed by the installer are isolated in the shell script ``dvinstall/glassfish-setup.sh`` (as ``asadmin`` commands). 

**IMPORTANT:** Please note, that "out of the box" the installer will configure the Dataverse to leave unrestricted access to the administration APIs from (and only from) localhost. Please consider the security implications of this arrangement (anyone with shell access to the server can potentially mess with your Dataverse). An alternative solution would be to block open access to these sensitive API endpoints completely; and to only allow requests supplying a pre-defined "unblock token" (password). If you prefer that as a solution, please consult the supplied script ``post-install-api-block.sh`` for examples on how to set it up.

Logging In
----------

Out of the box, Glassfish runs on port 8080 and 8181 rather than 80 and 443, respectively, so visiting http://localhost:8080 (substituting your hostname) should bring up a login page. See the :doc:`shibboleth` page for more on ports, but for now, let's confirm we can log in by using port 8080. Poke a temporary hole in your firewall, if needed. 

Superuser Account
+++++++++++++++++

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
+++++++++++++++++++++++++++

Check to make sure you used a fully qualified domain name when installing Dataverse. You can change the ``dataverse.fqdn`` JVM option after the fact per the :doc:`config` section.

Problems Sending Email
++++++++++++++++++++++

You can confirm the SMTP server being used with this command:

``asadmin get server.resources.mail-resource.mail/notifyMailSession.host``

UnknownHostException While Deploying
++++++++++++++++++++++++++++++++++++

If you are seeing "Caused by: java.net.UnknownHostException: myhost: Name or service not known" in server.log and your hostname is "myhost" the problem is likely that "myhost" doesn't appear in ``/etc/hosts``. See also http://stackoverflow.com/questions/21817809/glassfish-exception-during-deployment-project-with-stateful-ejb/21850873#21850873

Fresh Reinstall
---------------

Early on when you're installing Dataverse, you may think, "I just want to blow away what I've installed and start over." That's fine. You don't have to uninstall the various components like Glassfish, PostgreSQL and Solr, but you should be conscious of how to clear out their data.

Drop database
+++++++++++++

In order to drop the database, you have to stop Glassfish, which will have open connections. Before you stop Glassfish, you may as well undeploy the war file. First, find the name like this:

``asadmin list-applications``

Then undeploy it like this:

``asadmin undeploy dataverse-VERSION``

Stop Glassfish with the init script provided in the :doc:`prerequisites` section or just use:

``asadmin stop-domain``

With Glassfish down, you should now be able to drop your database and recreate it:

``psql -U dvnapp -c 'DROP DATABASE "dvndb"' template1``

Clear Solr
++++++++++

The database is fresh and new but Solr has stale data it in. Clear it out with this command:

``curl http://localhost:8983/solr/update/json?commit=true -H "Content-type: application/json" -X POST -d "{\"delete\": { \"query\":\"*:*\"}}"``


Deleting uploaded files
+++++++++++++++++++++++

The path below will depend on the value for ``dataverse.files.directory`` as described in the :doc:`config` section:

``rm -rf /usr/local/glassfish4/glassfish/domains/domain1/files``

Rerun Installer
+++++++++++++++

With all the data cleared out, you should be ready to rerun the installer per above.

Related to all this is a series of scripts at https://github.com/IQSS/dataverse/blob/develop/scripts/deploy/phoenix.dataverse.org/deploy that Dataverse developers use have the test server http://phoenix.dataverse.org rise from the ashes before integration tests are run against it. Your mileage may vary. :)
