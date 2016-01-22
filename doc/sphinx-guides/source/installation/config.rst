=============
Configuration
=============

Some of the configuration of Dataverse was performed by the installer script described in the :doc:`installation-main` section, but Dataverse settings can be further tweaked via settings in the database, JVM options, and configuration files.

.. contents:: :local:

Database Settings
+++++++++++++++++

These settings are stored in the ``setting`` table but are available via the "admin" API for easy scripting.

:ApplicationPrivacyPolicyUrl
----------------------------

Specify a URL where users can read your Privacy Policy, linked from the bottom of the page.

``curl -X PUT -d http://best-practices.dataverse.org/harvard-policies/harvard-privacy-policy.html http://localhost:8080/api/admin/settings/:ApplicationPrivacyPolicyUrl``

:ApplicationTermsOfUse
----------------------

Upload a text file containing the Terms of Use to be displayed at sign up.

``curl -X PUT -d@/tmp/apptou.html http://localhost:8080/api/admin/settings/:ApplicationTermsOfUse``

:ApiTermsOfUse
--------------

Upload a text file containing the API Terms of Use.

``curl -X PUT -d@/tmp/api-tos.txt http://localhost:8080/api/admin/settings/:ApiTermsOfUse``

:SolrHostColonPort
------------------

Set ``SolrHostColonPort`` to override ``localhost:8983``.

``curl -X PUT -d localhost:8983 http://localhost:8080/api/admin/settings/:SolrHostColonPort``

:SearchHighlightFragmentSize
----------------------------

Set ``SearchHighlightFragmentSize`` to override the default value of 100 from https://wiki.apache.org/solr/HighlightingParameters#hl.fragsize

``curl -X PUT -d 320 http://localhost:8080/api/admin/settings/:SearchHighlightFragmentSize``

:ShibEnabled
------------

This setting is experimental per :doc:`/installation/shibboleth`.

:MaxFileUploadSizeInBytes
-------------------------

Set `MaxFileUploadSizeInBytes` to "2147483648", for example, to limit the size of files uploaded to 2 GB. 
Notes:
- For SWORD, this size is limited by the Java Integer.MAX_VALUE of 2,147,483,647. (see: https://github.com/IQSS/dataverse/issues/2169)
- If the MaxFileUploadSizeInBytes is NOT set, uploads, including SWORD may be of unlimited size.

``curl -X PUT -d 2147483648 http://localhost:8080/api/admin/settings/:MaxFileUploadSizeInBytes``

:GuidesBaseUrl
--------------

Set ``GuidesBaseUrl`` to override the default value "http://guides.dataverse.org".

``curl -X PUT -d http://dataverse.example.edu http://localhost:8080/api/admin/settings/:GuidesBaseUrl``

:GeoconnectCreateEditMaps
-------------------------

Set ``GeoconnectCreateEditMaps`` to true to allow the user to create GeoConnect Maps. This boolean effects whether the user sees the map button on the dataset page and if the ingest will create a shape file.

``curl -X PUT -d true http://localhost:8080/api/admin/settings/:GeoconnectCreateEditMaps``

:GeoconnectViewMaps
-------------------

Set ``GeoconnectViewMaps`` to true to allow a user to view existing maps. This boolean effects whether a user will see the "Explore" button.

``curl -X PUT -d true http://localhost:8080/api/admin/settings/:GeoconnectViewMaps``


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

Dropbox Configuration
++++++++++++++++++++++

- Add JVM option in the domain.xml: 
``asadmin create-jvm-options "-Ddataverse.dropbox.key=<Enter your dropbox key here>"``


Solr
++++

The :doc:`prerequisites` section explained that Dataverse requires a specific Solr schema file called `schema.xml` that can be found in the Dataverse distribution. You should have already replaced the default `example/solr/collection1/conf/schema.xml` file that ships with Solr.

If ``WARN  org.eclipse.jetty.http.HttpParser  – HttpParser Full for /127.0.0.1:8983`` appears in the Solr log, adding ``<Set name="requestHeaderSize">8192</Set>`` (or a higher number of bytes) to Solr's jetty.xml in the section matching the XPath expression ``//Call[@name='addConnector']/Arg/New[@class='org.eclipse.jetty.server.bio.SocketConnector']`` may resolve the issue.  See also https://support.lucidworks.com/hc/en-us/articles/201424796-Error-when-submitting-large-query-strings-
