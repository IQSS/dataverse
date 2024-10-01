Big Data Support
================

Big data support includes some experimental options. Eventually more of this content will move to the Installation Guide.

.. contents:: |toctitle|
        :local:

Various components will need to be installed and/or configured for big data support via the methods described below.

S3 Direct Upload and Download
-----------------------------

A lightweight option for supporting file sizes beyond a few gigabytes - a size that can cause performance issues when uploaded through a Dataverse installation itself - is to configure an S3 store to provide direct upload and download via 'pre-signed URLs'. When these options are configured, file uploads and downloads are made directly to and from a configured S3 store using secure (https) connections that enforce a Dataverse installation's access controls. (The upload and download URLs are signed with a unique key that only allows access for a short time period and a Dataverse installation will only generate such a URL if the user has permission to upload/download the specific file in question.)

This option can handle files >300GB and could be appropriate for files up to a TB or larger. Other options can scale farther, but this option has the advantages that it is simple to configure and does not require any user training - uploads and downloads are done via the same interface as normal uploads to a Dataverse installation.

To configure these options, an administrator must set two JVM options for the Dataverse installation using the same process as for other configuration options:

``./asadmin create-jvm-options "-Ddataverse.files.<id>.download-redirect=true"``

``./asadmin create-jvm-options "-Ddataverse.files.<id>.upload-redirect=true"``


With multiple stores configured, it is possible to configure one S3 store with direct upload and/or download to support large files (in general or for specific Dataverse collections) while configuring only direct download, or no direct access for another store.

The direct upload option now switches between uploading the file in one piece (up to 1 GB by default) and sending it as multiple parts. The default can be changed by setting:
  
``./asadmin create-jvm-options "-Ddataverse.files.<id>.min-part-size=<size in bytes>"``

For AWS, the minimum allowed part size is 5*1024*1024 bytes and the maximum is 5 GB (5*1024**3). Other providers may set different limits.

It is also possible to set file upload size limits per store. See the :MaxFileUploadSizeInBytes setting described in the :doc:`/installation/config` guide.

At present, one potential drawback for direct-upload is that files are only partially 'ingested' - tabular and FITS files are processed, but zip files are not unzipped, and the file contents are not inspected to evaluate their mimetype. This could be appropriate for large files, or it may be useful to completely turn off ingest processing for performance reasons (ingest processing requires a copy of the file to be retrieved by the Dataverse installation from the S3 store). A store using direct upload can be configured to disable all ingest processing for files above a given size limit:

``./asadmin create-jvm-options "-Ddataverse.files.<id>.ingestsizelimit=<size in bytes>"``

.. _s3-direct-upload-features-disabled:

Features that are Disabled if S3 Direct Upload is Enabled
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The following features are disabled when S3 direct upload is enabled.

- Unzipping of zip files. (See :ref:`compressed-files`.)
- Extraction of metadata from FITS files. (See :ref:`fits`.)
- Creation of NcML auxiliary files (See :ref:`netcdf-and-hdf5`.)
- Extraction of a geospatial bounding box from NetCDF and HDF5 files (see :ref:`netcdf-and-hdf5`) unless :ref:`dataverse.netcdf.geo-extract-s3-direct-upload` is set to true.

.. _cors-s3-bucket:

Allow CORS for S3 Buckets
~~~~~~~~~~~~~~~~~~~~~~~~~

**IMPORTANT:** One additional step that is required to enable direct uploads via a Dataverse installation and for direct download to work with previewers and direct upload to work with dvwebloader (:ref:`folder-upload`) is to allow cross site (CORS) requests on your S3 store.
The example below shows how to enable CORS rules (to support upload and download) on a bucket using the AWS CLI command line tool. Note that you may want to limit the AllowedOrigins and/or AllowedHeaders further.  https://github.com/gdcc/dataverse-previewers/wiki/Using-Previewers-with-download-redirects-from-S3 has some additional information about doing this.

If you'd like to check the CORS configuration on your bucket before making changes:

``aws s3api get-bucket-cors --bucket <BUCKET_NAME>``

To proceed with making changes:

``aws s3api put-bucket-cors --bucket <BUCKET_NAME> --cors-configuration file://cors.json``

with the contents of the file cors.json as follows:

.. code-block:: json

        {
          "CORSRules": [
             {
                "AllowedOrigins": ["*"],
                "AllowedHeaders": ["*"],
                "AllowedMethods": ["PUT", "GET"],
                "ExposeHeaders": ["ETag", "Accept-Ranges", "Content-Encoding", "Content-Range"]
             }
          ]
        }

Alternatively, you can enable CORS using the AWS S3 web interface, using json-encoded rules as in the example above. 

.. _s3-tags-and-direct-upload:

S3 Tags and Direct Upload
~~~~~~~~~~~~~~~~~~~~~~~~~

Since the direct upload mechanism creates the final file rather than an intermediate temporary file, user actions, such as neither saving or canceling an upload session before closing the browser page, can leave an abandoned file in the store. The direct upload mechanism attempts to use S3 tags to aid in identifying/removing such files. Upon upload, files are given a "dv-state":"temp" tag which is removed when the dataset changes are saved and new files are added in the Dataverse installation. Note that not all S3 implementations support tags. Minio, for example, does not. With such stores, direct upload may not work and you might need to disable tagging. For details, see :ref:`s3-tagging` in the Installation Guide.

Trusted Remote Storage with the ``remote`` Store Type
-----------------------------------------------------

For very large, and/or very sensitive data, it may not make sense to transfer or copy files to Dataverse at all. The experimental ``remote`` store type in the Dataverse software now supports this use case. 

With this storage option Dataverse stores a URL reference for the file rather than transferring the file bytes to a store managed directly by Dataverse. Basic configuration for a remote store is described at :ref:`file-storage` in the Configuration Guide.

Once the store is configured, it can be assigned to a collection or individual datasets as with other stores. In a dataset using this store, users can reference remote files which will then appear the same basic way as other datafiles. 

Currently, remote files can only be added via the API. Users can also upload smaller files via the UI or API which will be stored in the configured base store.

If the store has been configured with a remote-store-name or remote-store-url, the dataset file table will include this information for remote files. These provide a visual indicator that the files are not managed directly by Dataverse and are stored/managed by a remote trusted store.

Rather than sending the file bytes, metadata for the remote file is added using the "jsonData" parameter.
jsonData normally includes information such as a file description, tags, provenance, whether the file is restricted, etc. For remote references, the jsonData object must also include values for:

* "storageIdentifier" - String, as specified in prior calls
* "fileName" - String
* "mimeType" - String
* fixity/checksum: either: 

  * "md5Hash" - String with MD5 hash value, or
  * "checksum" - Json Object with "@type" field specifying the algorithm used and "@value" field with the value from that algorithm, both Strings 

The allowed checksum algorithms are defined by the edu.harvard.iq.dataverse.DataFile.CheckSumType class and currently include MD5, SHA-1, SHA-256, and SHA-512

(The remote store leverages the same JSON upload syntax as the last step in direct upload to S3 described in the :ref:`Adding the Uploaded file to the Dataset <direct-add-to-dataset-api>` section of the :doc:`/developers/s3-direct-upload-api`.)

.. code-block:: bash

  export API_TOKEN=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  export SERVER_URL=https://demo.dataverse.org
  export PERSISTENT_IDENTIFIER=doi:10.5072/FK27U7YBV
  export JSON_DATA='{"description":"My description.","directoryLabel":"data/subdir1","categories":["Data"], "restrict":"false", "storageIdentifier":"trs://images/dataverse_project_logo.svg", "fileName":"dataverse_logo.svg", "mimeType":"image/svg+xml", "checksum": {"@type": "SHA-1", "@value": "123456"}}'

  curl -X POST -H "X-Dataverse-key: $API_TOKEN" "$SERVER_URL/api/datasets/:persistentId/add?persistentId=$PERSISTENT_IDENTIFIER" -F "jsonData=$JSON_DATA"
  
The variant allowing multiple files to be added once that is discussed in the :doc:`/developers/s3-direct-upload-api` document can also be used.

Considerations:

* Remote stores are configured with a base-url which limits what files can be referenced, i.e. the absolute URL for the file is <base-url>/<path in storageidentifier>.
* The current store will not prevent you from providing a relative URL that results in a 404 when resolved. (I.e. if you make a typo). You should check to make sure the file exists at the location you specify - by trying to download in Dataverse, by checking to see that Dataverse was able to get the file size (which it does by doing a HEAD call to that location), or just manually trying the URL in your browser.
* Admins are trusting the organization managing the site/service at base-url to maintain the referenced files for as long as the Dataverse instance needs them. Formal agreements are recommended for production
* For large files, direct-download should always be used with a remote store. (Otherwise the Dataverse will be involved in the download.)
* For simple websites, a remote store should be marked public which will turn off restriction and embargo functionality in Dataverse (since Dataverse cannot restrict access to the file on the remote website)
* Remote stores can be configured with a secret-key. This key will be used to sign URLs when Dataverse retrieves the file content or redirects a user for download. If remote service is able to validate the signature and reject invalid requests, the remote store mechanism can be used to manage restricted and embargoes files, access requests in Dataverse, etc. Dataverse contains Java code that validates these signatures which could be used, for example, to create a validation proxy in front of a web server to allow Dataverse to manage access. The secret-key is a shared secret between Dataverse and the remote service and is not shared with/is not accessible by users or those with access to user's machines.
* Sophisticated remote services may wish to register file URLs that do not directly reference the file contents (bytes) but instead direct the user to a website where further information about the remote service's download process can be found.
* Due to the current design, ingest cannot be done on remote files and administrators should disable ingest when using a remote store. This can be done by setting the ingest size limit for the store to 0 and/or using the recently added option to not perform tabular ingest on upload. 
* Dataverse will normally try to access the file contents itself, i.e. for ingest (in future versions), full-text indexing, thumbnail creation, etc. This processing may not be desirable for large/sensitive data, and, for the case where the URL does not reference the file itself, would not be possible. At present, administrators should configure the relevant size limits to avoid such actions.
* The current implementation of remote stores is experimental in the sense that future work to enhance it is planned. This work may result in changes to how the store works and lead to additional work when upgrading for sites that start using this mechanism now.

To configure the options mentioned above, an administrator must set two JVM options for the Dataverse installation using the same process as for other configuration options:

``./asadmin create-jvm-options "-Ddataverse.files.<id>.download-redirect=true"``
``./asadmin create-jvm-options "-Ddataverse.files.<id>.secret-key=somelongrandomalphanumerickeythelongerthebetter123456"``
``./asadmin create-jvm-options "-Ddataverse.files.<id>.public=true"``
``./asadmin create-jvm-options "-Ddataverse.files.<id>.ingestsizelimit=<size in bytes>"``

.. _globus-support:

Globus File Transfer
--------------------

Note: Globus file transfer is still experimental but feedback is welcome! See :ref:`support`.

Users can transfer files via `Globus <https://www.globus.org>`_ into and out of datasets, or reference files on a remote Globus endpoint, when their Dataverse installation is configured to use a Globus accessible store(s) 
and a community-developed `dataverse-globus <https://github.com/scholarsportal/dataverse-globus>`_ app has been properly installed and configured.

Globus endpoints can be in a variety of places, from data centers to personal computers. 
This means that from within the Dataverse software, a Globus transfer can feel like an upload or a download (with Globus Personal Connect running on your laptop, for example) or it can feel like a true transfer from one server to another (from a cluster in a data center into a Dataverse dataset or vice versa).

Globus transfer uses an efficient transfer mechanism and has additional features that make it suitable for large files and large numbers of files:

* robust file transfer capable of restarting after network or endpoint failures
* third-party transfer, which enables a user accessing a Dataverse installation in their desktop browser to initiate transfer of their files from a remote endpoint (i.e. on a local high-performance computing cluster), directly to an S3 store managed by the Dataverse installation

Note: Due to differences in the access control models of a Dataverse installation and Globus and the current Globus store model, Dataverse cannot enforce per-file-access restrictions.
It is therefore recommended that a store be configured as public, which disables the ability to restrict and embargo files in that store, when Globus access is allowed.

Dataverse supports three options for using Globus, two involving transfer to Dataverse-managed endpoints and one allowing Dataverse to reference files on remote endpoints.
Dataverse-managed endpoints must be Globus 'guest collections' hosted on either a file-system-based endpoint or an S3-based endpoint (the latter requires use of the Globus
S3 connector which requires a paid Globus subscription at the host institution). In either case, Dataverse is configured with the Globus credentials of a user account that can manage the endpoint.
Users will need a Globus account, which can be obtained via their institution or directly from Globus (at no cost).

With the file-system endpoint, Dataverse does not currently have access to the file contents. Thus, functionality related to ingest, previews, fixity hash validation, etc. are not available. (Using the S3-based endpoint, Dataverse has access via S3 and all functionality normally associated with direct uploads to S3 is available.)

For the reference use case, Dataverse must be configured with a list of allowed endpoint/base paths from which files may be referenced. In this case, since Dataverse is not accessing the remote endpoint itself, it does not need Globus credentials. 
Users will need a Globus account in this case, and the remote endpoint must be configured to allow them access (i.e. be publicly readable, or potentially involving some out-of-band mechanism to request access (that could be described in the dataset's Terms of Use and Access).

All of Dataverse's Globus capabilities are now store-based (see the store documentation) and therefore different collections/datasets can be configured to use different Globus-capable stores (or normal file, S3 stores, etc.)

More details of the setup required to enable Globus is described in the `Community Dataverse-Globus Setup and Configuration document <https://docs.google.com/document/d/1mwY3IVv8_wTspQC0d4ddFrD2deqwr-V5iAGHgOy4Ch8/edit?usp=sharing>`_ and the references therein.

As described in that document, Globus transfers can be initiated by choosing the Globus option in the dataset upload panel. (Globus, which does asynchronous transfers, is not available during dataset creation.) Analogously, "Globus Transfer" is one of the download options in the "Access Dataset" menu and optionally the file landing page download menu (if/when supported in the dataverse-globus app).

An overview of the control and data transfer interactions between components was presented at the 2022 Dataverse Community Meeting and can be viewed in the `Integrations and Tools Session Video <https://youtu.be/3ek7F_Dxcjk?t=5289>`_ around the 1 hr 28 min mark.

See also :ref:`Globus settings <:GlobusSettings>`.

An alternative, experimental implementation of Globus polling of ongoing upload transfers has been added in v6.4. This framework does not rely on the instance staying up continuously for the duration of the transfer and saves the state information about Globus upload requests in the database. Due to its experimental nature it is not enabled by default. See the ``globus-use-experimental-async-framework`` feature flag (see :ref:`feature-flags`) and the JVM option :ref:`dataverse.files.globus-monitoring-server`.
