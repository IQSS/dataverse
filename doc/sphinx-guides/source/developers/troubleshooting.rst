===============
Troubleshooting
===============

Over in the :doc:`dev-environment` section we described the "happy path" of when everything goes right as you set up your Dataverse Software development environment. Here are some common problems and solutions for when things go wrong.

.. contents:: |toctitle|
	:local:

context-root in glassfish-web.xml Munged by Netbeans
----------------------------------------------------

For unknown reasons, Netbeans will sometimes change the following line under ``src/main/webapp/WEB-INF/glassfish-web.xml``:

``<context-root>/</context-root>``

Sometimes Netbeans will change ``/`` to ``/dataverse``. Sometimes it will delete the line entirely. Either way, you will see very strange behavior when attempting to click around the Dataverse installation in a browser. The homepage will load but icons will be missing. Any other page will fail to load entirely and you'll see an app server error.

The solution is to put the file back to how it was before Netbeans touched it. If anyone knows of an open Netbeans bug about this, please let us know.

Configuring / Troubleshooting Mail Host
---------------------------------------

If you have trouble with the SMTP server, consider editing the ``install`` script to disable the SMTP check.

Out of the box, no emails will be sent from your development environment. This is because you have to set the ``:SystemEmail`` setting and make sure you've configured your SMTP server correctly.

You can configure ``:SystemEmail`` like this:

``curl -X PUT -d 'Davisverse SWAT Team <davisthedog@harvard.edu>' http://localhost:8080/api/admin/settings/:SystemEmail``

Unfortunately for developers not at Harvard, the installer script gives you by default an SMTP server of ``mail.hmdc.harvard.edu`` but you can specify an alternative SMTP server when you run the installer.

You can check the current SMTP server with the ``asadmin`` command:

``./asadmin get server.resources.mail-resource.mail/notifyMailSession.host``

This command helps verify what host your domain is using to send mail. Even if it's the correct hostname, you may still need to adjust settings. If all else fails, there are some free SMTP service options available such as Gmail and MailGun. This can be configured from the Payara console or the command line.

1. First, navigate to your Payara admin console: http://localhost:4848
2. From the left-side panel, select **JavaMail Sessions**
3. You should see one session named **mail/notifyMailSession** -- click on that.

From this window you can modify certain fields of your Dataverse installation's notifyMailSession, which is the JavaMail session for outgoing system email (such as on user signup or data publication). Two of the most important fields we need are:

- **Mail Host:** The DNS name of the default mail server (e.g. smtp.gmail.com)
- **Default User:** The username provided to your Mail Host when you connect to it (e.g. johndoe@gmail.com)

Most of the other defaults can safely be left as is. **Default Sender Address** indicates the address that your installation's emails are sent from.

If your user credentials for the SMTP server require a password, you'll need to configure some **Additional Properties** at the bottom.

**IMPORTANT:** Before continuing, it's highly recommended that your Default User account does NOT use a password you share with other accounts, as one of the additional properties includes entering the Default User's password (without concealing it on screen). For smtp.gmail.com you can safely use an `app password <https://support.google.com/accounts/answer/185833?hl=en>`_ or create an extra Gmail account for use with your Dataverse Software development environment.

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

Save these changes at the top of the page and restart your app server to try it out.

The mail session can also be set from command line. To use this method, you will need to delete your notifyMailSession and create a new one. See the below example:

- Delete: ``./asadmin delete-javamail-resource mail/MyMailSession``
- Create (remove brackets and replace the variables inside): ``./asadmin create-javamail-resource --mailhost [smtp.gmail.com] --mailuser [test\@test\.com] --fromaddress [test\@test\.com] --property mail.smtp.auth=[true]:mail.smtp.password=[password]:mail.smtp.port=[465]:mail.smtp.socketFactory.port=[465]:mail.smtp.socketFactory.fallback=[false]:mail.smtp.socketFactory.class=[javax.net.ssl.SSLSocketFactory] mail/notifyMailSession``

These properties can be tailored to your own preferred mail service, but if all else fails these settings work fine with Dataverse Software development environments for your localhost.

+ If you're seeing a "Relay access denied" error in your app server logs when the Dataverse installation attempts to send an email, double check your user/password credentials for the Mail Host you're using.
+ If you're seeing a "Connection refused" / similar error upon email sending, try another port.

As another example, here is how to create a Mail Host via command line for Amazon SES:

- Delete: ``./asadmin delete-javamail-resource mail/MyMailSession``
- Create (remove brackets and replace the variables inside): ``./asadmin create-javamail-resource --mailhost email-smtp.us-east-1.amazonaws.com --mailuser [test\@test\.com] --fromaddress [test\@test\.com] --transprotocol aws --transprotocolclass com.amazonaws.services.simpleemail.AWSJavaMailTransport --property mail.smtp.auth=true:mail.smtp.user=[aws_access_key]:mail.smtp.password=[aws_secret_key]:mail.transport.protocol=smtp:mail.smtp.port=587:mail.smtp.starttls.enable=true mail/notifyMailSession``

.. _rebuilding-dev-environment:

Rebuilding Your Dev Environment
-------------------------------

A script called :download:`dev-rebuild.sh <../../../../scripts/dev/dev-rebuild.sh>` is available that does the following:

- Drops the database.
- Clears our Solr.
- Deletes all data files uploaded by users (assuming you are using the default directory).
- Deploys the war file located in the ``target`` directory.
- Runs ``setup-all.sh`` in insecure mode so tests will pass.
- Runs post-install SQL statements.
- Publishes the root Dataverse collection.
- Adjusts permissions on on the root Dataverse collection so tests will pass.

To execute the script, make sure you have built a war file already and then ``cd`` to the root of the source tree and run ``scripts/dev/dev-rebuild.sh``. Feedback on this script is welcome!

DataCite
--------

If you are seeing ``Response code: 400, [url] domain of URL is not allowed`` it's probably because your ``dataverse.siteUrl`` JVM option is unset or set to localhost (``-Ddataverse.siteUrl=http://localhost:8080``). You can try something like this:

``./asadmin delete-jvm-options '-Ddataverse.siteUrl=http\://localhost\:8080'``

``./asadmin create-jvm-options '-Ddataverse.siteUrl=http\://demo.dataverse.org'``

----

Previous: :doc:`tips` | Next: :doc:`version-control`
