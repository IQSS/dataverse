Shibboleth
==========

.. contents:: :local:

Status: Experimental
--------------------

Shibboleth support in Dataverse should be considered **experimental** until the following issue is closed: https://github.com/IQSS/dataverse/issues/2117

In Dataverse 4.0, Shibboleth support requires fronting Glassfish with Apache as described below, but this configuration has not been heavily tested in a production environment and is not recommended until this issue is closed: https://github.com/IQSS/dataverse/issues/2180

System Requirements
-------------------

Only Red Hat Enterprise Linux (RHEL) 6 and derivatives such as CentOS have been tested and only on x86_64. RHEL 7 and Centos 7 **should** work but you'll need to adjust the yum repo config accordingly.

Debian and Ubuntu are not recommended until this issue is closed: https://github.com/IQSS/dataverse/issues/1059

Installation
------------

Install Apache
~~~~~~~~~~~~~~

We include ``mod_ssl`` for HTTPS support.

``yum install httpd mod_ssl``

Install Shibboleth
~~~~~~~~~~~~~~~~~~

Enable Shibboleth Yum Repo
^^^^^^^^^^^^^^^^^^^^^^^^^^

This yum repo is recommended at https://wiki.shibboleth.net/confluence/display/SHIB2/NativeSPLinuxRPMInstall

``cd /etc/yum.repos.d``

``wget http://download.opensuse.org/repositories/security:/shibboleth/CentOS_CentOS-6/security:shibboleth.repo``

Install Shibboleth Via Yum
^^^^^^^^^^^^^^^^^^^^^^^^^^

``yum install shibboleth shibboleth-embedded-ds``

Configuration
-------------

Configure Glassfish
~~~~~~~~~~~~~~~~~~~

Apply GRIZZLY-1787 Patch
^^^^^^^^^^^^^^^^^^^^^^^^

In order for the Dataverse "download as zip" feature to work well with large files without causing ``OutOfMemoryError`` problems on Glassfish 4.1, you should stop Glassfish, with ``asadmin stop-domain domain1``, make a backup of ``glassfish4/glassfish/modules/glassfish-grizzly-extra-all.jar``, replace it with a patched version of ``glassfish-grizzly-extra-all.jar`` downloaded from `here <../_static/installation/files/issues/2180/grizzly-patch/glassfish-grizzly-extra-all.jar>`_ (the md5 is in the `README <../_static/installation/files/issues/2180/grizzly-patch/readme.md>`_), and start Glassfish again with ``asadmin start-domain domain1``.

For more background on the patch, please see https://java.net/jira/browse/GRIZZLY-1787 and https://github.com/IQSS/dataverse/issues/2180 and https://github.com/payara/Payara/issues/350

This problem has been reported to Glassfish at https://java.net/projects/glassfish/lists/users/archive/2015-07/message/1 and when Glassfish 4.2 ships the Dataverse team plans to evaulate if the version of Grizzly included is new enough to include the bug fix, obviating the need for the patch.

Glassfish HTTP and HTTPS ports
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Ensure that the Glassfish HTTP service is listening on the default port (8080):

``asadmin set server-config.network-config.network-listeners.network-listener.http-listener-1.port=8080``

Ensure that the Glassfish HTTPS service is listening on the default port (8181):

``asadmin set server-config.network-config.network-listeners.network-listener.http-listener-2.port=8181``

AJP
^^^

A ``jk-connector`` network listener should have already been set up at installation time and you can verify this with ``asadmin list-network-listeners`` but for reference, here is the command that is used:

``asadmin create-network-listener --protocol http-listener-1 --listenerport 8009 --jkenabled true jk-connector``

This enables the `AJP protocol <http://en.wikipedia.org/wiki/Apache_JServ_Protocol>`_ used in Apache configuration files below.

SSLEngine Warning Workaround
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

When fronting Glassfish with Apache and using the jk-connector (AJP, mod_proxy_ajp), in your Glassfish server.log you can expect to see "WARNING ... org.glassfish.grizzly.http.server.util.RequestUtils ... jk-connector ... Unable to populate SSL attributes java.lang.IllegalStateException: SSLEngine is null".

To hide these warnings, run ``asadmin set-log-levels org.glassfish.grizzly.http.server.util.RequestUtils=SEVERE`` so that the WARNING level is hidden as recommended at https://java.net/jira/browse/GLASSFISH-20694 and https://github.com/IQSS/dataverse/issues/643#issuecomment-49654847

Configure Apache
~~~~~~~~~~~~~~~~

Enforce HTTPS
^^^^^^^^^^^^^

To prevent attacks such as `FireSheep <http://en.wikipedia.org/wiki/Firesheep>`_, HTTPS should be enforced. https://wiki.apache.org/httpd/RewriteHTTPToHTTPS provides a good method. Here is how it looks in a `sample file <../_static/installation/files/etc/httpd/conf.d/shibtest.dataverse.org.conf>`_ at ``/etc/httpd/conf.d/shibtest.dataverse.org.conf``:

.. literalinclude:: ../_static/installation/files/etc/httpd/conf.d/shibtest.dataverse.org.conf

Edit Apache Config Files
^^^^^^^^^^^^^^^^^^^^^^^^
``/etc/httpd/conf.d/ssl.conf`` should contain the FQDN of your hostname like this: ``ServerName shibtest.dataverse.org:443``

Near the bottom of ``/etc/httpd/conf.d/ssl.conf`` but before the closing ``</VirtualHost>`` directive add the following:

.. code-block:: text

    # don't pass paths used by rApache and TwoRavens to Glassfish
    ProxyPassMatch ^/RApacheInfo$ !
    ProxyPassMatch ^/custom !
    ProxyPassMatch ^/dataexplore !
    # don't pass paths used by Shibboleth to Glassfish
    ProxyPassMatch ^/Shibboleth.sso !
    ProxyPassMatch ^/shibboleth-ds !
    # pass everything else to Glassfish
    ProxyPass / ajp://localhost:8009/

    <Location /shib.xhtml>
      AuthType shibboleth
      ShibRequestSetting requireSession 1
      require valid-user
    </Location>

You can download a `sample ssl.conf file <../_static/installation/files/etc/httpd/conf.d/ssl.conf>`_.

Note that ``/etc/httpd/conf.d/shib.conf`` and ``/etc/httpd/conf.d/shibboleth-ds.conf`` are expected to be present from installing Shibboleth via yum.

Configure Shibboleth
~~~~~~~~~~~~~~~~~~~~

shibboleth2.xml
^^^^^^^^^^^^^^^

``/etc/shibboleth/shibboleth2.xml`` should look something like the `sample shibboleth2.xml file <../_static/installation/files/etc/shibboleth/shibboleth2.xml>`_ below, but you must substitute your hostname in the ``entityID`` value. If your starting point is a ``shibboleth2.xml`` file provided by someone else, you must ensure that ``attributePrefix="AJP_"`` is added under ``ApplicationDefaults`` per the `Shibboleth wiki <https://wiki.shibboleth.net/confluence/display/SHIB2/NativeSPJavaInstall>`_ . Without the ``AJP_`` configuration in place, the required :ref:`shibboleth-attributes` will be null and users will be unable to log in.

.. literalinclude:: ../_static/installation/files/etc/shibboleth/shibboleth2.xml
   :language: xml

attribute-map.xml
^^^^^^^^^^^^^^^^^

By default, some attributes ``/etc/shibboleth/attribute-map.xml`` are commented out. Edit the file to enable them.

You can download a `sample attribute-map.xml file <../_static/installation/files/etc/shibboleth/attribute-map.xml>`_.

dataverse-idp-metadata.xml
^^^^^^^^^^^^^^^^^^^^^^^^^^

The configuration above looks for the metadata for the Identity Providers (IdPs) in a file at ``/etc/shibboleth/dataverse-idp-metadata.xml``.  You can download a `sample dataverse-idp-metadata.xml file <../_static/installation/files/etc/shibboleth/dataverse-idp-metadata.xml>`_ and that includes the TestShib IdP from http://testshib.org .

Disable SELinux
~~~~~~~~~~~~~~~

You must set ``SELINUX=permisive`` in ``/etc/selinux/config`` and run ``setenforce permissive`` or otherwise disable SELinux for Shibboleth to work. "At the present time, we do not support the SP in conjunction with SELinux, and at minimum we know that communication between the mod_shib and shibd components will fail if it's enabled. Other problems may also occur." -- https://wiki.shibboleth.net/confluence/display/SHIB2/NativeSPSELinux

Restart Apache and Shibboleth
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

After configuration is complete:

``service shibd restart``

``service httpd restart``

As a sanity check, visit the following URLs for your hostname to make sure you see JSON and XML:

- https://shibtest.dataverse.org/Shibboleth.sso/DiscoFeed
- https://shibtest.dataverse.org/Shibboleth.sso/Metadata

The JSON in ``DiscoFeed`` comes from the list of IdPs in ``/etc/shibboleth/dataverse-idp-metadata.xml`` and will form a dropdown list on the Login Page.

Enable Shibboleth
~~~~~~~~~~~~~~~~~

``curl -X PUT -d true http://localhost:8080/api/admin/settings/:ShibEnabled``

After enabling Shibboleth, assuming the ``DiscoFeed`` is working per above, you should see a list of institutions to log into. You will not be able to log in via these institutions, however, until you have exchanged metadata with them.

.. _shibboleth-attributes:

Shibboleth Attributes
---------------------

The following attributes are required for a successful Shibboleth login:

- Shib-Identity-Provider
- eppn
- givenName
- sn
- email

See also https://www.incommon.org/federation/attributesummary.html and https://wiki.shibboleth.net/confluence/display/SHIB2/NativeSPAttributeAccess

For discussion of "eppn" vs. other attributes such as "eduPersonTargetedID" or "NameID", please see https://github.com/IQSS/dataverse/issues/1422

Testing
-------

http://testshib.org is a fantastic resource for testing Shibboleth configurations.

First, download your metadata like this (substituting your hostname in both places):

``curl https://shibtest.dataverse.org/Shibboleth.sso/Metadata > shibtest.dataverse.org``

Then upload it to http://testshib.org/register.html

Then try to log in.

Please note that when you try logging in via the TestShib IdP, it is expected that you'll see errors about the "mail" attribute being null. (TestShib is aware of this but it isn't a problem for testing.)

When you are done testing, you can delete the TestShib users you created like this:

``curl -X DELETE http://localhost:8080/api/admin/authenticatedUsers/myself``

Backups
-------

It's important to back up these files:

- ``/etc/shibboleth/sp-cert.pem``
- ``/etc/shibboleth/sp-key.pem``

Feedback
--------

Given that Shibboleth support is experimental and new, feedback is very welcome at support@dataverse.org or via http://community.dataverse.org/community-groups/auth.html .
