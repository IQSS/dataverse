=============
Configuration
=============

Now that you've successfully logged into Dataverse with a superuser account after going through a basic :doc:`installation-main`, you'll need to secure and configure your installation.

Settings within Dataverse itself are managed via JVM options or by manipulating values in the ``setting`` table directly or through API calls.

Once you have finished securing and configuring your Dataverse installation, you may proceed to the :doc:`/admin/index` for more information on the ongoing administration of a Dataverse installation. Advanced configuration topics are covered in the :doc:`r-rapache-tworavens`, :doc:`shibboleth` and :doc:`oauth2` sections.

.. contents:: |toctitle|
  :local:

.. _securing-your-installation:

Securing Your Installation
--------------------------

Changing the Superuser Password
+++++++++++++++++++++++++++++++

The default password for the "dataverseAdmin" superuser account is "admin", as mentioned in the :doc:`installation-main` section, and you should change it, of course.

.. _blocking-api-endpoints:

Blocking API Endpoints
++++++++++++++++++++++

The :doc:`/api/native-api` contains a useful but potentially dangerous API endpoint called "admin" that allows you to change system settings, make ordinary users into superusers, and more. The "builtin-users" endpoint lets admins create a local/builtin user account if they know the key defined in :ref:`BuiltinUsers.KEY`.

By default, most APIs can be operated on remotely and a number of endpoints do not require authentication. The endpoints "admin" and "builtin-users" are limited to localhost out of the box by the settings :ref:`:BlockedApiEndpoints` and :ref:`:BlockedApiPolicy`.

It is very important to keep the block in place for the "admin" endpoint, and to leave the "builtin-users" endpoint blocked unless you need to access it remotely. Documentation for the "admin" endpoint is spread across the :doc:`/api/native-api` section of the API Guide and the :doc:`/admin/index`.

It's also possible to prevent file uploads via API by adjusting the :ref:`:UploadMethods` database setting.

Forcing HTTPS
+++++++++++++

To avoid having your users send credentials in the clear, it's strongly recommended to force all web traffic to go through HTTPS (port 443) rather than HTTP (port 80). The ease with which one can install a valid SSL cert into Apache compared with the same operation in Payara might be a compelling enough reason to front Payara with Apache. In addition, Apache can be configured to rewrite HTTP to HTTPS with rules such as those found at https://wiki.apache.org/httpd/RewriteHTTPToHTTPS or in the section on :doc:`shibboleth`.


.. _user-ip-addresses-proxy-security:

Recording User IP Addresses
+++++++++++++++++++++++++++

By default, Dataverse captures the IP address from which requests originate. This is used for multiple purposes including controlling access to the admin API, IP-based user groups and Make Data Count reporting. When Dataverse is configured behind a proxy such as a load balancer, this default setup may not capture the correct IP address. In this case all the incoming requests will be logged in the access logs, MDC logs etc., as if they are all coming from the IP address(es) of the load balancer itself. Proxies usually save the original address in an added HTTP header, from which it can be extracted. For example, AWS LB records the "true" original address in the standard ``X-Forwarded-For`` header. If your Dataverse is running behind an IP-masking proxy, but you would like to use IP groups, or record the true geographical location of the incoming requests with Make Data Count, you may enable the IP address lookup from the proxy header using the JVM option  ``dataverse.useripaddresssourceheader``, described further below. 

Before doing so however, you must absolutely **consider the security risks involved**! This option must be enabled **only** on a Dataverse that is in fact fully behind a proxy that properly, and consistently, adds the ``X-Forwarded-For`` (or a similar) header to every request it forwards. Consider the implications of activating this option on a Dataverse that is not running behind a proxy, *or running behind one, but still accessible from the insecure locations bypassing the proxy*: Anyone can now add the header above to an incoming reqest, supplying an arbitrary IP address that Dataverse will trust as the true origin of  the call. Thus giving an attacker an easy way to, for example, get in a privileged IP group. The implications could be even more severe if an attacker were able to pretend to be coming from ``localhost``, if Dataverse is configured to trust localhost connections for unrestricted access to the admin API! We have addressed this by making it so that Dataverse should never accept ``localhost``, ``127.0.0.1``, ``0:0:0:0:0:0:0:1`` etc. when supplied in such a header. But if you have reasons to still find this risk unacceptable, you may want to consider turning open localhost access to the API off (See :ref:`Securing Your Installation <securing-your-installation>` for more information.)

This is how to verify that your proxy or load balancer, etc. is handling the originating address headers properly and securely: Make sure access logging is enabled in your application server (Payara) configuration. (``<http-service access-logging-enabled="true">`` in the ``domain.xml``). Add the address header to the access log format. For example, on a system behind AWS ELB, you may want to use something like ``%client.name% %datetime% %request% %status% %response.length% %header.referer% %header.x-forwarded-for%``. Once enabled, access the Dataverse from outside the LB. You should now see the real IP address of your remote client in the access log. For example, something like: 
``"1.2.3.4" "01/Jun/2020:12:00:00 -0500" "GET /dataverse.xhtml HTTP/1.1" 200 81082  "NULL-REFERER" "128.64.32.16"`` 

In this example, ``128.64.32.16`` is your remote address (that you should verify), and ``1.2.3.4`` is the address of your LB. If you're not seeing your remote address in the log, do not activate the JVM option! Also, verify that all the entries in the log have this header populated. The only entries in the access log that you should be seeing without this header (logged as ``"NULL-HEADER-X-FORWARDED-FOR"``) are local requests, made from localhost, etc. In this case, since the request is not coming through the proxy, the local IP address should be logged as the primary one (as the first value in the log entry, ``%client.name%``). If you see any requests coming in from remote, insecure subnets without this header - do not use the JVM option! 

Once you are ready, enable the :ref:`JVM option <useripaddresssourceheader>`. Verify that the remote locations are properly tracked in your MDC metrics, and/or your IP groups are working. As a final test, if your Dataverse is allowing unrestricted localhost access to the admin API, imitate an attack in which a malicious request is pretending to be coming from ``127.0.0.1``. Try the following from a remote, insecure location:

``curl https://your.dataverse.edu/api/admin/settings --header "X-FORWARDED-FOR: 127.0.0.1"``

First of all, confirm that access is denied! If you are in fact able to access the settings api from a location outside the proxy, **something is seriously wrong**, so please let us know, and stop using the JVM option.  Otherwise check the access log entry for the header value. What you should see is something like ``"127.0.0.1, 128.64.32.16"``. Where the second address should be the real IP of your remote client. The fact that the "fake" ``127.0.0.1`` you sent over is present in the header is perfectly ok. This is the proper proxy behavior - it preserves any incoming values in the ``X-Forwarded-Header``, if supplied, and adds the detected incoming address to it, *on the right*. It is only this rightmost comma-separated value that Dataverse should ever be using. 

Still feel like activating this option in your configuration? - Have fun and be safe!


.. _PrivacyConsiderations:

Privacy Considerations
++++++++++++++++++++++

Email Privacy
^^^^^^^^^^^^^

Out of the box, Dataverse will list email addresses of the contacts for datasets when users visit a dataset page and click the "Export Metadata" button. Additionally, out of the box, Dataverse will list email addresses of dataverse contacts via API (see :ref:`View a Dataverse <view-dataverse>` in the :doc:`/api/native-api` section of the API Guide). If you would like to exclude these email addresses from export, set :ref:`:ExcludeEmailFromExport <:ExcludeEmailFromExport>` to true.

Additional Recommendations
++++++++++++++++++++++++++

Run Payara as a User Other Than Root
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

See the :ref:`payara` section of :doc:`prerequisites` for details and init scripts for running Payara as non-root.

Related to this is that you should remove ``/root/.payara/pass`` to ensure that Payara isn't ever accidentally started as root. Without the password, Payara won't be able to start as root, which is a good thing.

Enforce Strong Passwords for User Accounts
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Dataverse only stores passwords (as salted hash, and using a strong hashing algorithm) for "builtin" users. You can increase the password complexity rules to meet your security needs. If you have configured your Dataverse installation to allow login from remote authentication providers such as Shibboleth, ORCID, GitHub or Google, you do not have any control over those remote providers' password complexity rules. See the :ref:`auth-modes` section below for more on login options.

Even if you are satisfied with the out-of-the-box password complexity rules Dataverse ships with, for the "dataverseAdmin" account you should use a strong password so the hash cannot easily be cracked through dictionary attacks.

Password complexity rules for "builtin" accounts can be adjusted with a variety of settings documented below. Here's a list:

- :ref:`:PVMinLength`
- :ref:`:PVMaxLength`
- :ref:`:PVNumberOfConsecutiveDigitsAllowed`
- :ref:`:PVCharacterRules`
- :ref:`:PVNumberOfCharacteristics`
- :ref:`:PVDictionaries`
- :ref:`:PVGoodStrength`
- :ref:`:PVCustomPasswordResetAlertMessage`

.. _network-ports:

Network Ports
-------------

Remember how under "Decisions to Make" in the :doc:`prep` section we mentioned you'll need to make a decision about whether or not to introduce a proxy in front of Dataverse such as Apache or nginx? The time has come to make that decision.

The need to redirect port HTTP (port 80) to HTTPS (port 443) for security has already been mentioned above and the fact that Payara puts these services on 8080 and 8181, respectively, was touched on in the :doc:`installation-main` section. In production, you don't want to tell your users to use Dataverse on ports 8080 and 8181. You should have them use the standard HTTPS port, which is 443.

Your decision to proxy or not should primarily be driven by which features of Dataverse you'd like to use. If you'd like to use Shibboleth, the decision is easy because proxying or "fronting" Payara with Apache is required. The details are covered in the :doc:`shibboleth` section.

If you'd like to use TwoRavens, you should also consider fronting with Apache because you will be required to install an Apache anyway to make use of the rApache module. For details, see the :doc:`r-rapache-tworavens` section.

Even if you have no interest in Shibboleth nor TwoRavens, you may want to front Dataverse with Apache or nginx to simply the process of installing SSL certificates. There are many tutorials on the Internet for adding certs to Apache, including a some `notes used by the Dataverse team <https://github.com/IQSS/dataverse/blob/v4.6.1/doc/shib/shib.md>`_, but the process of adding a certificate to Payara is arduous and not for the faint of heart. The Dataverse team cannot provide much help with adding certificates to Payara beyond linking to `tips <http://stackoverflow.com/questions/906402/importing-an-existing-x509-certificate-and-private-key-in-java-keystore-to-use-i>`_ on the web.

Still not convinced you should put Payara behind another web server? Even if you manage to get your SSL certificate into Payara, how are you going to run Payara on low ports such as 80 and 443? Are you going to run Payara as root? Bad idea. This is a security risk. Under "Additional Recommendations" under "Securing Your Installation" above you are advised to configure Payara to run as a user other than root.

There's also the issue of serving a production-ready version of robots.txt. By using a proxy such as Apache, this is a one-time "set it and forget it" step as explained below in the "Going Live" section.

If you are convinced you'd like to try fronting Payara with Apache, the :doc:`shibboleth` section should be good resource for you.

If you really don't want to front Payara with any proxy (not recommended), you can configure Payara to run HTTPS on port 443 like this:

``./asadmin set server-config.network-config.network-listeners.network-listener.http-listener-2.port=443``

What about port 80? Even if you don't front Dataverse with Apache, you may want to let Apache run on port 80 just to rewrite HTTP to HTTPS as described above. You can use a similar command as above to change the HTTP port that Payara uses from 8080 to 80 (substitute ``http-listener-1.port=80``). Payara can be used to enforce HTTPS on its own without Apache, but configuring this is an exercise for the reader. Answers here may be helpful: http://stackoverflow.com/questions/25122025/glassfish-v4-java-7-port-unification-error-not-able-to-redirect-http-to

If you are running an installation with Apache and Payara on the same server, and would like to restrict Payara from responding to any requests to port 8080 from external hosts (in other words, not through Apache), you can restrict the AJP listener to localhost only with:

``./asadmin set server-config.network-config.network-listeners.network-listener.http-listener-1.address=127.0.0.1``

You should **NOT** use the configuration option above if you are running in a load-balanced environment, or otherwise have the web server on a different host than the application server.

Root Dataverse Permissions
--------------------------

The user who creates a dataverse is given the "Admin" role on that dataverse. The root dataverse is created automatically for you by the installer and the "Admin" is the superuser account ("dataverseAdmin") we used in the :doc:`installation-main` section to confirm that we can log in. These next steps of configuring the root dataverse require the "Admin" role on the root dataverse, but not the much more powerful superuser attribute. In short, users with the "Admin" role are subject to the permission system. A superuser, on the other hand, completely bypasses the permission system. You can give non-superusers the "Admin" role on the root dataverse if you'd like them to configure the root dataverse.

In order for non-superusers to start creating dataverses or datasets, you need click "Edit" then "Permissions" and make choices about which users can add dataverses or datasets within the root dataverse. (There is an API endpoint for this operation as well.) Again, the user who creates a dataverse will be granted the "Admin" role on that dataverse. Non-superusers who are not "Admin" on the root dataverse will not be able to do anything useful until the root dataverse has been published.

As the person installing Dataverse you may or may not be a local metadata expert. You may want to have others sign up for accounts and grant them the "Admin" role at the root dataverse to configure metadata fields, templates, browse/search facets, guestbooks, etc. For more on these topics, consult the :doc:`/user/dataverse-management` section of the User Guide.

Persistent Identifiers and Publishing Datasets
----------------------------------------------

Persistent identifiers are a required and integral part of the Dataverse platform. They provide a URL that is guaranteed to resolve to the datasets or files they represent. Dataverse currently supports creating identifiers using DOI and Handle.

By default, the installer configures a default DOI namespace (10.5072) with DataCite as the registration provider. Please note that as of the release 4.9.3, we can no longer use EZID as the provider. Unlike EZID, DataCite requires that you register for a test account, configured with your own prefix (please contact support@datacite.org). Once you receive the login name, password, and prefix for the account, configure the credentials in your domain.xml, as the following two JVM options::

      <jvm-options>-Ddoi.username=...</jvm-options>
      <jvm-options>-Ddoi.password=...</jvm-options>

and restart Payara. The prefix can be configured via the API (where it is referred to as "Authority"):

``curl -X PUT -d 10.xxxx http://localhost:8080/api/admin/settings/:Authority``

Once this is done, you will be able to publish datasets and files, but the persistent identifiers will not be citable, and they will only resolve from the DataCite test environment (and then only if the Dataverse from which you published them is accessible - DOIs minted from your laptop will not resolve). Note that any datasets or files created using the test configuration cannot be directly migrated and would need to be created again once a valid DOI namespace is configured.

To properly configure persistent identifiers for a production installation, an account and associated namespace must be acquired for a fee from a DOI or HDL provider. **DataCite** (https://www.datacite.org) is the recommended DOI provider (see https://dataverse.org/global-dataverse-community-consortium for more on joining DataCite) but **EZID** (http://ezid.cdlib.org) is an option for the University of California according to https://www.cdlib.org/cdlinfo/2017/08/04/ezid-doi-service-is-evolving/ . **Handle.Net** (https://www.handle.net) is the HDL provider.

Once you have your DOI or Handle account credentials and a namespace, configure Dataverse to use them using the JVM options and database settings below.

Configuring Dataverse for DOIs
++++++++++++++++++++++++++++++

By default Dataverse attempts to register DOIs for each dataset and file under a test authority, though you must apply for your own credentials as explained above.

Here are the configuration options for DOIs:

**JVM Options:**

- :ref:`doi.baseurlstring`
- :ref:`doi.username`
- :ref:`doi.password`
- :ref:`doi.dataciterestapiurlstring`

**Database Settings:**

- :ref:`:DoiProvider <:DoiProvider>`
- :ref:`:Protocol <:Protocol>`
- :ref:`:Authority <:Authority>`
- :ref:`:Shoulder <:Shoulder>`
- :ref:`:IdentifierGenerationStyle <:IdentifierGenerationStyle>` (optional)
- :ref:`:DataFilePIDFormat <:DataFilePIDFormat>` (optional)
- :ref:`:FilePIDsEnabled <:FilePIDsEnabled>` (optional, defaults to true)

Configuring Dataverse for Handles
+++++++++++++++++++++++++++++++++

Here are the configuration options for handles:

**JVM Options:**

- :ref:`dataverse.handlenet.admcredfile`
- :ref:`dataverse.handlenet.admprivphrase`
- :ref:`dataverse.handlenet.index`

**Database Settings:**

- :ref:`:Protocol <:Protocol>`
- :ref:`:Authority <:Authority>`
- :ref:`:IdentifierGenerationStyle <:IdentifierGenerationStyle>` (optional)
- :ref:`:DataFilePIDFormat <:DataFilePIDFormat>` (optional)
- :ref:`:IndependentHandleService <:IndependentHandleService>` (optional)

Note: If you are **minting your own handles** and plan to set up your own handle service, please refer to `Handle.Net documentation <http://handle.net/hnr_documentation.html>`_.

.. _auth-modes:

Auth Modes: Local vs. Remote vs. Both
-------------------------------------

There are three valid configurations or modes for authenticating users to Dataverse:

Local Only Auth
+++++++++++++++

Out of the box, Dataverse is configured in "local only" mode. The "dataverseAdmin" superuser account mentioned in the :doc:`/installation/installation-main` section is an example of a local account. Internally, these accounts are called "builtin" because they are built in to the Dataverse application itself.

Both Local and Remote Auth
++++++++++++++++++++++++++

The ``authenticationproviderrow`` database table controls which "authentication providers" are available within Dataverse. Out of the box, a single row with an id of "builtin" will be present. For each user in Dataverse, the ``authenticateduserlookup`` table will have a value under ``authenticationproviderid`` that matches this id. For example, the default "dataverseAdmin" user will have the value "builtin" under  ``authenticationproviderid``. Why is this important? Users are tied to a specific authentication provider but conversion mechanisms are available to switch a user from one authentication provider to the other. As explained in the :doc:`/user/account` section of the User Guide, a graphical workflow is provided for end users to convert from the "builtin" authentication provider to a remote provider. Conversion from a remote authentication provider to the builtin provider can be performed by a sysadmin with access to the "admin" API. See the :doc:`/api/native-api` section of the API Guide for how to list users and authentication providers as JSON.

Adding and enabling a second authentication provider (:ref:`native-api-add-auth-provider` and :ref:`api-toggle-auth-provider`) will result in the Log In page showing additional providers for your users to choose from. By default, the Log In page will show the "builtin" provider, but you can adjust this via the :ref:`conf-default-auth-provider` configuration option. Further customization can be achieved by setting :ref:`conf-allow-signup` to "false", thus preventing users from creating local accounts via the web interface. Please note that local accounts can also be created through the API by enabling the ``builtin-users`` endpoint (:ref:`:BlockedApiEndpoints`) and setting the ``BuiltinUsers.KEY`` database setting (:ref:`BuiltinUsers.KEY`).

To configure Shibboleth see the :doc:`shibboleth` section and to configure OAuth see the :doc:`oauth2` section.

Remote Only Auth
++++++++++++++++

As for the "Remote only" authentication mode, it means that:

- Shibboleth or OAuth has been enabled.
- ``:AllowSignUp`` is set to "false" to prevent users from creating local accounts via the web interface.
- ``:DefaultAuthProvider`` has been set to use the desired authentication provider
- The "builtin" authentication provider has been disabled (:ref:`api-toggle-auth-provider`). Note that disabling the "builtin" authentication provider means that the API endpoint for converting an account from a remote auth provider will not work. Converting directly from one remote authentication provider to another (i.e. from GitHub to Google) is not supported. Conversion from remote is always to "builtin". Then the user initiates a conversion from "builtin" to remote. Note that longer term, the plan is to permit multiple login options to the same Dataverse account per https://github.com/IQSS/dataverse/issues/3487 (so all this talk of conversion will be moot) but for now users can only use a single login option, as explained in the :doc:`/user/account` section of the User Guide. In short, "remote only" might work for you if you only plan to use a single remote authentication provider such that no conversion between remote authentication providers will be necessary.

File Storage: Using a Local Filesystem and/or Swift and/or S3 object stores
---------------------------------------------------------------------------

By default, a Dataverse installation stores all data files (files uploaded by end users) on the filesystem at ``/usr/local/payara5/glassfish/domains/domain1/files``. This path can vary based on answers you gave to the installer (see the :ref:`dataverse-installer` section of the Installation Guide) or afterward by reconfiguring the ``dataverse.files.directory`` JVM option described below.

Dataverse can alternately store files in a Swift or S3-compatible object store, and can now be configured to support multiple stores at once. With a multi-store configuration, the location for new files can be controlled on a per-dataverse basis.

The following sections describe how to set up various types of stores and how to configure for multiple stores.

Multi-store Basics
++++++++++++++++++

To support multiple stores, Dataverse now requires an id, type, and label for each store (even for a single store configuration). These are configured by defining two required jvm options:

.. code-block:: none

    ./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.files.<id>.type=<type>"
    ./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.files.<id>.label=<label>"

Out of the box, Dataverse is configured to use local file storage in the 'file' store by default. You can add additional stores and, as a superuser, configure specific dataverses to use them (by editing the 'General Information' for the dataverse as described in the :doc:`/admin/dataverses-datasets` section).

Note that the "\-Ddataverse.files.directory", if defined, continues to control where temporary files are stored (in the /temp subdir of that directory), independent of the location of any 'file' store defined above.

If you wish to change which store is used by default, you'll need to delete the existing default storage driver and set a new one using jvm options.

.. code-block:: none

    ./asadmin $ASADMIN_OPTS delete-jvm-options "-Ddataverse.files.storage-driver-id=file"
    ./asadmin $ASADMIN_OPTS create-jvm-options "-Ddataverse.files.storage-driver-id=<id>"

It is also possible to set maximum file upload size limits per store. See the :ref:`:MaxFileUploadSizeInBytes` setting below.

File Storage
++++++++++++

File stores have one option - the directory where files should be stored. This can be set using

.. code-block:: none

    ./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.files.<id>.directory=<file directory>"

Multiple file stores should specify different directories (which would nominally be the reason to use multiple file stores), but one may share the same directory as "\-Ddataverse.files.directory" option - this would result in temp files being stored in the /temp subdirectory within the file store's root directory.

Swift Storage
+++++++++++++

Rather than storing data files on the filesystem, you can opt for an experimental setup with a `Swift Object Storage <http://swift.openstack.org>`_ backend. Each dataset that users create gets a corresponding "container" on the Swift side, and each data file is saved as a file within that container.

**In order to configure a Swift installation,** you need to complete these steps to properly modify the JVM options:

First, run all the following create commands with your Swift endpoint information and credentials:

.. code-block:: none

    ./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.files.<id>.type=swift"
    ./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.files.<id>.defaultEndpoint=endpoint1"
    ./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.files.<id>.authType.endpoint1=your-auth-type"
    ./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.files.<id>.authUrl.endpoint1=your-auth-url"
    ./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.files.<id>.tenant.endpoint1=your-tenant-name"
    ./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.files.<id>.username.endpoint1=your-username"
    ./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.files.<id>.endpoint.endpoint1=your-swift-endpoint"

``auth_type`` can either be ``keystone``, ``keystone_v3``, or it will assumed to be ``basic``. ``auth_url`` should be your keystone authentication URL which includes the tokens (e.g. for keystone, ``https://openstack.example.edu:35357/v2.0/tokens`` and for keystone_v3, ``https://openstack.example.edu:35357/v3/auth/tokens``). ``swift_endpoint`` is a URL that looks something like ``http://rdgw.swift.example.org/swift/v1``.

Then create a password alias by running (without changes):

.. code-block:: none

    ./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.files.swift.password.endpoint1='\${ALIAS=swiftpassword-alias}'"
    ./asadmin $ASADMIN_OPTS create-password-alias swiftpassword-alias

The second command will trigger an interactive prompt asking you to input your Swift password.

Second, update the JVM option ``dataverse.files.storage-driver-id`` by running the delete command:

``./asadmin $ASADMIN_OPTS delete-jvm-options "\-Ddataverse.files.storage-driver-id=file"``

Then run the create command:

``./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.files.storage-driver-id=swift"``

You also have the option to set a **custom container name separator.** It is initialized to ``_``, but you can change it by running the create command:

``./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.files.swift.folderPathSeparator=-"``

By default, your Swift installation will be public-only, meaning users will be unable to put access restrictions on their data. If you are comfortable with this level of privacy, the final step in your setup is to set the  :ref:`:PublicInstall` setting to `true`.

In order to **enable file access restrictions**, you must enable Swift to use temporary URLs for file access. To enable usage of temporary URLs, set a hash key both on your swift endpoint and in your swift.properties file. You can do so by running the create command:

``./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.files.swift.hashKey.endpoint1=your-hash-key"``

You also have the option to set a custom expiration length, in seconds, for a generated temporary URL. It is initialized to 60 seconds, but you can change it by running the create command:

``./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.files.swift.temporaryUrlExpiryTime=3600"``

In this example, you would be setting the expiration length for one hour.


Setting up Compute with Swift
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Once you have configured a Swift Object Storage backend, you also have the option of enabling a connection to a computing environment. To do so, you need to configure the database settings for :ref:`:ComputeBaseUrl` and  :ref:`:CloudEnvironmentName`.

Once you have set up ``:ComputeBaseUrl`` properly in both Dataverse and your cloud environment, validated users will have three options for accessing the computing environment:

- Compute on a single dataset
- Compute on multiple datasets
- Compute on a single datafile

The compute tool options on dataset and file pages will link validated users to your computing environment. If a user is computing on one dataset, the compute tool option will redirect to:

``:ComputeBaseUrl?datasetPersistentId``

If a user is computing on multiple datasets, the compute tool option will redirect to:

``:ComputeBaseUrl/multiparty?datasetPersistentId&anotherDatasetPersistentId&anotherDatasetPersistentId&...``

If a user is computing on a single file, depending on the configuration of your installation, the compute tool option will either redirect to:

``:ComputeBaseUrl?datasetPersistentId=yourObject``

if your installation's :ref:`:PublicInstall` setting is true, or:

``:ComputeBaseUrl?datasetPersistentId=yourObject&temp_url_sig=yourTempUrlSig&temp_url_expires=yourTempUrlExpiry``

You can configure this redirect properly in your cloud environment to generate a temporary URL for access to the Swift objects for computing.

Amazon S3 Storage (or Compatible)
+++++++++++++++++++++++++++++++++

Dataverse supports Amazon S3 storage as well as other S3-compatible stores (like Minio, Ceph RADOS S3 Gateway and many more) for files uploaded to Dataverse.

The Dataverse S3 driver supports multi-part upload for large files (over 1 GB by default - see the min-part-size option in the table below to change this).

**Note:** The Dataverse Team is most familiar with AWS S3, and can provide support on its usage with Dataverse. Thanks to community contributions, the application's architecture also allows non-AWS S3 providers. The Dataverse Team can provide very limited support on these other providers. We recommend reaching out to the wider Dataverse community if you have questions.

First: Set Up Accounts and Access Credentials
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Dataverse and the AWS SDK make use of the "AWS credentials profile file" and "AWS config profile file" located in
``~/.aws/`` where ``~`` is the home directory of the user you run Payara as. This file can be generated via either
of two methods described below:

1. Manually through creation of the credentials and config files or
2. Automatically via the AWS console commands.

Preparation When Using Amazon's S3 Service
##########################################

You'll need an AWS account with an associated S3 bucket for your installation to use. From the S3 management console
(e.g. `<https://console.aws.amazon.com/>`_), you can poke around and get familiar with your bucket.

**Make note** of the **bucket's name** and the **region** its data is hosted in.

To **create a user** with full S3 access and nothing more for security reasons, we recommend using IAM
(Identity and Access Management). See `IAM User Guide <http://docs.aws.amazon.com/IAM/latest/UserGuide/id_users.html>`_
for more info on this process.

**Generate the user keys** needed for Dataverse afterwards by clicking on the created user.
(You can skip this step when running on EC2, see below.)

.. TIP::
  If you are hosting Dataverse on an AWS EC2 instance alongside storage in S3, it is possible to use IAM Roles instead
  of the credentials file (the file at ``~/.aws/credentials`` mentioned below). Please note that you will still need the
  ``~/.aws/config`` file to specify the region. For more information on this option, see
  http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/iam-roles-for-amazon-ec2.html

Preparation When Using Custom S3-Compatible Service
###################################################

We assume you have your S3-compatible custom storage in place, up and running, ready for service.

Please make note of the following details:

- **Endpoint URL** - consult the documentation of your service on how to find it.

  * Example: https://play.minio.io:9000

- **Region:** Optional, but some services might use it. Consult your service documentation.

  * Example: *us-east-1*

- **Access key ID and secret access key:** Usually you can generate access keys within the user profile of your service.

  * Example:

    - ID: *Q3AM3UQ867SPQQA43P2F*

    - Key: *zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG*

- **Bucket name:** Dataverse will fail opening and uploading files on S3 if you don't create one.

  * Example: *dataverse*

Manually Set Up Credentials File
################################

To create the ``~/.aws/credentials`` file manually, you will need to generate a key/secret key (see above). Once you have
acquired the keys, they need to be added to the ``credentials`` file. The format for credentials is as follows:

::

  [default]
  aws_access_key_id = <insert key, no brackets>
  aws_secret_access_key = <insert secret key, no brackets>

While using Amazon's service, you must also specify the AWS region in the ``~/.aws/config`` file, for example:

::

  [default]
  region = us-east-1


Additional profiles can be added to these files by appending the relevant information in additional blocks:

::

  [default]
  aws_access_key_id = <insert key, no brackets>
  aws_secret_access_key = <insert secret key, no brackets>

  [profilename2]
  aws_access_key_id = <insert key, no brackets>
  aws_secret_access_key = <insert secret key, no brackets>

Place these two files in a folder named ``.aws`` under the home directory for the user running your Dataverse Payara
instance. (From the `AWS Command Line Interface Documentation <http://docs.aws.amazon.com/cli/latest/userguide/cli-config-files.html>`_:
"In order to separate credentials from less sensitive options, region and output format are stored in a separate file
named config in the same folder")

Console Commands to Set Up Access Configuration
###############################################

Begin by installing the CLI tool `pip <https://pip.pypa.io//en/latest/>`_ to install the
`AWS command line interface <https://aws.amazon.com/cli/>`_ if you don't have it.

First, we'll get our access keys set up. If you already have your access keys configured, skip this step.
From the command line, run:

- ``pip install awscli``
- ``aws configure``

You'll be prompted to enter your Access Key ID and secret key, which should be issued to your AWS account.
The subsequent config steps after the access keys are up to you. For reference, the keys will be stored in
``~/.aws/credentials``, and your AWS access region in ``~/.aws/config``.

**TIP:** When using a custom S3 URL endpoint, you need to add it to every ``aws`` call: ``aws --endpoint-url <URL> s3 ...``
  (you may omit it while configuring).

Second: Configure Dataverse to use S3 Storage
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

To set up an S3 store, you must define the id, type, and label as for any store:

.. code-block:: bash

    ./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.files.<id>.type=s3"
    ./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.files.<id>.label=<label>"


Then, we'll need to identify which S3 bucket we're using. Replace ``<your_bucket_name>`` with, of course, your bucket:

``./asadmin create-jvm-options "-Ddataverse.files.<id>.bucket-name=<your_bucket_name>"``

Optionally, you can have users download files from S3 directly rather than having files pass from S3 through Payara to your users. To accomplish this, set ``dataverse.files.<id>.download-redirect`` to ``true`` like this:

``./asadmin create-jvm-options "-Ddataverse.files.<id>.download-redirect=true"``

If you enable ``dataverse.files.<id>.download-redirect`` as described above, note that the S3 URLs expire after an hour by default but you can configure the expiration time using the ``dataverse.files.<id>.url-expiration-minutes`` JVM option. Here's an example of setting the expiration time to 120 minutes:

``./asadmin create-jvm-options "-Ddataverse.files.<id>.url-expiration-minutes=120"``

By default, your store will use the [default] profile in you .aws configuration files. To use a different profile, which would be necessary if you have two s3 stores at different locations, you can specify the profile to use:

``./asadmin create-jvm-options "-Ddataverse.files.<id>.profile=<profilename>"``

Larger installations may want to increase the number of open S3 connections allowed (default is 256): For example, 

``./asadmin create-jvm-options "-Ddataverse.files.<id>.connection-pool-size=4096"``

In case you would like to configure Dataverse to use a custom S3 service instead of Amazon S3 services, please
add the options for the custom URL and region as documented below. Please read above if your desired combination has
been tested already and what other options have been set for a successful integration.

Lastly, go ahead and restart your Payara server. With Dataverse deployed and the site online, you should be able to upload datasets and data files and see the corresponding files in your S3 bucket. Within a bucket, the folder structure emulates that found in local file storage.

S3 Storage Options
##################

===========================================  ==================  =========================================================================  =============
JVM Option                                   Value               Description                                                                Default value
===========================================  ==================  =========================================================================  =============
dataverse.files.storage-driver-id            <id>                Enable <id> as the default storage driver.                                 ``file``
dataverse.files.<id>.bucket-name             <?>                 The bucket name. See above.                                                (none)
dataverse.files.<id>.download-redirect       ``true``/``false``  Enable direct download or proxy through Dataverse.                         ``false``
dataverse.files.<id>.upload-redirect         ``true``/``false``  Enable direct upload of files added to a dataset  to the S3 store.         ``false``
dataverse.files.<id>.ingestsizelimit         <size in bytes>     Maximum size of directupload files that should be ingested                 (none)
dataverse.files.<id>.url-expiration-minutes  <?>                 If direct uploads/downloads: time until links expire. Optional.            60
dataverse.files.<id>.min-part-size           <?>                 Multipart direct uploads will occur for files larger than this. Optional.  ``1024**3``
dataverse.files.<id>.custom-endpoint-url     <?>                 Use custom S3 endpoint. Needs URL either with or without protocol.         (none)
dataverse.files.<id>.custom-endpoint-region  <?>                 Only used when using custom endpoint. Optional.                            ``dataverse``
dataverse.files.<id>.path-style-access       ``true``/``false``  Use path style buckets instead of subdomains. Optional.                    ``false``
dataverse.files.<id>.payload-signing         ``true``/``false``  Enable payload signing. Optional                                           ``false``
dataverse.files.<id>.chunked-encoding        ``true``/``false``  Disable chunked encoding. Optional                                         ``true``
dataverse.files.<id>.connection-pool-size    <?>                 The maximum number of open connections to the S3 server                    ``256``
===========================================  ==================  =========================================================================  =============

Reported Working S3-Compatible Storage
######################################

`Minio v2018-09-12 <http://minio.io>`_
  Set ``dataverse.files.<id>.path-style-access=true``, as Minio works path-based. Works pretty smooth, easy to setup.
  **Can be used for quick testing, too:** just use the example values above. Uses the public (read: unsecure and
  possibly slow) https://play.minio.io:9000 service.

`Surf Object Store v2019-10-30 <https://www.surf.nl/en>`_
  Set ``dataverse.files.<id>.payload-signing=true`` and ``dataverse.files.<id>.chunked-encoding=false`` to use Surf Object
  Store.

**HINT:** If you are successfully using an S3 storage implementation not yet listed above, please feel free to
`open an issue at Github <https://github.com/IQSS/dataverse/issues/new>`_ and describe your setup.
We will be glad to add it here.

Migrating from Local Storage to S3
##################################

Is currently documented on the :doc:`/developers/deployment` page.


.. _Branding Your Installation:

Branding Your Installation
--------------------------

The name of your root dataverse is the brand of your installation of Dataverse and appears in various places such as notifications and support links, as outlined in the :ref:`systemEmail` section below. To further brand your installation and make it your own, Dataverse provides configurable options for easy-to-add (and maintain) custom branding for your Dataverse installation. Here are the custom branding and content options you can add:

- Custom welcome/homepage
- Logo image to navbar
- Header
- Footer
- CSS stylesheet

Downloadable sample HTML and CSS files are provided below which you can edit as you see fit. It's up to you to create a directory in which to store these files, such as ``/var/www/dataverse`` in the examples below.

You may also want to look at samples at https://github.com/shlake/LibraDataHomepage from community member Sherry Lake as well as her poster from the Dataverse Community Meeting 2018 called "Branding Your Local Dataverse": https://github.com/IQSS/dataverse/files/2128735/UVaHomePage.pdf

A simpler option to brand and customize your installation is to utilize the dataverse theme, which each dataverse has, that allows you to change colors, add a logo, tagline or website link to the dataverse header section of the page. Those options are outlined in the :doc:`/user/dataverse-management` section of the User Guide.

Custom Homepage
++++++++++++++++

Dataverse allows you to use a custom homepage or welcome page in place of the default root dataverse page. This allows for complete control over the look and feel of your installation's homepage.

Download this sample: :download:`custom-homepage.html </_static/installation/files/var/www/dataverse/branding/custom-homepage.html>` and place it at ``/var/www/dataverse/branding/custom-homepage.html``.

Once you have the location of your custom homepage HTML file, run this curl command to add it to your settings:

``curl -X PUT -d '/var/www/dataverse/branding/custom-homepage.html' http://localhost:8080/api/admin/settings/:HomePageCustomizationFile``

If you prefer to start with less of a blank slate, you can download the :download:`custom-homepage-dynamic.html </_static/installation/files/var/www/dataverse/branding/custom-homepage-dynamic.html>` template which was built for the Harvard Dataverse, and includes branding messaging, action buttons, search input, subject links, and recent dataset links. This page was built to utilize the :doc:`/api/metrics` to deliver dynamic content to the page via javascript.

Note that the ``custom-homepage.html`` and ``custom-homepage-dynamic.html`` files provided have multiple elements that assume your root dataverse still has an alias of "root". While you were branding your root dataverse, you may have changed the alias to "harvard" or "librascholar" or whatever and you should adjust the custom homepage code as needed.

For more background on what this curl command above is doing, see the "Database Settings" section below. If you decide you'd like to remove this setting, use the following curl command:

``curl -X DELETE http://localhost:8080/api/admin/settings/:HomePageCustomizationFile``

Custom Navbar Logo
+++++++++++++++++++

Dataverse allows you to replace the default Dataverse icon and name branding in the navbar with your own custom logo. Note that this logo is separate from the *root dataverse theme* logo.

The custom logo image file is expected to be small enough to fit comfortably in the navbar, no more than 50 pixels in height and 160 pixels in width. Create a ``navbar`` directory in your Payara ``logos`` directory and place your custom logo there. By default, your logo image file will be located at ``/usr/local/payara5/glassfish/domains/domain1/docroot/logos/navbar/logo.png``.

Once you have the location of your custom logo image file, run this curl command to add it to your settings:

``curl -X PUT -d '/logos/navbar/logo.png' http://localhost:8080/api/admin/settings/:LogoCustomizationFile``

Custom Header
+++++++++++++

Download this sample: :download:`custom-header.html </_static/installation/files/var/www/dataverse/branding/custom-header.html>` and place it at ``/var/www/dataverse/branding/custom-header.html``.

Once you have the location of your custom header HTML file, run this curl command to add it to your settings:

``curl -X PUT -d '/var/www/dataverse/branding/custom-header.html' http://localhost:8080/api/admin/settings/:HeaderCustomizationFile``

If you have enabled a custom header or navbar logo, you might prefer to disable the theme of the root dataverse. You can do so by setting ``:DisableRootDataverseTheme`` to ``true`` like this:

``curl -X PUT -d 'true' http://localhost:8080/api/admin/settings/:DisableRootDataverseTheme``

Please note: Disabling the display of the root dataverse theme also disables your ability to edit it. Remember that dataverse owners can set their dataverses to "inherit theme" from the root. Those dataverses will continue to inherit the root dataverse theme (even though it no longer displays on the root). If you would like to edit the root dataverse theme in the future, you will have to re-enable it first.


Custom Footer
+++++++++++++

Download this sample: :download:`custom-footer.html </_static/installation/files/var/www/dataverse/branding/custom-footer.html>` and place it at ``/var/www/dataverse/branding/custom-footer.html``.

Once you have the location of your custom footer HTML file, run this curl command to add it to your settings:

``curl -X PUT -d '/var/www/dataverse/branding/custom-footer.html' http://localhost:8080/api/admin/settings/:FooterCustomizationFile``

Custom Stylesheet
+++++++++++++++++

You can style your custom homepage, footer and header content with a custom CSS file. With advanced CSS know-how, you can achieve custom branding and page layouts by utilizing ``position``, ``padding`` or ``margin`` properties.

Download this sample: :download:`custom-stylesheet.css </_static/installation/files/var/www/dataverse/branding/custom-stylesheet.css>` and place it at ``/var/www/dataverse/branding/custom-stylesheet.css``.

Once you have the location of your custom CSS file, run this curl command to add it to your settings:

``curl -X PUT -d '/var/www/dataverse/branding/custom-stylesheet.css' http://localhost:8080/api/admin/settings/:StyleCustomizationFile``

.. _i18n:

Internationalization
--------------------

Dataverse is being translated into multiple languages by the Dataverse community! Please see below for how to help with this effort!

Adding Multiple Languages to the Dropdown in the Header
++++++++++++++++++++++++++++++++++++++++++++++++++++++++

The presence of the :ref:`:Languages` database setting adds a dropdown in the header for multiple languages. For example to add English and French to the dropdown:

``curl http://localhost:8080/api/admin/settings/:Languages -X PUT -d '[{"locale":"en","title":"English"},{"locale":"fr","title":"Français"}]'``

Configuring the "lang" Directory
++++++++++++++++++++++++++++++++

Translations for Dataverse are stored in "properties" files in a directory on disk (e.g. ``/home/dataverse/langBundles``) that you specify with the :ref:`dataverse.lang.directory` ``dataverse.lang.directory`` JVM option, like this:

``./asadmin create-jvm-options '-Ddataverse.lang.directory=/home/dataverse/langBundles'``

Go ahead and create the directory you specified.

``mkdir /home/dataverse/langBundles``

Creating a languages.zip File
+++++++++++++++++++++++++++++

Dataverse provides and API endpoint for adding languages using a zip file.

First, clone the "dataverse-language-packs" git repo.

``git clone https://github.com/GlobalDataverseCommunityConsortium/dataverse-language-packs.git``

Take a look at https://github.com/GlobalDataverseCommunityConsortium/dataverse-language-packs/branches to see if the version of Dataverse you're running has translations.

Change to the directory for the git repo you just cloned.

``cd dataverse-language-packs``

Switch (``git checkout``) to the branch based on Dataverse version you are running. The branch "dataverse-v4.13" is used in the example below.

``export BRANCH_NAME=dataverse-v4.13``

``git checkout $BRANCH_NAME``

Create a "languages" directory in "/tmp".

``mkdir /tmp/languages``

Copy the properties files into the "languages" directory

``cp -R en_US/*.properties /tmp/languages``

``cp -R fr_CA/*.properties /tmp/languages``

Create the zip file

``cd /tmp/languages``

``zip languages.zip *.properties``

Load the languages.zip file into Dataverse
++++++++++++++++++++++++++++++++++++++++++

Now that you have a "languages.zip" file, you can load it into Dataverse with the command below.

``curl http://localhost:8080/api/admin/datasetfield/loadpropertyfiles -X POST --upload-file /tmp/languages/languages.zip -H "Content-Type: application/zip"``

Click on the languages using the drop down in the header to try them out.

How to Help Translate Dataverse Into Your Language
++++++++++++++++++++++++++++++++++++++++++++++++++

Please join the `dataverse-internationalization-wg`_ mailing list and contribute to https://github.com/GlobalDataverseCommunityConsortium/dataverse-language-packs to help translate Dataverse into various languages!

Some external tools are also ready to be translated, especially if they are using the ``{localeCode}`` reserved word in their tool manifest. For details, see the :doc:`/api/external-tools` section of the API Guide.

.. _dataverse-internationalization-wg: https://groups.google.com/forum/#!forum/dataverse-internationalization-wg

.. _Web-Analytics-Code:

Web Analytics Code
------------------

Your analytics code can be added to your Dataverse installation in a similar fashion to how you brand it, by adding a custom HTML file containing the analytics code snippet and adding the file location to your settings.

Popular analytics providers Google Analytics (https://www.google.com/analytics/) and Matomo (formerly "Piwik"; https://matomo.org/) have been set up with Dataverse. Use the documentation they provide to add the analytics code to your custom HTML file. This allows for more control of your analytics, making it easier to customize what you prefer to track.

Create your own ``analytics-code.html`` file using the analytics code snippet provided by Google or Matomo and place it somewhere on the server, outside the application deployment directory; for example: ``/var/www/dataverse/branding/analytics-code.html``. Here is an *example* of what your HTML file will look like:

.. code-block:: none

    <!-- Global Site Tag (gtag.js) - Google Analytics -->
    <script async="async" src="https://www.googletagmanager.com/gtag/js?id=YOUR-ACCOUNT-CODE"></script>
    <script>
        //<![CDATA[
	window.dataLayer = window.dataLayer || [];
	function gtag(){dataLayer.push(arguments);}
	gtag('js', new Date());

	gtag('config', 'YOUR-ACCOUNT-CODE');
        //]]>
    </script>

**IMPORTANT:** Note the "async" attribute in the first script line above. In the documentation provided by Google, its value is left blank (as in ``<script async src="...">``). It must be set as in the example above (``<script async="async" src="...">``), otherwise it may cause problems with some browsers.

Once you have created the analytics file, run this curl command to add it to your settings (using the same file location as in the example above):

``curl -X PUT -d '/var/www/dataverse/branding/analytics-code.html' http://localhost:8080/api/admin/settings/:WebAnalyticsCode``

Tracking Button Clicks
++++++++++++++++++++++

The basic analytics configuration above tracks page navigation. However, it does not capture potentially interesting events, such as those from users clicking buttons on pages, that do not result in a new page opening. In Dataverse, these events include file downloads, requesting access to restricted data, exporting metadata, social media sharing, requesting citation text, launching external tools or WorldMap, contacting authors, and launching computations.

Both Google and Matomo provide the optional capability to track such events and Dataverse has added CSS style classes (btn-compute, btn-contact, btn-download, btn-explore, btn-export, btn-preview, btn-request, btn-share, and downloadCitation) to it's HTML to facilitate it.

For Google Analytics, the example script at :download:`analytics-code.html </_static/installation/files/var/www/dataverse/branding/analytics-code.html>` will track both page hits and events within Dataverse. You would use this file in the same way as the shorter example above, putting it somewhere outside your deployment directory, replacing ``YOUR ACCOUNT CODE`` with your actual code and setting :WebAnalyticsCode to reference it.

Once this script is running, you can look in the Google Analytics console (Realtime/Events or Behavior/Events) and view events by type and/or the Dataset or File the event involves.

.. _BagIt Export:

BagIt Export
------------

Dataverse may be configured to submit a copy of published Datasets, packaged as `Research Data Alliance conformant <https://www.rd-alliance.org/system/files/Research%20Data%20Repository%20Interoperability%20WG%20-%20Final%20Recommendations_reviewed_0.pdf>`_ zipped `BagIt <https://tools.ietf.org/html/draft-kunze-bagit-17>`_ bags to `Chronopolis <https://libraries.ucsd.edu/chronopolis/>`_ via `DuraCloud <https://duraspace.org/duracloud/>`_ or alternately to any folder on the local filesystem.

Dataverse offers an internal archive workflow which may be configured as a PostPublication workflow via an admin API call to manually submit previously published Datasets and prior versions to a configured archive such as Chronopolis. The workflow creates a `JSON-LD <http://www.openarchives.org/ore/0.9/jsonld>`_ serialized `OAI-ORE <https://www.openarchives.org/ore/>`_ map file, which is also available as a metadata export format in the Dataverse web interface.

At present, the DPNSubmitToArchiveCommand, LocalSubmitToArchiveCommand, and GoogleCloudSubmitToArchive are the only implementations extending the AbstractSubmitToArchiveCommand and using the configurable mechanisms discussed below.

.. _Duracloud Configuration:

Duracloud Configuration
+++++++++++++++++++++++

Also note that while the current Chronopolis implementation generates the bag and submits it to the archive's DuraCloud interface, the step to make a 'snapshot' of the space containing the Bag (and verify it's successful submission) are actions a curator must take in the DuraCloud interface.

The minimal configuration to support an archiver integration involves adding a minimum of two Dataverse Keys and any required Payara jvm options. The example instructions here are specific to the DuraCloud Archiver\:

\:ArchiverClassName - the fully qualified class to be used for archiving. For example:

``curl http://localhost:8080/api/admin/settings/:ArchiverClassName -X PUT -d "edu.harvard.iq.dataverse.engine.command.impl.DuraCloudSubmitToArchiveCommand"``

\:ArchiverSettings - the archiver class can access required settings including existing Dataverse settings and dynamically defined ones specific to the class. This setting is a comma-separated list of those settings. For example\:

``curl http://localhost:8080/api/admin/settings/:ArchiverSettings -X PUT -d ":DuraCloudHost, :DuraCloudPort, :DuraCloudContext"``

The DPN archiver defines three custom settings, one of which is required (the others have defaults):

\:DuraCloudHost - the URL for your organization's Duracloud site. For example:

``curl http://localhost:8080/api/admin/settings/:DuraCloudHost -X PUT -d "qdr.duracloud.org"``

:DuraCloudPort and :DuraCloudContext are also defined if you are not using the defaults ("443" and "duracloud" respectively). (Note\: these settings are only in effect if they are listed in the \:ArchiverSettings. Otherwise, they will not be passed to the DuraCloud Archiver class.)

Archivers may require JVM options as well. For the Chronopolis archiver, the username and password associated with your organization's Chronopolis/DuraCloud account should be configured in Payara:

``./asadmin create-jvm-options '-Dduracloud.username=YOUR_USERNAME_HERE'``

``./asadmin create-jvm-options '-Dduracloud.password=YOUR_PASSWORD_HERE'``

.. _Local Path Configuration:

Local Path Configuration
++++++++++++++++++++++++

ArchiverClassName - the fully qualified class to be used for archiving. For example\:

``curl -X PUT -d "edu.harvard.iq.dataverse.engine.command.impl.LocalSubmitToArchiveCommand" http://localhost:8080/api/admin/settings/:ArchiverClassName``

\:BagItLocalPath - the path to where you want to store BagIt. For example\:

``curl -X PUT -d /home/path/to/storage http://localhost:8080/api/admin/settings/:BagItLocalPath``

\:ArchiverSettings - the archiver class can access required settings including existing Dataverse settings and dynamically defined ones specific to the class. This setting is a comma-separated list of those settings. For example\:

``curl http://localhost:8080/api/admin/settings/:ArchiverSettings -X PUT -d ":BagItLocalPath"``

:BagItLocalPath is the file path that you've set in :ArchiverSettings.

.. _Google Cloud Configuration:

Google Cloud Configuration
++++++++++++++++++++++++++

The Google Cloud Archiver can send Dataverse Bags to a bucket in Google's cloud, including those in the 'Coldline' storage class (cheaper, with slower access) 

``curl http://localhost:8080/api/admin/settings/:ArchiverClassName -X PUT -d "edu.harvard.iq.dataverse.engine.command.impl.GoogleCloudSubmitToArchiveCommand"``

``curl http://localhost:8080/api/admin/settings/:ArchiverSettings -X PUT -d ":GoogleCloudBucket, :GoogleCloudProject"``

The Google Cloud Archiver defines two custom settings, both are required. The credentials for your account, in the form of a json key file, must also be obtained and stored locally (see below):

In order to use the Google Cloud Archiver, you must have a Google account. You will need to create a project and bucket within that account and provide those values in the settings:

\:GoogleCloudBucket - the name of the bucket to use. For example:

``curl http://localhost:8080/api/admin/settings/:GoogleCloudBucket -X PUT -d "qdr-archive"``

\:GoogleCloudProject - the name of the project managing the bucket. For example:

``curl http://localhost:8080/api/admin/settings/:GoogleCloudProject -X PUT -d "qdr-project"``

The Google Cloud Archiver also requires a key file that must be renamed to 'googlecloudkey.json' and placed in the directory identified by your 'dataverse.files.directory' jvm option. This file can be created in the Google Cloud Console. (One method: Navigate to your Project 'Settings'/'Service Accounts', create an account, give this account the 'Cloud Storage'/'Storage Admin' role, and once it's created, use the 'Actions' menu to 'Create Key', selecting the 'JSON' format option. Use this as the 'googlecloudkey.json' file.)

For example:

``cp <your key file> /usr/local/payara5/glassfish/domains/domain1/files/googlecloudkey.json``

.. _Archiving API Call:

API Call
++++++++

Once this configuration is complete, you, as a user with the *PublishDataset* permission, should be able to use the API call to manually submit a DatasetVersion for processing:

``curl -H "X-Dataverse-key: <key>" http://localhost:8080/api/admin/submitDataVersionToArchive/{id}/{version}``

where:

``{id}`` is the DatasetId (or ``:persistentId`` with the ``?persistentId="<DOI>"`` parameter), and

``{version}`` is the friendly version number, e.g. "1.2".

The submitDataVersionToArchive API (and the workflow discussed below) attempt to archive the dataset version via an archive specific method. For Chronopolis, a DuraCloud space named for the dataset (it's DOI with ':' and '.' replaced with '-') is created and two files are uploaded to it: a version-specific datacite.xml metadata file and a BagIt bag containing the data and an OAI-ORE map file. (The datacite.xml file, stored outside the Bag as well as inside is intended to aid in discovery while the ORE map file is 'complete', containing all user-entered metadata and is intended as an archival record.)

In the Chronopolis case, since the transfer from the DuraCloud front-end to archival storage in Chronopolis can take significant time, it is currently up to the admin/curator to submit a 'snap-shot' of the space within DuraCloud and to monitor its successful transfer. Once transfer is complete the space should be deleted, at which point the Dataverse API call can be used to submit a Bag for other versions of the same Dataset. (The space is reused, so that archival copies of different Dataset versions correspond to different snapshots of the same DuraCloud space.).

PostPublication Workflow
++++++++++++++++++++++++

To automate the submission of archival copies to an archive as part of publication, one can setup a Dataverse Workflow using the "archiver" workflow step - see the :doc:`/developers/workflows` guide.

The archiver step uses the configuration information discussed above including the :ArchiverClassName setting. The workflow step definition should include the set of properties defined in \:ArchiverSettings in the workflow definition.

To active this workflow, one must first install a workflow using the archiver step. A simple workflow that invokes the archiver step configured to submit to DuraCloud as its only action is included in dataverse at /scripts/api/data/workflows/internal-archiver-workflow.json.

Using the Workflow Native API (see the :doc:`/api/native-api` guide) this workflow can be installed using:

``curl -X POST -H 'Content-type: application/json' --upload-file internal-archiver-workflow.json http://localhost:8080/api/admin/workflows``

The workflow id returned in this call (or available by doing a GET of /api/admin/workflows ) can then be submitted as the default PostPublication workflow:

``curl -X PUT -d {id} http://localhost:8080/api/admin/workflows/default/PostPublishDataset``

Once these steps are taken, new publication requests will automatically trigger submission of an archival copy to the specified archiver, Chronopolis' DuraCloud component in this example. For Chronopolis, as when using the API, it is currently the admin's responsibility to snap-shot the DuraCloud space and monitor the result. Failure of the workflow, (e.g. if DuraCloud is unavailable, the configuration is wrong, or the space for this dataset already exists due to a prior publication action or use of the API), will create a failure message but will not affect publication itself.

Going Live: Launching Your Production Deployment
------------------------------------------------

This guide has attempted to take you from kicking the tires on Dataverse to finalizing your installation before letting real users in. In theory, all this work could be done on a single server but better would be to have separate staging and production environments so that you can deploy upgrades to staging before deploying to production. This "Going Live" section is about launching your **production** environment.

Before going live with your installation of Dataverse, you must take the steps above under "Securing Your Installation" and you should at least review the various configuration options listed below. An attempt has been made to put the more commonly-configured options earlier in the list.

Out of the box, Dataverse attempts to block search engines from crawling your installation of Dataverse so that test datasets do not appear in search results until you're ready.

Letting Search Engines Crawl Your Installation
++++++++++++++++++++++++++++++++++++++++++++++

Ensure robots.txt Is Not Blocking Search Engines
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

For a public production Dataverse installation, it is probably desired that search agents be able to index published pages (AKA - pages that are visible to an unauthenticated user).
Polite crawlers usually respect the `Robots Exclusion Standard <https://en.wikipedia.org/wiki/Robots_exclusion_standard>`_; we have provided an example of a production robots.txt :download:`here </_static/util/robots.txt>`).

We **strongly recommend** using the crawler rules in the sample robots.txt linked above. Note that they make the dataverse and dataset pages accessible to the search engine bots; but discourage them from actually crawling the site, by following any search links - facets and such - on the dataverse pages. Such crawling is very inefficient in terms of system resources, and often results in confusing search results for the end users of the search engines (for example, when partial search results are indexed as individual pages).

The recommended solution instead is to directly point the bots to the dataset and dataverse pages that need to be indexed, by advertising them via an explicit sitemap (please see the next section for details on how to generate the sitemap).

You can of course modify your own robots.txt to suit your specific needs as necessary. If you don't want your datasets to be indexed at all, you can tell the bots to stay away from your site completely. But, as noted above, keep in mind that only the good, "polite" bots honor these rules! You are not really blocking anyone from accessing your site by adding a "Disallow" rule in robots.txt - it is a suggestion only. A rogue bot can and will violate it. If you are having trouble with the site being overloaded with what looks like heavy automated crawling, you may have to resort to blocking this traffic by other means - for example, via rewrite rules in Apache, or even by a Firewall.

(See the sample robots.txt file linked above for some comments on how to set up different "Allow" and "Disallow" rules for different crawler bots)

You have a couple of options for putting an updated robots.txt file into production. If you are fronting Payara with Apache as recommended above, you can place robots.txt in the root of the directory specified in your ``VirtualHost`` and to your Apache config a ``ProxyPassMatch`` line like the one below to prevent Payara from serving the version of robots.txt that is embedded in the Dataverse war file:

.. code-block:: text

    # don't let Payara serve its version of robots.txt
    ProxyPassMatch ^/robots.txt$ !

For more of an explanation of ``ProxyPassMatch`` see the :doc:`shibboleth` section.

If you are not fronting Payara with Apache you'll need to prevent Payara from serving the robots.txt file embedded in the war file by overwriting robots.txt after the war file has been deployed. The downside of this technique is that you will have to remember to overwrite robots.txt in the "exploded" war file each time you deploy the war file, which probably means each time you upgrade to a new version of Dataverse. Furthermore, since the version of Dataverse is always incrementing and the version can be part of the file path, you will need to be conscious of where on disk you need to replace the file. For example, for Dataverse 4.6.1 the path to robots.txt may be ``/usr/local/payara5/glassfish/domains/domain1/applications/dataverse-4.6.1/robots.txt`` with the version number ``4.6.1`` as part of the path.

Creating a Sitemap and Submitting it to Search Engines
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Search engines have an easier time indexing content when you provide them a sitemap. The Dataverse sitemap includes URLs to all published dataverses and all published datasets that are not harvested or deaccessioned.

Create or update your sitemap by adding the following curl command to cron to run nightly or as you see fit:

``curl -X POST http://localhost:8080/api/admin/sitemap``

This will create or update a file in the following location unless you have customized your installation directory for Payara:

``/usr/local/payara5/glassfish/domains/domain1/docroot/sitemap/sitemap.xml``

On an installation of Dataverse with many datasets, the creation or updating of the sitemap can take a while. You can check Payara's server.log file for "BEGIN updateSiteMap" and "END updateSiteMap" lines to know when the process started and stopped and any errors in between.

https://demo.dataverse.org/sitemap.xml is the sitemap URL for the Dataverse Demo site and yours should be similar.

Once the sitemap has been generated and placed in the domain docroot directory, it will become available to the outside callers at <YOUR_SITE_URL>/sitemap/sitemap.xml; it will also be accessible at <YOUR_SITE_URL>/sitemap.xml (via a *pretty-faces* rewrite rule). Some search engines will be able to find it at this default location. Some, **including Google**, need to be **specifically instructed** to retrieve it.

One way to submit your sitemap URL to Google is by using their "Search Console" (https://search.google.com/search-console). In order to use the console, you will need to authenticate yourself as the owner of your Dataverse site. Various authentication methods are provided; but if you are already using Google Analytics, the easiest way is to use that account. Make sure you are logged in on Google with the account that has the edit permission on your Google Analytics property; go to the search console and enter the root URL of your Dataverse server, then choose Google Analytics as the authentication method. Once logged in, click on "Sitemaps" in the menu on the left. (todo: add a screenshot?) Consult `Google's "submit a sitemap" instructions`_ for more information; and/or similar instructions for other search engines.

.. _Google's "submit a sitemap" instructions: https://support.google.com/webmasters/answer/183668


Putting Your Dataverse Installation on the Map at dataverse.org
+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

Congratulations! You've gone live! It's time to announce your new data repository to the world! You are also welcome to contact support@dataverse.org to have the Dataverse team add your installation to the map at http://dataverse.org . Thank you for installing Dataverse!

Administration of Your Dataverse Installation
+++++++++++++++++++++++++++++++++++++++++++++

Now that you're live you'll want to review the :doc:`/admin/index` for more information about the ongoing administration of a Dataverse installation.

Setting Up Integrations
+++++++++++++++++++++++

Before going live, you might want to consider setting up integrations to make it easier for your users to deposit or explore data. See the :doc:`/admin/integrations` section of the Admin Guide for details.

.. _jvm-options:

JVM Options
-----------

JVM stands for Java Virtual Machine and as a Java application, Payara can read JVM options when it is started. A number of JVM options are configured by the installer below is a complete list of the Dataverse-specific JVM options. You can inspect the configured options by running:

``./asadmin list-jvm-options | egrep 'dataverse|doi'``

When changing values these values with ``asadmin``, you'll need to delete the old value before adding a new one, like this:

``./asadmin delete-jvm-options "-Ddataverse.fqdn=old.example.com"``

``./asadmin create-jvm-options "-Ddataverse.fqdn=dataverse.example.com"``

It's also possible to change these values by stopping Payara, editing ``payara5/glassfish/domains/domain1/config/domain.xml``, and restarting Payara.

dataverse.fqdn
++++++++++++++

If the Dataverse server has multiple DNS names, this option specifies the one to be used as the "official" host name. For example, you may want to have dataverse.example.edu, and not the less appealing server-123.socsci.example.edu to appear exclusively in all the registered global identifiers, Data Deposit API records, etc.

The password reset feature requires ``dataverse.fqdn`` to be configured.

.. note::

	Do note that whenever the system needs to form a service URL, by default, it will be formed with ``https://`` and port 443. I.e.,
	``https://{dataverse.fqdn}/``
	If that does not suit your setup, you can define an additional option, ``dataverse.siteUrl``, explained below.

.. _dataverse.siteUrl:

dataverse.siteUrl
+++++++++++++++++

.. note::

	and specify the protocol and port number you would prefer to be used to advertise the URL for your Dataverse.
	For example, configured in domain.xml:
	``<jvm-options>-Ddataverse.fqdn=dataverse.example.edu</jvm-options>``
	``<jvm-options>-Ddataverse.siteUrl=http://${dataverse.fqdn}:8080</jvm-options>``

dataverse.files.directory
+++++++++++++++++++++++++

This is how you configure the path to which files uploaded by users are stored.

dataverse.auth.password-reset-timeout-in-minutes
++++++++++++++++++++++++++++++++++++++++++++++++

Users have 60 minutes to change their passwords by default. You can adjust this value here.

dataverse.db.name
+++++++++++++++++

The PostgreSQL database name to use for Dataverse.

Defaults to ``dataverse`` (but the installer sets it to ``dvndb``).

Can also be set via *MicroProfile Config API* sources, e.g. the environment variable ``DATAVERSE_DB_NAME``.

dataverse.db.user
+++++++++++++++++

The PostgreSQL user name to connect with.

Defaults to ``dataverse`` (but the installer sets it to ``dvnapp``).

Can also be set via *MicroProfile Config API* sources, e.g. the environment variable ``DATAVERSE_DB_USER``.

dataverse.db.password
+++++++++++++++++++++

The PostgreSQL users password to connect with.

Preferrably use a JVM alias, as passwords in environment variables aren't safe.

.. code-block:: shell

  echo "AS_ADMIN_ALIASPASSWORD=change-me-super-secret" > /tmp/password.txt
  asadmin create-password-alias --passwordfile /tmp/password.txt dataverse.db.password
  rm /tmp/password.txt

Can also be set via *MicroProfile Config API* sources, e.g. the environment variable ``DATAVERSE_DB_PASSWORD``.

dataverse.db.host
+++++++++++++++++

The PostgreSQL server to connect to.

Defaults to ``localhost``.

Can also be set via *MicroProfile Config API* sources, e.g. the environment variable ``DATAVERSE_DB_HOST``.

dataverse.db.port
+++++++++++++++++

The PostgreSQL server port to connect to.

Defaults to ``5432``, the default PostgreSQL port.

Can also be set via *MicroProfile Config API* sources, e.g. the environment variable ``DATAVERSE_DB_PORT``.

dataverse.rserve.host
+++++++++++++++++++++

Configuration for :doc:`r-rapache-tworavens`.

dataverse.rserve.port
+++++++++++++++++++++

Configuration for :doc:`r-rapache-tworavens`.

dataverse.rserve.user
+++++++++++++++++++++

Configuration for :doc:`r-rapache-tworavens`.

dataverse.rserve.tempdir
++++++++++++++++++++++++
Configuration for :doc:`r-rapache-tworavens`.

dataverse.rserve.password
+++++++++++++++++++++++++

Configuration for :doc:`r-rapache-tworavens`.

.. _dataverse.dropbox.key:

dataverse.dropbox.key
+++++++++++++++++++++

Dropbox provides a Chooser app, which is a Javascript component that allows you to upload files to Dataverse from Dropbox. It is an optional configuration setting, which requires you to pass it an app key and configure the ``:UploadMethods`` database setting. For more information on setting up your Chooser app, visit https://www.dropbox.com/developers/chooser.

``./asadmin create-jvm-options "-Ddataverse.dropbox.key={{YOUR_APP_KEY}}"``

dataverse.path.imagemagick.convert
++++++++++++++++++++++++++++++++++

For overriding the default path to the ``convert`` binary from ImageMagick (``/usr/bin/convert``).

dataverse.dataAccess.thumbnail.image.limit
++++++++++++++++++++++++++++++++++++++++++

For limiting the size (in bytes) of thumbnail images generated from files.

dataverse.dataAccess.thumbnail.pdf.limit
++++++++++++++++++++++++++++++++++++++++

For limiting the size (in bytes) of thumbnail images generated from files.

.. _doi.baseurlstring:

doi.baseurlstring
+++++++++++++++++

As of this writing, "https://mds.datacite.org" (DataCite) and "https://ezid.cdlib.org" (EZID) are the main valid values.

While the above two options are recommended because they have been tested by the Dataverse team, it is also possible to use a DataCite Client API as a proxy to DataCite. In this case, requests made to the Client API are captured and passed on to DataCite for processing. The application will interact with the DataCite Client API exactly as if it were interacting directly with the DataCite API, with the only difference being the change to the base endpoint URL.

For example, the Australian Data Archive (ADA) successfully uses the Australian National Data Service (ANDS) API (a proxy for DataCite) to mint their DOIs through Dataverse using a ``doi.baseurlstring`` value of "https://researchdata.ands.org.au/api/doi/datacite" as documented at https://documentation.ands.org.au/display/DOC/ANDS+DataCite+Client+API . As ADA did for ANDS DOI minting, any DOI provider (and their corresponding DOI configuration parameters) other than DataCite must be tested with Dataverse to establish whether or not it will function properly.

Out of the box, Dataverse is configured to use a test MDS DataCite base URL string. You can delete it like this:

``./asadmin delete-jvm-options '-Ddoi.baseurlstring=https\://mds.test.datacite.org'``

Then, to switch to production DataCite, you can issue the following command:

``./asadmin create-jvm-options '-Ddoi.baseurlstring=https\://mds.datacite.org'``

See also these related database settings below:

- :ref:`:DoiProvider`
- :ref:`:Protocol`
- :ref:`:Authority`
- :ref:`:Shoulder`

.. _doi.dataciterestapiurlstring:

doi.dataciterestapiurlstring
++++++++++++++++++++++++++++

This configuration option affects the ``updateCitationsForDataset`` API endpoint documented under :ref:`MDC-updateCitationsForDataset` in the Admin Guide as well as the /pids/* API.

As of this writing, "https://api.datacite.org" (DataCite) and "https://api.test.datacite.org" (DataCite Testing) are the main valid values.

Out of the box, Dataverse is configured to use a test DataCite REST API base URL string. You can delete it like this:

``./asadmin delete-jvm-options '-Ddoi.dataciterestapiurlstring=https\://api.test.datacite.org'``

Then, to switch to production DataCite, you can issue the following command:

``./asadmin create-jvm-options '-Ddoi.dataciterestapiurlstring=https\://api.datacite.org'``

For backward compatibility, if this option is not defined, the value of '-Ddoi.mdcbaseurlstring' is used if set. If not the default used is "https\://api.datacite.org:.

See also these related database settings below:

- :ref:`:MDCLogPath`
- :ref:`:DisplayMDCMetrics`

.. _doi.username:

doi.username
++++++++++++

Used in conjuction with ``doi.baseurlstring``.

Once you have a username from your provider, you can enter it like this:

``./asadmin create-jvm-options '-Ddoi.username=YOUR_USERNAME_HERE'``

.. _doi.password:

doi.password
++++++++++++

Used in conjuction with ``doi.baseurlstring``.

Once you have a password from your provider, you can enter it like this:

``./asadmin create-jvm-options '-Ddoi.password=YOUR_PASSWORD_HERE'``

.. _dataverse.handlenet.admcredfile:

dataverse.handlenet.admcredfile
+++++++++++++++++++++++++++++++

If you're using **handles**, this JVM setting configures access credentials so your dataverse can talk to your Handle.Net server. This is the private key generated during Handle.Net server installation. Typically the full path is set to ``handle/svr_1/admpriv.bin``. Please refer to `Handle.Net's documentation <http://handle.net/hnr_documentation.html>`_ for more info.

.. _dataverse.handlenet.admprivphrase:

dataverse.handlenet.admprivphrase
+++++++++++++++++++++++++++++++++
This JVM setting is also part of **handles** configuration. The Handle.Net installer lets you choose whether to encrypt the admcredfile private key or not. If you do encrypt it, this is the pass phrase that it's encrypted with.

.. _dataverse.handlenet.index:

dataverse.handlenet.index
+++++++++++++++++++++++++
If you want to use different index than the default 300

.. _dataverse.timerServer:

dataverse.timerServer
+++++++++++++++++++++

This JVM option is only relevant if you plan to run multiple Payara servers for redundancy. Only one Payara server can act as the dedicated timer server and for details on promoting or demoting a Payara server to handle this responsibility, see :doc:`/admin/timers`.

.. _dataverse.lang.directory:

dataverse.lang.directory
++++++++++++++++++++++++

This JVM option is used to configure the path where all the language specific property files are to be stored.  If this option is set then the English property file must be present in the path along with any other language property file. You can download language property files from https://github.com/GlobalDataverseCommunityConsortium/dataverse-language-packs

``./asadmin create-jvm-options '-Ddataverse.lang.directory=PATH_LOCATION_HERE'``

If this value is not set, by default, a Dataverse installation will read the English language property files from the Java Application.

See also :ref:`i18n`.

dataverse.files.hide-schema-dot-org-download-urls
+++++++++++++++++++++++++++++++++++++++++++++++++

Please note that this setting is experimental.

By default, download URLs to files will be included in Schema.org JSON-LD output. To prevent these URLs from being included in the output, set ``dataverse.files.hide-schema-dot-org-download-urls`` to true as in the example below.

``./asadmin create-jvm-options '-Ddataverse.files.hide-schema-dot-org-download-urls=true'``

Please note that there are other reasons why download URLs may not be included for certain files such as if a guestbook entry is required or if the file is restricted.

For more on Schema.org JSON-LD, see the :doc:`/admin/metadataexport` section of the Admin Guide.

.. _useripaddresssourceheader:

dataverse.useripaddresssourceheader
+++++++++++++++++++++++++++++++++++

**Make sure** to read the section about the :ref:`Security Implications 
<user-ip-addresses-proxy-security>` of using this option earlier in the guide!

If set, specifies an HTTP Header such as X-Forwarded-For to use to retrieve the user's IP address. For example:

``./asadmin create-jvm-options '-Ddataverse.useripaddresssourceheader=X-Forwarded-For'``

This setting is useful in cases such as running Dataverse behind load balancers where the default option of getting the Remote Address from the servlet isn't correct (e.g. it would be the load balancer IP address). Note that unless your installation always sets the header you configure here, this could be used as a way to spoof the user's address. Allowed values are: 

.. code::

	"X-Forwarded-For",
	"Proxy-Client-IP",
	"WL-Proxy-Client-IP",
	"HTTP_X_FORWARDED_FOR",
	"HTTP_X_FORWARDED",
	"HTTP_X_CLUSTER_CLIENT_IP",
	"HTTP_CLIENT_IP",
	"HTTP_FORWARDED_FOR",
	"HTTP_FORWARDED",
	"HTTP_VIA",
	"REMOTE_ADDR"

.. _:ApplicationServerSettings:

Application Server Settings
---------------------------

http.request-timeout-seconds
++++++++++++++++++++++++++++

To facilitate large file upload and download, the Dataverse installer bumps the Payara **server-config.network-config.protocols.protocol.http-listener-1.http.request-timeout-seconds** setting from its default 900 seconds (15 minutes) to 1800 (30 minutes). Should you wish to shorten or lengthen this window, issue for example:

``./asadmin set server-config.network-config.protocols.protocol.http-listener-1.http.request-timeout-seconds=3600``

and restart Payara to apply your change.

.. _database-settings:

Database Settings
-----------------

These settings are stored in the ``setting`` database table but can be read and modified via the "admin" endpoint of the :doc:`/api/native-api` for easy scripting.

The most commonly used configuration options are listed first.

The pattern you will observe in curl examples below is that an HTTP ``PUT`` is used to add or modify a setting. If you perform an HTTP ``GET`` (the default when using curl), the output will contain the value of the setting, if it has been set. You can also do a ``GET`` of all settings with ``curl http://localhost:8080/api/admin/settings`` which you may want to pretty-print by piping the output through a tool such as jq by appending ``| jq .``. If you want to remove a setting, use an HTTP ``DELETE`` such as ``curl -X DELETE http://localhost:8080/api/admin/settings/:GuidesBaseUrl`` .

.. _:BlockedApiPolicy:

:BlockedApiPolicy
+++++++++++++++++

``:BlockedApiPolicy`` affects access to the list of API endpoints defined in :ref:`:BlockedApiEndpoints`.

Out of the box, ``localhost-only`` is the default policy, as mentioned in :ref:`blocking-api-endpoints`. The other valid options are the following.

- localhost-only: Allow from localhost.
- unblock-key: Require a key defined in :ref:`:BlockedApiKey`.
- drop: Disallow the blocked endpoints completely.

Below is an example of setting ``localhost-only``.

``curl -X PUT -d localhost-only http://localhost:8080/api/admin/settings/:BlockedApiPolicy``

.. _:BlockedApiEndpoints:

:BlockedApiEndpoints
++++++++++++++++++++

A comma-separated list of API endpoints to be blocked. For a standard production installation, the installer blocks both "admin" and "builtin-users" by default per the security section above:

``curl -X PUT -d "admin,builtin-users" http://localhost:8080/api/admin/settings/:BlockedApiEndpoints``

See the :ref:`list-of-dataverse-apis` for lists of API endpoints.

.. _:BlockedApiKey:

:BlockedApiKey
++++++++++++++

``:BlockedApiKey`` is used in conjunction with :ref:`:BlockedApiEndpoints` and :ref:`:BlockedApiPolicy` and will not be enabled unless the policy is set to ``unblock-key`` as demonstrated below. Please note that the order is significant. You should set ``:BlockedApiKey`` first to prevent locking yourself out.

``curl -X PUT -d s3kretKey http://localhost:8080/api/admin/settings/:BlockedApiKey``

``curl -X PUT -d unblock-key http://localhost:8080/api/admin/settings/:BlockedApiPolicy``

Now that ``:BlockedApiKey`` has been enabled, blocked APIs can be accessed using the query parameter ``unblock-key=theKeyYouChose`` as in the example below.

``curl https://demo.dataverse.org/api/admin/settings?unblock-key=theKeyYouChose``

.. _BuiltinUsers.KEY:

BuiltinUsers.KEY
++++++++++++++++

The key required to create users via API as documented at :doc:`/api/native-api`. Unlike other database settings, this one doesn't start with a colon.

``curl -X PUT -d builtInS3kretKey http://localhost:8080/api/admin/settings/BuiltinUsers.KEY``

:SearchApiRequiresToken
+++++++++++++++++++++++

In Dataverse 4.7 and lower, the :doc:`/api/search` required an API token, but as of Dataverse 4.7.1 this is no longer the case. If you prefer the old behavior of requiring API tokens to use the Search API, set ``:SearchApiRequiresToken`` to ``true``.

``curl -X PUT -d true http://localhost:8080/api/admin/settings/:SearchApiRequiresToken``

.. _systemEmail:

:SystemEmail
++++++++++++

This is the email address that "system" emails are sent from such as password reset links. Your Dataverse installation will not send mail without this setting in place.

``curl -X PUT -d 'LibraScholar SWAT Team <support@librascholar.edu>' http://localhost:8080/api/admin/settings/:SystemEmail``

Note that only the email address is required, which you can supply without the ``<`` and ``>`` signs, but if you include the text, it's the way to customize the name of your support team, which appears in the "from" address in emails as well as in help text in the UI.

Please note that if you're having any trouble sending email, you can refer to "Troubleshooting" under :doc:`installation-main`.

:HomePageCustomizationFile
++++++++++++++++++++++++++

See :ref:`Branding Your Installation` above.

:LogoCustomizationFile
++++++++++++++++++++++

See :ref:`Branding Your Installation` above.

:HeaderCustomizationFile
++++++++++++++++++++++++

See :ref:`Branding Your Installation` above.

:DisableRootDataverseTheme
++++++++++++++++++++++++++

See :ref:`Branding Your Installation` above.

:FooterCustomizationFile
++++++++++++++++++++++++

See :ref:`Branding Your Installation` above.

:StyleCustomizationFile
+++++++++++++++++++++++

See :ref:`Branding Your Installation` above.

:WebAnalyticsCode
+++++++++++++++++

See :ref:`Web-Analytics-Code` above.

:FooterCopyright
++++++++++++++++

By default the footer says "Copyright © [YYYY]" but you can add text after the year, as in the example below.

``curl -X PUT -d ", Your Institution" http://localhost:8080/api/admin/settings/:FooterCopyright``

.. _:DoiProvider:

:DoiProvider
++++++++++++

As of this writing "DataCite" and "EZID" are the only valid options for production installations. Developers using version 4.10 and above are welcome to use the keyword "FAKE" to configure a non-production installation with an non-resolving, in-code provider, which will basically short-circuit the DOI publishing process. ``:DoiProvider`` is only needed if you are using DOI.

``curl -X PUT -d DataCite http://localhost:8080/api/admin/settings/:DoiProvider``

This setting relates to the ``:Protocol``, ``:Authority``, ``:Shoulder``, and ``:IdentifierGenerationStyle`` database settings below as well as the following JVM options:

- :ref:`doi.baseurlstring`
- :ref:`doi.username`
- :ref:`doi.password`

.. _:Protocol:

:Protocol
+++++++++

As of this writing "doi" and "hdl" are the only valid option for the protocol for a persistent ID.

``curl -X PUT -d doi http://localhost:8080/api/admin/settings/:Protocol``

.. _:Authority:

:Authority
++++++++++

Use the authority assigned to you by your DoiProvider or HandleProvider.

Please note that the authority cannot have a slash ("/") in it.

``curl -X PUT -d 10.xxxx http://localhost:8080/api/admin/settings/:Authority``

.. _:Shoulder:

:Shoulder
++++++++++++

Out of the box, the DOI shoulder is set to "FK2/" but this is for testing only! When you apply for your DOI namespace, you may have requested a shoulder. The following is only an example and a trailing slash is optional.

``curl -X PUT -d "MyShoulder/" http://localhost:8080/api/admin/settings/:Shoulder``

.. _:IdentifierGenerationStyle:

:IdentifierGenerationStyle
++++++++++++++++++++++++++

By default, Dataverse generates a random 6 character string, pre-pended by the Shoulder if set, to use as the identifier
for a Dataset. Set this to ``sequentialNumber`` to use sequential numeric values
instead (again pre-pended by the Shoulder if set). (the assumed default setting is ``randomString``).
In addition to this setting, a database sequence must be created in the database.
We provide the script below (downloadable :download:`here </_static/util/createsequence.sql>`).
You may need to make some changes to suit your system setup, see the comments for more information:

.. literalinclude:: ../_static/util/createsequence.sql

Note that the SQL above is Postgres-specific. If necessary, it can be reimplemented
in any other SQL flavor - the standard JPA code in the application simply expects
the database to have a saved function ("stored procedure") named ``generateIdentifierAsSequentialNumber``
with the single return argument ``identifier``.

Please note that ``:IdentifierGenerationStyle`` also plays a role for the "identifier" for files. See the section on ``:DataFilePIDFormat`` below for more details.

.. _:DataFilePIDFormat:

:DataFilePIDFormat
++++++++++++++++++

This setting controls the way that the "identifier" component of a file's persistent identifier (PID) relates to the PID of its "parent" dataset.

By default the identifier for a file is dependent on its parent dataset. For example, if the identifier of a dataset is "TJCLKP", the identifier for a file within that dataset will consist of the parent dataset's identifier followed by a slash ("/"), followed by a random 6 character string, yielding "TJCLKP/MLGWJO". Identifiers in this format are what you should expect if you leave ``:DataFilePIDFormat`` undefined or set it to ``DEPENDENT`` and have not changed the ``:IdentifierGenerationStyle`` setting from its default.

Alternatively, the identifier for File PIDs can be configured to be independent of Dataset PIDs using the setting "``INDEPENDENT``". In this case, file PIDs will not contain the PIDs of their parent datasets, and their PIDs will be generated the exact same way that datasets' PIDs are, based on the ``:IdentifierGenerationStyle`` setting described above (random 6 character strings or sequential numbers, pre-pended by any shoulder).

The chart below shows examples from each possible combination of parameters from the two settings. ``:IdentifierGenerationStyle`` can be either ``randomString`` (the default) or ``sequentialNumber`` and ``:DataFilePIDFormat`` can be either ``DEPENDENT`` (the default) or ``INDEPENDENT``. In the examples below the "identifier" for the dataset is "TJCLKP" for "randomString" and "100001" for "sequentialNumber".

+-----------------+---------------+------------------+
|                 | randomString  | sequentialNumber |
|                 |               |                  |
+=================+===============+==================+
| **DEPENDENT**   | TJCLKP/MLGWJO |     100001/1     |
+-----------------+---------------+------------------+
| **INDEPENDENT** |    MLGWJO     |      100002      |
+-----------------+---------------+------------------+

As seen above, in cases where ``:IdentifierGenerationStyle`` is set to *sequentialNumber* and ``:DataFilePIDFormat`` is set to *DEPENDENT*, each file within a dataset will be assigned a number *within* that dataset starting with "1".

Otherwise, if ``:DataFilePIDFormat`` is set to *INDEPENDENT*, then each file will be assigned a PID with the next number in the overall sequence, regardless of what dataset it is in. If the file is created after a dataset with the PID 100001, then the file will be assigned the PID 100002. This option is functional, but it is not a recommended use case.

Note that in either case, when using the ``sequentialNumber`` option, datasets and files share the same database sequence that was created as part of the setup described in ``:IdentifierGenerationStyle`` above.

.. _:FilePIDsEnabled:

:FilePIDsEnabled
++++++++++++++++

Toggles publishing of file-based PIDs for the entire installation. By default this setting is absent and Dataverse assumes it to be true. If enabled, the registration will be performed asynchronously (in the background) during publishing of a dataset.

If you don't want to register file-based PIDs for your installation, set:

``curl -X PUT -d 'false' http://localhost:8080/api/admin/settings/:FilePIDsEnabled``

Note: File-level PID registration was added in 4.9; it could not be disabled until version 4.9.3.

.. _:IndependentHandleService:

:IndependentHandleService
+++++++++++++++++++++++++++

Specific for Handle PIDs. Set this setting to true if you want to use a Handle service which is setup to work 'independently' (No communication with the Global Handle Registry).
By default this setting is absent and Dataverse assumes it to be false.

``curl -X PUT -d 'true' http://localhost:8080/api/admin/settings/:IndependentHandleService``

.. _:FileValidationOnPublishEnabled:

:FileValidationOnPublishEnabled
+++++++++++++++++++++++++++++++

Toggles validation of the physical files in the dataset when it's published, by recalculating the checksums and comparing against the values stored in the DataFile table. By default this setting is absent and Dataverse assumes it to be true. If enabled, the validation will be performed asynchronously, similarly to how we handle assigning persistent identifiers to datafiles, with the dataset locked for the duration of the publishing process. 

If you don't want the datafiles to be validated on publish, set:

``curl -X PUT -d 'false' http://localhost:8080/api/admin/settings/:FileValidationOnPublishEnabled``


:ApplicationTermsOfUse
++++++++++++++++++++++

Upload an default language HTML file containing the Terms of Use to be displayed at sign up. Supported HTML tags are listed under the :doc:`/user/dataset-management` section of the User Guide.

``curl -X PUT -d@/tmp/apptou.html http://localhost:8080/api/admin/settings/:ApplicationTermsOfUse``

To upload a language specific Terms of Use file,

``curl -X PUT -d@/tmp/apptou_fr.html http://localhost:8080/api/admin/settings/:ApplicationTermsOfUse/lang/fr``

To delete language specific option,

``curl -X DELETE http://localhost:8080/api/admin/settings/:ApplicationTermsOfUse/lang/fr``

:ApplicationPrivacyPolicyUrl
++++++++++++++++++++++++++++

Specify a URL where users can read your Privacy Policy, linked from the bottom of the page.

``curl -X PUT -d https://dataverse.org/best-practices/harvard-dataverse-privacy-policy http://localhost:8080/api/admin/settings/:ApplicationPrivacyPolicyUrl``

:ApiTermsOfUse
++++++++++++++

Specify a URL where users can read your API Terms of Use.
API users can retrieve this URL from the SWORD Service Document or the :ref:`info` section of our :doc:`/api/native-api` documentation.

``curl -X PUT -d https://dataverse.org/best-practices/harvard-api-tou http://localhost:8080/api/admin/settings/:ApiTermsOfUse``


.. _:ExcludeEmailFromExport:

:ExcludeEmailFromExport
+++++++++++++++++++++++

See also :ref:`Privacy Considerations <PrivacyConsiderations>` above.

Set ``:ExcludeEmailFromExport`` to prevent email addresses for contacts from being exposed in XML or JSON representations of dataset and dataverse metadata. For a list exported formats such as DDI, see the :doc:`/admin/metadataexport` section of the Admin Guide.

``curl -X PUT -d true http://localhost:8080/api/admin/settings/:ExcludeEmailFromExport``

Note: After making a change to this setting, a reExportAll needs to be run before the changes will be reflected in the exports:

``curl http://localhost:8080/api/admin/metadata/reExportAll``

This will *force* a re-export of every published, local dataset, regardless of whether it has already been exported or not.

:NavbarAboutUrl
+++++++++++++++

Set ``NavbarAboutUrl`` to a fully-qualified URL which will be used for the "About" link in the navbar.

Note: The "About" link will not appear in the navbar until this option is set.

``curl -X PUT -d http://dataverse.example.edu http://localhost:8080/api/admin/settings/:NavbarAboutUrl``

:NavbarGuidesUrl
++++++++++++++++

Set ``:NavbarGuidesUrl`` to a fully-qualified URL which will be used for the "User Guide" link in the navbar.

Note: by default, the URL is composed from the settings ``:GuidesBaseUrl`` and ``:GuidesVersion`` below.

``curl -X PUT -d http://example.edu/fancy-dataverse-guide http://localhost:8080/api/admin/settings/:NavbarGuidesUrl``

:GuidesBaseUrl
++++++++++++++

Set ``GuidesBaseUrl`` to override the default value "http://guides.dataverse.org". If you are interested in writing your own version of the guides, you may find the :doc:`/developers/documentation` section of the Developer Guide helpful.

``curl -X PUT -d http://dataverse.example.edu http://localhost:8080/api/admin/settings/:GuidesBaseUrl``

:GuidesVersion
++++++++++++++

Set ``:GuidesVersion`` to override the version number in the URL of guides. For example, rather than http://guides.dataverse.org/en/4.6/user/account.html the version is overriden to http://guides.dataverse.org/en/1234-new-feature/user/account.html in the example below:

``curl -X PUT -d 1234-new-feature http://localhost:8080/api/admin/settings/:GuidesVersion``

:NavbarSupportUrl
+++++++++++++++++
Set ``:NavbarSupportUrl`` to a fully-qualified URL which will be used for the "Support" link in the navbar.

Note that this will override the default behaviour for the "Support" menu option, which is to display the dataverse 'feedback' dialog.

``curl -X PUT -d http://dataverse.example.edu/supportpage.html http://localhost:8080/api/admin/settings/:NavbarSupportUrl``

:MetricsUrl
+++++++++++

Make the metrics component on the root dataverse a clickable link to a website where you present metrics on your Dataverse installation, perhaps one of the community-supported tools mentioned in the :doc:`/admin/reporting-tools-and-queries` section of the Admin Guide.

``curl -X PUT -d http://metrics.dataverse.example.edu http://localhost:8080/api/admin/settings/:MetricsUrl``

.. _:MaxFileUploadSizeInBytes:

:MaxFileUploadSizeInBytes
+++++++++++++++++++++++++

This setting controls the maximum size of uploaded files.
- To have one limit for all stores, set `MaxFileUploadSizeInBytes` to "2147483648", for example, to limit the size of files uploaded to 2 GB:

``curl -X PUT -d 2147483648 http://localhost:8080/api/admin/settings/:MaxFileUploadSizeInBytes``

- To have limits per store with an optional default, use a serialized json object for the value of `MaxFileUploadSizeInBytes` with an entry per store, as in the following example, which maintains a 2 GB default and adds higher limits for stores with ids "fileOne" and "s3".

``curl -X PUT -d '{"default":"2147483648","fileOne":"4000000000","s3":"8000000000"}' http://localhost:8080/api/admin/settings/:MaxFileUploadSizeInBytes``

Notes:

- For SWORD, this size is limited by the Java Integer.MAX_VALUE of 2,147,483,647. (see: https://github.com/IQSS/dataverse/issues/2169)

- If the MaxFileUploadSizeInBytes is NOT set, uploads, including SWORD may be of unlimited size.

- For larger file upload sizes, you may need to configure your reverse proxy timeout. If using apache2 (httpd) with Shibboleth, add a timeout to the ProxyPass defined in etc/httpd/conf.d/ssl.conf (which is described in the :doc:`/installation/shibboleth` setup).



:ZipDownloadLimit
+++++++++++++++++

For performance reasons, Dataverse will only create zip files on the fly up to 100 MB but the limit can be increased. Here's an example of raising the limit to 1 GB:

``curl -X PUT -d 1000000000 http://localhost:8080/api/admin/settings/:ZipDownloadLimit``

:TabularIngestSizeLimit
+++++++++++++++++++++++

Threshold in bytes for limiting whether or not "ingest" it attempted for tabular files (which can be resource intensive). For example, with the below in place, files greater than 2 GB in size will not go through the ingest process:

``curl -X PUT -d 2000000000 http://localhost:8080/api/admin/settings/:TabularIngestSizeLimit``

(You can set this value to 0 to prevent files from being ingested at all.)

You can override this global setting on a per-format basis for the following formats:

- DTA
- POR
- SAV
- Rdata
- CSV
- XLSX

For example, if you want your installation of Dataverse to not attempt to ingest Rdata files larger than 1 MB, use this setting:

``curl -X PUT -d 1000000 http://localhost:8080/api/admin/settings/:TabularIngestSizeLimit:Rdata``

:ZipUploadFilesLimit
++++++++++++++++++++

Limit the number of files in a zip that Dataverse will accept. In the absence of this setting, Dataverse defaults to a limit of 1,000 files per zipfile.

``curl -X PUT -d 2048 http://localhost:8080/api/admin/settings/:ZipUploadFilesLimit``

:SolrHostColonPort
++++++++++++++++++

By default Dataverse will attempt to connect to Solr on port 8983 on localhost. Use this setting to change the hostname or port. You must restart Payara after making this change.

``curl -X PUT -d localhost:8983 http://localhost:8080/api/admin/settings/:SolrHostColonPort``

:SolrFullTextIndexing
+++++++++++++++++++++

Whether or not to index the content of files such as PDFs. The default is false.

``curl -X PUT -d true http://localhost:8080/api/admin/settings/:SolrFullTextIndexing``

:SolrMaxFileSizeForFullTextIndexing
+++++++++++++++++++++++++++++++++++

If ``:SolrFullTextIndexing`` is set to true, the content of files of any size will be indexed. To set a limit in bytes for which files to index in this way:

``curl -X PUT -d 314572800 http://localhost:8080/api/admin/settings/:SolrMaxFileSizeForFullTextIndexing``

:SignUpUrl
++++++++++

The relative path URL to which users will be sent for signup. The default setting is below.

``curl -X PUT -d '/dataverseuser.xhtml?editMode=CREATE' http://localhost:8080/api/admin/settings/:SignUpUrl``

:LoginSessionTimeout
++++++++++++++++++++

Session timeout (in minutes) for logged-in users. The default is 8 hours (480 minutes). For the anonymous user sessions, the timeout is set to the default value, configured in the web.xml file of the Dataverse application.

In the example below we reduce the timeout to 4 hours:

``curl -X PUT -d 240 http://localhost:8080/api/admin/settings/:LoginSessionTimeout``

:TwoRavensUrl
+++++++++++++

The ``:TwoRavensUrl`` option is no longer valid. See :doc:`r-rapache-tworavens` and the :doc:`/admin/external-tools` section of the Admin Guide.

:TwoRavensTabularView
+++++++++++++++++++++

The ``:TwoRavensTabularView`` option is no longer valid. See :doc:`r-rapache-tworavens` and the :doc:`/admin/external-tools` section of the Admin Guide.

:GeoconnectCreateEditMaps
+++++++++++++++++++++++++

Set ``GeoconnectCreateEditMaps`` to true to allow the user to create maps using Geoconnect. This boolean enables the map configure tool option for a data file and the ingest to create a shape file.

``curl -X PUT -d true http://localhost:8080/api/admin/settings/:GeoconnectCreateEditMaps``

:GeoconnectViewMaps
+++++++++++++++++++

Set ``GeoconnectViewMaps`` to true to allow a user to view existing maps. This boolean enables the map explore tool option for a data file.

``curl -X PUT -d true http://localhost:8080/api/admin/settings/:GeoconnectViewMaps``

.. _:DatasetPublishPopupCustomText:

:DatasetPublishPopupCustomText
++++++++++++++++++++++++++++++

Set custom text a user will view when publishing a dataset. Note that this text is exposed via the "Info" endpoint of the :doc:`/api/native-api`.

``curl -X PUT -d "Deposit License Requirements" http://localhost:8080/api/admin/settings/:DatasetPublishPopupCustomText``

If you have a long text string, you can upload it as a file as in the example below.

``curl -X PUT --upload-file /tmp/long.txt http://localhost:8080/api/admin/settings/:DatasetPublishPopupCustomText``

:DatasetPublishPopupCustomTextOnAllVersions
+++++++++++++++++++++++++++++++++++++++++++

Set whether a user will see the custom text when publishing all versions of a dataset

``curl -X PUT -d true http://localhost:8080/api/admin/settings/:DatasetPublishPopupCustomTextOnAllVersions``

:SearchHighlightFragmentSize
++++++++++++++++++++++++++++

Set ``SearchHighlightFragmentSize`` to override the default value of 100 from https://wiki.apache.org/solr/HighlightingParameters#hl.fragsize . In practice, a value of "320" seemed to fix the issue at https://github.com/IQSS/dataverse/issues/2191

``curl -X PUT -d 320 http://localhost:8080/api/admin/settings/:SearchHighlightFragmentSize``

:ScrubMigrationData
+++++++++++++++++++

Allow for migration of non-conformant data (especially dates) from DVN 3.x to Dataverse 4.

:MinutesUntilConfirmEmailTokenExpires
+++++++++++++++++++++++++++++++++++++

The duration in minutes before "Confirm Email" URLs expire. The default is 1440 minutes (24 hours).  See also the :doc:`/admin/user-administration` section of our Admin Guide.

.. _conf-default-auth-provider:

:DefaultAuthProvider
++++++++++++++++++++

If you have enabled Shibboleth and/or one or more OAuth providers, you may wish to make one of these authentication providers the default when users visit the Log In page. If unset, this will default to ``builtin`` but these valid options (depending if you've done the setup described in the :doc:`shibboleth` or :doc:`oauth2` sections) are:

- ``builtin``
- ``shib``
- ``orcid``
- ``github``
- ``google``

Here is an example of setting the default auth provider back to ``builtin``:

``curl -X PUT -d builtin http://localhost:8080/api/admin/settings/:DefaultAuthProvider``

.. _conf-allow-signup:

:AllowSignUp
++++++++++++

Set to false to disallow local accounts from being created. See also the sections on :doc:`shibboleth` and :doc:`oauth2`.

``curl -X PUT -d 'false' http://localhost:8080/api/admin/settings/:AllowSignUp``

:FileFixityChecksumAlgorithm
++++++++++++++++++++++++++++

Dataverse calculates checksums for uploaded files so that users can determine if their file was corrupted via upload or download. This is sometimes called "file fixity": https://en.wikipedia.org/wiki/File_Fixity

The default checksum algorithm used is MD5 and should be sufficient for establishing file fixity. "SHA-1", "SHA-256" and "SHA-512" are alternate values for this setting. For example:

``curl -X PUT -d 'SHA-512' http://localhost:8080/api/admin/settings/:FileFixityChecksumAlgorithm``

The fixity algorithm used on existing files can be changed by a superuser using the API. An optional query parameter (num) can be used to limit the number of updates attempted.
The API call will only update the algorithm and checksum for a file if the existing checksum can be validated against the file.
Statistics concerning the updates are returned in the response to the API call with details in the log.

``curl http://localhost:8080/api/admin/updateHashValues/{alg}``
``curl http://localhost:8080/api/admin/updateHashValues/{alg}?num=1``

.. _:PVMinLength:

:PVMinLength
++++++++++++

Password policy setting for builtin user accounts: a password's minimum valid character length. The default is 6.

``curl -X PUT -d 6 http://localhost:8080/api/admin/settings/:PVMinLength``


.. _:PVMaxLength:

:PVMaxLength
++++++++++++

Password policy setting for builtin user accounts: a password's maximum valid character length.

``curl -X PUT -d 0 http://localhost:8080/api/admin/settings/:PVMaxLength``


.. _:PVNumberOfConsecutiveDigitsAllowed:

:PVNumberOfConsecutiveDigitsAllowed
+++++++++++++++++++++++++++++++++++

By default, passwords can contain an unlimited number of digits in a row. However, if your password policy specifies otherwise (e.g. only four digits in a row are allowed), then you can issue the following curl command to set the number of consecutive digits allowed (this example uses 4):

``curl -X PUT -d 4 http://localhost:8080/api/admin/settings/:PVNumberOfConsecutiveDigitsAllowed``

.. _:PVCharacterRules:

:PVCharacterRules
+++++++++++++++++

Password policy setting for builtinuser accounts: dictates which types of characters can be required in a password. This setting goes hand-in-hand with :ref:`:PVNumberOfCharacteristics`. The default setting contains two rules:

- one letter
- one digit

The default setting above is equivalent to specifying "Alphabetical:1,Digit:1".

By specifying "UpperCase:1,LowerCase:1,Digit:1,Special:1", for example, you can put the following four rules in place instead:

- one uppercase letter
- one lowercase letter
- one digit
- one special character

If you have implemented 4 different character rules in this way, you can also optionally increase ``:PVNumberOfCharacteristics`` to as high as 4. However, please note that ``:PVNumberOfCharacteristics`` cannot be set to a number higher than the number of character rules or you will see the error, "Number of characteristics must be <= to the number of rules".

Also note that the Alphabetical setting should not be used in tandem with the UpperCase or LowerCase settings. The Alphabetical setting encompasses both of those more specific settings, so using it with them will cause your password policy to be unnecessarily confusing, and potentially easier to bypass.

``curl -X PUT -d 'UpperCase:1,LowerCase:1,Digit:1,Special:1' http://localhost:8080/api/admin/settings/:PVCharacterRules``

``curl -X PUT -d 3 http://localhost:8080/api/admin/settings/:PVNumberOfCharacteristics``

.. _:PVNumberOfCharacteristics:

:PVNumberOfCharacteristics
++++++++++++++++++++++++++

Password policy setting for builtin user accounts: the number indicates how many of the character rules defined by ``:PVCharacterRules`` are required as part of a password. The default is 2. ``:PVNumberOfCharacteristics`` cannot be set to a number higher than the number of rules or you will see the error, "Number of characteristics must be <= to the number of rules".

``curl -X PUT -d 2 http://localhost:8080/api/admin/settings/:PVNumberOfCharacteristics``


.. _:PVDictionaries:

:PVDictionaries
+++++++++++++++

Password policy setting for builtin user accounts: set a comma separated list of dictionaries containing words that cannot be used in a user password. ``/usr/share/dict/words`` is suggested and shown modified below to not contain words 3 letters or less. You are free to choose a different dictionary. By default, no dictionary is checked.

``DIR=THE_PATH_YOU_WANT_YOUR_DICTIONARY_TO_RESIDE``
``sed '/^.\{,3\}$/d' /usr/share/dict/words > $DIR/pwdictionary``
``curl -X PUT -d "$DIR/pwdictionary" http://localhost:8080/api/admin/settings/:PVDictionaries``


.. _:PVGoodStrength:

:PVGoodStrength
+++++++++++++++

Password policy setting for builtin user accounts: passwords of equal or greater character length than the :PVGoodStrength setting are always valid, regardless of other password constraints.

``curl -X PUT -d 20 http://localhost:8080/api/admin/settings/:PVGoodStrength``

Recommended setting: 20.

.. _:PVCustomPasswordResetAlertMessage:

:PVCustomPasswordResetAlertMessage
++++++++++++++++++++++++++++++++++

Changes the default info message displayed when a user is required to change their password on login. The default is:

``{0} Reset Password{1} – Our password requirements have changed. Please pick a strong password that matches the criteria below.``

Where the {0} and {1} denote surrounding HTML **bold** tags. It's recommended to put a single space before your custom message for better appearance (as in the default message above). Including the {0} and {1} to bolden part of your message is optional.

Customize the message using the following curl command's syntax:

``curl -X PUT -d '{0} Action Required:{1} Your current password does not meet all requirements. Please enter a new password meeting the criteria below.' http://localhost:8080/api/admin/settings/:PVCustomPasswordResetAlertMessage``

:ShibPassiveLoginEnabled
++++++++++++++++++++++++

Set ``:ShibPassiveLoginEnabled`` to true to enable passive login for Shibboleth. When this feature is enabled, an additional Javascript file (isPassive.js) will be loaded for every page. It will generate a passive login request to your Shibboleth SP when an anonymous user navigates to the site. A cookie named "_check_is_passive_dv" will be created to keep track of whether or not a passive login request has already been made for the user.

This implementation follows the example on the Shibboleth wiki documentation page for the isPassive feature: https://wiki.shibboleth.net/confluence/display/SHIB2/isPassive

It is recommended that you configure additional error handling for your Service Provider if you enable passive login. A good way of doing this is described in the Shibboleth wiki documentation:

- *In your Service Provider 2.x shibboleth2.xml file, add redirectErrors="#THIS PAGE#" to the Errors element.*

You can set the value of "#THIS PAGE#" to the URL of your Dataverse homepage, or any other page on your site that is accessible to anonymous users and will have the isPassive.js file loaded.

``curl -X PUT -d true http://localhost:8080/api/admin/settings/:ShibPassiveLoginEnabled``

:ShibAffiliationAttribute
+++++++++++++++++++++++++

The Shibboleth affiliation attribute holds information about the affiliation of the user (e.g. "OU") and is read from the DiscoFeed at each login. ``:ShibAffiliationAttribute`` is a name of a Shibboleth attribute in the Shibboleth header which Dataverse will read from instead of DiscoFeed. If this value is not set or empty, Dataverse uses the DiscoFeed.

If the attribute is not yet set for the Shibboleth, please consult the Shibboleth Administrators at your institution. Typically it requires changing of the `/etc/shibboleth/attribute-map.xml` file by adding an attribute request, e.g.

.. code::

    <Attribute name="urn:oid:2.5.4.11" id="ou">
        <AttributeDecoder xsi:type="StringAttributeDecoder" caseSensitive="false"/>
    </Attribute>


In order to implement the change, you should restart Shibboleth and Apache2 services:

.. code::

	sudo service shibd restart
	sudo service apache2 restart

To check if the attribute is sent, you should log in again to Dataverse and check Shibboleth's transaction log. You should see something like this:

.. code::

	INFO Shibboleth-TRANSACTION [25]: Cached the following attributes with session (ID: _9d1f34c0733b61c0feb0ca7596ef43b2) for (applicationId: default) {
	INFO Shibboleth-TRANSACTION [25]: 	givenName (1 values)
	INFO Shibboleth-TRANSACTION [25]: 	ou (1 values)
	INFO Shibboleth-TRANSACTION [25]: 	sn (1 values)
	INFO Shibboleth-TRANSACTION [25]: 	eppn (1 values)
	INFO Shibboleth-TRANSACTION [25]: 	mail (1 values)
	INFO Shibboleth-TRANSACTION [25]: 	displayName (1 values)
	INFO Shibboleth-TRANSACTION [25]: }

If you see the attribue you requested in this list, you can set the attribute in Dataverse.

To set ``:ShibAffiliationAttribute``:

``curl -X PUT -d "ou" http://localhost:8080/api/admin/settings/:ShibAffiliationAttribute``

To delete ``:ShibAffiliationAttribute``:

``curl -X DELETE http://localhost:8080/api/admin/settings/:ShibAffiliationAttribute``

To check the current value of ``:ShibAffiliationAttribute``:

``curl -X GET http://localhost:8080/api/admin/settings/:ShibAffiliationAttribute``

:ShibAttributeCharacterSetConversionEnabled
+++++++++++++++++++++++++++++++++++++++++++

It seems that the application server (usually Glassfish or Payara) will interpret all Shibboleth attributes that come through AJP as ISO-8859-1, even if they where originally UTF-8.
To circumvent that, we re-encode all received Shibboleth attributes manually as UTF-8 by default. 
In the case you get garbled characters in Shibboleth-supplied fields (e.g. given name, surname, affiliation), you can disable this behaviour by setting ShibAttributeCharacterSetConversionEnabled to false:

``curl -X PUT -d false http://localhost:8080/api/admin/settings/:ShibAttributeCharacterSetConversionEnabled``

If you managed to get correct accented characters from shibboleth while this setting is _false_, please contact us with your application server and Shibboleth configuration!

.. _:ComputeBaseUrl:

:ComputeBaseUrl
+++++++++++++++

Set the base URL for the "Compute" button for a dataset.

``curl -X PUT -d 'https://giji.massopencloud.org/application/dataverse' http://localhost:8080/api/admin/settings/:ComputeBaseUrl``

.. _:CloudEnvironmentName:

:CloudEnvironmentName
+++++++++++++++++++++

Set the name of the cloud environment you've integrated with your Dataverse installation.

``curl -X PUT -d 'Massachusetts Open Cloud (MOC)' http://localhost:8080/api/admin/settings/:CloudEnvironmentName``

.. _:PublicInstall:

:PublicInstall
+++++++++++++++++++++

Setting an installation to public will remove the ability to restrict data files or datasets. This functionality of Dataverse will be disabled from your installation.

This is useful for specific cases where an installation's files are stored in public access. Because files stored this way do not obey Dataverse's file restrictions, users would still be able to access the files even when they're restricted. In these cases it's best to use :PublicInstall to disable the feature altogether.

``curl -X PUT -d true http://localhost:8080/api/admin/settings/:PublicInstall``

:DataCaptureModuleUrl
+++++++++++++++++++++

The URL for your Data Capture Module (DCM) installation. This component is experimental and can be downloaded from https://github.com/sbgrid/data-capture-module .

``curl -X PUT -d 'https://dcm.example.edu' http://localhost:8080/api/admin/settings/:DataCaptureModuleUrl``

:RepositoryStorageAbstractionLayerUrl
+++++++++++++++++++++++++++++++++++++

The URL for your Repository Storage Abstraction Layer (RSAL) installation. This component is experimental and can be downloaded from https://github.com/sbgrid/rsal .

``curl -X PUT -d 'https://rsal.example.edu' http://localhost:8080/api/admin/settings/:RepositoryStorageAbstractionLayerUrl``

.. _:UploadMethods:

:UploadMethods
++++++++++++++

This setting controls which upload methods are available to users of your installation of Dataverse. The following upload methods are available:

- ``native/http``: Corresponds to "Upload with HTTP via your browser" and APIs that use HTTP (SWORD and native).
- ``dcm/rsync+ssh``: Corresponds to "Upload with rsync+ssh via Data Capture Module (DCM)". A lot of setup is required, as explained in the :doc:`/developers/big-data-support` section of the Developer Guide.

Out of the box only ``native/http`` is enabled and will work without further configuration. To add multiple upload method, separate them using a comma like this:

``curl -X PUT -d 'native/http,dcm/rsync+ssh' http://localhost:8080/api/admin/settings/:UploadMethods``

You'll always want at least one upload method, so the easiest way to remove one of them is to simply ``PUT`` just the one you want, like this:

``curl -X PUT -d 'native/http' http://localhost:8080/api/admin/settings/:UploadMethods``

:DownloadMethods
++++++++++++++++

This setting is experimental and related to Repository Storage Abstraction Layer (RSAL).

``curl -X PUT -d 'rsal/rsync' http://localhost:8080/api/admin/settings/:DownloadMethods``

:GuestbookResponsesPageDisplayLimit
+++++++++++++++++++++++++++++++++++

Limit on how many guestbook entries to display on the guestbook-responses page. By default, only the 5000 most recent entries will be shown. Use the standard settings API in order to change the limit. For example, to set it to 10,000, make the following API call:

``curl -X PUT -d 10000 http://localhost:8080/api/admin/settings/:GuestbookResponsesPageDisplayLimit``

:CustomDatasetSummaryFields
+++++++++++++++++++++++++++

You can replace the default dataset metadata fields that are displayed above files table on the dataset page with a custom list separated by commas using the curl command below.

``curl http://localhost:8080/api/admin/settings/:CustomDatasetSummaryFields -X PUT -d 'producer,subtitle,alternativeTitle'``

You have to put the datasetFieldType name attribute in the :CustomDatasetSummaryFields setting for this to work.

:AllowApiTokenLookupViaApi
++++++++++++++++++++++++++

Dataverse 4.8.1 and below allowed API Token lookup via API but for better security this has been disabled by default. Set this to true if you really want the old behavior.

``curl -X PUT -d 'true' http://localhost:8080/api/admin/settings/:AllowApiTokenLookupViaApi``

:ProvCollectionEnabled
++++++++++++++++++++++

Enable the collection of provenance metadata on Dataverse via the provenance popup.

``curl -X PUT -d 'true' http://localhost:8080/api/admin/settings/:ProvCollectionEnabled``

:MetricsCacheTimeoutMinutes
+++++++++++++++++++++++++++

Sets how long a cached metrics result is used before re-running the query for a request. This timeout is only applied to some of the metrics that query the current state of the system, previous months queries are cached indefinitely. See :doc:`/api/metrics` for more info. The default timeout value is 7 days (10080 minutes).

``curl -X PUT -d 10080 http://localhost:8080/api/admin/settings/:MetricsCacheTimeoutMinutes``

.. _:MDCLogPath:

:MDCLogPath
+++++++++++

Sets the path where the raw Make Data Count logs are stored before being processed. If not set, no logs will be created for Make Data Count. See also the :doc:`/admin/make-data-count` section of the Admin Guide.

``curl -X PUT -d '/usr/local/payara5/glassfish/domains/domain1/logs' http://localhost:8080/api/admin/settings/:MDCLogPath``

.. _:DisplayMDCMetrics:

:DisplayMDCMetrics
++++++++++++++++++

``:DisplayMDCMetrics`` can be set to false to disable display of MDC metrics (e.g. to enable collection of MDC metrics for some period prior to completing the set-up of Counter and performing the other steps described in the :doc:`/admin/make-data-count` section of the Admin Guide).

``curl -X PUT -d 'false' http://localhost:8080/api/admin/settings/:DisplayMDCMetrics``

.. _:Languages:

:Languages
++++++++++

Sets which languages should be available. If there is more than one, a dropdown is displayed
in the header.

See :ref:`i18n` for a curl example and related settings.

:InheritParentRoleAssignments
+++++++++++++++++++++++++++++

``:InheritParentRoleAssignments`` can be set to a comma-separated list of role aliases or '*' (all) to cause newly created Dataverses to inherit the set of users and/or internal groups who have assignments for those role(s) on the parent Dataverse, i.e. those users/groups will be assigned the same role(s) on the new Dataverse (in addition to the creator of the new Dataverse having an admin role).
This can be helpful in situations where multiple organizations are sharing one Dataverse instance. The default, if ``:InheritParentRoleAssignments`` is not set is for the creator of the new Dataverse to be the only one assigned a role.

``curl -X PUT -d 'admin, curator' http://localhost:8080/api/admin/settings/:InheritParentRoleAssignments``
or
``curl -X PUT -d '*' http://localhost:8080/api/admin/settings/:InheritParentRoleAssignments``

:AllowCors
++++++++++

Allows Cross-Origin Resource sharing(CORS). By default this setting is absent and Dataverse assumes it to be true.

If you don’t want to allow CORS for your installation, set:

``curl -X PUT -d 'false' http://localhost:8080/api/admin/settings/:AllowCors``

:ChronologicalDateFacets
++++++++++++++++++++++++

Unlike other facets, those indexed by Date/Year are sorted chronologically by default, with the most recent value first. To have them sorted by number of hits, e.g. with the year with the most results first, set this to false 

If you don’t want date facets to be sorted chronologically, set:

``curl -X PUT -d 'false' http://localhost:8080/api/admin/settings/:ChronologicalDateFacets``

:CustomZipDownloadServiceUrl
++++++++++++++++++++++++++++

The location of the "Standalone Zipper" service. If this option is specified, Dataverse will be redirecing bulk/mutli-file zip download requests to that location, instead of serving them internally. See the "Advanced" section of the Installation guide for information on how to install the external zipper. (This is still an experimental feature, as of v5.0).

To enable redirects to the zipper installed on the same server as the main Dataverse application: 

``curl -X PUT -d '/cgi-bin/zipdownload' http://localhost:8080/api/admin/settings/:CustomZipDownloadServiceUrl``

To enable redirects to the zipper on a different server: 

``curl -X PUT -d 'https://zipper.example.edu/cgi-bin/zipdownload' http://localhost:8080/api/admin/settings/:CustomZipDownloadServiceUrl`` 

:ArchiverClassName
++++++++++++++++++

Dataverse can export archival "Bag" files to an extensible set of storage systems (see :ref:`BagIt Export` above for details about this and for further explanation of the other archiving related settings below).
This setting specifies which storage system to use by identifying the particular Java class that should be run. Current options include DuraCloudSubmitToArchiveCommand, LocalSubmitToArchiveCommand, and GoogleCloudSubmitToArchiveCommand.

``curl -X PUT -d 'LocalSubmitToArchiveCommand' http://localhost:8080/api/admin/settings/:ArchiverClassName`` 
 
:ArchiverSettings
+++++++++++++++++

Each Archiver class may have its own custom settings. Along with setting which Archiver class to use, one must use this setting to identify which setting values should be sent to it when it is invoked. The value should be a comma-separated list of setting names.
For example, the LocalSubmitToArchiveCommand only uses the :BagItLocalPath setting. To allow the class to use that setting, this setting must set as:

``curl -X PUT -d ':BagItLocalPath' http://localhost:8080/api/admin/settings/:ArchiverSettings`` 

:DuraCloudHost
++++++++++++++
:DuraCloudPort
++++++++++++++
:DuraCloudContext
+++++++++++++++++

These three settings define the host, port, and context used by the DuraCloudSubmitToArchiveCommand. :DuraCloudHost is required. The other settings have default values as noted in the :ref:`Duracloud Configuration` section above.

:BagItLocalPath
+++++++++++++++

This is the local file system path to be used with the LocalSubmitToArchiveCommand class. It is recommended to use an absolute path. See the :ref:`Local Path Configuration` section above.

:GoogleCloudBucket
++++++++++++++++++ 
:GoogleCloudProject
+++++++++++++++++++

These are the bucket and project names to be used with the GoogleCloudSubmitToArchiveCommand class. Further information is in the :ref:`Google Cloud Configuration` section above.
