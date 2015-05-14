Optional Packages
==========================



Apache Installation and Configuration
--------------------------------------
``Note: Apache is required when enforcing HTTPS``

- Install Apache HTTPD. ::

	$ yum install -y httpd mod_ssl 
	
- Configure proxy pass. ::

	/etc/httpd/conf.d
	wget https://raw.githubusercontent.com/IQSS/dataverse/master/conf/httpd/conf.d/dataverse.conf
	service httpd start (needs to have shiboleth installed otherwise start Apache will fail)
	
- Copy SSH Certs ::

	/etc/pki/tls/certs/servername.crt
	/etc/pki/tls/private/servername.key
	/etc/pki/tls/certs/servername_server-chain.crt

- Enable SSL redirect at /etc/httpd/conf.d/ssl.conf. ::

	# Specify server nane:
	ServerName servername:443
	# Specify certs location: 
	SSLCertificateFile /etc/pki/tls/certs/apitest.dataverse.org.crt
	SSLCertificateKeyFile /etc/pki/tls/private/apitest.dataverse.org.key
	SSLCertificateChainFile /etc/pki/tls/certs/apitest.dataverse.org.crt
		

Shobboleth SP Installation and Configuration
---------------------------------------------
Requirements: Apache HTTPD, Apache SSL Certs from Trusted Certificate Authority, Shibboleth.

- Install Shibboleth EPEL repository ::

	$ sudo curl -k -o /etc/yum.repos.d/security:shibboleth.repo  http://download.opensuse.org/repositories/security://shibboleth/CentOS_CentOS-6/security:shibboleth.repo
	$ yum install shibboleth shibboleth-embedded-ds
	
- Front Glassfish with Apache:
Move Glassfish HTTP from port 80 to 8080 ::
	
	$ asadmin set server-config.network-config.network-listeners.network-listener.http-listener-1.port=8080
	
Move Glassfish HTTPS from 443 to 8181::
	
	$ asadmin set server-config.network-config.network-listeners.network-listener.http-listener-2.port=8181

Set up connector Apache and Glassfish ::

	$ asadmin create-network-listener --protocol http-listener-1 --listenerport 8009 --jkenabled true jk-connector
	$ service glassfish restart

- Configure Shiboleth to authenticate against SP for TestShib IdP:
Backup **shibboleth2.xml** and download shibboleth2.xml from dataverse repository ::

	$ cd /etc/shibboleth/
	$ wget https://raw.githubusercontent.com/IQSS/dataverse/master/conf/vagrant/etc/shibboleth/shibboleth2.xml

Modify two lines in *shibboleth2.xml* to reflect your server name:

``<ApplicationDefaults entityID="https://<servername>/shibboleth"``
``<SSO discoveryProtocol="SAMLDS" discoveryURL="https://<servername>/loginpage.xhtml">``

Backup **attribute-map.xml** and download attribute-map.xml from `Dataverse Repository <https://github.com/IQSS/dataverse>`__::

	$ cd /etc/shibboleth/
	$ mv attribute-map.xml attribute-map.xml.orig
	$ wget https://raw.githubusercontent.com/IQSS/dataverse/master/conf/vagrant/etc/shibboleth/attribute-map.xml
	
Download **dataverse-idp-metadata.xml** from `Dataverse Repository <https://github.com/IQSS/dataverse>`__::

	$ cd /etc/shibboleth/
	$ wget https://raw.githubusercontent.com/IQSS/dataverse/master/conf/vagrant/etc/shibboleth/dataverse-idp-metadata.xml

- SE Linux (required if you have SE Linux enabled::

	$ setenforce permissive
	$ service shibd restart
To use “Permissive” mode permanently modify /etc/selinix/config to ``SELINUX=permisive``

- Enable Shibboleth login:
Set ShibEnabled to true after Dataverse intallation::

	$ curl -X PUT -d true http://localhost:8080/api/admin/settings/:ShibEnabled
	
	
- Register with `TestShib <http://www.testshib.org/>`__

Go to ``https://<servername>`` and download your server metadata to your local machine. ::

	$ wget https://<servername>/Shibboleth.sso/Metadata
Rename the metadata file to be exactly your server hostname ``ie: shibtest.dataverse.org`` and Upload the file to Testshib.

