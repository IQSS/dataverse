Shibboleth
==========

.. contents:: |toctitle|
  :local:
  
Introduction
------------

By configuring and enabling Shibboleth support in Dataverse, your users will be able to log in using the identity system managed by their institution ("single sign on", or at least "single password") rather than having to create yet another password local to your Dataverse installation. Typically, users know their login system by some sort of internal branding such as "HarvardKey" or "Touchstone" (MIT) but within the Dataverse application, the Shibboleth feature is known as :ref:`institutional-log-in` as explained to end users in the :doc:`/user/account` section of the User Guide.

Shibboleth is an implementation of the `Security Assertion Markup Language (SAML) <https://en.wikipedia.org/wiki/Security_Assertion_Markup_Language>`_ protocol which is similar in spirit to systems used by many webapps that allow you to log in via Google, Facebook, or Twitter.

Shibboleth can be compared and contrasted with OAuth2, which you can read about in the :doc:`oauth2` section.

Installation
------------

We assume you've already gone through a basic installation as described in the :doc:`/installation/installation-main` section and that you've paid particular attention to the :ref:`auth-modes` explanation in the :doc:`/installation/config` section. You're going to give Shibboleth a whirl. Let's get started.

System Requirements
~~~~~~~~~~~~~~~~~~~

Support for Shibboleth in Dataverse is built on the popular `"mod_shib" Apache module, "shibd" daemon <https://shibboleth.net/products/service-provider.html>`_, and the `Embedded Discovery Service (EDS) <https://shibboleth.net/products/embedded-discovery-service.html>`_ Javascript library, all of which are distributed by the `Shibboleth Consortium <https://shibboleth.net>`_. EDS is bundled with Dataverse, but ``mod_shib`` and ``shibd`` must be installed and configured per below.

Only Red Hat Enterprise Linux (RHEL) and derivatives such as CentOS have been tested (x86_64 versions) by the Dataverse team. See https://wiki.shibboleth.net/confluence/display/SHIB2/NativeSPLinuxInstall for details and note that (according to that page) as of this writing Ubuntu and Debian are not offically supported by the Shibboleth project.

Install Apache
~~~~~~~~~~~~~~

We will be "fronting" the app server with Apache so that we can make use of the ``mod_shib`` Apache module. We will also make use of the ``mod_proxy_ajp`` module built in to Apache.

We include the ``mod_ssl`` package to enforce HTTPS per below.

``yum install httpd mod_ssl``

Install Shibboleth
~~~~~~~~~~~~~~~~~~

Installing Shibboleth will give us both the ``shibd`` service and the ``mod_shib`` Apache module.

Enable Shibboleth Yum Repo
^^^^^^^^^^^^^^^^^^^^^^^^^^

This yum repo is recommended at https://wiki.shibboleth.net/confluence/display/SHIB2/NativeSPLinuxRPMInstall

``cd /etc/yum.repos.d``

Install ``wget`` if you don't have it already:

``yum install wget``

If you are running el8 (RHEL/CentOS 8):

``wget http://download.opensuse.org/repositories/security:/shibboleth/CentOS_8/security:shibboleth.repo``

If you are running el7 (RHEL/CentOS 7):

``wget http://download.opensuse.org/repositories/security:/shibboleth/CentOS_7/security:shibboleth.repo``

If you are running el6 (RHEL/CentOS 6):

``wget http://download.opensuse.org/repositories/security:/shibboleth/CentOS_CentOS-6/security:shibboleth.repo``

Install Shibboleth Via Yum
^^^^^^^^^^^^^^^^^^^^^^^^^^

Please note that during the installation it's ok to import GPG keys from the Shibboleth project. We trust them.

``yum install shibboleth``

Configure Payara
----------------

App Server HTTP and HTTPS ports
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Apache will be listening on ports 80 and 443 so we need to make sure the app server isn't using them. If you've been changing the default ports used by the app server per the :doc:`config` section, revert the HTTP service to listen on 8080, the default port:

``./asadmin set server-config.network-config.network-listeners.network-listener.http-listener-1.port=8080``

Likewise, if necessary, revert the HTTPS service to listen on port 8181:

``./asadmin set server-config.network-config.network-listeners.network-listener.http-listener-2.port=8181``

AJP
~~~

A ``jk-connector`` network listener should have already been set up when you ran the installer mentioned in the :doc:`installation-main` section, but for reference, here is the command that is used:

``./asadmin create-network-listener --protocol http-listener-1 --listenerport 8009 --jkenabled true jk-connector``

You can verify this with ``./asadmin list-network-listeners``. 

This enables the `AJP protocol <http://en.wikipedia.org/wiki/Apache_JServ_Protocol>`_ used in Apache configuration files below.

SSLEngine Warning Workaround
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This workaround was required for Glassfish 4 but it is unknown if it is required under Payara.

When fronting Payara with Apache and using the jk-connector (AJP, mod_proxy_ajp), in your Payara server.log you can expect to see "WARNING ... org.glassfish.grizzly.http.server.util.RequestUtils ... jk-connector ... Unable to populate SSL attributes java.lang.IllegalStateException: SSLEngine is null".

To hide these warnings, run ``./asadmin set-log-levels org.glassfish.grizzly.http.server.util.RequestUtils=SEVERE`` so that the WARNING level is hidden as recommended at https://java.net/jira/browse/GLASSFISH-20694 and https://github.com/IQSS/dataverse/issues/643#issuecomment-49654847

Configure Apache
----------------

Enforce HTTPS
~~~~~~~~~~~~~

To prevent attacks such as `FireSheep <http://en.wikipedia.org/wiki/Firesheep>`_, HTTPS should be enforced. https://wiki.apache.org/httpd/RewriteHTTPToHTTPS provides a good method. You **could** copy and paste that those "rewrite rule" lines into Apache's main config file at ``/etc/httpd/conf/httpd.conf`` but using Apache's "virtual hosts" feature is recommended so that you can leave the main configuration file alone and drop a host-specific file into place.

Below is an example of how "rewrite rule" lines look within a ``VirtualHost`` block. Download a :download:`sample file <../_static/installation/files/etc/httpd/conf.d/dataverse.example.edu.conf>` , edit it to substitute your own hostname under ``ServerName``, and place it at ``/etc/httpd/conf.d/dataverse.example.edu.conf`` or a filename that matches your hostname. The file must be in ``/etc/httpd/conf.d`` and must end in ".conf" to be included in Apache's configuration.

.. literalinclude:: ../_static/installation/files/etc/httpd/conf.d/dataverse.example.edu.conf

Edit Apache ssl.conf File
~~~~~~~~~~~~~~~~~~~~~~~~~

``/etc/httpd/conf.d/ssl.conf`` should be edited to contain the FQDN of your hostname like this: ``ServerName dataverse.example.edu:443`` (substituting your hostname).

Near the bottom of ``/etc/httpd/conf.d/ssl.conf`` but before the closing ``</VirtualHost>`` directive, add the following:

.. code-block:: text

    # don't pass paths used by rApache and TwoRavens to Payara
    ProxyPassMatch ^/RApacheInfo$ !
    ProxyPassMatch ^/custom !
    ProxyPassMatch ^/dataexplore !
    # don't pass paths used by Shibboleth to Payara
    ProxyPassMatch ^/Shibboleth.sso !
    ProxyPassMatch ^/shibboleth-ds !
    # pass everything else to Payara
    ProxyPass / ajp://localhost:8009/

    <Location /shib.xhtml>
      AuthType shibboleth
      ShibRequestSetting requireSession 1
      require valid-user
    </Location>

You can download a :download:`sample ssl.conf file <../_static/installation/files/etc/httpd/conf.d/ssl.conf>` to compare it against the file you edited.

Note that ``/etc/httpd/conf.d/shib.conf`` and ``/etc/httpd/conf.d/shibboleth-ds.conf`` are expected to be present from installing Shibboleth via yum.

You may wish to also add a timeout directive to the ProxyPass line within ssl.conf. This is especially useful for larger file uploads as apache may prematurely kill the connection before the upload is processed. 

e.g. ``ProxyPass / ajp://localhost:8009/ timeout=600`` defines a timeout of 600 seconds. 

Try to strike a balance with the timeout setting. Again a timeout too low will impact file uploads. A timeout too high may cause additional stress on the server as it will have to service idle clients for a longer period of time.

Configure Shibboleth
--------------------

shibboleth2.xml
~~~~~~~~~~~~~~~

``/etc/shibboleth/shibboleth2.xml`` should look something like the :download:`sample shibboleth2.xml file <../_static/installation/files/etc/shibboleth/shibboleth2.xml>` below, but you must substitute your hostname in the ``entityID`` value. If your starting point is a ``shibboleth2.xml`` file provided by someone else, you must ensure that ``attributePrefix="AJP_"`` is added under ``ApplicationDefaults`` per the `Shibboleth wiki <https://wiki.shibboleth.net/confluence/display/SHIB2/NativeSPJavaInstall>`_ . Without the ``AJP_`` configuration in place, the required :ref:`shibboleth-attributes` will be null and users will be unable to log in.

.. literalinclude:: ../_static/installation/files/etc/shibboleth/shibboleth2.xml
   :language: xml

Specific Identity Provider(s)
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

When configuring the ``MetadataProvider`` section of ``shibboleth2.xml`` you should consider if your users will all come from the same Identity Provider (IdP) or not.

Most Dataverse installations will probably only want to authenticate users via Shibboleth using their home institution's Identity Provider (IdP).  The configuration above in ``shibboleth2.xml`` looks for the metadata for the Identity Providers (IdPs) in a file at ``/etc/shibboleth/dataverse-idp-metadata.xml``.  You can download a :download:`sample dataverse-idp-metadata.xml file <../_static/installation/files/etc/shibboleth/dataverse-idp-metadata.xml>` and that includes the SAMLtest IdP from https://samltest.id but you will want to edit this file to include the metadata from the Identity Provider you care about. The identity people at your institution will be able to provide you with this metadata and they will very likely ask for a list of attributes that Dataverse requires, which are listed at :ref:`shibboleth-attributes`.

Identity Federation
^^^^^^^^^^^^^^^^^^^

Rather than or in addition to specifying individual Identity Provider(s) you may wish to broaden the number of users who can log into your Dataverse installation by registering your Dataverse installation as a Service Provider (SP) within an identity federation. For example, in the United States, users from the `many institutions registered with the "InCommon" identity federation <https://incommon.org/federation/info/all-entities.html#IdPs>`_ that release the `"Research & Scholarship Attribute Bundle" <https://spaces.internet2.edu/display/InCFederation/Research+and+Scholarship+Attribute+Bundle>`_  will be able to log into your Dataverse installation if you register it as an `InCommon Service Provider <https://incommon.org/federation/info/all-entities.html#SPs>`_ that is part of the `Research & Scholarship (R&S) category <https://incommon.org/federation/info/all-entity-categories.html#SPs>`_.

The details of how to register with an identity federation are out of scope for this document, but a good starting point may be this list of identity federations across the world: http://www.protectnetwork.org/support/faq/identity-federations

One of the benefits of using ``shibd`` is that it can be configured to periodically poll your identity federation for updates as new Identity Providers (IdPs) join the federation you've registered with. For the InCommon federation, the following page describes how to download and verify signed InCommon metadata every hour: https://spaces.internet2.edu/display/InCFederation/Shibboleth+Metadata+Config#ShibbolethMetadataConfig-ConfiguretheShibbolethSP . You can also see an example of this as ``maxRefreshDelay="3600"`` in the commented out section of the ``shibboleth2.xml`` file above.

Once you've joined a federation the list of IdPs in the dropdown can be quite long! If you're curious how many are in the list you could try something like this: ``curl https://dataverse.example.edu/Shibboleth.sso/DiscoFeed | jq '.[].entityID' | wc -l``

.. _shibboleth-attributes:

Shibboleth Attributes
~~~~~~~~~~~~~~~~~~~~~

The following attributes are required for a successful Shibboleth login:

- Shib-Identity-Provider
- eppn
- givenName
- sn
- email

See also https://www.incommon.org/federation/attributesummary.html and https://wiki.shibboleth.net/confluence/display/SHIB2/NativeSPAttributeAccess

attribute-map.xml
~~~~~~~~~~~~~~~~~

By default, some attributes ``/etc/shibboleth/attribute-map.xml`` are commented out and "subject-id" is used instead of "eppn". We recommend downloading and using :download:`attribute-map.xml<../_static/installation/files/etc/shibboleth/attribute-map.xml>` instead which has these changes and should be compatible with Dataverse.

Shibboleth and ADFS
~~~~~~~~~~~~~~~~~~~
With appropriate configuration, Dataverse and Shibboleth can make use of "single sign on" using Active Directory.
This requires configuring ``shibd`` and ``httpd`` to load appropriate libraries, and insuring that the attribute mapping matches those provided.
Example configuration files for :download:`shibboleth2.xml <../_static/installation/files/etc/shibboleth/shibboleth2_adfs.xml>` and :download:`attribute-map.xml <../_static/installation/files/etc/shibboleth/attribute-map_adfs.xml>` may be helpful.
Note that your ADFS server hostname goes in the file referenced under "MetadataProvider" in your shibboleth2.xml file.

Disable or Reconfigure SELinux
------------------------------

SELinux is set to "enforcing" by default on RHEL/CentOS, but unfortunately Shibboleth does not "just work" with SELinux. You have two options. You can disable SELinux or you can reconfigure SELinux to accommodate Shibboleth.

Disable SELinux
~~~~~~~~~~~~~~~

The first and easiest option is to set ``SELINUX=permisive`` in ``/etc/selinux/config`` and run ``setenforce permissive`` or otherwise disable SELinux to get Shibboleth to work. This is apparently what the Shibboleth project expects because their wiki page at https://wiki.shibboleth.net/confluence/display/SHIB2/NativeSPSELinux says, "At the present time, we do not support the SP in conjunction with SELinux, and at minimum we know that communication between the mod_shib and shibd components will fail if it's enabled. Other problems may also occur."

Reconfigure SELinux to Accommodate Shibboleth
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The second (more involved) option is to use the ``checkmodule``, ``semodule_package``, and ``semodule`` tools to apply a local policy to make Shibboleth work with SELinux. Let's get started.

Put Type Enforcement (TE) File in misc directory
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Copy and paste or download the :download:`shibboleth.te <../_static/installation/files/etc/selinux/targeted/src/policy/domains/misc/shibboleth.te>` Type Enforcement (TE) file below and put it at ``/etc/selinux/targeted/src/policy/domains/misc/shibboleth.te``.

.. literalinclude:: ../_static/installation/files/etc/selinux/targeted/src/policy/domains/misc/shibboleth.te
   :language: text

(If you would like to know where the ``shibboleth.te`` came from and how to hack on it, please see the :doc:`/developers/selinux` section of the Developer Guide. Pull requests are welcome!)

Navigate to misc directory
^^^^^^^^^^^^^^^^^^^^^^^^^^

``cd /etc/selinux/targeted/src/policy/domains/misc``

Run checkmodule
^^^^^^^^^^^^^^^

``checkmodule -M -m -o shibboleth.mod shibboleth.te``

Run semodule_package
^^^^^^^^^^^^^^^^^^^^

``semodule_package -o shibboleth.pp -m shibboleth.mod``

Silent is golden. No output is expected.

Run semodule
^^^^^^^^^^^^

``semodule -i shibboleth.pp``

Silent is golden. No output is expected. This will place a file in ``/etc/selinux/targeted/modules/active/modules/shibboleth.pp`` and include "shibboleth" in the output of ``semodule -l``. See the ``semodule`` man page if you ever want to remove or disable the module you just added.

Congrats! You've made the creator of http://stopdisablingselinux.com proud. :)

Restart Apache and Shibboleth
-----------------------------

After configuration is complete, restart ``shib`` and ``httpd``.

On CentOS 7:

``systemctl restart shibd.service``

``systemctl restart httpd.service``

On CentOS 6:

``service shibd restart``

``service httpd restart``

Configure Apache and shibd to Start at Boot
-------------------------------------------

On CentOS 7/8:

``systemctl enable httpd.service``

``systemctl enable shibd.service``

On CentOS 6:

``chkconfig httpd on``

``chkconfig shibd on``

Verify DiscoFeed and Metadata URLs
----------------------------------

As a sanity check, visit the following URLs (substituting your hostname) to make sure you see JSON and XML:

- https://dataverse.example.edu/Shibboleth.sso/DiscoFeed
- https://dataverse.example.edu/Shibboleth.sso/Metadata

The JSON in ``DiscoFeed`` comes from the list of IdPs you configured in the ``MetadataProvider`` section of ``shibboleth2.xml`` and will form a dropdown list on the Login Page.

Add the Shibboleth Authentication Provider to Dataverse
-------------------------------------------------------

Now that you've configured your app server, Apache, and ``shibd``, you are ready to turn your attention back to Dataverse to enable Shibboleth as an "authentication provider." You will be using ``curl`` to POST the `following JSON file <../_static/installation/files/etc/shibboleth/shibAuthProvider.json>`_ to the ``authenticationProviders`` endpoint of the :doc:`/api/native-api`.

.. literalinclude:: ../_static/installation/files/etc/shibboleth/shibAuthProvider.json
   :language: json

``curl -X POST -H 'Content-type: application/json' --upload-file shibAuthProvider.json http://localhost:8080/api/admin/authenticationProviders``

Now that you've added the Shibboleth authentication provider to Dataverse, as described in the :doc:`/user/account` section of the User Guide, you should see a new "Your Institution" button under "Other Log In Options" on the Log In page. After clicking "Your Institution", you should see the institutions you configured in ``/etc/shibboleth/shibboleth2.xml`` above. If not, double check the content of the ``DiscoFeed`` URL above. If you don't see the "Your Institution" button, confirm that the the "shib" authentication provider has been added by listing all the authentication providers Dataverse knows about:

``curl http://localhost:8080/api/admin/authenticationProviders``

Once you have confirmed that the Dataverse web interface is listing the institutions you expect, you'll want to temporarily remove the Shibboleth authentication provider you just added because users won't be able to log in via their institution until you have exchanged metadata with one or more Identity Providers (IdPs), which is described below.  As explained in the section of the :doc:`/api/native-api` of the API Guide, you can delete an authentication provider by passing its ``id``:

``curl -X DELETE http://localhost:8080/api/admin/authenticationProviders/shib``

Before contacting your actual Identity Provider, we recommend testing first with the "SAMLtest" Identity Provider (IdP) to ensure that you have configured everything correctly. This process is described next.

Exchange Metadata with Your Identity Provider
---------------------------------------------

https://samltest.id (SAMLtest) is a fantastic resource for testing Shibboleth configurations. Depending on your relationship with your identity people you may want to avoid bothering them until you have tested your Dataverse configuration with the SAMLtest Identity Provider (IdP). This process is explained below.

If you've temporarily configured your ``MetadataProvider`` to use the SAMLtest Identity Provider (IdP) as outlined above, you can download your metadata like this (substituting your hostname in both places):

``curl https://dataverse.example.edu/Shibboleth.sso/Metadata > dataverse.example.edu``

Then upload your metadata to https://samltest.id/upload.php (or click "Fetch").

Then try to log in to Dataverse using the SAMLtest IdP. After logging in, you can visit the https://dataverse.example.edu/Shibboleth.sso/Session (substituting your hostname) to troubleshoot which attributes are being received. You should see something like the following:

.. code-block:: none

    Miscellaneous
    Session Expiration (barring inactivity): 479 minute(s)
    Client Address: 75.69.182.6
    SSO Protocol: urn:oasis:names:tc:SAML:2.0:protocol
    Identity Provider: https://samltest.id/saml/idp
    Authentication Time: 2019-11-28T01:23:28.381Z
    Authentication Context Class: urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport
    Authentication Context Decl: (none)
    
    Attributes
    displayName: Rick Sanchez
    eppn: rsanchez@samltest.id
    givenName: Rick
    mail: rsanchez@samltest.id
    sn: Sanchez
    telephoneNumber: +1-555-555-5515
    uid: rick

When you are done testing, you can delete the SAMLtest users you created like this (after you have deleted any data and permisions associated with the users):

``curl -X DELETE http://localhost:8080/api/admin/authenticatedUsers/rick``

(Of course, you are also welcome to do a fresh reinstall per the :doc:`installation-main` section.)

If your Dataverse installation is working with SAMLtest it **should** work with your institution's Identity Provider (IdP). Next, you should:

- Send your identity people your metadata file above (or a link to download it themselves). From their perspective you are a Service Provider (SP).
- Ask your identity people to send you the metadata for the Identity Provider (IdP) they operate. See the section above on ``shibboleth2.xml`` and ``MetadataProvider`` for what to do with the IdP metadata. Restart ``shibd`` and ``httpd`` as necessary.
- Re-add Shibboleth as an authentication provider to Dataverse as described above.
- Test login to Dataverse via your institution's Identity Provider (IdP).

Backup sp-cert.pem and sp-key.pem Files
---------------------------------------

Especially if you have gotten authentication working with your institution's Identity Provider (IdP), now is the time to make sure you have backups.

The installation and configuration of Shibboleth will result in the following cert and key files being created and it's important to back them up. The cert is in the metadata you shared with your IdP:

- ``/etc/shibboleth/sp-cert.pem``
- ``/etc/shibboleth/sp-key.pem``

If you have more than one Payara server, you should use the same ``sp-cert.pem`` and ``sp-key.pem`` files on all of them. If these files are compromised and you need to regenerate them, you can ``cd /etc/shibboleth`` and run ``keygen.sh`` like this (substituting you own hostname):

``./keygen.sh -f -u shibd -g shibd -h dataverse.example.edu -e https://dataverse.example.edu/sp``

Debugging
---------

The :doc:`/admin/troubleshooting` section of the Admin Guide explains how to increase Payara logging levels. The relevant classes and packages are:

- edu.harvard.iq.dataverse.Shib
- edu.harvard.iq.dataverse.authorization.providers.shib
- edu.harvard.iq.dataverse.authorization.groups.impl.shib

Converting Accounts
-------------------

As explained in the :doc:`/user/account` section of the User Guide, users can convert from one login option to another.

Converting Local Users to Shibboleth
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

If you are running in "remote and local" mode and have existing local users that you'd like to convert to Shibboleth users, give them the following steps to follow, which are also explained in the :doc:`/user/account` section of the User Guide:

- Log in with your local account to make sure you know your password, which will be needed for the account conversion process.
- Log out of your local account.
- Log in with your Shibboleth account.
- If the email address associated with your local account matches the email address asserted by the Identity Provider (IdP), you will be prompted for the password of your local account and asked to confirm the conversion of your account. You're done! Browse around to ensure you see all the data you expect to see. Permissions have been preserved. 
- If the email address asserted by the Identity Provider (IdP) does not match the email address of any local user, you will be prompted to create a new account. If you were expecting account conversion, you should decline creating a new Shibboleth account, log back in to your local account, and let Support know the email on file for your local account. Support may ask you to change your email address for your local account to the one that is being asserted by the Identity Provider. Someone with access to the Payara logs will see this email address there.

.. _converting-shibboleth-users-to-local:

Converting Shibboleth Users to Local
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Whereas users convert their own accounts from local to Shibboleth as described above, conversion in the opposite direction is performed by a sysadmin. A common scenario may be as follows:

- A user emails Support saying, "I left the university (or wherever) and can't log in to Dataverse anymore. What should I do?"
- Support replies asking the user for a new email address (Gmail, new institution email, etc.) to associate with their Dataverse account.
- The user replies with a new email address to associate with their Dataverse account.
- Support runs the curl command below, supplying the database id of the user to convert and the new email address and notes the username returned.
- Support emails the user and indicates that that they should use the password reset feature to set a new password and to make sure to take note of their username under Account Information (or the password reset confirmation email) since the user never had a username before.
- The user resets password and is able to log in with their local account. All permissions have been preserved with the exception of any permissions assigned to an institution-wide Shibboleth group to which the user formerly belonged.

In the example below, the user has indicated that the new email address they'd like to have associated with their account is "former.shib.user@mailinator.com" and their user id from the ``authenticateduser`` database table is "2". The API token must belong to a superuser (probably the sysadmin executing the command). Note that the old version of this call, `convertShibToBuiltIn`, is deprecated and will be deleted in a future release.

``curl -H "X-Dataverse-key: $API_TOKEN" -X PUT -d "former.shib.user@mailinator.com" http://localhost:8080/api/admin/authenticatedUsers/id/2/convertRemoteToBuiltIn``

Rather than looking up the user's id in the ``authenticateduser`` database table, you can issue this command to get a listing of all users:

``curl -H "X-Dataverse-key: $API_TOKEN" http://localhost:8080/api/admin/authenticatedUsers``

Per above, you now need to tell the user to use the password reset feature to set a password for their local account.

Institution-Wide Shibboleth Groups
----------------------------------

Dataverse allows you to optionally define "institution-wide Shibboleth groups" based on the the entityID of the Identity Provider (IdP) used to authenticate. For example, an "institution-wide Shibboleth group" with ``https://samltest.id/saml/idp`` as the IdP would include everyone who logs in via the SAMLtest IdP mentioned above.

To create an institution-wide Shibboleth groups, create a JSON file like :download:`shibGroupSAMLtest.json<../_static/installation/files/etc/shibboleth/shibGroupSAMLtest.json>` as below and issue this curl command:

``curl http://localhost:8080/api/admin/groups/shib -X POST -H 'Content-type:application/json' --upload-file shibGroupSAMLtest.json``

.. literalinclude:: ../_static/installation/files/etc/shibboleth/shibGroupSAMLtest.json

Institution-wide Shibboleth groups are based on the "Shib-Identity-Provider" SAML attribute asserted at runtime after successful authentication with the Identity Provider (IdP) and held within the browser session rather than being persisted in the database for any length of time. It is for this reason that roles based on these groups, such as the ability to create a dataset, are not honored by non-browser interactions, such as through the SWORD API. 

To list institution-wide Shibboleth groups: ``curl http://localhost:8080/api/admin/groups/shib``

To delete an institution-wide Shibboleth group (assuming id 1): ``curl -X DELETE http://localhost:8080/api/admin/groups/shib/1``

Support for arbitrary attributes beyond "Shib-Identity-Provider" such as "eduPersonScopedAffiliation", etc. is being tracked at https://github.com/IQSS/dataverse/issues/1515
