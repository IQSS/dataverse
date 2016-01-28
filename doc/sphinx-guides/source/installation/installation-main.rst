============
Installation
============

Now that the :doc:`prerequisites` are in place, we are ready to execute the Dataverse installation script (the "installer") and verify that the installation was successful by logging in with a "superuser" account.

.. contents:: :local:

Running the Dataverse Installer
-------------------------------

A scripted, interactive installer is provided. This script will configure your Glassfish environment, create the database, set some required options and start the application. Some configuration tasks will still be required after you run the installer! So make sure to consult the next section. 
At this point the installer only runs on RHEL 6 and similar.

You should have already downloaded the installer from https://github.com/IQSS/dataverse/releases when setting up and starting Solr under the :doc:`prerequisites` section. Again, it's a zip file with "dvinstall" in the name.

Unpack the zip file - this will create the directory ``dvinstall``.

Execute the installer script like this::

        # cd dvinstall
        # ./install

The script will prompt you for some configuration values. If this is a test/evaluation installation, it should be safe to accept the defaults for most of the settings:

- Internet Address of your host: localhost
- Glassfish Directory: /usr/local/glassfish4
- SMTP (mail) server to relay notification messages: localhost
- Postgres Server: localhost
- Postgres Server Port: 5432
- Name of the Postgres Database: dvndb
- Name of the Postgres User: dvnapp
- Postgres user password: secret
- Rserve Server: localhost
- Rserve Server Port: 6311
- Rserve User Name: rserve
- Rserve User Password: rserve

The script is to a large degree a derivative of the old installer from DVN 3.x. It is written in Perl. If someone in the community is eager to rewrite it, perhaps in a different language, please get in touch. :)

All the Glassfish configuration tasks performed by the installer are isolated in the shell script ``dvinstall/glassfish-setup.sh`` (as ``asadmin`` commands). 

As the installer finishes, it mentions a script called ``post-install-api-block.sh`` which is **very important** to execute for any production installation of Dataverse. Security will be covered in :doc:`config` section but for now, let's make sure your installation is working.

Logging In
----------

Out of the box, Glassfish runs on port 8080 and 8181 rather than 80 and 443, respectively, so visiting http://localhost:8080 (substituting your hostname) should bring up a login page. See the :doc:`shibboleth` page for more on ports, but for now, let's confirm we can log in by using port 8080. Poke a temporary hole in your firewall.

Superuser Account
+++++++++++++++++

We'll use the superuser account created by the installer to make sure you can log into Dataverse. For more on the difference between being a superuser and having the "Admin" role, read about configuring the root dataverse in the :doc:`config` section.

(The ``dvinstall/setup-all.sh`` script, which is called by the installer sets the password for the superuser account account and the username and email address come from a file it references at ``dvinstall/data/user-admin.json``.)

Use the following credentials to log in:

- URL: http://localhost:8080
- username: dataverseAdmin
- password: admin

Congratulations! You have a working Dataverse installation. Soon you'll be tweeting at `@dataverseorg <https://twitter.com/dataverseorg>`_ asking to be added to the map at http://dataverse.org :)

(While you're logged in, you should go ahead and change the email address of the dataverseAdmin account to a real one rather than "dataverse@mailinator.com" so that you receive notifications.)

Trouble? See if you find an answer in the troubleshooting section below.

Next you'll want to check out the :doc:`config` section.

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
