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

Solr Security
-------------

Solr must be firewalled off from all hosts except the server(s) running Dataverse. Otherwise, any host that can reach the Solr port (8983 by default) can add or delete data, search unpublished data, and even reconfigure Solr. For more information, please see https://wiki.apache.org/solr/SolrSecurity

Settings
++++++++

SolrHostColonPort
-----------------

Set ``SolrHostColonPort`` to override ``localhost:8983``.

``curl -X PUT http://localhost:8080/api/s/settings/:SolrHostColonPort/localhost:8983``

ShibEnabled
-----------

Set ``ShibEnabled`` to ``true`` to enable Shibboleth login.

``curl -X PUT http://localhost:8080/api/s/settings/:ShibEnabled/true``

DataDepositApiMaxUploadInBytes
------------------------------

Set `DataDepositApiMaxUploadInBytes` to "2147483648", for example, to limit the size of files uploaded to 2 GB.

``curl -X PUT http://localhost:8080/api/s/settings/:DataDepositApiMaxUploadInBytes/2147483648``

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


Dropbox Configuration
++++++++++++++++++++++

- Add JVM option in the domain.xml: 
``asadmin create-jvm-options "-Ddataverse.dropbox.key=<Enter your dropbox key here>"``


Shobboleth SP 
++++++++++++++++++++++
Requirements: Apache HTTPD, Apache SSL Certs from Trusted Certificate Authority, Shibboleth.

Apache HTTPD Installation and Configuration
-------------------------------------------
$ yum install -y httpd mod_ssl; service httpd start

- Download and Copy SSL Certs from Trusted Certificate Authority to:

.. code-block:: guess

	SSL Certificate File (described as "X509 Certificate only, Base64 encoded") to ``/etc/pki/tls/certs/<servername>.crt``
	Server Certificate Chain (described as "X509 Intermediates/root only, Base64 	encoded"):``/etc/pki/tls/certs/<servername>chain.crt``
	Server Private Key (SSLCertificateKeyFile) to ``/etc/pki/tls/private/<servername>.key``

- Update /etc/httpd/conf.d/**ssl.conf** resplectively with the file locations from above                        
- Update ServerName accessible through https /etc/httpd/conf.d/**ssl.conf** ``ServerName <servername>:443``                                                     
- Configure Apache with ProxyPass 

.. code-block:: guess

	cd /etc/httpd/conf.d
	wget https://raw.githubusercontent.com/IQSS/dataverse/master/conf/httpd/conf.d/dataverse.conf
	service httpd restart


Front Glassfish with Apache
-------------------------------

- Move Glassfish HTTP from port 80 to 8080
``asadmin set server-config.network-config.network-listeners.network-listener.http-listener-1.port=8080``

- Move Glassfish HTTPS from 443 to 8181
``asadmin set server-config.network-config.network-listeners.network-listener.http-listener-2.port=8181``

- Set up connector Apache and Glassfish
``asadmin create-network-listener --protocol http-listener-1 --listenerport 8009 --jkenabled true jk-connector``

$ service glassfish restart

Shibboleth Installation and Configuration
------------------------------------------

``sudo curl -k -o /etc/yum.repos.d/security:shibboleth.repo  http://download.opensuse.org/repositories/security://shibboleth/CentOS_CentOS-6/security:shibboleth.repo``
$ yum install -y shibboleth shibboleth-embedded-ds

Configure Shibboleth to authenticate against SP for TestShib IdP 
****************************************************************

- Set ShibEnabled to true to enable Shibboleth login
``curl -X PUT http://localhost:8080/api/s/settings/:ShibEnabled/true``


- Backup **shibboleth2.xml** and download shibboleth2.xml from dataverse repository
$ cd /etc/shibboleth

$ mv shibboleth2.xml shibboleth2.xml.orig

``wget https://raw.githubusercontent.com/IQSS/dataverse/master/conf/vagrant/etc/shibboleth/shibboleth2.xml``

Modify two lines in **shibboleth2.xml** to reflect your server name:

``<ApplicationDefaults entityID="https://<servername>/shibboleth"``

``<SSO discoveryProtocol="SAMLDS" discoveryURL="https://<servername>/loginpage.xhtml">``

- Backup **attribute-map.xml** and download attribute-map.xml from Dataverse repository
$ cd /etc/shibboleth/

$ mv attribute-map.xml attribute-map.xml.orig  

``wget https://raw.githubusercontent.com/IQSS/dataverse/master/conf/vagrant/etc/shibboleth/attribute-map.xml``

- Download **dataverse-idp-metadata.xml** from Dataverse repository
$ cd /etc/shibboleth/

``wget https://raw.githubusercontent.com/IQSS/dataverse/master/conf/vagrant/etc/shibboleth/dataverse-idp-metadata.xml``

- SE Linux (required if you have SE Linux enabled): 

$ setenforce permissive

$ service shibd restart

To use "Permissive" mode permanently modify /etc/selinix/config to SELINUX=permisive 

Register  with `TestShib <http://www.testshib.org/>`__ by uploading your server metadata
******************************************************************************************

- Go to https://<servername> and download your server metadata to your local machine
``wget https://<servername>/Shibboleth.sso/Metadata``

- Rename the metadata file to be exactly your server hostname ie: shibtest.dataverse.org

- Upload the file to `Testshib <http://www.testshib.org/register.html>`__.









The guide is intended for anyone who needs to install the Dataverse app.

If you encounter any problems during installation, please contact the
development team
at `support@thedata.org <mailto:support@thedata.org>`__
or our `Dataverse Users
Community <https://groups.google.com/forum/?fromgroups#!forum/dataverse-community>`__.

