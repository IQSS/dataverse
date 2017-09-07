=============
Configuration
=============

Now that you've successfully logged into Dataverse with a superuser account after going through a basic :doc:`installation-main`, you'll need to secure and configure your installation.

Settings within Dataverse itself are managed via JVM options or by manipulating values in the ``setting`` table directly or through API calls. Configuring Solr requires manipulating XML files.

Once you have finished securing and configuring your Dataverse installation, you may proceed to the :doc:`/admin/index` for more information on the ongoing administration of a Dataverse installation. Advanced configuration topics are covered in the :doc:`r-rapache-tworavens`, :doc:`shibboleth` and :doc:`oauth2` sections.

.. contents:: |toctitle|
  :local:

Securing Your Installation
--------------------------

Changing the Superuser Password
+++++++++++++++++++++++++++++++

The default password for the "dataverseAdmin" superuser account is "admin", as mentioned in the :doc:`installation-main` section, and you should change it, of course.

Blocking API Endpoints
++++++++++++++++++++++

The :doc:`/api/native-api` contains a useful but potentially dangerous API endpoint called "admin" that allows you to change system settings, make ordinary users into superusers, and more. The ``builtin-users`` endpoint lets people create a local/builtin user account if they know the ``BuiltinUsers.KEY`` value described below.

By default, all APIs can be operated on remotely and a number of endpoints do not require authentication. https://github.com/IQSS/dataverse/issues/1886 was opened to explore changing these defaults, but until then it is very important to block both the "admin" endpoint (and at least consider blocking ``builtin-users``). For details please see also the section on ``:BlockedApiPolicy`` below.

Forcing HTTPS
+++++++++++++

To avoid having your users send credentials in the clear, it's strongly recommended to force all web traffic to go through HTTPS (port 443) rather than HTTP (port 80). The ease with which one can install a valid SSL cert into Apache compared with the same operation in Glassfish might be a compelling enough reason to front Glassfish with Apache. In addition, Apache can be configured to rewrite HTTP to HTTPS with rules such as those found at https://wiki.apache.org/httpd/RewriteHTTPToHTTPS or in the section on :doc:`shibboleth`.

Run Glassfish as a User Other Than Root
+++++++++++++++++++++++++++++++++++++++

See the Glassfish section of :doc:`prerequisites` for details and init scripts for running Glassfish as non-root.

Related to this is that you should remove ``/root/.glassfish/pass`` to ensure that Glassfish isn't ever accidentally started as root. Without the password, Glassfish won't be able to start as root, which is a good thing.

Enforce Strong Passwords for User Accounts
++++++++++++++++++++++++++++++++++++++++++

Dataverse only stores passwords (as salted hash, and using a strong hashing algorithm) for "builtin" users. You can increase the password complexity rules to meet your security needs. If you have configured your Dataverse installation to allow login from remote authentication providers such as Shibboleth, ORCID, GitHub or Google, you do not have any control over those remote providers' password complexity rules. See the "Auth Modes: Local vs. Remote vs. Both" section below for more on login options.

Even if you are satisfied with the out-of-the-box password complexity rules Dataverse ships with, for the "dataverseAdmin" account you should use a strong password so the hash cannot easily be cracked through dictionary attacks.

Password complexity rules for "builtin" accounts can be adjusted with a variety of settings documented below. Here's a list:

- :ref:`:PVDictionaries`
- :ref:`:PVGoodStrength`
- :ref:`:PVMinLength`
- :ref:`:PVMaxLength`
- :ref:`:PVCharacterRules`
- :ref:`:PVNumberOfCharacteristics`
- :ref:`:PVNumberOfConsecutiveDigitsAllowed`
- :ref:`:PVCustomPasswordResetAlertMessage`



Solr
----

schema.xml
++++++++++

The :doc:`prerequisites` section explained that Dataverse requires a specific Solr schema file called ``schema.xml`` that can be found in the Dataverse distribution. You should have already replaced the default ``example/solr/collection1/conf/schema.xml`` file that ships with Solr.

jetty.xml
+++++++++

Stop Solr and edit ``solr-4.6.0/example/etc/jetty.xml`` to add a line having to do with ``requestHeaderSize`` as follows:

.. code-block:: xml

    <Call name="addConnector">
      <Arg>
          <New class="org.eclipse.jetty.server.bio.SocketConnector">
            <Set name="host"><SystemProperty name="jetty.host" /></Set>
            <Set name="port"><SystemProperty name="jetty.port" default="8983"/></Set>
            <Set name="maxIdleTime">50000</Set>
            <Set name="lowResourceMaxIdleTime">1500</Set>
            <Set name="statsOn">false</Set>
            <Set name="requestHeaderSize">102400</Set>
          </New>
      </Arg>
    </Call>

Without this ``requestHeaderSize`` line in place, which increases the default size, it will appear that no data has been added to your Dataverse installation and ``WARN  org.eclipse.jetty.http.HttpParser  – HttpParser Full for /127.0.0.1:8983`` will appear in the Solr log. See also https://support.lucidworks.com/hc/en-us/articles/201424796-Error-when-submitting-large-query-strings-

Network Ports
-------------

Remember how under "Decisions to Make" in the :doc:`prep` section we mentioned you'll need to make a decision about whether or not to introduce a proxy in front of Dataverse such as Apache or nginx? The time has come to make that decision.

The need to redirect port HTTP (port 80) to HTTPS (port 443) for security has already been mentioned above and the fact that Glassfish puts these services on 8080 and 8181, respectively, was touched on in the :doc:`installation-main` section. In production, you don't want to tell your users to use Dataverse on ports 8080 and 8181. You should have them use the stardard HTTPS port, which is 443.

Your decision to proxy or not should primarily be driven by which features of Dataverse you'd like to use. If you'd like to use Shibboleth, the decision is easy because proxying or "fronting" Glassfish with Apache is required. The details are covered in the :doc:`shibboleth` section.

If you'd like to use TwoRavens, you should also consider fronting with Apache because you will be required to install an Apache anyway to make use of the rApache module. For details, see the :doc:`r-rapache-tworavens` section.

Even if you have no interest in Shibboleth nor TwoRavens, you may want to front Dataverse with Apache or nginx to simply the process of installing SSL certificates. There are many tutorials on the Internet for adding certs to Apache, including a some `notes used by the Dataverse team <https://github.com/IQSS/dataverse/blob/v4.6.1/doc/shib/shib.md>`_, but the process of adding a certificate to Glassfish is arduous and not for the faint of heart. The Dataverse team cannot provide much help with adding certificates to Glassfish beyond linking to `tips <http://stackoverflow.com/questions/906402/importing-an-existing-x509-certificate-and-private-key-in-java-keystore-to-use-i>`_ on the web.

Still not convinced you should put Glassfish behind another web server? Even if you manage to get your SSL certificate into Glassfish, how are you going to run Glassfish on low ports such as 80 and 443? Are you going to run Glassfish as root? Bad idea. This is a security risk. Under "Additional Recommendations" under "Securing Your Installation" above you are advised to configure Glassfish to run as a user other than root. (The Dataverse team will close https://github.com/IQSS/dataverse/issues/1934 after updating the Glassfish init script provided in the :doc:`prerequisites` section to not require root.)

There's also the issue of serving a production-ready version of robots.txt. By using a proxy such as Apache, this is a one-time "set it and forget it" step as explained below in the "Going Live" section.

If you are convinced you'd like to try fronting Glassfish with Apache, the :doc:`shibboleth` section should be good resource for you.

If you really don't want to front Glassfish with any proxy (not recommended), you can configure Glassfish to run HTTPS on port 443 like this:

``asadmin set server-config.network-config.network-listeners.network-listener.http-listener-2.port=443``

What about port 80? Even if you don't front Dataverse with Apache, you may want to let Apache run on port 80 just to rewrite HTTP to HTTPS as described above. You can use a similar command as above to change the HTTP port that Glassfish uses from 8080 to 80 (substitute ``http-listener-1.port=80``). Glassfish can be used to enforce HTTPS on its own without Apache, but configuring this is an exercise for the reader. Answers here may be helpful: http://stackoverflow.com/questions/25122025/glassfish-v4-java-7-port-unification-error-not-able-to-redirect-http-to

Root Dataverse Configuration
----------------------------

The user who creates a dataverse is given the "Admin" role on that dataverse. The root dataverse is created automatically for you by the installer and the "Admin" is the superuser account ("dataverseAdmin") we used in the :doc:`installation-main` section to confirm that we can log in. These next steps of configuring the root dataverse require the "Admin" role on the root dataverse, but not the much more powerful superuser attribute. In short, users with the "Admin" role are subject to the permission system. A superuser, on the other hand, completely bypasses the permission system. You can give non-superusers the "Admin" role on the root dataverse if you'd like them to configure the root dataverse.

Root Dataverse Permissions
++++++++++++++++++++++++++

In order for non-superusers to start creating dataverses or datasets, you need click "Edit" then "Permissions" and make choices about which users can add dataverses or datasets within the root dataverse. (There is an API endpoint for this operation as well.) Again, the user who creates a dataverse will be granted the "Admin" role on that dataverse.

Publishing the Root Dataverse
+++++++++++++++++++++++++++++

Non-superusers who are not "Admin" on the root dataverse will not be able to to do anything useful until the root dataverse has been published.

Customizing the Root Dataverse
++++++++++++++++++++++++++++++

As the person installing Dataverse you may or may not be a local metadata expert. You may want to have others sign up for accounts and grant them the "Admin" role at the root dataverse to configure metadata fields, templates, browse/search facets, guestbooks, etc. For more on these topics, consult the :doc:`/user/dataverse-management` section of the User Guide.

You are also welcome to adjust the theme of the root dataverse by adding a logo or changing header colors, etc.

Once this configuration is complete, your Dataverse installation should be ready for users to start playing with. That said, there are many more configuration options available, which will be explained below.

Persistent Identifiers and Publishing Datasets
----------------------------------------------

Persistent identifiers are a required and integral part of the Dataverse platform. They provide a URL that is guaranteed to resolve to the datasets they represent. Dataverse currently supports creating identifiers using DOI and Handle.

By default and for testing convenience, the installer configures a temporary DOI test namespace through EZID. This is sufficient to create and publish datasets but they are not citable nor guaranteed to be preserved. Note that any datasets creating using the test configuration cannot be directly migrated and would need to be created again once a valid DOI namespace is configured. 

To properly configure persistent identifiers for a production installation, an account and associated namespace must be acquired for a fee from a DOI or HDL provider: **EZID** (http://ezid.cdlib.org), **DataCite** (https://www.datacite.org), **Handle.Net** (https://www.handle.net). 

Once you have your DOI or Handle account credentials and a namespace, configure Dataverse to use them using the JVM options and database settings below.

Configuring Dataverse for DOIs
++++++++++++++++++++++++++++++

Out of the box, Dataverse is configured for DOIs. Here are the configuration options for DOIs:

**JVM Options:**

- :ref:`doi.baseurlstring`
- :ref:`doi.username`
- :ref:`doi.password`

**Database Settings:**

- :ref:`:DoiProvider <:DoiProvider>`
- :ref:`:Protocol <:Protocol>`
- :ref:`:Authority <:Authority>`
- :ref:`:DoiSeparator <:DoiSeparator>`

Configuring Dataverse for Handles
+++++++++++++++++++++++++++++++++

Here are the configuration options for handles:

**JVM Options:**

- :ref:`dataverse.handlenet.admcredfile`
- :ref:`dataverse.handlenet.admprivphrase`

**Database Settings:**

- :ref:`:Protocol <:Protocol>`
- :ref:`:Authority <:Authority>`

Note: If you are **minting your own handles** and plan to set up your own handle service, please refer to `Handle.Net documentation <http://handle.net/hnr_documentation.html>`_.

Auth Modes: Local vs. Remote vs. Both
-------------------------------------

There are three valid configurations or modes for authenticating users to Dataverse:

- Local only (also known as "builtin" or "Username/Email").
- Both local and remote (Shibboleth and/or OAuth).
- Remote (Shibboleth and/or OAuth) only.

Out of the box, Dataverse is configured in "local only" mode. The "dataverseAdmin" superuser account mentioned in the :doc:`/installation/installation-main` section is an example of a local account. Internally, these accounts are called "builtin" because they are built in to the Dataverse application itself.

To configure Shibboleth see the :doc:`shibboleth` section and to configure OAuth see the :doc:`oauth2` section.

The ``authenticationproviderrow`` database table controls which "authentication providers" are available within Dataverse. Out of the box, a single row with an id of "builtin" will be present. For each user in Dataverse, the ``authenticateduserlookup`` table will have a value under ``authenticationproviderid`` that matches this id. For example, the default "dataverseAdmin" user will have the value "builtin" under  ``authenticationproviderid``. Why is this important? Users are tied to a specific authentication provider but conversion mechanisms are available to switch a user from one authentication provider to the other. As explained in the :doc:`/user/account` section of the User Guide, a graphical workflow is provided for end users to convert from the "builtin" authentication provider to a remote provider. Conversion from a remote authentication provider to the builtin provider can be performed by a sysadmin with access to the "admin" API. See the :doc:`/api/native-api` section of the API Guide for how to list users and authentication providers as JSON.

Enabling a second authentication provider will result in the Log In page showing additional providers for your users to choose from. By default, the Log In page will show the "builtin" provider, but you can adjust this via the ``:DefaultAuthProvider`` configuration option. 

"Remote only" mode should be considered experimental until https://github.com/IQSS/dataverse/issues/2974 is resolved. For now, "remote only" means:

- Shibboleth or OAuth has been enabled.
- ``:AllowSignUp`` is set to "false" per the :doc:`config` section to prevent users from creating local accounts via the web interface. Please note that local accounts can also be created via API, and the way to prevent this is to block the ``builtin-users`` endpoint or scramble (or remove) the ``BuiltinUsers.KEY`` database setting per the :doc:`config` section. 
- The "builtin" authentication provider has been disabled. Note that disabling the builting auth provider means that the API endpoint for converting an account from a remote auth provider will not work. This is the main reason why https://github.com/IQSS/dataverse/issues/2974 is still open. Converting directly from one remote authentication provider to another (i.e. from GitHub to Google) is not supported. Conversion from remote is always to builtin. Then the user initiates a conversion from builtin to remote. Note that longer term, the plan is to permit multiple login options to the same Dataverse account per https://github.com/IQSS/dataverse/issues/3487 (so all this talk of conversion will be moot) but for now users can only use a single login option, as explained in the :doc:`/user/account` section of the User Guide. In short, "remote only" might work for you if you only plan to use a single remote authentication provider such that no conversion between remote authentication providers will be necessary.

File Storage: Local Filesystem vs. Swift vs. S3
-----------------------------------------------

By default, a Dataverse installation stores data files (files uploaded by end users) on the filesystem at ``/usr/local/glassfish4/glassfish/domains/domain1/files`` but this path can vary based on answers you gave to the installer (see the :ref:`dataverse-installer` section of the Installation Guide) or afterward by reconfiguring the ``dataverse.files.directory`` JVM option described below.

Swift Storage
+++++++++++++

Rather than storing data files on the filesystem, you can opt for an experimental setup with a `Swift Object Storage <http://swift.openstack.org>`_ backend. Each dataset that users create gets a corresponding "container" on the Swift side, and each data file is saved as a file within that container.

**In order to configure a Swift installation,** there are two steps you need to complete:

First, create a file named ``swift.properties`` as follows in the ``config`` directory for your installation of Glassfish (by default, this would be ``/usr/local/glassfish4/glassfish/domains/domain1/config/swift.properties``):

.. code-block:: none

    swift.default.endpoint=endpoint1
    swift.auth_type.endpoint1=your-authentication-type
    swift.auth_url.endpoint1=your-auth-url
    swift.tenant.endpoint1=your-tenant-name
    swift.username.endpoint1=your-username
    swift.password.endpoint1=your-password
    swift.swift_endpoint.endpoint1=your-swift-endpoint

``auth_type`` can either be ``keystone`` or it will assumed to be ``basic``. ``auth_url`` should be your keystone authentication URL which includes the tokens (e.g. ``https://openstack.example.edu:35357/v2.0/tokens``). ``swift_endpoint`` is a URL that look something like ``http://rdgw.swift.example.org/swift/v1``.

Second, update the JVM option ``dataverse.files.storage-driver-id`` by running the delete command:

``./asadmin $ASADMIN_OPTS delete-jvm-options "\-Ddataverse.files.storage-driver-id=file"``

Then run the create command:

``./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.files.storage-driver-id=swift"``

You also have the option to set a **custom container name separator.** It is initialized to ``_``, but you can change it by running the create command:

``./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.files.swift-folder-path-separator=-"``

By default, your Swift installation will be public-only, meaning users will be unable to put access restrictions on their data. If you are comfortable with this level of privacy, the final step in your setup is to set the  :ref:`:PublicInstall` setting to `true`.

In order to **enable file access restrictions**, you must enable Swift to use temporary URLs for file access. To enable usage of temporary URLs, set a hash key both on your swift endpoint and in your swift.properties file. You can do so by adding 

.. code-block:: none

    swift.hash_key.endpoint1=your-hash-key

to your swift.properties file.

You also have the option to set a custom expiration length for a generated temporary URL. It is initalized to 60 seconds, but you can change it by running the create command:

``./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.files.temp_url_expire=3600"``

In this example, you would be setting the expiration length for one hour.


Setting up Compute
+++++++++++++++++++

Once you have configured a Swift Object Storage backend, you also have the option of enabling a connection to a computing environment. To do so, you need to configure the database settings for :ref:`:ComputeBaseUrl` and  :ref:`:CloudEnvironmentName`.

Once you have set up ``:ComputeBaseUrl`` properly in both Dataverse and your cloud environment, the compute button on dataset and file pages will link validated users to your computing environment. Depending on the configuration of your installation, the compute button will either redirect to: 

``:ComputeBaseUrl?containerName=yourContainer&objectName=yourObject``

if your installation's :ref:`:PublicInstall` setting is true, or:

``:ComputeBaseUrl?containerName=yourContainer&objectName=yourObject&temp_url_sig=yourTempUrlSig&temp_url_expires=yourTempUrlExpiry``

You can configure this redirect properly in your cloud environment to generate a temporary URL for access to the Swift objects for computing.

Amazon S3 Storage
+++++++++++++++++

For institutions and organizations looking to use Amazon's S3 cloud storage for their installation, this can be set up manually through creation of a credentials file or automatically via the aws console commands. 

You'll need an AWS account with an associated S3 bucket for your installation to use. From the S3 management console (e.g. `<https://console.aws.amazon.com/>`_), you can poke around and get familiar with your bucket. We recommend using IAM (Identity and Access Management) to create a user with full S3 access and nothing more, for security reasons. See `<http://docs.aws.amazon.com/IAM/latest/UserGuide/id_users.html>`_ for more info on this process.

Make note of the bucket's name and the region its data is hosted in. Dataverse and the aws SDK rely on the placement of a key file located in ``~/.aws/credentials``, which can be generated via either of these two methods.

Setup aws manually
^^^^^^^^^^^^^^^^^^

To create ``credentials`` manually, you will need to generate a key/secret key. The first step is to log onto your aws web console (e.g. `<https://console.aws.amazon.com/>`_). If you have created a user in AWS IAM, you can click on that user and generate the keys needed for dataverse. 

Once you have acquired the keys, they need to be added to``credentials``. The format for credentials is as follows:

| ``[default]``
| ``aws_access_key_id = <insert key, no brackets>``
| ``aws_secret_access_key = <insert secret key, no brackets>``

Place this file ina a folder named ``.aws`` under the home directory for the user running your dataverse installation.

Setup aws via command line tools
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Begin by installing the CLI tool `pip <https://pip.pypa.io//en/latest/>`_ to install the `AWS command line interface <https://aws.amazon.com/cli/>`_ if you don't have it.

First, we'll get our access keys set up. If you already have your access keys configured, skip this step. From the command line, run:

``pip install awscli``

``aws configure``

You'll be prompted to enter your Access Key ID and secret key, which should be issued to your AWS account. The subsequent config steps after the access keys are up to you. For reference, these keys are stored in ``~/.aws/credentials``.

Configure dataverse to use aws/S3
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

With your access to your bucket in place, we'll want to navigate to ``/usr/local/glassfish4/glassfish/bin/`` and execute the following ``asadmin`` commands to set up the proper JVM options. Recall that out of the box, Dataverse is configured to use local file storage. You'll need to delete the existing storage driver before setting the new one.

``./asadmin $ASADMIN_OPTS delete-jvm-options "\-Ddataverse.files.storage-driver-id=file"``

``./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.files.storage-driver-id=s3"``

Then, we'll need to identify which S3 bucket we're using. Replace ``your_bucket_name`` with, of course, your bucket:

``./asadmin create-jvm-options "-Ddataverse.files.s3-bucket-name=your_bucket_name"``

Lastly, go ahead and restart your glassfish server. With Dataverse deployed and the site online, you should be able to upload datasets and data files and see the corresponding files in your S3 bucket. Within a bucket, the folder structure emulates that found in local file storage.

.. _Branding Your Installation:

Branding Your Installation
--------------------------

The name of your root dataverse is the brand of your installation of Dataverse and appears in various places such as notifications. Notifications and support links also contain the name of your support team, which will either be "[YourBrand] Support" or a name of your choosing when you configure a name for the ``:SystemEmail`` database setting described below. 

Dataverse provides configurable options for easy-to-add (and maintain) custom branding for your Dataverse installation. Downloadable sample HTML and CSS files are provided below which you can edit as you see fit. It's up to you to create a directory in which to store these files, such as ``/var/www/dataverse`` in the examples below.

You can add a custom welcome/homepage as well as other custom content, to further brand your installation and make it your own. Here are the custom branding and content options you can add:

- Custom welcome/homepage
- Logo image to navbar
- Header
- Footer
- CSS stylesheet

Custom Homepage
++++++++++++++++

Dataverse allows you to use a custom homepage or welcome page in place of the default root dataverse page. This allows for complete control over the look and feel of your installation's homepage.

Download this sample: :download:`custom-homepage.html </_static/installation/files/var/www/dataverse/branding/custom-homepage.html>` and place it at ``/var/www/dataverse/branding/custom-homepage.html``. Then run this curl command:

``curl -X PUT -d '/var/www/dataverse/branding/custom-homepage.html' http://localhost:8080/api/admin/settings/:HomePageCustomizationFile``

Note that the ``custom-homepage.html`` file provided has a "Browse Data" button that assumes that your root dataverse still has an alias of "root". While you were branding your root dataverse, you may have changed the alias to "harvard" or "librascholar" or whatever and you should adjust the ``custom-homepage.html`` file as needed.

For more background on what this curl command above is doing, see the "Database Settings" section below. If you decide you'd like to remove this setting, use the following curl command:

``curl -X DELETE http://localhost:8080/api/admin/settings/:HomePageCustomizationFile``

Custom Navbar Logo
+++++++++++++++++++

Dataverse allows you to replace the default Dataverse icon and name branding in the navbar with your own custom logo. Note that this logo is separate from the *root dataverse theme* logo.

Create a "navbar" folder in your Glassfish "logos" directory and place your custom logo there. By Glassfish default, it'll be located at ``/usr/local/glassfish4/glassfish/domains/domain1/docroot/logos/navbar/logo.png``. Then run this curl command:

``curl -X PUT -d '/logos/navbar/logo.png' http://localhost:8080/api/admin/settings/:LogoCustomizationFile``

Note that the logo is expected to be small enough to fit comfortably in the navbar, perhaps only 30 pixels high.

Custom Header
+++++++++++++

Download this sample: :download:`custom-header.html </_static/installation/files/var/www/dataverse/branding/custom-header.html>` and place it at ``/var/www/dataverse/branding/custom-header.html``. Then run this curl command:

``curl -X PUT -d '/var/www/dataverse/branding/custom-header.html' http://localhost:8080/api/admin/settings/:HeaderCustomizationFile``

Custom Footer
+++++++++++++

Download this sample: :download:`custom-footer.html </_static/installation/files/var/www/dataverse/branding/custom-footer.html>` and place it at ``/var/www/dataverse/branding/custom-footer.html``. Then run this curl command:

``curl -X PUT -d '/var/www/dataverse/branding/custom-footer.html' http://localhost:8080/api/admin/settings/:FooterCustomizationFile``

Custom CSS Stylesheet
+++++++++++++++++++++

Download this sample: :download:`custom-stylesheet.css </_static/installation/files/var/www/dataverse/branding/custom-stylesheet.css>` and place it at ``/var/www/dataverse/branding/custom-stylesheet.css``. Then run this curl command:

``curl -X PUT -d '/var/www/dataverse/branding/custom-stylesheet.css' http://localhost:8080/api/admin/settings/:StyleCustomizationFile``

Going Live: Launching Your Production Deployment
------------------------------------------------

This guide has attempted to take you from kicking the tires on Dataverse to finalizing your installation before letting real users in. In theory, all this work could be done on a single server but better would be to have separate staging and production environments so that you can deploy upgrades to staging before deploying to production. This "Going Live" section is about launching your **production** environment.

Before going live with your installation of Dataverse, you must take the steps above under "Securing Your Installation" and you should at least review the various configuration options listed below. An attempt has been made to put the more commonly-configured options earlier in the list.

Out of the box, Dataverse attempts to block search engines from crawling your installation of Dataverse so that test datasets do not appear in search results until you're ready.

Letting Search Engines Crawl Your Installation
++++++++++++++++++++++++++++++++++++++++++++++

For a public production Dataverse installation, it is probably desired that search agents be able to index published pages (aka - pages that are visible to an unauthenticated user).
Polite crawlers usually respect the `Robots Exclusion Standard <https://en.wikipedia.org/wiki/Robots_exclusion_standard>`_; we have provided an example of a production robots.txt :download:`here </_static/util/robots.txt>`).

You have a couple of options for putting an updated robots.txt file into production. If you are fronting Glassfish with Apache as recommended above, you can place robots.txt in the root of the directory specified in your ``VirtualHost`` and to your Apache config a ``ProxyPassMatch`` line like the one below to prevent Glassfish from serving the version of robots.txt that embedded in the Dataverse war file:

.. code-block:: text

    # don't let Glassfish serve its version of robots.txt
    ProxyPassMatch ^/robots.txt$ !

For more of an explanation of ``ProxyPassMatch`` see the :doc:`shibboleth` section.

If you are not fronting Glassfish with Apache you'll need to prevent Glassfish from serving the robots.txt file embedded in the war file by overwriting robots.txt after the war file has been deployed. The downside of this technique is that you will have to remember to overwrite robots.txt in the "exploded" war file each time you deploy the war file, which probably means each time you upgrade to a new version of Dataverse. Furthermore, since the version of Dataverse is always incrementing and the version can be part of the file path, you will need to be conscious of where on disk you need to replace the file. For example, for Dataverse 4.6.1 the path to robots.txt may be ``/usr/local/glassfish4/glassfish/domains/domain1/applications/dataverse-4.6.1/robots.txt`` with the version number ``4.6.1`` as part of the path.

Putting Your Dataverse Installation on the Map at dataverse.org
+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

Congratulations! You've gone live! It's time to announce your new data respository to the world! You are also welcome to contact support@dataverse.org to have the Dataverse team add your installation to the map at http://dataverse.org . Thank you for installing Dataverse!

Administration of Your Dataverse Installation
+++++++++++++++++++++++++++++++++++++++++++++

Now that you're live you'll want to review the :doc:`/admin/index` for more information about the ongoing administration of a Dataverse installation.

JVM Options
-----------

JVM stands Java Virtual Machine and as a Java application, Glassfish can read JVM options when it is started. A number of JVM options are configured by the installer below is a complete list of the Dataverse-specific JVM options. You can inspect the configured options by running:

``asadmin list-jvm-options | egrep 'dataverse|doi'``

When changing values these values with ``asadmin``, you'll need to delete the old value before adding a new one, like this:

``asadmin delete-jvm-options "-Ddataverse.fqdn=old.example.com"``

``asadmin create-jvm-options "-Ddataverse.fqdn=dataverse.example.com"``

It's also possible to change these values by stopping Glassfish, editing ``glassfish4/glassfish/domains/domain1/config/domain.xml``, and restarting Glassfish.

dataverse.fqdn
++++++++++++++

If the Dataverse server has multiple DNS names, this option specifies the one to be used as the "official" host name. For example, you may want to have dataverse.foobar.edu, and not the less appealling server-123.socsci.foobar.edu to appear exclusively in all the registered global identifiers, Data Deposit API records, etc.

The password reset feature requires ``dataverse.fqdn`` to be configured.

| Do note that whenever the system needs to form a service URL, by default, it will be formed with ``https://`` and port 443. I.e.,
| ``https://{dataverse.fqdn}/``
| If that does not suit your setup, you can define an additional option, ``dataverse.siteUrl``, explained below.

dataverse.siteUrl
+++++++++++++++++

| and specify the protocol and port number you would prefer to be used to advertise the URL for your Dataverse.
| For example, configured in domain.xml:
| ``<jvm-options>-Ddataverse.fqdn=dataverse.foobar.edu</jvm-options>``
| ``<jvm-options>-Ddataverse.siteUrl=http://${dataverse.fqdn}:8080</jvm-options>``

dataverse.files.directory
+++++++++++++++++++++++++

This is how you configure the path to which files uploaded by users are stored.

dataverse.auth.password-reset-timeout-in-minutes
++++++++++++++++++++++++++++++++++++++++++++++++

Users have 60 minutes to change their passwords by default. You can adjust this value here.

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

dataverse.dropbox.key
+++++++++++++++++++++

Dropbox integration is optional. Enter your key here.

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

As of this writing, "https://ezid.cdlib.org" (EZID) and "https://mds.datacite.org" (DataCite) are the main valid values. 

While the above two options are recommended because they have been tested by the Dataverse team, it is also possible to use a DataCite Client API as a proxy to DataCite. In this case, requests made to the Client API are captured and passed on to DataCite for processing. The application will interact with the DataCite Client API exactly as if it were interacting directly with the DataCite API, with the only difference being the change to the base endpoint URL.

For example, the Australian Data Archive (ADA) successfully uses the Australian National Data Service (ANDS) API (a proxy for DataCite) to mint their DOIs through Dataverse using a ``doi.baseurlstring`` value of "https://researchdata.ands.org.au/api/doi/datacite" as documented at https://documentation.ands.org.au/display/DOC/ANDS+DataCite+Client+API . As ADA did for ANDS DOI minting, any DOI provider (and their corresponding DOI configuration parameters) other than DataCite and EZID must be tested with Dataverse to establish whether or not it will function properly.

Out of the box, Dataverse is configured to use base URL string from EZID. You can delete it like this:

``asadmin delete-jvm-options '-Ddoi.baseurlstring=https\://ezid.cdlib.org'``

Then, to switch to DataCite, you can issue the following command:

``asadmin create-jvm-options '-Ddoi.baseurlstring=https\://mds.datacite.org'``

See also these related database settings below:

- :ref:`:DoiProvider`
- :ref:`:Protocol`  
- :ref:`:Authority`
- :ref:`:DoiSeparator`

.. _doi.username:

doi.username
++++++++++++

Used in conjuction with ``doi.baseurlstring``.

Out of the box, Dataverse is configured with a test username from EZID. You can delete it with the following command:

``asadmin delete-jvm-options '-Ddoi.username=apitest'``

Once you have a username from your provider, you can enter it like this:

``asadmin create-jvm-options '-Ddoi.username=YOUR_USERNAME_HERE'``

.. _doi.password:

doi.password
++++++++++++

Out of the box, Dataverse is configured with a test password from EZID. You can delete it with the following command:

Used in conjuction with ``doi.baseurlstring``.

``asadmin delete-jvm-options '-Ddoi.password=apitest'``

Once you have a password from your provider, you can enter it like this:

``asadmin create-jvm-options '-Ddoi.password=YOUR_PASSWORD_HERE'``

.. _dataverse.handlenet.admcredfile:

dataverse.handlenet.admcredfile
+++++++++++++++++++++++++++++++

If you're using **handles**, this JVM setting configures access credentials so your dataverse can talk to your Handle.Net server. This is the private key generated during Handle.Net server installation. Typically the full path is set to ``handle/svr_1/admpriv.bin``. Please refer to `Handle.Net's documentation <http://handle.net/hnr_documentation.html>`_ for more info.

.. _dataverse.handlenet.admprivphrase:

dataverse.handlenet.admprivphrase
+++++++++++++++++++++++++++++++++
This JVM setting is also part of **handles** configuration. The Handle.Net installer lets you choose whether to encrypt the admcredfile private key or not. If you do encrypt it, this is the pass phrase that it's encrypted with. 

Database Settings
-----------------

These settings are stored in the ``setting`` database table but can be read and modified via the "admin" endpoint of the :doc:`/api/native-api` for easy scripting.

The most commonly used configuration options are listed first.

The pattern you will observe in curl examples below is that an HTTP ``PUT`` is used to add or modify a setting. If you perform an HTTP ``GET`` (the default when using curl), the output will contain the value of the setting, if it has been set. You can also do a ``GET`` of all settings with ``curl http://localhost:8080/api/admin/settings`` which you may want to pretty-print by piping the output through a tool such as jq by appending ``| jq .``. If you want to remove a setting, use an HTTP ``DELETE`` such as ``curl -X DELETE http://localhost:8080/api/admin/settings/:GuidesBaseUrl`` .

:BlockedApiPolicy
+++++++++++++++++

Out of the box, all API endpoints are completely open, as mentioned in the section on security above. It is highly recommended that you choose one of the policies below and also configure ``:BlockedApiEndpoints``.

- localhost-only: Allow from localhost.
- unblock-key: Require a key defined in ``:BlockedApiKey``.
- drop: Disallow the blocked endpoints completely.

``curl -X PUT -d localhost-only http://localhost:8080/api/admin/settings/:BlockedApiPolicy``

:BlockedApiEndpoints
++++++++++++++++++++

A comma separated list of API endpoints to be blocked. For a production installation, "admin" should be blocked (and perhaps "builtin-users" as well), as mentioned in the section on security above:

``curl -X PUT -d "admin,builtin-users" http://localhost:8080/api/admin/settings/:BlockedApiEndpoints``

See the :doc:`/api/index` for a list of API endpoints.

:BlockedApiKey
++++++++++++++

Used in conjunction with the ``:BlockedApiPolicy`` being set to ``unblock-key``. When calling blocked APIs, add a query parameter of ``unblock-key=theKeyYouChose`` to use the key.

``curl -X PUT -d s3kretKey http://localhost:8080/api/admin/settings/:BlockedApiKey``

BuiltinUsers.KEY
++++++++++++++++

The key required to create users via API as documented at :doc:`/api/native-api`. Unlike other database settings, this one doesn't start with a colon.

``curl -X PUT -d builtInS3kretKey http://localhost:8080/api/admin/settings/BuiltinUsers.KEY``

:SearchApiRequiresToken
+++++++++++++++++++++++

In Dataverse 4.7 and lower, the :doc:`/api/search` required an API token, but as of Dataverse 4.7.1 this is no longer the case. If you prefer the old behavior of requiring API tokens to use the Search API, set ``:SearchApiRequiresToken`` to ``true``.

``curl -X PUT -d true http://localhost:8080/api/admin/settings/:SearchApiRequiresToken``

:SystemEmail
++++++++++++

This is the email address that "system" emails are sent from such as password reset links. Your Dataverse installation will not send mail without this setting in place.

``curl -X PUT -d 'LibraScholar SWAT Team <support@librascholar.edu>' http://localhost:8080/api/admin/settings/:SystemEmail``

Note that only the email address is required, which you can supply without the ``<`` and ``>`` signs, but if you include the text, it's the way to customize the name of your support team, which appears in the "from" address in emails as well as in help text in the UI.

Please note that if you're having any trouble sending email, you can refer to "Troubleshooting" under :doc:`installation-main`.

:HomePageCustomizationFile
++++++++++++++++++++++++++

See :ref:`Branding Your Installation` above.

:HeaderCustomizationFile
++++++++++++++++++++++++

See :ref:`Branding Your Installation` above.

:FooterCustomizationFile
++++++++++++++++++++++++

See :ref:`Branding Your Installation` above.

:StyleCustomizationFile
+++++++++++++++++++++++

See :ref:`Branding Your Installation` above.

:FooterCopyright
++++++++++++++++

By default the footer says "Copyright © [YYYY]" but you can add text after the year, as in the example below.

``curl -X PUT -d ", The President &#38; Fellows of Harvard College" http://localhost:8080/api/admin/settings/:FooterCopyright``

.. _:DoiProvider:

:DoiProvider
++++++++++++

As of this writing "EZID" and "DataCite" are the only valid options. DoiProvider is only needed if you are using DOI.

``curl -X PUT -d EZID http://localhost:8080/api/admin/settings/:DoiProvider``

This setting relates to the ``:Protocol``, ``:Authority``, ``:DoiSeparator``, and ``:IdentifierGenerationStyle`` database settings below as well as the following JVM options:

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

``curl -X PUT -d 10.xxxx http://localhost:8080/api/admin/settings/:Authority``

.. _:DoiSeparator:

:DoiSeparator
+++++++++++++

It is recommended that you keep this as a slash ("/").

``curl -X PUT -d "/" http://localhost:8080/api/admin/settings/:DoiSeparator``

**Note:** The name DoiSeparator is a misnomer. This setting is used by some **handles**-specific code too. It *must* be set to '/' when using handles.

.. _:IdentifierGenerationStyle:

:IdentifierGenerationStyle
++++++++++++++++++++++++++

By default, Dataverse generates a random 6 character string to use as the identifier
for a Dataset. Set this to "``sequentialNumber``" to use sequential numeric values 
instead. (the assumed default setting is "``randomString``"). 
In addition to this setting, a database sequence must be created in the database. 
We provide the script below (downloadable :download:`here </_static/util/createsequence.sql>`).
You may need to make some changes to suit your system setup, see the comments for more information: 

.. literalinclude:: ../_static/util/createsequence.sql

Note that the SQL above is Postgres-specific. If necessary, it can be reimplemented 
in any other SQL flavor - the standard JPA code in the application simply expects 
the database to have a saved function ("stored procedure") named ``generateIdentifierAsSequentialNumber``
with the single return argument ``identifier``. 

For systems using Postgresql 8.4 or older, the procedural language `plpgsql` should be enabled first.
We have provided an example :download:`here </_static/util/pg8-createsequence-prep.sql>`.



:ApplicationTermsOfUse
++++++++++++++++++++++

Upload an HTML file containing the Terms of Use to be displayed at sign up. Supported HTML tags are listed under the :doc:`/user/dataset-management` section of the User Guide.

``curl -X PUT -d@/tmp/apptou.html http://localhost:8080/api/admin/settings/:ApplicationTermsOfUse``

Unfortunately, in most cases, the text file will probably be too big to upload (>1024 characters) due to a bug. A workaround has been posted to https://github.com/IQSS/dataverse/issues/2669

:ApplicationPrivacyPolicyUrl
++++++++++++++++++++++++++++

Specify a URL where users can read your Privacy Policy, linked from the bottom of the page.

``curl -X PUT -d http://best-practices.dataverse.org/harvard-policies/harvard-privacy-policy.html http://localhost:8080/api/admin/settings/:ApplicationPrivacyPolicyUrl``

:ApiTermsOfUse
++++++++++++++

Specify a URL where users can read your API Terms of Use.

``curl -X PUT -d http://best-practices.dataverse.org/harvard-policies/harvard-api-tou.html http://localhost:8080/api/admin/settings/:ApiTermsOfUse``

:ExcludeEmailFromExport
+++++++++++++++++++++++

Set ``:ExcludeEmailFromExport`` to prevent email addresses for dataset contacts from being exposed in XML or JSON representations of dataset metadata. For a list exported formats such as DDI, see the :doc:`/admin/metadataexport` section of the Admin Guide.

``curl -X PUT -d true http://localhost:8080/api/admin/settings/:ExcludeEmailFromExport``

:NavbarAboutUrl
+++++++++++++++

Set ``NavbarAboutUrl`` to a fully-qualified url which will be used for the "About" link in the navbar. 

Note: The "About" link will not appear in the navbar until this option is set.

``curl -X PUT -d http://dataverse.example.edu http://localhost:8080/api/admin/settings/:NavbarAboutUrl``


:GuidesBaseUrl
++++++++++++++

Set ``GuidesBaseUrl`` to override the default value "http://guides.dataverse.org". If you are interested in writing your own version of the guides, you may find the :doc:`/developers/documentation` section of the Developer Guide helpful.

``curl -X PUT -d http://dataverse.example.edu http://localhost:8080/api/admin/settings/:GuidesBaseUrl``

:GuidesVersion
++++++++++++++

Set ``:GuidesVersion`` to override the version number in the URL of guides. For example, rather than http://guides.dataverse.org/en/4.6/user/account.html the version is overriden to http://guides.dataverse.org/en/1234-new-feature/user/account.html in the example below:

``curl -X PUT -d 1234-new-feature http://localhost:8080/api/admin/settings/:GuidesVersion``

:MetricsUrl
+++++++++++

Make the metrics component on the root dataverse a clickable link to a website where you present metrics on your Dataverse installation. This could perhaps be an installation of https://github.com/IQSS/miniverse or any site.

``curl -X PUT -d http://metrics.dataverse.example.edu http://localhost:8080/api/admin/settings/:MetricsUrl``

:StatusMessageHeader
++++++++++++++++++++

For dynamically adding information to the top of every page. For example, "For testing only..." at the top of https://demo.dataverse.org is set with this:

``curl -X PUT -d "For testing only..." http://localhost:8080/api/admin/settings/:StatusMessageHeader``

You can make the text clickable and include an additional message in a pop up by setting ``:StatusMessageText``.

:StatusMessageText
++++++++++++++++++

After you've set ``:StatusMessageHeader`` you can also make it clickable to have it include text if a popup with this:

``curl -X PUT -d "This appears in a popup." http://localhost:8080/api/admin/settings/:StatusMessageText``

:MaxFileUploadSizeInBytes
+++++++++++++++++++++++++

Set `MaxFileUploadSizeInBytes` to "2147483648", for example, to limit the size of files uploaded to 2 GB.
Notes:
- For SWORD, this size is limited by the Java Integer.MAX_VALUE of 2,147,483,647. (see: https://github.com/IQSS/dataverse/issues/2169)
- If the MaxFileUploadSizeInBytes is NOT set, uploads, including SWORD may be of unlimited size.

``curl -X PUT -d 2147483648 http://localhost:8080/api/admin/settings/:MaxFileUploadSizeInBytes``

:ZipDownloadLimit
+++++++++++++++++

For performance reasons, Dataverse will only create zip files on the fly up to 100 MB but the limit can be increased. Here's an example of raising the limit to 1 GB:

``curl -X PUT -d 1000000000 http://localhost:8080/api/admin/settings/:ZipDownloadLimit``

:TabularIngestSizeLimit
+++++++++++++++++++++++

Threshold in bytes for limiting whether or not "ingest" it attempted for tabular files (which can be resource intensive). For example, with the below in place, files greater than 2 GB in size will not go through the ingest process:

``curl -X PUT -d 2000000000 http://localhost:8080/api/admin/settings/:TabularIngestSizeLimit``

(You can set this value to 0 to prevent files from being ingested at all.)

You can overide this global setting on a per-format basis for the following formats:

- dta
- por
- sav
- Rdata
- CSV
- xlsx

For example, if you want your installation of Dataverse to not attempt to ingest Rdata files larger that 1 MB, use this setting:

``curl -X PUT -d 1000000 http://localhost:8080/api/admin/settings/:TabularIngestSizeLimit:Rdata``

:ZipUploadFilesLimit
++++++++++++++++++++

Limit the number of files in a zip that Dataverse will accept.

:GoogleAnalyticsCode
++++++++++++++++++++

Set your Google Analytics Tracking ID thusly:

``curl -X PUT -d 'trackingID' http://localhost:8080/api/admin/settings/:GoogleAnalyticsCode``

:SolrHostColonPort
++++++++++++++++++

By default Dataverse will attempt to connect to Solr on port 8983 on localhost. Use this setting to change the hostname or port. You must restart Glassfish after making this change.

``curl -X PUT -d localhost:8983 http://localhost:8080/api/admin/settings/:SolrHostColonPort``

:SignUpUrl
++++++++++

The relative path URL to which users will be sent after signup. The default setting is below.

``curl -X PUT -d '/dataverseuser.xhtml?editMode=CREATE' http://localhost:8080/api/admin/settings/:SignUpUrl``

:TwoRavensUrl
+++++++++++++

The location of your TwoRavens installation.  Activation of TwoRavens also requires the setting below, ``TwoRavensTabularView``

:TwoRavensTabularView
+++++++++++++++++++++

Set ``TwoRavensTabularView`` to true to allow a user to view tabular files via the TwoRavens application. This boolean affects whether a user will see the "Explore" button.

``curl -X PUT -d true http://localhost:8080/api/admin/settings/:TwoRavensTabularView``

:GeoconnectCreateEditMaps
+++++++++++++++++++++++++

Set ``GeoconnectCreateEditMaps`` to true to allow the user to create GeoConnect Maps. This boolean effects whether the user sees the map button on the dataset page and if the ingest will create a shape file.

``curl -X PUT -d true http://localhost:8080/api/admin/settings/:GeoconnectCreateEditMaps``

:GeoconnectViewMaps
+++++++++++++++++++

Set ``GeoconnectViewMaps`` to true to allow a user to view existing maps. This boolean effects whether a user will see the "Explore" button.

``curl -X PUT -d true http://localhost:8080/api/admin/settings/:GeoconnectViewMaps``

:GeoconnectDebug
+++++++++++++++++++

For Development only.  Set ``GeoconnectDebug`` to true to allow a user to see SQL that can be used to insert mock map data into the database.

``curl -X PUT -d true http://localhost:8080/api/admin/settings/:GeoconnectDebug``

:DatasetPublishPopupCustomText
++++++++++++++++++++++++++++++

Set custom text a user will view when publishing a dataset. Note that this text is exposed via the "Info" endpoint of the :doc:`/api/native-api`.

``curl -X PUT -d "Deposit License Requirements" http://localhost:8080/api/admin/settings/:DatasetPublishPopupCustomText``

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

:DefaultAuthProvider
++++++++++++++++++++

If you have enabled Shibboleth and/or one or more OAuth providers, you may wish to make one of these authentication providers the default when users visit the Log In page. If unset, this will default to ``builtin`` but thes valid options (depending if you've done the setup described in the :doc:`shibboleth` or :doc:`oauth2` sections) are:

- ``builtin``
- ``shib``
- ``orcid``
- ``github``
- ``google``

Here is an example of setting the default auth provider back to ``builtin``:

``curl -X PUT -d builtin http://localhost:8080/api/admin/settings/:DefaultAuthProvider``

:AllowSignUp
++++++++++++

Set to false to disallow local accounts to be created. See also the sections on :doc:`shibboleth` and :doc:`oauth2`.

:PiwikAnalyticsId
++++++++++++++++++++

Site identifier created in your Piwik instance. Example:

``curl -X PUT -d 42 http://localhost:8080/api/admin/settings/:PiwikAnalyticsId``

:PiwikAnalyticsHost
++++++++++++++++++++

Host FQDN or URL of your Piwik instance before the ``/piwik.php``. Examples:

``curl -X PUT -d stats.domain.tld http://localhost:8080/api/admin/settings/:PiwikAnalyticsHost``

or

``curl -X PUT -d hostname.domain.tld/stats http://localhost:8080/api/admin/settings/:PiwikAnalyticsHost``

:FileFixityChecksumAlgorithm
++++++++++++++++++++++++++++

Dataverse calculates checksums for uploaded files so that users can determine if their file was corrupted via upload or download. This is sometimes called "file fixity": https://en.wikipedia.org/wiki/File_Fixity

The default checksum algorithm used is MD5 and should be sufficient for establishing file fixity. "SHA-1" is an experimental alternate value for this setting.

.. _:PVDictionaries:

:PVDictionaries
+++++++++++++++

Password policy setting for builtin user accounts: set a comma separated list of dictionaries containing words that cannot be used in a user password. ``/usr/share/dict/words`` (modified to not contain short words) is suggested below but you are welcome to use dictionary files of your choosing. By default, no dictionary is checked.

First, change directory to the directory you want your dictionary file to reside.

``DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"``
``sed '/^.\{,3\}$/d' /usr/share/dict/words > $DIR/pwdictionary``
``curl -X PUT -d "$DIR/pwdictionary" http://localhost:8080/api/admin/settings/:PVDictionaries``

This setting can be overruled with VM argument ``pv.dictionaries``

.. _:PVGoodStrength:

:PVGoodStrength
+++++++++++++++

Password policy setting for builtin user accounts: passwords of equal or greater character length than the :PVGoodStrength setting are always valid, regardless of other password constraints.

``curl -X PUT -d 20 http://localhost:8080/api/admin/settings/:PVGoodStrength``

This setting can be overruled with VM argument ``pv.goodstrength``

Recommended setting: 20.

.. _:PVMinLength:

:PVMinLength
++++++++++++

Password policy setting for builtin user accounts: a password's minimum valid character length. The default is 6.

``curl -X PUT -d 6 http://localhost:8080/api/admin/settings/:PVMinLength``

This setting can be overruled with VM argument ``pv.minlength``

.. _:PVMaxLength:

:PVMaxLength
++++++++++++

Password policy setting for builtin user accounts: a password's maximum valid character length.

``curl -X PUT -d 0 http://localhost:8080/api/admin/settings/:PVMaxLength``

This setting can be overruled with VM argument ``pv.maxlength``

.. _:PVCharacterRules:

:PVCharacterRules
+++++++++++++++++

Password policy setting for builtinuser accounts: dictates which types of characters can be required in a password. This setting goes hand in hand with :ref:`:PVNumberOfCharacteristics`. The default setting contains two rules:

- one letter
- one digit

The default setting above is equivalent to specifying "Alphabetical:1,Digit:1".

By specifying "UpperCase:1,LowerCase:1,Digit:1,Special:1", for example, you can put the following four rules in place instead:

- one uppercase letter
- one lowercase letter
- one digit
- one special character

If you have implemented 4 different character rules in this way, you can also optionally increase ``:PVNumberOfCharacteristics`` to as high as 4. However, please note that ``:PVNumberOfCharacteristics`` cannot be set to a number higher than the number of rules or you will see the error, "Number of characteristics must be <= to the number of rules".

``curl -X PUT -d 'UpperCase:1,LowerCase:1,Digit:1,Special:1' http://localhost:8080/api/admin/settings/:PVCharacterRules``

``curl -X PUT -d 3 http://localhost:8080/api/admin/settings/:PVNumberOfCharacteristics``

.. _:PVNumberOfCharacteristics:

:PVNumberOfCharacteristics
++++++++++++++++++++++++++

Password policy setting for builtin user accounts: the number indicates how many of the character rules defined by ``:PVCharacterRules`` are required as part of a password. The default is 2. ``:PVNumberOfCharacteristics`` cannot be set to a number higher than the number of rules or you will see the error, "Number of characteristics must be <= to the number of rules".

``curl -X PUT -d 2 http://localhost:8080/api/admin/settings/:PVNumberOfCharacteristics``

This setting can be overruled with VM argument ``pv.numberofcharacteristics``

.. _:PVNumberOfConsecutiveDigitsAllowed

:PVNumberOfConsecutiveDigitsAllowed
+++++++++++++++++++++++++++++++++++

By default, passwords can contain an unlimited number of digits in a row, but if your password policy specifies that only four digists in a row are allowed, for example, you can issue the following curl command:

``curl -X PUT -d 4 http://localhost:8080/api/admin/settings/:PVNumberOfConsecutiveDigitsAllowed``

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

You can set the value of "#THIS PAGE#" to the url of your Dataverse homepage, or any other page on your site that is accessible to anonymous users and will have the isPassive.js file loaded.

``curl -X PUT -d true http://localhost:8080/api/admin/settings/:ShibPassiveLoginEnabled``

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

:UploadMethods
++++++++++++++

This setting is experimental and to be used with the Data Capture Module (DCM). For now, if you set the upload methods to ``dcm/rsync+ssh`` it will allow your users to download rsync scripts from the DCM.

``curl -X PUT -d 'dcm/rsync+ssh' http://localhost:8080/api/admin/settings/:UploadMethods``

:DownloadMethods
++++++++++++++++

This setting is experimental and related to Repository Storage Abstraction Layer (RSAL). As of this writing it has no effect.

:GuestbookResponsesPageDisplayLimit
+++++++++++++++++++++++++++++++++++

Limit on how many guestbook entries to display on the guestbook-responses page. By default, only the 5000 most recent entries will be shown. Use the standard settings API in order to change the limit. For example, to set it to 10,000, make the following API call: 

``curl -X PUT -d 10000 http://localhost:8080/api/admin/settings/:GuestbookResponsesPageDisplayLimit`` 
