===============
Troubleshooting
===============

.. contents:: |toctitle|
	:local:

Troubleshooting Your Dev Environment
------------------------------------

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

----

Previous: :doc:`dev-environment` | Next: :doc:`version-control`
