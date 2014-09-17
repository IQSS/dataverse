====================================
Installers Guide
====================================

.. _introduction:

**Introduction**

JVM Options
+++++++++++

dataverse.fqdn
--------------

If you need to change the hostname the Data Deposit API returns:

``asadmin delete-jvm-options "-Ddataverse.fqdn=old.example.com"``

``asadmin create-jvm-options "-Ddataverse.fqdn=dataverse.example.com"``

The ``dataverse.fqdn`` JVM option also affects the password reset feature.

dataverse.auth.password-reset-timeout-in-minutes
------------------------------------------------

Set the ``dataverse.auth.password-reset-timeout-in-minutes`` option if you'd like to override the default value put into place by the installer.

**Enforce SSL on SWORD**

- Set up connector Apache and Glassfish
``asadmin create-network-listener --protocol http-listener-1 --listenerport 8009 --jkenabled true jk-connector``

- Apache dataverse.conf

Add the following to ``/etc/httpd/conf.d/dataverse.conf``

.. code-block:: guess

  # From https://wiki.apache.org/httpd/RewriteHTTPToHTTPS
  RewriteEngine On
 
  # This will enable the Rewrite capabilities
  RewriteCond %{HTTPS} !=on
 
  # This checks to make sure the connection is not already HTTPS
  # RewriteRule ^/?(.*) https://%{SERVER_NAME}/$1 [R,L] 
  RewriteRule ^/dvn/api/data-deposit/?(.*) https://%{SERVER_NAME}/dvn/api/data-deposit/$1 [R,L]
  # This rule will redirect users from their original location, to the same location but using HTTPS.
  # i.e.  http://www.example.com/foo/ to https://www.example.com/foo/
  # The leading slash is made optional so that this will work either in httpd.conf or .htaccess context



The guide is intended for anyone who needs to install the Dataverse app.

If you encounter any problems during installation, please contact the
development team
at `support@thedata.org <mailto:support@thedata.org>`__
or our `Dataverse Users
Community <https://groups.google.com/forum/?fromgroups#!forum/dataverse-community>`__.

