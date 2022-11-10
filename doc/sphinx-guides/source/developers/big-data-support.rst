Big Data Support
================

Big data support includes some highly experimental options. Eventually more of this content will move to the Installation Guide.

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


**IMPORTANT:** One additional step that is required to enable direct uploads via a Dataverse installation and for direct download to work with previewers is to allow cross site (CORS) requests on your S3 store. 
The example below shows how to enable CORS rules (to support upload and download) on a bucket using the AWS CLI command line tool. Note that you may want to limit the AllowedOrigins and/or AllowedHeaders further.  https://github.com/gdcc/dataverse-previewers/wiki/Using-Previewers-with-download-redirects-from-S3 has some additional information about doing this.

``aws s3api put-bucket-cors --bucket <BUCKET_NAME> --cors-configuration file://cors.json``

with the contents of the file cors.json as follows:

.. code-block:: json

        {
          "CORSRules": [
             {
                "AllowedOrigins": ["*"],
                "AllowedHeaders": ["*"],
                "AllowedMethods": ["PUT", "GET"],
                "ExposeHeaders": ["ETag"]
             }
          ]
        }

Alternatively, you can enable CORS using the AWS S3 web interface, using json-encoded rules as in the example above. 

Since the direct upload mechanism creates the final file rather than an intermediate temporary file, user actions, such as neither saving or canceling an upload session before closing the browser page, can leave an abandoned file in the store. The direct upload mechanism attempts to use S3 Tags to aid in identifying/removing such files. Upon upload, files are given a "dv-state":"temp" tag which is removed when the dataset changes are saved and the new file(s) are added in the Dataverse installation. Note that not all S3 implementations support Tags: Minio does not. WIth such stores, direct upload works, but Tags are not used.

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
  export JSON_DATA="{'description':'My description.','directoryLabel':'data/subdir1','categories':['Data'], 'restrict':'false', 'storageIdentifier':'trs://images/dataverse_project_logo.svg', 'fileName':'dataverse_logo.svg', 'mimeType':'image/svg+xml', 'checksum': {'@type': 'SHA-1', '@value': '123456'}}"

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

Users can transfer files via `Globus <ttps://www.globus.org>`_ into and out of datasets when their Dataverse installation is configured to use a Globus accessible S3 store and a community-developed `dataverse-globus <https://github.com/scholarsportal/dataverse-globus>`_ "transfer" app has been properly installed and configured.

Due to differences in the access control models of a Dataverse installation and Globus, enabling the Globus capability on a store will disable the ability to restrict and embargo files in that store.

As Globus aficionados know, Globus endpoints can be in a variety of places, from data centers to personal computers. This means that from within the Dataverse software, a Globus transfer can feel like an upload or a download (with Globus Personal Connect running on your laptop, for example) or it can feel like a true transfer from one server to another (from a cluster in a data center into a Dataverse dataset or vice versa).

Globus transfer uses a very efficient transfer mechanism and has additional features that make it suitable for large files and large numbers of files:

* robust file transfer capable of restarting after network or endpoint failures
* third-party transfer, which enables a user accessing a Dataverse installation in their desktop browser to initiate transfer of their files from a remote endpoint (i.e. on a local high-performance computing cluster), directly to an S3 store managed by the Dataverse installation

Globus transfer requires use of the Globus S3 connector which requires a paid Globus subscription at the host institution. Users will need a Globus account which could be obtained via their institution or directly from Globus (at no cost).

The setup required to enable Globus is described in the `Community Dataverse-Globus Setup and Configuration document <https://docs.google.com/document/d/1mwY3IVv8_wTspQC0d4ddFrD2deqwr-V5iAGHgOy4Ch8/edit?usp=sharing>`_ and the references therein.

As described in that document, Globus transfers can be initiated by choosing the Globus option in the dataset upload panel. (Globus, which does asynchronous transfers, is not available during dataset creation.) Analogously, "Globus Transfer" is one of the download options in the "Access Dataset" menu and optionally the file landing page download menu (if/when supported in the dataverse-globus app).

An overview of the control and data transfer interactions between components was presented at the 2022 Dataverse Community Meeting and can be viewed in the `Integrations and Tools Session Video <https://youtu.be/3ek7F_Dxcjk?t=5289>`_ around the 1 hr 28 min mark.

See also :ref:`Globus settings <:GlobusBasicToken>`.

Data Capture Module (DCM)
-------------------------

Data Capture Module (DCM) is an experimental component that allows users to upload large datasets via rsync over ssh.

DCM was developed and tested using Glassfish but these docs have been updated with references to Payara.

Install a DCM
~~~~~~~~~~~~~

Installation instructions can be found at https://github.com/sbgrid/data-capture-module/blob/master/doc/installation.md. Note that shared storage (posix or AWS S3) between your Dataverse installation and your DCM is required. You cannot use a DCM with Swift at this point in time.

.. FIXME: Explain what ``dataverse.files.dcm-s3-bucket-name`` is for and what it has to do with ``dataverse.files.s3.bucket-name``.

Once you have installed a DCM, you will need to configure two database settings on the Dataverse installation side. These settings are documented in the :doc:`/installation/config` section of the Installation Guide:

- ``:DataCaptureModuleUrl`` should be set to the URL of a DCM you installed.
- ``:UploadMethods`` should include ``dcm/rsync+ssh``.
  
This will allow your Dataverse installation to communicate with your DCM, so that your Dataverse installation can download rsync scripts for your users.

Downloading rsync scripts via Your Dataverse Installation's API
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The rsync script can be downloaded from your Dataverse installation via API using an authorized API token. In the curl example below, substitute ``$PERSISTENT_ID`` with a DOI or Handle:

``curl -H "X-Dataverse-key: $API_TOKEN" $DV_BASE_URL/api/datasets/:persistentId/dataCaptureModule/rsync?persistentId=$PERSISTENT_ID``

How a DCM reports checksum success or failure to your Dataverse Installation
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Once the user uploads files to a DCM, that DCM will perform checksum validation and report to your Dataverse installation the results of that validation. The DCM must be configured to pass the API token of a superuser. The implementation details, which are subject to change, are below.

The JSON that a DCM sends to your Dataverse installation on successful checksum validation looks something like the contents of :download:`checksumValidationSuccess.json <../_static/installation/files/root/big-data-support/checksumValidationSuccess.json>` below:

.. literalinclude:: ../_static/installation/files/root/big-data-support/checksumValidationSuccess.json
   :language: json

- ``status`` - The valid strings to send are ``validation passed`` and ``validation failed``.
- ``uploadFolder`` - This is the directory on disk where your Dataverse installation should attempt to find the files that a DCM has moved into place. There should always be a ``files.sha`` file and a least one data file. ``files.sha`` is a manifest of all the data files and their checksums. The ``uploadFolder`` directory is inside the directory where data is stored for the dataset and may have the same name as the "identifier" of the persistent id (DOI or Handle). For example, you would send ``"uploadFolder": "DNXV2H"`` in the JSON file when the absolute path to this directory is ``/usr/local/payara5/glassfish/domains/domain1/files/10.5072/FK2/DNXV2H/DNXV2H``.
- ``totalSize`` - Your Dataverse installation will use this value to represent the total size in bytes of all the files in the "package" that's created. If 360 data files and one ``files.sha`` manifest file are in the ``uploadFolder``, this value is the sum of the 360 data files.


Here's the syntax for sending the JSON.

``curl -H "X-Dataverse-key: $API_TOKEN" -X POST -H 'Content-type: application/json' --upload-file checksumValidationSuccess.json $DV_BASE_URL/api/datasets/:persistentId/dataCaptureModule/checksumValidation?persistentId=$PERSISTENT_ID``


Steps to set up a DCM mock for Development
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

See instructions at https://github.com/sbgrid/data-capture-module/blob/master/doc/mock.md


Add Dataverse Installation settings to use mock (same as using DCM, noted above):

- ``curl http://localhost:8080/api/admin/settings/:DataCaptureModuleUrl -X PUT -d "http://localhost:5000"``
- ``curl http://localhost:8080/api/admin/settings/:UploadMethods -X PUT -d "dcm/rsync+ssh"``

At this point you should be able to download a placeholder rsync script. Your Dataverse installation is then waiting for news from the DCM about if checksum validation has succeeded or not. First, you have to put files in place, which is usually the job of the DCM. You should substitute "X1METO" for the "identifier" of the dataset you create. You must also use the proper path for where you store files in your dev environment.

- ``mkdir /usr/local/payara5/glassfish/domains/domain1/files/10.5072/FK2/X1METO``
- ``mkdir /usr/local/payara5/glassfish/domains/domain1/files/10.5072/FK2/X1METO/X1METO``
- ``cd /usr/local/payara5/glassfish/domains/domain1/files/10.5072/FK2/X1METO/X1METO``
- ``echo "hello" > file1.txt``
- ``shasum file1.txt > files.sha``



Now the files are in place and you need to send JSON to your Dataverse installation with a success or failure message as described above. Make a copy of ``doc/sphinx-guides/source/_static/installation/files/root/big-data-support/checksumValidationSuccess.json`` and put the identifier in place such as "X1METO" under "uploadFolder"). Then use curl as described above to send the JSON.

Troubleshooting
~~~~~~~~~~~~~~~

The following low level command should only be used when troubleshooting the "import" code a DCM uses but is documented here for completeness.

``curl -H "X-Dataverse-key: $API_TOKEN" -X POST "$DV_BASE_URL/api/batch/jobs/import/datasets/files/$DATASET_DB_ID?uploadFolder=$UPLOAD_FOLDER&totalSize=$TOTAL_SIZE"``

Steps to set up a DCM via Docker for Development
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

If you need a fully operating DCM client for development purposes, these steps will guide you to setting one up. This includes steps to set up the DCM on S3 variant.

Docker Image Set-up
^^^^^^^^^^^^^^^^^^^

See https://github.com/IQSS/dataverse/blob/develop/conf/docker-dcm/readme.md

- Install docker if you do not have it
      
Optional steps for setting up the S3 Docker DCM Variant
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

- Before: the default bucket for DCM to hold files in S3 is named test-dcm. It is coded into `post_upload_s3.bash` (line 30). Change to a different bucket if needed.
- Also Note: With the new support for multiple file store in the Dataverse Software, DCM requires a store with id="s3" and DCM will only work with this store.

  - Add AWS bucket info to dcmsrv
    - Add AWS credentials to ``~/.aws/credentials``

      - ``[default]``
      - ``aws_access_key_id =``
      - ``aws_secret_access_key =``

- Dataverse installation configuration (on dvsrv):

  - Set S3 as the storage driver

    - ``cd /opt/payara5/bin/``
    - ``./asadmin delete-jvm-options "\-Ddataverse.files.storage-driver-id=file"``
    - ``./asadmin create-jvm-options "\-Ddataverse.files.storage-driver-id=s3"``
    - ``./asadmin create-jvm-options "\-Ddataverse.files.s3.type=s3"``
    - ``./asadmin create-jvm-options "\-Ddataverse.files.s3.label=s3"``
    

  - Add AWS bucket info to your Dataverse installation
    - Add AWS credentials to ``~/.aws/credentials``
    
      - ``[default]``
      - ``aws_access_key_id =``
      - ``aws_secret_access_key =``

    - Also: set region in ``~/.aws/config`` to create a region file. Add these contents:

      - ``[default]``
      - ``region = us-east-1``

  - Add the S3 bucket names to your Dataverse installation

    - S3 bucket for your Dataverse installation

      - ``/usr/local/payara5/glassfish/bin/asadmin create-jvm-options "-Ddataverse.files.s3.bucket-name=iqsstestdcmbucket"``

    - S3 bucket for DCM (as your Dataverse installation needs to do the copy over)

      - ``/usr/local/payara5/glassfish/bin/asadmin create-jvm-options "-Ddataverse.files.dcm-s3-bucket-name=test-dcm"``

  - Set download method to be HTTP, as DCM downloads through S3 are over this protocol ``curl -X PUT "http://localhost:8080/api/admin/settings/:DownloadMethods" -d "native/http"``

Using the DCM Docker Containers
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

For using these commands, you will need to connect to the shell prompt inside various containers (e.g. ``docker exec -it dvsrv /bin/bash``)

- Create a dataset and download rsync upload script

  - connect to client container: ``docker exec -it dcm_client bash``
  - create dataset: ``cd /mnt ; ./create.bash`` ; this will echo the database ID to stdout
  - download transfer script: ``./get_transfer.bash $database_id_from_create_script``
  - execute the transfer script: ``bash ./upload-${database_id_from-create_script}.bash`` , and follow instructions from script.

- Run script

  - e.g. ``bash ./upload-3.bash`` (``3`` being the database id from earlier commands in this example).

- Manually run post upload script on dcmsrv

  - for posix implementation: ``docker exec -it dcmsrv /opt/dcm/scn/post_upload.bash``
  - for S3 implementation: ``docker exec -it dcmsrv /opt/dcm/scn/post_upload_s3.bash``

Additional DCM docker development tips
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

- You can completely blow away all the docker images with these commands (including non DCM ones!)
  - ``docker-compose -f docmer-compose.yml down -v``

- There are a few logs to tail

  - dvsrv : ``tail -n 2000 -f /opt/payara5/glassfish/domains/domain1/logs/server.log``
  - dcmsrv : ``tail -n 2000 -f /var/log/lighttpd/breakage.log``
  - dcmsrv : ``tail -n 2000 -f /var/log/lighttpd/access.log``

- You may have to restart the app server domain occasionally to deal with memory filling up. If deployment is getting reallllllly slow, its a good time.

Repository Storage Abstraction Layer (RSAL)
-------------------------------------------

Steps to set up a DCM via Docker for Development
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

See https://github.com/IQSS/dataverse/blob/develop/conf/docker-dcm/readme.md

Using the RSAL Docker Containers
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

- Create a dataset (either with the procedure mentioned in DCM Docker Containers, or another process)
- Publish the dataset (from the client container): ``cd /mnt; ./publish_major.bash ${database_id}``
- Run the RSAL component of the workflow (from the host): ``docker exec -it rsalsrv /opt/rsal/scn/pub.py``
- If desired, from the client container you can download the dataset following the instructions in the dataset access section of the dataset page.

Configuring the RSAL Mock
~~~~~~~~~~~~~~~~~~~~~~~~~

Info for configuring the RSAL Mock: https://github.com/sbgrid/rsal/tree/master/mocks

Also, to configure your Dataverse installation to use the new workflow you must do the following (see also the :doc:`workflows` section):

1. Configure the RSAL URL:

``curl -X PUT -d 'http://<myipaddr>:5050' http://localhost:8080/api/admin/settings/:RepositoryStorageAbstractionLayerUrl``

2. Update workflow json with correct URL information:

Edit internal-httpSR-workflow.json and replace url and rollbackUrl to be the url of your RSAL mock.

3. Create the workflow:

``curl http://localhost:8080/api/admin/workflows -X POST --data-binary @internal-httpSR-workflow.json -H "Content-type: application/json"``

4. List available workflows:

``curl http://localhost:8080/api/admin/workflows``

5. Set the workflow (id) as the default workflow for the appropriate trigger:

``curl http://localhost:8080/api/admin/workflows/default/PrePublishDataset -X PUT -d 2``

6. Check that the trigger has the appropriate default workflow set:

``curl http://localhost:8080/api/admin/workflows/default/PrePublishDataset``

7. Add RSAL to whitelist

8. When finished testing, unset the workflow:

``curl -X DELETE http://localhost:8080/api/admin/workflows/default/PrePublishDataset``

Configuring download via rsync
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

In order to see the rsync URLs, you must run this command:

``curl -X PUT -d 'rsal/rsync' http://localhost:8080/api/admin/settings/:DownloadMethods``

..  TODO: Document these in the Installation Guide once they're final.

To specify replication sites that appear in rsync URLs:

Download :download:`add-storage-site.json <../../../../scripts/api/data/storageSites/add-storage-site.json>` and adjust it to meet your needs. The file should look something like this:

.. literalinclude:: ../../../../scripts/api/data/storageSites/add-storage-site.json

Then add the storage site using curl:

``curl -H "Content-type:application/json" -X POST http://localhost:8080/api/admin/storageSites --upload-file add-storage-site.json``

You make a storage site the primary site by passing "true". Pass "false" to make it not the primary site. (id "1" in the example):

``curl -X PUT -d true http://localhost:8080/api/admin/storageSites/1/primaryStorage``

You can delete a storage site like this (id "1" in the example):

``curl -X DELETE http://localhost:8080/api/admin/storageSites/1``

You can view a single storage site like this: (id "1" in the example):

``curl http://localhost:8080/api/admin/storageSites/1``

You can view all storage site like this:

``curl http://localhost:8080/api/admin/storageSites``

In the GUI, this is called "Local Access". It's where you can compute on files on your cluster.

``curl http://localhost:8080/api/admin/settings/:LocalDataAccessPath -X PUT -d "/programs/datagrid"``


