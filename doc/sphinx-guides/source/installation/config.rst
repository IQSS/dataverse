=============
Configuration
=============

Now that you've successfully logged into Dataverse with a superuser account after going through a basic :doc:`installation-main`, you'll need to secure and configure your installation.

Settings within Dataverse itself are managed via JVM options or by manipulating values in the ``setting`` table directly or through API calls. Configuring Solr requires manipulating XML files.

Once you have finished securing and configuring your Dataverse installation, proceed to the :doc:`administration` section. Advanced configuration topics are covered in the :doc:`r-rapache-tworavens` and :doc:`shibboleth` sections.

.. contents:: :local:

Securing Your Installation
--------------------------

Blocking API Endpoints
++++++++++++++++++++++

The :doc:`/api/native-api` contains a useful but potentially dangerous API endpoint called "admin" that allows you to change system settings, make ordinary users into superusers, and more. The ``builtin-users`` endpoint lets people create a local/builtin user account if they know the ``BuiltinUsers.KEY`` value described below.

By default, all APIs can be operated on remotely and without the need for any authentication. https://github.com/IQSS/dataverse/issues/1886 was opened to explore changing these defaults, but until then it is very important to block both the "admin" endpoint (and at least consider blocking ``builtin-users``). For details please see also the section on ``:BlockedApiPolicy`` below.

Forcing HTTPS
+++++++++++++

To avoid having your users send credentials in the clear, it's strongly recommended to force all web traffic to go through HTTPS (port 443) rather than HTTP (port 80). The ease with which one can install a valid SSL cert into Apache compared with the same operation in Glassfish might be a compelling enough reason to front Glassfish with Apache. In addition, Apache can be configured to rewrite HTTP to HTTPS with rules such as those found at https://wiki.apache.org/httpd/RewriteHTTPToHTTPS or in the section on :doc:`shibboleth`.

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

The need to redirect port HTTP (port 80) to HTTPS (port 443) for security has already been mentioned above and the fact that Glassfish puts these services on 8080 and 8181, respectively, was touched on in the :doc:`installation-main` section. You have a few options that basically boil down to if you want to introduce Apache into the mix or not. If you need :doc:`shibboleth` support you need Apache and you should proceed directly to that doc for guidance on fronting Glassfish with Apache.

If you don't want to front Glassfish with a proxy such as Apache or nginx, you will need to configure Glassfish to run HTTPS on 443 like this:

``asadmin set server-config.network-config.network-listeners.network-listener.http-listener-2.port=443``

Most likely you'll want to put a valid cert into Glassfish, which is certainly possible but out of scope for this guide.

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

Persistent Identifiers and Publishing Datasets
++++++++++++++++++++++++++++++++++++++++++++++

Persistent identifiers are a required and integral part of the Dataverse platform. They provide a URL that is guaranteed to resolve to the datasets they represent. Dataverse currently supports creating identifiers using DOI and additionally displaying identifiers created using HDL. By default and for testing convenience, the installer configures a temporary DOI test namespace through EZID. This is sufficient to create and publish datasets but they are not citable nor guaranteed to be preserved. To properly configure persistent identifiers for a production installation, an account and associated namespace must be acquired for a fee from one of two DOI providers: EZID (http://ezid.cdlib.org)  or DataCite (https://www.datacite.org). Once account credentials and DOI namespace have been acquired, please complete the following identifier configuration parameters:

JVM Options: :ref:`doi.baseurlstring`, :ref:`doi.username`, :ref:`doi.password`

Database Settings: :ref:`:DoiProvider <:DoiProvider>`, :ref:`:Protocol <:Protocol>`, :ref:`:Authority <:Authority>`, :ref:`:DoiSeparator <:DoiSeparator>`

Please note that any datasets creating using the test configuration cannot be directly migrated and would need to be created again once a valid DOI namespace is configured.

Customizing the Root Dataverse
++++++++++++++++++++++++++++++

As the person installing Dataverse you may or may not be local metadata expert. You may want to have others sign up for accounts and grant them the "Admin" role at the root dataverse to configure metadata fields, browse/search facets, templates, guestbooks, etc. For more on these topics, consult the :doc:`/user/dataverse-management` section of the User Guide.

Once this configuration is complete, your Dataverse installation should be ready for users to start playing with it. That said, there are many more configuration options available, which will be explained below.

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

For limiting the size of thumbnail images generated from files.

dataverse.dataAccess.thumbnail.pdf.limit
++++++++++++++++++++++++++++++++++++++++

For limiting the size of thumbnail images generated from files.

.. _doi.baseurlstring:

doi.baseurlstring
+++++++++++++++++

As of this writing "https://ezid.cdlib.org" and "https://mds.datacite.org" are the only valid values. See also these related database settings below:

- :DoiProvider
- :Protocol
- :Authority
- :DoiSeparator

.. _doi.username:

doi.username
++++++++++++

Used in conjuction with ``doi.baseurlstring``.

.. _doi.password:

doi.password
++++++++++++

Used in conjuction with ``doi.baseurlstring``.

dataverse.handlenet.admcredfile
+++++++++++++++++++++++++++++++

For Handle support (not fully developed).

dataverse.handlenet.admprivphrase
+++++++++++++++++++++++++++++++++
For Handle support (not fully developed).

Database Settings
-----------------

These settings are stored in the ``setting`` table but can be read and modified via the "admin" endpoint of the :doc:`/api/native-api` for easy scripting.

The most commonly used configuration options are listed first.

:BlockedApiPolicy
+++++++++++++++++

Out of the box, all API endpoints are completely open as mentioned in the section on security above. It is highly recommend that you choose one of the policies below and also configure ``:BlockedApiEndpoints``.

- localhost-only: Allow from localhost.
- unblock-key: Require a key defined in ``:BlockedApiKey``.
- drop: Disallow the blocked endpoints completely.

``curl -X PUT -d localhost-only http://localhost:8080/api/admin/settings/:BlockedApiEndpoints``

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

``curl -X PUT -d builtInS3kretKey http://localhost:8080/api/admin/settings/:BuiltinUsers.KEY``

:SystemEmail
++++++++++++

This is the email address that "system" emails are sent from such as password reset links.

``curl -X PUT -d "Support <support@example.edu>" http://localhost:8080/api/admin/settings/:SystemEmail``

:FooterCopyright
++++++++++++++++

By default the footer says "Copyright © [YYYY]" but you can add text after the year, as in the example below.

``curl -X PUT -d ", The President &#38; Fellows of Harvard College" http://localhost:8080/api/admin/settings/:FooterCopyright``

.. _:DoiProvider:

:DoiProvider
++++++++++++

As of this writing "EZID" and "DataCite" are the only valid options.

``curl -X PUT -d EZID http://localhost:8080/api/admin/settings/:DoiProvider``

.. _:Protocol:

:Protocol
+++++++++

As of this writing "doi" is the only valid option for the protocol for a persistent ID.

``curl -X PUT -d doi http://localhost:8080/api/admin/settings/:Protocol``

.. _:Authority:

:Authority
++++++++++

Use the DOI authority assigned to you by your DoiProvider.

``curl -X PUT -d 10.xxxx http://localhost:8080/api/admin/settings/:Authority``

.. _:DoiSeparator:

:DoiSeparator
+++++++++++++

It is recommended that you keep this as a slash ("/").

``curl -X PUT -d "/" http://localhost:8080/api/admin/settings/:DoiSeparator``

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

:GuidesBaseUrl
++++++++++++++

Set ``GuidesBaseUrl`` to override the default value "http://guides.dataverse.org". If you are interested in writing your own version of the guides, you may find the :doc:`/developers/documentation` section of the Developer Guide helpful.

``curl -X PUT -d http://dataverse.example.edu http://localhost:8080/api/admin/settings/:GuidesBaseUrl``

:StatusMessageHeader
++++++++++++++++++++

For dynamically adding information to the top of every page. For example, "For testing only..." at the top of https://demo.dataverse.org is set with this:

``curl -X PUT -d "For testing only..." http://localhost:8080/api/admin/settings/:StatusMessageHeader``

:MaxFileUploadSizeInBytes
+++++++++++++++++++++++++

Set `MaxFileUploadSizeInBytes` to "2147483648", for example, to limit the size of files uploaded to 2 GB.
Notes:
- For SWORD, this size is limited by the Java Integer.MAX_VALUE of 2,147,483,647. (see: https://github.com/IQSS/dataverse/issues/2169)
- If the MaxFileUploadSizeInBytes is NOT set, uploads, including SWORD may be of unlimited size.

``curl -X PUT -d 2147483648 http://localhost:8080/api/admin/settings/:MaxFileUploadSizeInBytes``

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

By default Dataverse will attempt to connect to Solr on port 8983 on localhost. Use this setting to change the hostname or port.

``curl -X PUT -d localhost:8983 http://localhost:8080/api/admin/settings/:SolrHostColonPort``

:SignUpUrl
++++++++++

The relative path URL to which users will be sent after signup. The default setting is below.

``curl -X PUT -d true /dataverseuser.xhtml?editMode=CREATE http://localhost:8080/api/admin/settings/:SignUpUrl``

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

The duration in minutes before "Confirm Email" URLs expire. The default is 1440 minutes (24 hours).  See also :doc:`/installation/administration`.

:ShibEnabled
++++++++++++

This setting is experimental per :doc:`/installation/shibboleth`.

:AllowSignUp
++++++++++++

Set to false to disallow local accounts to be created if you are using :doc:`shibboleth` but not for production use until https://github.com/IQSS/dataverse/issues/2838 has been fixed.

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
