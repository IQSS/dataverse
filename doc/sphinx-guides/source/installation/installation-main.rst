====================================
Installation Guide
====================================

.. _introduction:

Glassfish Configuration
+++++++++++++++++++++++

SSLEngine is null workaround
----------------------------

If you are fronting Glassfish with Apache and using the jk-connector (AJP, mod_proxy_ajp), in your Glassfish server.log you can expect to see "WARNING ... org.glassfish.grizzly.http.server.util.RequestUtils ... jk-connector ... Unable to populate SSL attributes java.lang.IllegalStateException: SSLEngine is null". 

To hide these warnings, run ``asadmin set-log-levels org.glassfish.grizzly.http.server.util.RequestUtils=SEVERE`` so that the WARNING level is hidden as recommended at https://java.net/jira/browse/GLASSFISH-20694 and https://github.com/IQSS/dataverse/issues/643#issuecomment-49654847

Solr Configuration
++++++++++++++++++

Dataverse requires a specific Solr schema file called `schema.xml` that can be found in the Dataverse distribution. It should replace the default `example/solr/collection1/conf/schema.xml` file that ships with Solr.

If ``WARN  org.eclipse.jetty.http.HttpParser  – HttpParser Full for /127.0.0.1:8983`` appears in the Solr log, adding ``<Set name="requestHeaderSize">8192</Set>`` (or a higher number of bytes) to Solr's jetty.xml in the section matching the XPath expression ``//Call[@name='addConnector']/Arg/New[@class='org.eclipse.jetty.server.bio.SocketConnector']`` may resolve the issue.  See also https://support.lucidworks.com/hc/en-us/articles/201424796-Error-when-submitting-large-query-strings-

Solr Security
-------------

Solr must be firewalled off from all hosts except the server(s) running Dataverse. Otherwise, any host that can reach the Solr port (8983 by default) can add or delete data, search unpublished data, and even reconfigure Solr. For more information, please see https://wiki.apache.org/solr/SolrSecurity

Settings
++++++++

ApplicationPrivacyPolicyUrl
---------------------------

Specify a URL where users can read your Privacy Policy.

``curl -X PUT -d http://best-practices.dataverse.org/harvard-policies/harvard-privacy-policy.html http://localhost:8080/api/admin/settings/:ApplicationPrivacyPolicyUrl``

ApiTermsOfUse
-------------

Upload a text file containing the API Terms of Use.

``curl -X PUT -d@/tmp/api-tos.txt http://localhost:8080/api/admin/settings/:ApiTermsOfUse``

SolrHostColonPort
-----------------

Set ``SolrHostColonPort`` to override ``localhost:8983``.

``curl -X PUT -d localhost:8983 http://localhost:8080/api/admin/settings/:SolrHostColonPort``

ShibEnabled
-----------

Set ``ShibEnabled`` to ``true`` to enable Shibboleth login.

``curl -X PUT -d true http://localhost:8080/api/admin/settings/:ShibEnabled``

MaxFileUploadSizeInBytes
------------------------------

Set `MaxFileUploadSizeInBytes` to "10737418240", for example, to limit the size of files uploaded to 10 GB.

``curl -X PUT -d 10737418240 http://localhost:8080/api/admin/settings/:MaxFileUploadSizeInBytes``

JVM Options
+++++++++++

dataverse.fqdn
--------------

If the Dataverse server has multiple DNS names, this option specifies the one to be used as the "official" host name. For example, you may want to have dataverse.foobar.edu, and not the less appealling server-123.socsci.foobar.edu to appear exclusively in all the registered global identifiers, Data Deposit API records, etc. 

To change the option on the command line: 

``asadmin delete-jvm-options "-Ddataverse.fqdn=old.example.com"``

``asadmin create-jvm-options "-Ddataverse.fqdn=dataverse.example.com"``

The ``dataverse.fqdn`` JVM option also affects the password reset feature.

| Do note that whenever the system needs to form a service URL, by default, it will be formed with ``https://`` and port 443. I.e., 
| ``https://{dataverse.fqdn}/``
| If that does not suit your setup, you can define an additional option - 

dataverse.siteUrl
-----------------

| and specify the alternative protocol and port number. 
| For example, configured in domain.xml:
| ``<jvm-options>-Ddataverse.fqdn=dataverse.foobar.edu</jvm-options>``
| ``<jvm-options>-Ddataverse.siteUrl=http://${dataverse.fqdn}:8080</jvm-options>``


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


Dropbox Configuration
++++++++++++++++++++++

- Add JVM option in the domain.xml: 
``asadmin create-jvm-options "-Ddataverse.dropbox.key=<Enter your dropbox key here>"``











The guide is intended for anyone who needs to install the Dataverse app.

If you encounter any problems during installation, please contact the
development team
at `support@thedata.org <mailto:support@thedata.org>`__
or our `Dataverse Users
Community <https://groups.google.com/forum/?fromgroups#!forum/dataverse-community>`__.

