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

As the installer finishes, it mentions a script called ``post-install-api-block.sh`` which is **very important** to execute for any production installation of Dataverse.

Logging In
----------

Out of the box, Glassfish runs on port 8080 and 8181 rather than 80 and 443, respectively, so visiting http://localhost:8080 (substituting your hostname) should bring up a login page. See the :doc:`shibboleth` page for more on ports, but for now, let's confirm we can log in by using port 8080. Poke a temporary hole in your firewall.

Dataverse Admin Account
+++++++++++++++++++++++

Now that you've run the application installer and have your own Dataverse instance, you need to configure the Dataverse Administrator user. 
By default installer pre-sets the Admin credentials as follows:

.. code-block:: none

    First Name: Dataverse
    Last Name:  Admin
    Affiliation: Dataverse.org
    Position: Admin
    Email: dataverse@mailinator.com

Log in as the user dataverseAdmin with the password "admin" and change these values to suit your installation.

(Alteratively, you can modify the file ``dvinstall/data/user-admin.json`` in the installer bundle **before** you run the installer. The password is in ``dvinstall/setup-all.sh``, which references this JSON file.)

Congratulations! You have a working Dataverse installation. Soon you'll be tweeting at `@dataverseorg <https://twitter.com/dataverseorg>`_ asking to be added to the map at http://dataverse.org :)

Trouble? Please get in touch as explained in the :doc:`intro`.

Next you'll want to check out the :doc:`config` section.
