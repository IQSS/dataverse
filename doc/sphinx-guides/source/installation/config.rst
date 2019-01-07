=============
Configuration
=============

Now that you've successfully logged into Dataverse with a superuser account after going through a basic :doc:`installation-main`, you'll need to secure and configure your installation.

Settings within Dataverse itself are managed via JVM options or by manipulating values in the ``setting`` table directly or through API calls.

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

It's also possible to prevent file uploads via API by adjusting the ``:UploadMethods`` database setting.

Forcing HTTPS
+++++++++++++

To avoid having your users send credentials in the clear, it's strongly recommended to force all web traffic to go through HTTPS (port 443) rather than HTTP (port 80). The ease with which one can install a valid SSL cert into Apache compared with the same operation in Glassfish might be a compelling enough reason to front Glassfish with Apache. In addition, Apache can be configured to rewrite HTTP to HTTPS with rules such as those found at https://wiki.apache.org/httpd/RewriteHTTPToHTTPS or in the section on :doc:`shibboleth`.

Privacy Considerations
++++++++++++++++++++++

Out of the box, Dataverse will list email addresses of the "contacts" for datasets when users visit a dataset page and click the "Export Metadata" button. If you prefer to exclude email addresses of dataset contacts from metadata export, set :ref:`:ExcludeEmailFromExport <:ExcludeEmailFromExport>` to true.

Additional Recommendations
++++++++++++++++++++++++++
Run Glassfish as a User Other Than Root
+++++++++++++++++++++++++++++++++++++++

See the Glassfish section of :doc:`prerequisites` for details and init scripts for running Glassfish as non-root.

Related to this is that you should remove ``/root/.glassfish/pass`` to ensure that Glassfish isn't ever accidentally started as root. Without the password, Glassfish won't be able to start as root, which is a good thing.

Enforce Strong Passwords for User Accounts
++++++++++++++++++++++++++++++++++++++++++

Dataverse only stores passwords (as salted hash, and using a strong hashing algorithm) for "builtin" users. You can increase the password complexity rules to meet your security needs. If you have configured your Dataverse installation to allow login from remote authentication providers such as Shibboleth, ORCID, GitHub or Google, you do not have any control over those remote providers' password complexity rules. See the "Auth Modes: Local vs. Remote vs. Both" section below for more on login options.

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

Network Ports
-------------

Remember how under "Decisions to Make" in the :doc:`prep` section we mentioned you'll need to make a decision about whether or not to introduce a proxy in front of Dataverse such as Apache or nginx? The time has come to make that decision.

The need to redirect port HTTP (port 80) to HTTPS (port 443) for security has already been mentioned above and the fact that Glassfish puts these services on 8080 and 8181, respectively, was touched on in the :doc:`installation-main` section. In production, you don't want to tell your users to use Dataverse on ports 8080 and 8181. You should have them use the standard HTTPS port, which is 443.

Your decision to proxy or not should primarily be driven by which features of Dataverse you'd like to use. If you'd like to use Shibboleth, the decision is easy because proxying or "fronting" Glassfish with Apache is required. The details are covered in the :doc:`shibboleth` section.

If you'd like to use TwoRavens, you should also consider fronting with Apache because you will be required to install an Apache anyway to make use of the rApache module. For details, see the :doc:`r-rapache-tworavens` section.

Even if you have no interest in Shibboleth nor TwoRavens, you may want to front Dataverse with Apache or nginx to simply the process of installing SSL certificates. There are many tutorials on the Internet for adding certs to Apache, including a some `notes used by the Dataverse team <https://github.com/IQSS/dataverse/blob/v4.6.1/doc/shib/shib.md>`_, but the process of adding a certificate to Glassfish is arduous and not for the faint of heart. The Dataverse team cannot provide much help with adding certificates to Glassfish beyond linking to `tips <http://stackoverflow.com/questions/906402/importing-an-existing-x509-certificate-and-private-key-in-java-keystore-to-use-i>`_ on the web.

Still not convinced you should put Glassfish behind another web server? Even if you manage to get your SSL certificate into Glassfish, how are you going to run Glassfish on low ports such as 80 and 443? Are you going to run Glassfish as root? Bad idea. This is a security risk. Under "Additional Recommendations" under "Securing Your Installation" above you are advised to configure Glassfish to run as a user other than root. (The Dataverse team will close https://github.com/IQSS/dataverse/issues/1934 after updating the Glassfish init script provided in the :doc:`prerequisites` section to not require root.)

There's also the issue of serving a production-ready version of robots.txt. By using a proxy such as Apache, this is a one-time "set it and forget it" step as explained below in the "Going Live" section.

If you are convinced you'd like to try fronting Glassfish with Apache, the :doc:`shibboleth` section should be good resource for you.

If you really don't want to front Glassfish with any proxy (not recommended), you can configure Glassfish to run HTTPS on port 443 like this:

``./asadmin set server-config.network-config.network-listeners.network-listener.http-listener-2.port=443``

What about port 80? Even if you don't front Dataverse with Apache, you may want to let Apache run on port 80 just to rewrite HTTP to HTTPS as described above. You can use a similar command as above to change the HTTP port that Glassfish uses from 8080 to 80 (substitute ``http-listener-1.port=80``). Glassfish can be used to enforce HTTPS on its own without Apache, but configuring this is an exercise for the reader. Answers here may be helpful: http://stackoverflow.com/questions/25122025/glassfish-v4-java-7-port-unification-error-not-able-to-redirect-http-to

If you are running an installation with Apache and Glassfish on the same server, and would like to restrict Glassfish from responding to any requests to port 8080 from external hosts (in other words, not through Apache), you can restrict the AJP listener to localhost only with:

``./asadmin set server-config.network-config.network-listeners.network-listener.http-listener-1.address=127.0.0.1``

You should **NOT** use the configuration option above if you are running in a load-balanced environment, or otherwise have the web server on a different host than the application server.

Root Dataverse Permissions
--------------------------

The user who creates a dataverse is given the "Admin" role on that dataverse. The root dataverse is created automatically for you by the installer and the "Admin" is the superuser account ("dataverseAdmin") we used in the :doc:`installation-main` section to confirm that we can log in. These next steps of configuring the root dataverse require the "Admin" role on the root dataverse, but not the much more powerful superuser attribute. In short, users with the "Admin" role are subject to the permission system. A superuser, on the other hand, completely bypasses the permission system. You can give non-superusers the "Admin" role on the root dataverse if you'd like them to configure the root dataverse.

In order for non-superusers to start creating dataverses or datasets, you need click "Edit" then "Permissions" and make choices about which users can add dataverses or datasets within the root dataverse. (There is an API endpoint for this operation as well.) Again, the user who creates a dataverse will be granted the "Admin" role on that dataverse. Non-superusers who are not "Admin" on the root dataverse will not be able to to do anything useful until the root dataverse has been published.

As the person installing Dataverse you may or may not be a local metadata expert. You may want to have others sign up for accounts and grant them the "Admin" role at the root dataverse to configure metadata fields, templates, browse/search facets, guestbooks, etc. For more on these topics, consult the :doc:`/user/dataverse-management` section of the User Guide.

Persistent Identifiers and Publishing Datasets
----------------------------------------------

Persistent identifiers are a required and integral part of the Dataverse platform. They provide a URL that is guaranteed to resolve to the datasets or files they represent. Dataverse currently supports creating identifiers using DOI and Handle.

By default, the installer configures a test DOI namespace (10.5072) with DataCite as the registration provider. Please note that as of the release 4.9.3, we can no longer use EZID as the provider. Unlike EZID, DataCite requires that you register for a test account (please contact support@datacite.org). Once you receive the login name and password for the account, configure it in your domain.xml, as the following two JVM options::

      <jvm-options>-Ddoi.username=...</jvm-options>
      <jvm-options>-Ddoi.password=...</jvm-options>

and restart Glassfish. Once this is done, you will be able to publish datasets and files, but the persistent identifiers will not be citable or guaranteed to be preserved. Note that any datasets or files created using the test configuration cannot be directly migrated and would need to be created again once a valid DOI namespace is configured. 

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

``auth_type`` can either be ``keystone``, ``keystone_v3``, or it will assumed to be ``basic``. ``auth_url`` should be your keystone authentication URL which includes the tokens (e.g. for keystone, ``https://openstack.example.edu:35357/v2.0/tokens`` and for keystone_v3, ``https://openstack.example.edu:35357/v3/auth/tokens``). ``swift_endpoint`` is a URL that look something like ``http://rdgw.swift.example.org/swift/v1``.

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

You also have the option to set a custom expiration length for a generated temporary URL. It is initialized to 60 seconds, but you can change it by running the create command:

``./asadmin $ASADMIN_OPTS create-jvm-options "\-Ddataverse.files.temp_url_expire=3600"``

In this example, you would be setting the expiration length for one hour.


Setting up Compute
+++++++++++++++++++

Once you have configured a Swift Object Storage backend, you also have the option of enabling a connection to a computing environment. To do so, you need to configure the database settings for :ref:`:ComputeBaseUrl` and  :ref:`:CloudEnvironmentName`.

Once you have set up ``:ComputeBaseUrl`` properly in both Dataverse and your cloud environment, validated users will have three options for accessing the computing environment:

- Compute on a single dataset
- Compute on multiple datasets
- Compute on a single datafile

The compute buttons on dataset and file pages will link validated users to your computing environment. If a user is computing on one dataset, the compute button will redirect to:

``:ComputeBaseUrl?datasetPersistentId``

If a user is computing on multiple datasets, the compute button will redirect to:

``:ComputeBaseUrl/multiparty?datasetPersistentId&anotherDatasetPersistentId&anotherDatasetPersistentId&...``

If a user is computing on a single file, depending on the configuration of your installation, the compute button will either redirect to: 

``:ComputeBaseUrl?datasetPersistentId=yourObject``

if your installation's :ref:`:PublicInstall` setting is true, or:

``:ComputeBaseUrl?datasetPersistentId=yourObject&temp_url_sig=yourTempUrlSig&temp_url_expires=yourTempUrlExpiry``

You can configure this redirect properly in your cloud environment to generate a temporary URL for access to the Swift objects for computing.

Amazon S3 Storage (or Compatible)
+++++++++++++++++++++++++++++++++

For institutions and organizations looking to use some kind of S3-based object storage for files uploaded to Dataverse,
this is entirely possible. You can either use Amazon Web Services or use some other, even on-site S3-compatible
storage (like Minio, Ceph RADOS S3 Gateway and many more). 

**Note:** The Dataverse Team is most familiar with AWS S3, and can provide support on its usage with Dataverse. Thanks to community contributions, the application's architecture also allows non-AWS S3 providers. The Dataverse Team can provide very limited support on these other providers. We recommend reaching out to the wider Dataverse community if you have questions.

First: Set Up Accounts and Access Credentials
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Dataverse and the AWS SDK make use of the "AWS credentials profile file" and "AWS config profile file" located in
``~/.aws/`` where ``~`` is the home directory of the user you run Glassfish as. This file can be generated via either
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


Reported Working S3-Compatible Storage
######################################

`Minio v2018-09-12 <http://minio.io>`_
  Set ``dataverse.files.s3-path-style-access=true``, as Minio works path-based. Works pretty smooth, easy to setup.
  **Can be used for quick testing, too:** just use the example values above. Uses the public (read: unsecure and
  possibly slow) https://play.minio.io:9000 service.


**HINT:** If you are successfully using an S3 storage implementation not yet listed above, please feel free to
`open an issue at Github <https://github.com/IQSS/dataverse/issues/new>`_ and describe your setup.
We will be glad to add it here.


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

Place these two files in a folder named ``.aws`` under the home directory for the user running your Dataverse Glassfish
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

With access to your bucket in place, we'll want to navigate to ``/usr/local/glassfish4/glassfish/bin/``
and execute the following ``asadmin`` commands to set up the proper JVM options. Recall that out of the box, Dataverse
is configured to use local file storage. You'll need to delete the existing storage driver before setting the new one.

::

  ./asadmin $ASADMIN_OPTS delete-jvm-options "-Ddataverse.files.storage-driver-id=file"
  ./asadmin $ASADMIN_OPTS create-jvm-options "-Ddataverse.files.storage-driver-id=s3"

Then, we'll need to identify which S3 bucket we're using. Replace ``your_bucket_name`` with, of course, your bucket:

``./asadmin create-jvm-options "-Ddataverse.files.s3-bucket-name=your_bucket_name"``

Optionally, you can have users download files from S3 directly rather than having files pass from S3 through Glassfish to your users. To accomplish this, set ``dataverse.files.s3-download-redirect`` to ``true`` like this:

``./asadmin create-jvm-options "-Ddataverse.files.s3-download-redirect=true"``

If you enable ``dataverse.files.s3-download-redirect`` as described above, note that the S3 URLs expire after an hour by default but you can configure the expiration time using the ``dataverse.files.s3-url-expiration-minutes`` JVM option. Here's an example of setting the expiration time to 120 minutes:

``./asadmin create-jvm-options "-Ddataverse.files.s3-url-expiration-minutes=120"``

In case you would like to configure Dataverse to use a custom S3 service instead of Amazon S3 services, please
add the options for the custom URL and region as documented below. Please read above if your desired combination has
been tested already and what other options have been set for a successful integration.

Lastly, go ahead and restart your glassfish server. With Dataverse deployed and the site online, you should be able to upload datasets and data files and see the corresponding files in your S3 bucket. Within a bucket, the folder structure emulates that found in local file storage.

S3 Storage Options
##################

=========================================  ==================  ==================================================================  =============
JVM Option                                 Value               Description                                                         Default value
=========================================  ==================  ==================================================================  =============
dataverse.files.storage-driver-id          s3                  Enable S3 storage driver.                                           ``file``
dataverse.files.s3-bucket-name             <?>                 The bucket name. See above.                                         (none)
dataverse.files.s3-download-redirect       ``true``/``false``  Enable direct download or proxy through Dataverse.                  ``false``
dataverse.files.s3-url-expiration-minutes  <?>                 If direct downloads: time until links expire. Optional.             60
dataverse.files.s3-custom-endpoint-url     <?>                 Use custom S3 endpoint. Needs URL either with or without protocol.  (none)
dataverse.files.s3-custom-endpoint-region  <?>                 Only used when using custom endpoint. Optional.                     ``dataverse``
dataverse.files.s3-path-style-access       ``true``/``false``  Use path style buckets instead of subdomains. Optional.             ``false``
=========================================  ==================  ==================================================================  =============

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

The custom logo image file is expected to be small enough to fit comfortably in the navbar, no more than 50 pixels in height and 160 pixels in width. Create a ``navbar`` directory in your Glassfish ``logos`` directory and place your custom logo there. By Glassfish default, your logo image file will be located at ``/usr/local/glassfish4/glassfish/domains/domain1/docroot/logos/navbar/logo.png``.

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

You have a couple of options for putting an updated robots.txt file into production. If you are fronting Glassfish with Apache as recommended above, you can place robots.txt in the root of the directory specified in your ``VirtualHost`` and to your Apache config a ``ProxyPassMatch`` line like the one below to prevent Glassfish from serving the version of robots.txt that is embedded in the Dataverse war file:

.. code-block:: text

    # don't let Glassfish serve its version of robots.txt
    ProxyPassMatch ^/robots.txt$ !

For more of an explanation of ``ProxyPassMatch`` see the :doc:`shibboleth` section.

If you are not fronting Glassfish with Apache you'll need to prevent Glassfish from serving the robots.txt file embedded in the war file by overwriting robots.txt after the war file has been deployed. The downside of this technique is that you will have to remember to overwrite robots.txt in the "exploded" war file each time you deploy the war file, which probably means each time you upgrade to a new version of Dataverse. Furthermore, since the version of Dataverse is always incrementing and the version can be part of the file path, you will need to be conscious of where on disk you need to replace the file. For example, for Dataverse 4.6.1 the path to robots.txt may be ``/usr/local/glassfish4/glassfish/domains/domain1/applications/dataverse-4.6.1/robots.txt`` with the version number ``4.6.1`` as part of the path.

Creating a Sitemap and Submitting it to Search Engines
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Search engines have an easier time indexing content when you provide them a sitemap. The Dataverse sitemap includes URLs to all published dataverses and all published datasets that are not harvested or deaccessioned.

Create or update your sitemap by adding the following curl command to cron to run nightly or as you see fit:

``curl -X POST http://localhost:8080/api/admin/sitemap``

This will create or update a file in the following location unless you have customized your installation directory for Glassfish:

``/usr/local/glassfish4/glassfish/domains/domain1/docroot/sitemap/sitemap.xml``

On an installation of Dataverse with many datasets, the creation or updating of the sitemap can take a while. You can check Glassfish's server.log file for "BEGIN updateSiteMap" and "END updateSiteMap" lines to know when the process started and stopped and any errors in between.

https://demo.dataverse.org/sitemap.xml is the sitemap URL for the Dataverse Demo site and yours should be similar. Submit your sitemap URL to Google by following `Google's "submit a sitemap" instructions`_ or similar instructions for other search engines.

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

JVM Options
-----------

JVM stands Java Virtual Machine and as a Java application, Glassfish can read JVM options when it is started. A number of JVM options are configured by the installer below is a complete list of the Dataverse-specific JVM options. You can inspect the configured options by running:

``./asadmin list-jvm-options | egrep 'dataverse|doi'``

When changing values these values with ``asadmin``, you'll need to delete the old value before adding a new one, like this:

``./asadmin delete-jvm-options "-Ddataverse.fqdn=old.example.com"``

``./asadmin create-jvm-options "-Ddataverse.fqdn=dataverse.example.com"``

It's also possible to change these values by stopping Glassfish, editing ``glassfish4/glassfish/domains/domain1/config/domain.xml``, and restarting Glassfish.

dataverse.fqdn
++++++++++++++

If the Dataverse server has multiple DNS names, this option specifies the one to be used as the "official" host name. For example, you may want to have dataverse.example.edu, and not the less appealing server-123.socsci.example.edu to appear exclusively in all the registered global identifiers, Data Deposit API records, etc.

The password reset feature requires ``dataverse.fqdn`` to be configured.

| Do note that whenever the system needs to form a service URL, by default, it will be formed with ``https://`` and port 443. I.e.,
| ``https://{dataverse.fqdn}/``
| If that does not suit your setup, you can define an additional option, ``dataverse.siteUrl``, explained below.

dataverse.siteUrl
+++++++++++++++++

| and specify the protocol and port number you would prefer to be used to advertise the URL for your Dataverse.
| For example, configured in domain.xml:
| ``<jvm-options>-Ddataverse.fqdn=dataverse.example.edu</jvm-options>``
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
+++++++++++++++++++++++++++++++++
If you want to use different index than the default 300

.. _dataverse.timerServer:

dataverse.timerServer
+++++++++++++++++++++

This JVM option is only relevant if you plan to run multiple Glassfish servers for redundancy. Only one Glassfish server can act as the dedicated timer server and for details on promoting or demoting a Glassfish server to handle this responsibility, see :doc:`/admin/timers`.

.. _dataverse.lang.directory:

dataverse.lang.directory
++++++++++++++++++++++++

This JVM option is used to configure the path where all the language specific property files are to be stored.  If this option is set then the english property file must be present in the path along with any other language property file.

``./asadmin create-jvm-options '-Ddataverse.lang.directory=PATH_LOCATION_HERE'``

If this value is not set, by default, a Dataverse installation will read the English language property files from the Java Application.

dataverse.files.hide-schema-dot-org-download-urls
+++++++++++++++++++++++++++++++++++++++++++++++++

Please note that this setting is experimental.

By default, download URLs to files will be included in Schema.org JSON-LD output. To prevent these URLs from being included in the output, set ``dataverse.files.hide-schema-dot-org-download-urls`` to true as in the example below.

``./asadmin create-jvm-options '-Ddataverse.files.hide-schema-dot-org-download-urls=true'``

Please note that there are other reasons why download URLs may not be included for certain files such as if a guestbook entry is required or if the file is restricted.

For more on Schema.org JSON-LD, see the :doc:`/admin/metadataexport` section of the Admin Guide.

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

:FooterCopyright
++++++++++++++++

By default the footer says "Copyright © [YYYY]" but you can add text after the year, as in the example below.

``curl -X PUT -d ", Your Institution" http://localhost:8080/api/admin/settings/:FooterCopyright``

.. _:DoiProvider:

:DoiProvider
++++++++++++

As of this writing "DataCite" and "EZID" are the only valid options for production installations. Developers are welcome to use "FAKE". ``:DoiProvider`` is only needed if you are using DOI.

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

For systems using Postgresql 8.4 or older, the procedural language `plpgsql` should be enabled first.
We have provided an example :download:`here </_static/util/pg8-createsequence-prep.sql>`.

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

Toggles publishing of file-based PIDs for the entire installation. By default this setting is absent and Dataverse assumes it to be true.

If you don't want to register file-based PIDs for your installation, set:

``curl -X PUT -d 'false' http://localhost:8080/api/admin/settings/:FilePIDsEnabled``

Note: File-level PID registration was added in 4.9 and is required until version 4.9.3.

:ApplicationTermsOfUse
++++++++++++++++++++++

Upload an HTML file containing the Terms of Use to be displayed at sign up. Supported HTML tags are listed under the :doc:`/user/dataset-management` section of the User Guide.

``curl -X PUT -d@/tmp/apptou.html http://localhost:8080/api/admin/settings/:ApplicationTermsOfUse``

Unfortunately, in most cases, the text file will probably be too big to upload (>1024 characters) due to a bug. A workaround has been posted to https://github.com/IQSS/dataverse/issues/2669

:ApplicationPrivacyPolicyUrl
++++++++++++++++++++++++++++

Specify a URL where users can read your Privacy Policy, linked from the bottom of the page.

``curl -X PUT -d https://dataverse.org/best-practices/harvard-dataverse-privacy-policy http://localhost:8080/api/admin/settings/:ApplicationPrivacyPolicyUrl``

:ApiTermsOfUse
++++++++++++++

Specify a URL where users can read your API Terms of Use.
API users can retrieve this URL from the SWORD Service Document or the "info" section of our :doc:`/api/native-api` documentation.

``curl -X PUT -d https://dataverse.org/best-practices/harvard-api-tou http://localhost:8080/api/admin/settings/:ApiTermsOfUse``


.. _:ExcludeEmailFromExport:

:ExcludeEmailFromExport
+++++++++++++++++++++++

Set ``:ExcludeEmailFromExport`` to prevent email addresses for dataset contacts from being exposed in XML or JSON representations of dataset metadata. For a list exported formats such as DDI, see the :doc:`/admin/metadataexport` section of the Admin Guide.

``curl -X PUT -d true http://localhost:8080/api/admin/settings/:ExcludeEmailFromExport``

:NavbarAboutUrl
+++++++++++++++

Set ``NavbarAboutUrl`` to a fully-qualified URL which will be used for the "About" link in the navbar. 

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

:NavbarSupportUrl
+++++++++++++++++
Set ``:NavbarSupportUrl`` to a fully-qualified URL which will be used for the "Support" link in the navbar.

Note that this will override the default behaviour for the "Support" menu option, which is to display the dataverse 'feedback' dialog.

``curl -X PUT -d http://dataverse.example.edu/supportpage.html http://localhost:8080/api/admin/settings/:NavbarSupportUrl``

:MetricsUrl
+++++++++++

Make the metrics component on the root dataverse a clickable link to a website where you present metrics on your Dataverse installation. This could perhaps be an installation of https://github.com/IQSS/miniverse or any site.

``curl -X PUT -d http://metrics.dataverse.example.edu http://localhost:8080/api/admin/settings/:MetricsUrl``

:StatusMessageHeader
++++++++++++++++++++

For dynamically adding an informational header to the top of every page. StatusMessageText must also be set for a message to show. For example, "For testing only..." at the top of https://demo.dataverse.org is set with this:

``curl -X PUT -d "For testing only..." http://localhost:8080/api/admin/settings/:StatusMessageHeader``

You can make the text clickable and include an additional message in a pop up by setting ``:StatusMessageText``.

:StatusMessageText
++++++++++++++++++

Alongside the ``:StatusMessageHeader`` you need to add StatusMessageText for the message to show.:

``curl -X PUT -d "This appears in a popup." http://localhost:8080/api/admin/settings/:StatusMessageText``

:MaxFileUploadSizeInBytes
+++++++++++++++++++++++++

Set `MaxFileUploadSizeInBytes` to "2147483648", for example, to limit the size of files uploaded to 2 GB.

Notes:

- For SWORD, this size is limited by the Java Integer.MAX_VALUE of 2,147,483,647. (see: https://github.com/IQSS/dataverse/issues/2169)

- If the MaxFileUploadSizeInBytes is NOT set, uploads, including SWORD may be of unlimited size.

- For larger file upload sizes, you may need to configure your reverse proxy timeout. If using apache2 (httpd) with Shibboleth, add a timeout to the ProxyPass defined in etc/httpd/conf.d/ssl.conf (which is described in the :doc:`/installation/shibboleth` setup).

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

You can override this global setting on a per-format basis for the following formats:

- DTA
- POR
- SAV
- Rdata
- CSV
- XLSX

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

:TwoRavensUrl
+++++++++++++

The ``:TwoRavensUrl`` option is no longer valid. See :doc:`r-rapache-tworavens` and :doc:`external-tools`.

:TwoRavensTabularView
+++++++++++++++++++++

The ``:TwoRavensTabularView`` option is no longer valid. See :doc:`r-rapache-tworavens` and :doc:`external-tools`.

:GeoconnectCreateEditMaps
+++++++++++++++++++++++++

Set ``GeoconnectCreateEditMaps`` to true to allow the user to create GeoConnect Maps. This boolean effects whether the user sees the map button on the dataset page and if the ingest will create a shape file.

``curl -X PUT -d true http://localhost:8080/api/admin/settings/:GeoconnectCreateEditMaps``

:GeoconnectViewMaps
+++++++++++++++++++

Set ``GeoconnectViewMaps`` to true to allow a user to view existing maps. This boolean effects whether a user will see the "Explore" button.

``curl -X PUT -d true http://localhost:8080/api/admin/settings/:GeoconnectViewMaps``

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

:PiwikAnalyticsTrackerFileName
++++++++++++++++++++++++++++++

Filename for the 'php' and 'js' tracker files used in the Piwik code (piwik.php and piwik.js).
Sometimes these files are renamed in order to prevent ad-blockers (in the browser) to block the Piwik tracking code.
This sets the base name (without dot and extension), if not set it defaults to 'piwik'.

``curl -X PUT -d domainstats http://localhost:8080/api/admin/settings/:PiwikAnalyticsTrackerFileName``


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

This setting controls which upload methods are available to users of your installation of Dataverse. The following upload methods are available:

- ``native/http``: Corresponds to "Upload with HTTP via your browser" and APIs that use HTTP (SWORD and native).
- ``dcm/rsync+ssh``: Corresponds to "Upload with rsync+ssh via Data Capture Module (DCM)". A lot of setup is required, as explained in the :doc:`/developers/big-data-support` section of the Dev Guide.

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

:Languages
++++++++++

Sets which languages should be available. If there is more than one, a dropdown is displayed
in the header. This should be formated as a JSON array as shown below.

``curl http://localhost:8080/api/admin/settings/:Languages -X PUT -d '[{  "locale":"en", "title":"English"},  {  "locale":"fr", "title":"Français"}]'``

:InheritParentRoleAssignments
+++++++++++++++++++++++++++++

``:InheritParentRoleAssignments`` can be set to a comma-separated list of role aliases or '*' (all) to cause newly created Dataverses to inherit the set of users and/or internal groups who have assignments for those role(s) on the parent Dataverse, i.e. those users/groups will be assigned the same role(s) on the new Dataverse (in addition to the creator of the new Dataverse having an admin role). 
This can be helpful in situations where multiple organizations are sharing one Dataverse instance. The default, if ``::InheritParentRoleAssignments`` is not set is for the creator of the new Dataverse to be the only one assigned a role.

``curl -X PUT -d 'admin, curator' http://localhost:8080/api/admin/settings/:InheritParentRoleAssignments``
or 
``curl -X PUT -d '*' http://localhost:8080/api/admin/settings/:InheritParentRoleAssignments``

