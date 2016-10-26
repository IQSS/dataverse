Shibboleth
==========

.. contents:: :local:

Introduction
------------

By configuring and enabling Shibboleth support in Dataverse, your users will be able to log in using the identity system managed by their institution ("single sign on", or at least "single password") rather than having to create yet another password local to your Dataverse installation. Typically, users know their login system by some sort of internal branding such as "HarvardKey" or "Touchstone" (MIT) but within the Dataverse application, the Shibboleth feature is known as "Institutional Log In" as explained to end users in the :doc:`/user/account` section of the User Guide.

Shibboleth is an implementation of the `Security Assertion Markup Language (SAML) <https://en.wikipedia.org/wiki/Security_Assertion_Markup_Language>`_ protocol which is similar in spirit to systems used by many webapps that allow you to log in via Google, Facebook, or Twitter.

Auth Modes: Local vs. Remote vs. Both
-------------------------------------

There are three valid configurations or modes for authenticating users to Dataverse:

- Local only.
- Both local and remote (Shibboleth).
- Remote (Shibboleth) only.

Out of the box, Dataverse is configured in "local only" mode. The "dataverseAdmin" superuser account mentioned in the :doc:`/installation/installation-main` section is an example of a local account.

Enabling Shibboleth support results in a second login screen appearing next to the regular login screen. This is "both local and remote" mode. This mode is especially useful if you have external collaborators or if you want to let users who have left your institution to continue to log into your installation of Dataverse. See also the section below about converting users from Shibboleth users to local users when they have left your institution.

"Remote only" mode means that Shibboleth has been enabled and that ``:AllowSignUp`` is set to "false" per the :doc:`config` section to prevent users from creating local accounts via the web interface. Please note that local accounts can also be created via API, and the way to prevent this is to block the ``builtin-users`` endpoint or scramble (or remove) the ``BuiltinUsers.KEY`` database setting per the :doc:`config` section. The fact that preventing local users from being created does not hide the login screen for local users is being discussed at https://github.com/IQSS/dataverse/issues/2974

Installation
------------

We assume you've already gone through a basic installation as described in the :doc:`/installation/installation-main` section.

System Requirements
~~~~~~~~~~~~~~~~~~~

Support for Shibboleth in Dataverse is built on the popular `"mod_shib" Apache module, "shibd" daemon <https://shibboleth.net/products/service-provider.html>`_, and the `Embedded Discovery Service (EDS) <https://shibboleth.net/products/embedded-discovery-service.html>`_ Javascript library, all of which are distributed by the `Shibboleth Consortium <https://shibboleth.net>`_. EDS is bundled with Dataverse, but ``mod_shib`` and ``shibd`` must be installed and configured per below.

Only Red Hat Enterprise Linux (RHEL) 6 and derivatives such as CentOS have been tested (x86_64 versions) by the Dataverse team. Newer versions of RHEL and CentOS **should** work but you'll need to adjust the yum repo config accordingly. See https://wiki.shibboleth.net/confluence/display/SHIB2/NativeSPLinuxInstall for details and note that (according to that page) as of this writing Ubuntu and Debian are not offically supported by the Shibboleth project.

Install Apache
~~~~~~~~~~~~~~

We will be "fronting" Glassfish with Apache so that we can make use of the ``mod_shib`` Apache module. We will also make use of the ``mod_proxy_ajp`` module built in to Apache.

We include the ``mod_ssl`` package to enforce HTTPS per below.

``yum install httpd mod_ssl``

Install Shibboleth
~~~~~~~~~~~~~~~~~~

Installing Shibboleth will give us both the ``shibd`` service and the ``mod_shib`` Apache module.

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

In order for the Dataverse "download as zip" feature to work well with large files without causing ``OutOfMemoryError`` problems on Glassfish 4.1 when fronted with Apache, you should stop Glassfish, with ``asadmin stop-domain domain1``, make a backup of ``glassfish4/glassfish/modules/glassfish-grizzly-extra-all.jar``, replace it with a patched version of ``glassfish-grizzly-extra-all.jar`` downloaded from `here <../_static/installation/files/issues/2180/grizzly-patch/glassfish-grizzly-extra-all.jar>`_ (the md5 is in the `README <../_static/installation/files/issues/2180/grizzly-patch/readme.md>`_), and start Glassfish again with ``asadmin start-domain domain1``.

For more background on the patch, please see https://java.net/jira/browse/GRIZZLY-1787 and https://github.com/IQSS/dataverse/issues/2180 and https://github.com/payara/Payara/issues/350

This problem has been reported to Glassfish at https://java.net/projects/glassfish/lists/users/archive/2015-07/message/1 and while Glassfish 4.1.1 includes a new enough version of Grizzly to fix the bug, other complicating factors prevent its adoption (look for "Glassfish 4.1.1" in the :doc:`prerequisites` section for details on why it is not recommended).

Glassfish HTTP and HTTPS ports
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Apache will be listening on ports 80 and 443 so we need to make sure Glassfish isn't using them. If you've been changing the default ports used by Glassfish per the :doc:`config` section, revert the Glassfish HTTP service to listen on 8080, the default port:

``asadmin set server-config.network-config.network-listeners.network-listener.http-listener-1.port=8080``

Likewise, if necessary, revert the Glassfish HTTPS service to listen on port 8181:

``asadmin set server-config.network-config.network-listeners.network-listener.http-listener-2.port=8181``

AJP
^^^

A ``jk-connector`` network listener should have already been set up when you ran the installer mentioned in the :doc:`installation-main` section, but for reference, here is the command that is used:

``asadmin create-network-listener --protocol http-listener-1 --listenerport 8009 --jkenabled true jk-connector``

You can verify this with ``asadmin list-network-listeners``. 

This enables the `AJP protocol <http://en.wikipedia.org/wiki/Apache_JServ_Protocol>`_ used in Apache configuration files below.

SSLEngine Warning Workaround
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

When fronting Glassfish with Apache and using the jk-connector (AJP, mod_proxy_ajp), in your Glassfish server.log you can expect to see "WARNING ... org.glassfish.grizzly.http.server.util.RequestUtils ... jk-connector ... Unable to populate SSL attributes java.lang.IllegalStateException: SSLEngine is null".

To hide these warnings, run ``asadmin set-log-levels org.glassfish.grizzly.http.server.util.RequestUtils=SEVERE`` so that the WARNING level is hidden as recommended at https://java.net/jira/browse/GLASSFISH-20694 and https://github.com/IQSS/dataverse/issues/643#issuecomment-49654847

Configure Apache
~~~~~~~~~~~~~~~~

Enforce HTTPS
^^^^^^^^^^^^^

To prevent attacks such as `FireSheep <http://en.wikipedia.org/wiki/Firesheep>`_, HTTPS should be enforced. https://wiki.apache.org/httpd/RewriteHTTPToHTTPS provides a good method. You **could** copy and paste that those "rewrite rule" lines into Apache's main config file at ``/etc/httpd/conf/httpd.conf`` but using Apache's "virtual hosts" feature is recommended so that you can leave the main configuration file alone and drop a host-specific file into place.

Below is an example of how "rewrite rule" lines look within a ``VirtualHost`` block. Download a `sample file <../_static/installation/files/etc/httpd/conf.d/dataverse.example.edu.conf>`_ , edit it to substitute your own hostname under ``ServerName``, and place it at ``/etc/httpd/conf.d/dataverse.example.edu.conf`` or a filename that matches your hostname. The file must be in ``/etc/httpd/conf.d`` and must end in ".conf" to be included in Apache's configuration.

.. literalinclude:: ../_static/installation/files/etc/httpd/conf.d/dataverse.example.edu.conf

Edit Apache ssl.conf File
^^^^^^^^^^^^^^^^^^^^^^^^^
``/etc/httpd/conf.d/ssl.conf`` should be edited to contain the FQDN of your hostname like this: ``ServerName dataverse.example.edu:443`` (substituting your hostname).

Near the bottom of ``/etc/httpd/conf.d/ssl.conf`` but before the closing ``</VirtualHost>`` directive, add the following:

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

You can download a `sample ssl.conf file <../_static/installation/files/etc/httpd/conf.d/ssl.conf>`_ to compare it against the file you edited.

Note that ``/etc/httpd/conf.d/shib.conf`` and ``/etc/httpd/conf.d/shibboleth-ds.conf`` are expected to be present from installing Shibboleth via yum.

Configure Shibboleth
~~~~~~~~~~~~~~~~~~~~

shibboleth2.xml
^^^^^^^^^^^^^^^

``/etc/shibboleth/shibboleth2.xml`` should look something like the `sample shibboleth2.xml file <../_static/installation/files/etc/shibboleth/shibboleth2.xml>`_ below, but you must substitute your hostname in the ``entityID`` value. If your starting point is a ``shibboleth2.xml`` file provided by someone else, you must ensure that ``attributePrefix="AJP_"`` is added under ``ApplicationDefaults`` per the `Shibboleth wiki <https://wiki.shibboleth.net/confluence/display/SHIB2/NativeSPJavaInstall>`_ . Without the ``AJP_`` configuration in place, the required :ref:`shibboleth-attributes` will be null and users will be unable to log in.

.. literalinclude:: ../_static/installation/files/etc/shibboleth/shibboleth2.xml
   :language: xml

Specific Identity Provider(s) vs. Identity Federation
`````````````````````````````````````````````````````

When configuring the ``MetadataProvider`` section of ``shibboleth2.xml`` you should consider if your users will all come from the same Identity Provider (IdP) or not.

Specific Identity Provider(s)
+++++++++++++++++++++++++++++

Most Dataverse installations will probably only want to authenticate users via Shibboleth using their home institution's Identity Provider (IdP).  The configuration above in ``shibboleth2.xml`` looks for the metadata for the Identity Providers (IdPs) in a file at ``/etc/shibboleth/dataverse-idp-metadata.xml``.  You can download a `sample dataverse-idp-metadata.xml file <../_static/installation/files/etc/shibboleth/dataverse-idp-metadata.xml>`_ and that includes the TestShib IdP from http://testshib.org but you will want to edit this file to include the metadata from the Identity Provider(s) you care about. The identity people at your institution will be able to provide you with this metadata and they will very likely ask for a list of attributes that Dataverse requires, which are listed at :ref:`shibboleth-attributes`.

Identity Federation
+++++++++++++++++++

Rather than or in addition to specifying individual Identity Provider(s) you may wish to broaden the number of users who can log into your Dataverse installation by registering your Dataverse installation as a Service Provider (SP) within an identity federation. For example, in the United States, users from the `many institutions registered with the "InCommon" identity federation <https://incommon.org/federation/info/all-entities.html#IdPs>`_ that release the `"Research & Scholarship Attribute Bundle" <https://spaces.internet2.edu/display/InCFederation/Research+and+Scholarship+Attribute+Bundle>`_  will be able to log into your Dataverse installation if you register it as an `InCommon Service Provider <https://incommon.org/federation/info/all-entities.html#SPs>`_ that is part of the `Research & Scholarship (R&S) category <https://incommon.org/federation/info/all-entity-categories.html#SPs>`_.

The details of how to register with an identity federation are out of scope for this document, but a good starting point may be this list of identity federations across the world: http://www.protectnetwork.org/support/faq/identity-federations

One of the benefits of using ``shibd`` is that it can be configured to periodically poll your identity federation for updates as new Identity Providers (IdPs) join the federation you've registered with. For the InCommon federation, the following page describes how to download and verify signed InCommon metadata every hour: https://spaces.internet2.edu/display/InCFederation/Shibboleth+Metadata+Config#ShibbolethMetadataConfig-ConfiguretheShibbolethSP . You can also see an example of this as ``maxRefreshDelay="3600"`` in the commented out section of the ``shibboleth2.xml`` file above.

Once you've joined a federation the list of IdPs in the dropdown can be quite long! If you're curious how many are in the list you could try something like this: ``curl https://dataverse.example.edu/Shibboleth.sso/DiscoFeed | jq '.[].entityID' | wc -l``

.. _shibboleth-attributes:

Shibboleth Attributes
^^^^^^^^^^^^^^^^^^^^^

The following attributes are required for a successful Shibboleth login:

- Shib-Identity-Provider
- eppn
- givenName
- sn
- email

See also https://www.incommon.org/federation/attributesummary.html and https://wiki.shibboleth.net/confluence/display/SHIB2/NativeSPAttributeAccess

attribute-map.xml
^^^^^^^^^^^^^^^^^

By default, some attributes ``/etc/shibboleth/attribute-map.xml`` are commented out. Edit the file to enable them so that all the require attributes come through. You can download a `sample attribute-map.xml file <../_static/installation/files/etc/shibboleth/attribute-map.xml>`_.

Disable or Reconfigure SELinux
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

SELinux is set to "enforcing" by default on RHEL/CentOS, but unfortunately Shibboleth does not "just work" with SELinux. You have two options. You can disable SELinux or you can reconfigure SELinux to accommodate Shibboleth.

Disable SELinux
^^^^^^^^^^^^^^^

The first and easiest option is to set ``SELINUX=permisive`` in ``/etc/selinux/config`` and run ``setenforce permissive`` or otherwise disable SELinux to get Shibboleth to work. This is apparently what the Shibboleth project expects because their wiki page at https://wiki.shibboleth.net/confluence/display/SHIB2/NativeSPSELinux says, "At the present time, we do not support the SP in conjunction with SELinux, and at minimum we know that communication between the mod_shib and shibd components will fail if it's enabled. Other problems may also occur."

Reconfigure SELinux to Accommodate Shibboleth
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The second (more involved) option is to use the ``checkmodule``, ``semodule_package``, and ``semodule`` tools to apply a local policy to make Shibboleth work with SELinux. Let's get started.

Put Type Enforcement (TE) File in misc directory
````````````````````````````````````````````````

Copy and paste or download the `shibboleth.te <../_static/installation/files/etc/selinux/targeted/src/policy/domains/misc/shibboleth.te>`_ Type Enforcement (TE) file below and put it at ``/etc/selinux/targeted/src/policy/domains/misc/shibboleth.te``.

.. literalinclude:: ../_static/installation/files/etc/selinux/targeted/src/policy/domains/misc/shibboleth.te
   :language: text

(If you would like to know where the ``shibboleth.te`` came from and how to hack on it, please see the :doc:`/developers/selinux` section of the Developer Guide. Pull requests are welcome!)

Navigate to misc directory
``````````````````````````

``cd /etc/selinux/targeted/src/policy/domains/misc``

Run checkmodule
```````````````

``checkmodule -M -m -o shibboleth.mod shibboleth.te``

Run semodule_package
````````````````````

``semodule_package -o shibboleth.pp -m shibboleth.mod``

Silent is golden. No output is expected.

Run semodule
````````````

``semodule -i shibboleth.pp``

Silent is golden. No output is expected. This will place a file in ``/etc/selinux/targeted/modules/active/modules/shibboleth.pp`` and include "shibboleth" in the output of ``semodule -l``. See the ``semodule`` man page if you ever want to remove or disable the module you just added.

Congrats! You've made the creator of http://stopdisablingselinux.com proud. :)

Restart Apache and Shibboleth
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

After configuration is complete:

``service shibd restart``

``service httpd restart``

Configure Apache and shibd to Start at Boot
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

``chkconfig httpd on``

``chkconfig shibd on``

Verify DiscoFeed and Metadata URLs
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

As a sanity check, visit the following URLs (substituting your hostname) to make sure you see JSON and XML:

- https://dataverse.example.edu/Shibboleth.sso/DiscoFeed
- https://dataverse.example.edu/Shibboleth.sso/Metadata

The JSON in ``DiscoFeed`` comes from the list of IdPs you configured in the ``MetadataProvider`` section of ``shibboleth2.xml`` and will form a dropdown list on the Login Page.

Enable Shibboleth
~~~~~~~~~~~~~~~~~

``curl -X PUT -d true http://localhost:8080/api/admin/settings/:ShibEnabled``

After enabling Shibboleth, assuming the ``DiscoFeed`` is working per above, you should see a list of institutions to log into. You will not be able to log in via these institutions, however, until you have exchanged metadata with them. You can change the boolean above to ``false`` while you wait for the metadata exchange to be complete since it only affects if the Shibboleth login screen is shown.

Exchange Metadata with Your Identity Provider
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

http://testshib.org (TestShib) is a fantastic resource for testing Shibboleth configurations. Depending on your relationship with your identity people you may want to avoid bothering them until you have tested your Dataverse configuration with the TestShib Identity Provider (IdP). This process is explained below.

If you've temporarily configured your ``MetadataProvider`` to use the TestShib Identity Provider (IdP) as outlined above, you can download your metadata like this (substituting your hostname in both places):

``curl https://dataverse.example.edu/Shibboleth.sso/Metadata > dataverse.example.edu``

Then upload your metadata to http://testshib.org/register.html

Then try to log in to Dataverse using the TestShib IdP. After logging in, you can visit the https://dataverse.example.edu/Shibboleth.sso/Session (substituting your hostname) to troubleshoot which attributes are being received. You should see something like the following:

.. code-block:: none

    Miscellaneous
    Session Expiration (barring inactivity): 479 minute(s)
    Client Address: 65.112.10.82
    SSO Protocol: urn:oasis:names:tc:SAML:2.0:protocol
    Identity Provider: https://idp.testshib.org/idp/shibboleth
    Authentication Time: 2016-03-08T13:45:10.922Z
    Authentication Context Class: urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport
    Authentication Context Decl: (none)

    Attributes
    affiliation: Member@testshib.org;Staff@testshib.org
    cn: Me Myself And I
    entitlement: urn:mace:dir:entitlement:common-lib-terms
    eppn: myself@testshib.org
    givenName: Me Myself
    persistent-id: https://idp.testshib.org/idp/shibboleth!https://dataverse.example.edu/sp!RuyCiLvUcgmKqyh/rOQPh+wyR7s=
    sn: And I
    telephoneNumber: 555-5555
    uid: myself
    unscoped-affiliation: Member;Staff

(As of this writing the TestShib IdP does not send the "mail" attribute, a required attribute, but for testing purposes, Dataverse compensates for this for the TestShib IdP and permits login anyway.)

When you are done testing, you can delete the TestShib users you created like this (after you have deleted any data and permisions associated with the users):

``curl -X DELETE http://localhost:8080/api/admin/authenticatedUsers/myself``

(Of course, you are also welcome to do a fresh reinstall per the :doc:`installation-main` section.)

If your Dataverse installation is working with TestShib it **should** work with your institution's Identity Provider (IdP). Next, you should:

- Send your identity people your metadata file above (or a link to download it themselves). From their perspective you are a Service Provider (SP).
- Ask your identity people to send you the metadata for the Identity Provider (IdP) they operate. See the section above on ``shibboleth2.xml`` and ``MetadataProvider`` for what to do with the IdP metadata. Restart ``shibd`` and ``httpd`` as necessary.
- Test login to Dataverse via your institution's Identity Provider (IdP).

Administration
--------------

Institution-Wide Shibboleth Groups
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Dataverse allows you to optionally define "institution-wide Shibboleth groups" based on the the entityID of the Identity Provider (IdP) used to authenticate. For example, an "institution-wide Shibboleth group" with ``https://idp.testshib.org/idp/shibboleth`` as the IdP would include everyone who logs in via the TestShib IdP mentioned above.

To create an institution-wide Shibboleth groups, create a JSON file as below and issue this curl command: ``curl http://localhost:8080/api/admin/groups/shib -X POST -H 'Content-type:application/json' --upload-file shibGroupTestShib.json``

.. literalinclude:: ../_static/installation/files/etc/shibboleth/shibGroupTestShib.json 

Institution-wide Shibboleth groups are based on the "Shib-Identity-Provider" SAML attribute asserted at runtime after successful authentication with the Identity Provider (IdP) and held within the browser session rather than being persisted in the database for any length of time. It is for this reason that roles based on these groups, such as the ability to create a dataset, are not honored by non-browser interactions, such as through the SWORD API. 

To list institution-wide Shibboleth groups: ``curl http://localhost:8080/api/admin/groups/shib``

To delete an institution-wide Shibboleth group (assuming id 1): ``curl -X DELETE http://localhost:8080/api/admin/groups/shib/1``

Support for arbitrary attributes beyond "Shib-Identity-Provider" such as "eduPersonScopedAffiliation", etc. is being tracked at https://github.com/IQSS/dataverse/issues/1515

Converting Local Users to Shibboleth
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

If you are running in "remote and local" mode and have existing local users that you'd like to convert to Shibboleth users, give them the following steps to follow:

- Log in with your local account to make sure you know your password, which will be needed for the account conversion process.
- Log out of your local account.
- Log in with your Shibboleth account.
- If the email address associated with your local account matches the email address asserted by the Identity Provider (IdP), you will be prompted for the password of your local account and asked to confirm the conversion of your account. You're done! Browse around to ensure you see all the data you expect to see. Permissions have been preserved. 
- If the email address asserted by the Identity Provider (IdP) does not match the email address of any local user, you will be prompted to create a new account. If you were expecting account conversion, you should decline creating a new Shibboleth account, log back in to your local account, and let Support know the email on file for your local account. Support may ask you to change your email address for your local account to the one that is being asserted by the Identity Provider. Someone with access to the Glassfish logs will see this email address there.

Converting Shibboleth Users to Local
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Whereas users convert their own accounts from local to Shibboleth as described above, conversion in the opposite direction is performed by a sysadmin. A common scenario may be as follows:

- A user emails Support saying, "I left the university (or wherever) and can't log in to Dataverse anymore. What should I do?"
- Support replies asking the user for a new email address (Gmail, new institution email, etc.) to associate with their Dataverse account.
- The user replies with a new email address to associate with their Dataverse account.
- Support runs the curl command below, supplying the database id of the user to convert and the new email address and notes the username returned.
- Support emails the user and indicates that that they should use the password reset feature to set a new password and to make sure to take note of their username under Account Information (or the password reset confirmation email) since the user never had a username before.
- The user resets password and is able to log in with their local account. All permissions have been preserved with the exception of any permissions assigned to an institution-wide Shibboleth group to which the user formerly belonged.

In the example below, the user has indicated that the new email address they'd like to have associated with their account is "former.shib.user@mailinator.com" and their user id from the ``authenticateduser`` database table is "2". The API token must belong to a superuser (probably the sysadmin executing the command).

``curl -H "X-Dataverse-key: $API_TOKEN" -X PUT -d "former.shib.user@mailinator.com" http://localhost:8080/api/admin/authenticatedUsers/id/2/convertShibToBuiltIn``

Rather than looking up the user's id in the ``authenticateduser`` database table, you can issue this command to get a listing of all users:

``curl -H "X-Dataverse-key: $API_TOKEN" http://localhost:8080/api/admin/authenticatedUsers``

Per above, you now need to tell the user to use the password reset feature to set a password for their local account.

Debugging
~~~~~~~~~

The :doc:`administration` section explains how to increase Glassfish logging levels. The relevant classes and packages are:

- edu.harvard.iq.dataverse.Shib
- edu.harvard.iq.dataverse.authorization.providers.shib
- edu.harvard.iq.dataverse.authorization.groups.impl.shib

Backups
~~~~~~~

The installation and configuration of Shibboleth will result in the following cert and key files being created and it's important to back them up:

- ``/etc/shibboleth/sp-cert.pem``
- ``/etc/shibboleth/sp-key.pem``

If you have more than one Glassfish server, you should use the same ``sp-cert.pem`` and ``sp-key.pem`` files on all of them. If these files are compromised and you need to regenerate them, you can ``cd /etc/shibboleth`` and run ``keygen.sh`` like this (substituting you own hostname):

``./keygen.sh -f -u shibd -g shibd -h dataverse.example.edu -e https://dataverse.example.edu/sp``
