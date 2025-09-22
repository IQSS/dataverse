Scaling Dataverse with Data Size
================================

This guide is intended to help administrators configure Dataverse appropriately to handle the amount of data their installation needs to manage.

Scaling is a complex subject: there are many options available in Dataverse that can improve performance with larger scale data, some of which
work differently than Dataverse's default configuration, potentially requiring user education, and some which can require additional expertise to manage.

In general, there are three dimensions in which Dataverse can scale:
  
1. **Storage size of individual files and aggregate storage size**
2. **Number of Files per Dataset**
3. **Number of Datasets**


.. contents:: |toctitle|
        :local:

Choosing the Right Storage
--------------------------

The main issues for handling larger files and larger aggregate data size relate to the performance of the storage used and how involved the Dataverse server is in the data transfer. 
With appropriate configuration, Dataverse can support file sizes and aggregate dataset sizes into the terabyte scale and beyond.

File Storage
~~~~~~~~~~~~

The default storage option in Dataverse is based on a local file system. When files are transferred to Dataverse, they are first stored in a
temporary location on the Dataverse server. Any zip files uploaded are unzipped to create multiple individual file entries. Once an upload is completed, 
Dataverse copies the files to permanent storage. Dataverse also takes advantage of the file being local to inspect it's bytes to determine it
s MIME type, and, for tabular data, to 'ingest' it - extracting metadata about the variables used in the file and creating a tab-separated value (TSV) version of the file.

Benefits: 

- This option requires no external services and can potentially handle files into the gigabyte (GB) size range. For smaller institutions,
and in disciplines where datasets do not have more than a few hundred files and files are not too large, this can be the simplest option.
- Unzipping of zip archives can be simpler for users than having to upload many individual files and was, at one time, the only way to 
preserve file path names when uploading.

Challenges: In general file storage is not a good option for larger data sizes - both in terms of file size and number of files. Contributing factors include:
 
- Because temporary storage is used, transfers will temporarily use several times as much space as the final transfer. Unzipping also increases the final storage size of a dataset.
- Because all uploads use the same temporary storage, temporary storage must be large enough to handle multiple users uploading data.
- Each file is uploaded as a single HTTP request, which can cause long transfer times which, in turn, can trigger timeout errors in Dataverse or any proxy or load balancer in front of Dataverse.
- Uploading many files at once can trigger any rate limiter in front of the Dataverse server (i.e. used to throttle use by AI) resulting in failures
- Because transfers (both uploads and downloads) are handled by the Dataverse server, they add to server processing load which can affect overall performance.
- Cost: local file storage must be provisioned in advance based on anticipated demand. It can involve up-front costs (for a local disk), or, when procured from a cloud provider, is likely to be more expensive than object storage (see below) 


Object Storage via S3
~~~~~~~~~~~~~~~~~~~~~

A more scalable option for storage is to use an object store with transfers managed using the Simple Storage Service (S3) protocol. S3-compatible storage can 
easily be bought (rented) from major cloud providers, but may also be available from institutional clouds. It is also possible to run open-source software to provide S3 storage 
over a local file system (making it possible to enjoy the advantages discussed below while still leveraging local file storage). 

While Dataverse can be configured to handle uploads and downloads as with file storage (with zip files being unzipped, but having many of the same challenges in terms of temporary storage and server load as discussed above) 
it can also be configured to use 'direct' upload and download. In this configuration, the actual transfer of file bytes is from/to the user's local machine to/from
the S3 store. In this configuration, Dataverse does not attempt to unzip zip files and they are stored as a single file in the dataset.

Benefits: S3 offers several advantages over file storage which :
 
- Scalability: S3 is designed to handle large amounts of data. It can handle individual files up to several TB in size. 
Because S3 supports breaking files into pieces, Dataverse can transfer a file in pieces (several in parallel, potentially thousands of pieces per file) making transfers faster
and more robust (a failure requires only resending the failed piece). It may also be the case that users will have a faster network connection to the S3 store 
(e.g. in a commercial cloud or High Performance Computing center) than they do to the Dataverse server, reducing transfer time.
- High Availability: S3 provides redundancy beyond what is available with a single disk (valuable for preservation, potentially reducing the need to perform data integrity checks).

Challenges:

- Cost: S3 offers a pricing model that allows you to pay for the storage and transfer of data based on current usage (versus long term demand) but commercial 
providers charge more per TB than the equivalent cost of a local disk (though commercial S3 storage is cheaper than commercial file storage).
There can also be egress and other charges. Overall, S3 storage is generally more expensive than local file storage but cheaper than cloud file storage.
Running a local S3 storage or leveraging an institutional service can further reduce costs.
- S3 Storage without direct upload/download provides minimal benefits with Dataverse as files still pass through the server, files are still uploaded as a single HTTP/HTTPS stream, and temporary storage is still used.

Other Considerations
^^^^^^^^^^^^^^^^^^^^

- While not having files unzipped can be confusing to users who are used to it from using Dataverse with file storage, there are ways to minimize the impact. 
For example, Dataverse can be configured to use a 'Zip File Previewer' that allows users to see the contents of a zip file and even download individual files from within it. 
For users who still want their data stored as individual files, Dataverse can be configured with the 'DVWebloader' which allows users to select an entire folder tree of files and 
upload them, with their relative paths intact, to dataverse. (DVWebloader can only be used with S3/direct upload, but it is much more efficient in this case than using the 
standard upload interface in Dataverse).
- Using direct upload stops Dataverse from inspecting the file bytes to determine the MIME type (with one exception - Stata files). Dataverse will still look at the file name and extension to determine the MIME type.
- To perform the 'ingest' processing, Dataverse currently has to copy the file to local storage, somewhat negating the benefit of sending data directly to S3. To manage larger files, one can set a per-store
ingest size limit (which can be 0 bytes) to stop ingest or limit it to smaller files. 
- Dataverse's mechanism for downloading a whole dataset or multiple selected files involves zipping those files together. Even When using S3 with direct upload/download,
the file bytes are transferred to the Dataverse server as part of the zipping process. There are ways to reduce the performance impact of this:
  - There is a 'ZipDownloader' app that can be run separate from Dataverse to handle the zipping process.
  - Dataverse has a :ZipDownloadLimit that can be used to limit the amount of data that can be zipped. If a dataset is larger than this limit, Dataverse will only add some of the files to the zip and list others in the included manifest file.
  - There are tools such as the Dataverse Dataset Downloader (https://github.com/gdcc/dataverse-recipes/tree/main/shell/download#dataverse-dataset-downloader) that can be used to download all off the files individually. This avoids sending any of the files to/through the Dataverse server when S3 direct download is enabled.  
- Dataverse leverages S3 features that are not implemented by all servers and has several configuration options geared towards handling variations between servers. Site admins should be sure to test with their preferred S3 implementation.
- The part-size used when directly transferring files to S3 is configurable (at AWS, from 5 MiB to 5GiB). The default in Dataverse is 1 GiB (1024^3). If the primary use case is with smaller files than that, decreasing the part size may improve upload speeds.

Remote Storage
~~~~~~~~~~~~~~

Note: Remote Storage is still experimental: feedback is welcome! See :ref:`support`.

For very large, and/or very sensitive data, it may not make sense to transfer or copy files to Dataverse at all.
The ``remote`` store type in the Dataverse software supports these use cases.
It allows Dataverse to store a URL reference for the file rather than transferring the file bytes to a store managed directly by Dataverse.
In the most basic configuration a site administrator configures the base URL for the store, e.g. "https://thirdpartystorage.edu/long-term-storage/" 
and users can then create files referencing any URL starting with that base, e.g. "https://thirdpartystorage.edu/long-term-storage/my_project_dir/my_file.txt".
If the remote site is a public web server, the remote store in Dataverse should be configured to be 'public' which will disable the ability to restrict
or embargo files (as they are public on the remote site and Dataverse cannot block access.) Conversely, Dataverse can be configured to sign requests to the 
remote server which the remote server can then, if it is capable of validating them, use to reject requests not approved by Dataverse. In this configuration,
users can restrict and embargo files and Dataverse and the remote server will cooperate to manage access control. Another alternative, with a more advanced
remote store, would be, instead of using URLs that directly enable download of the file, to use URLs that point to a landing page at the remote server that 
may require the user to login, or go through some other authentication/validation process before being able to access the file.

Dataverse considers remote storage to be read-only, or, in cases where the remote service does not provide a way for Dataverse to download the file bytes
(due to access control or because the URL refers to a landing page), inacessible. Depending on whether Dataverse can access the bytes of the file,
functionality such as ingest and integrity checking may or may not be possible. If the file bytes are not accessible the remote store in Dataverse should be
configured to disable operations that attempt to access the file (see  files-not-accessible-by-dataverse). If the file bytes are accessible, Dataverse can still
support features such as ingest and thumbnail creation, as well as local storage of other files  and auxilliary files. These are handled by configuring a 'base' store
with the remote store that is used for these purposes. (This means that while the specified files remain on the remote store, other files in the dataset, and
potentially the ingested TSV format of a remote file would be managed by Dataverse in some other store. If ingest is not desired, the ingest size limit for the
store can be set to 0 bytes).

Benefits: 

- This is a relatively simple way to off-load the management of large and/or sensitive data files to other organizations while still providing Dataverse's overall capabilities for dataset curation and publication to users.   
- If the store has been configured with a remote-store-name or remote-store-url, the dataset file table will include this information for remote files. These provide a visual indicator that the files are not managed directly by Dataverse and are stored/managed by a remote trusted store.

Challenges:

- Currently, remote files can only be added via the API. (This may be addressed in future versions).
- Since remote files can only be added as the dataset is created in the UI. However, the UI will still allow upload of files to the base store (at dataset creation and when editing)
- As Dataverse is relying on the remote service to main the integrity and availability of the files, it is likely that the Dataverse site admin will want to have a formal agreement with the remote service 
operator about their policies.
- Site admins need to consider carefully how to configure file size limits, ingest size limits, etc. on the remote store and it's base store, and whether the remote store is public-only, and whether it files there can be read by Dataverse to assure the
requirements of a specific use case(s) are addressed.
- The current remote store implementation will not prevent you from providing a relative URL that results in a 404 when resolved. (I.e. if you make a typo). You should check to make sure the file exists at the location you specify - by trying to download in Dataverse, by checking to see that Dataverse was able to get the file size (which it does by doing a HEAD call to that location), or just manually trying the URL in your browser.
- For large files, direct-download should always be used with a remote store. (Otherwise the Dataverse will be involved in the download.)
- When multiple files are selected for download, Dataverse will try to include remote files in the zip file being created (up to the max zip size limit) which is inefficent, and will not be able to include remote files that are inaccessible (possibly confusing). 

Globus Transfer
~~~~~~~~~~~~~~~

Note: Globus Transfer is still experimental: feedback is welcome! See :ref:`support`.

`Globus <https://www.globus.org>`_ provides file transfer service that is widely used for the world's largest datasets (in terms of both file size and number of files). It provides:

- robust file transfer capable of handling delays (e.g. due to the time it takes to mount tapes) and restarting after network or endpoint failures
- rapid parallel file transfers, potentially between clusters of computers on both ends
- third-party transfer, which enables a user working with their desktop browser to initiate transfer of files from one remote endpoint (i.e. on a local high-performance computing cluster) to/from another (e.g. one associated with a Dataverse store)

Dataverse can be configured to support Globus transfers in multiple ways:

- A Dataverse-managed Globus File Endpoint: Dataverse controls user access to the endpoint, access is only via Globus
- A Dataverse-managed Globus S3 Endpoint: Dataverse controls user access to the endpoint, access is available via S3 and via Globus
- A Globus Endpoint treated as Remote Storage: Dataverse references files on a Globus endpoint managed by a third party

Benefits: 

- Globus scales to higher data volumes than any other option. Users working with large data are often familiar with Globus and are interested in transferring data to/from computational clusters rather than their local machine
- Globus transfers can be initiated by choosing the Globus option in the dataset upload panel. Analogously, "Globus Transfer" is one of the download options in the "Access Dataset" menu.
- For the non-S3 options, Dataverse supports having a base store (e.g. a local file system or an S3-based store), which can be used internally by Dataverse (e.g. for thumbnails, etc.) and can allow users to upload smaller files (e.g. Readmes, documentation) that might not be suited to a given Globus endpoint (e.g. a tape store)

Challenges: 

- Globus is complex to manage and Dataverse installations will need to develop Globus expertise or partner with another organization (i.e. a institutional high-performance computing center) to manage Globus endpoints.
- For users not familiar with Globus, managing transfers can be confusing. For the non-S3 options, users cannot just download files - they must have access to a destination Globus endpoint. Globus does provide a free 'Globus Personal Connect' service which installed on any machine to allow transfers to/from it.
- Globus transfers are not enabled at dataset-creation time. Once the draft version is created, users can initiate Globus transfers to upload files from remote endpoints.
- For Dataverse-managed endpoints, a community-developed `dataverse-globus <https://github.com/scholarsportal/dataverse-globus>`_ app must be installed and configured in the Dataverse instance. 
This app manages granting and revoking access for users to upload/download files from Dataverse and handles the translation between Dataverse's internal file naming/organization to that see by the user.
- Users familiar with Globus sometimes expect to be able to find the Dataverse endpoint in Globus' online service and download files from there. Due to the fact that Dataverse is managing permissions and handling file naming, this doesn't work.
- Due to differences between Dataverse's and Globus's access control models, Dataverse cannot enforce per-file-access restrictions - restriction can only be done today at the level of providing access to all files in a dataset.
Globus stores can be defined as public to disable Dataverse's ability to restrict and embargo files in that store. If the store is configured to support restriction and embargo,
Dataverse and it's Dataverse-Globus app will limit users to downloading only the files they have been granted access to, but a technically knowledgeable user could access other files in the same dataset if they are give access to one.
(Data depositors would need to be aware of this limitation and could be guided to restrict all files/only grant access to all dataset files as a work-around).
- Dataverse-managed endpoints must be Globus 'guest collections' hosted on either a file-system-based endpoint or an S3-based endpoint (the latter requires use of the Globus
S3 connector which requires a paid Globus subscription at the host institution). In either case, Dataverse is configured with the Globus credentials of a user account that can manage the endpoint.
Users will need their own Globus account, which can be obtained via their institution or directly from Globus (at no cost).
- With the file-system endpoint, Dataverse does not currently have access to the file contents. Thus, functionality related to ingest, previews, fixity hash validation, etc. are not available. 
(Using the S3-based endpoint, Dataverse has access via S3 and all functionality normally associated with direct uploads to S3 is available. In this case admins should be sure to set the maximum size for ingest and avoid requiring hash validation at publication, etc.)
- For the reference use case, Dataverse must be configured with a list of allowed endpoint/base paths from which files may be referenced. In this case, since Dataverse is not accessing the remote endpoint itself, it does not need Globus credentials. 
Users will also need a Globus account in this case, and the remote endpoint must be configured to allow them access (i.e. be publicly readable, or potentially involving some out-of-band mechanism to request access (that could be described, for example, in the dataset's Terms of Use and Access).
- While Globus itself can handle many (millions of) files of any size, Dataverse cannot handle more than thousands of files per dataset (at best) and some Globus endpoints may have limits on file sizes - both maximums and minimums (e.g. for tape storage where small files are inefficient).
Users will need to be made aware of these limitations and the possibilities for managing them (e.g. by aggregating multiple files in a single larger file, or storing smaller files in the base-store via the normal Dataverse upload UI).
- There is currently (a bug)[https://github.com/gdcc/dataverse-globus/issues/2] that won't allow users to transfer files from/to endpoints where they do not have permission to list the overall file tree (i.e. an institution manages <endpoint>/institution_name but the user only has access to <endpoint>/institution_name/my_dir.)
Until that is fixed, a work-around is to first transfer data to an endpoint without this restriction.
- An alternative, experimental implementation of Globus polling of ongoing upload transfers has been added in v6.4. This framework does not rely on the instance staying up continuously for the duration of the transfer and saves the state information about Globus upload requests in the database. While it is now the recommended option, it is not enabled by default. See the ``globus-use-experimental-async-framework`` feature flag (see :ref:`feature-flags`) and the JVM option :ref:`dataverse.files.globus-monitoring-server`.

More details of the setup required to enable Globus is described in the `Community Dataverse-Globus Setup and Configuration document <https://docs.google.com/document/d/1mwY3IVv8_wTspQC0d4ddFrD2deqwr-V5iAGHgOy4Ch8/edit?usp=sharing>`_ and the references therein.

An overview of the control and data transfer interactions between components was presented at the 2022 Dataverse Community Meeting and can be viewed in the `Integrations and Tools Session Video <https://youtu.be/3ek7F_Dxcjk?t=5289>`_ around the 1 hr 28 min mark.

See also :ref:`Globus settings <:GlobusSettings>`.

Managing more files per dataset and more datasets
-------------------------------------------------

Dataverse can be configured to handle datasets with hundreds or thousands of files and hundreds of thousands of datasets. However, reaching these levels can require significant effort to appropriately configure the server.

Technically, there are two factors that can limit scaling the number of files per dataset: how much the Dataverse server is involved in data transfer, and constraints based on Dataverse's code and database configuration.
The former is dramatically affected by the choice for file storage and options such as the S3 direct upload/download settings and ingest size limits. The are fewer ways to affect the latter beyond increasing the amount of memory and CPU resources available
or rewriting the relevant parts of Dataverse. (There are continuing efforts to improve Dataverse's performance and scaling, so it is also advisable to use the latest version if you are pushing the boundaries on scaling. Progress is being made.)

Scaling to larger numbers of datasets (and to some extent scaling files per dataset) also depends on Dataverse's Solr search engine. There have been very significant improvements in indexing and search performance in recent releases, including some that are not turned on by default.

Avoiding Many Files
~~~~~~~~~~~~~~~~~~~

Before describing things that can be done to improve scaling, it is important to note that there are configuration options and best practices to suggest to users to help avoid larger datasets and users hitting performance issues by going beyond the file counts you know work in your instance.

        /* zip upload number of files limit */
        ZipUploadFilesLimit,
        /* the number of files the GUI user is allowed to upload in one batch, 
            via drag-and-drop, or through the file select dialog */
        MultipleUploadFilesLimit,
                UseStorageQuotas, 
        /** 
         * Placeholder storage quota (defines the same quota setting for every user; used to test the concept of a quota.
         */
        StorageQuotaSizeInBytes,

Ways to Scale
~~~~~~~~~~~~~

   
       /**
     * For published (public) objects, don't use a join when searching Solr. 
     * Experimental! Requires a reindex with the following feature flag enabled,
     * in order to add the boolean publicObject_b:true field to all the public
     * Solr documents. 
     *
     * @apiNote Raise flag by setting
     * "dataverse.feature.avoid-expensive-solr-join"
     * @since Dataverse 6.3
     */
AVOID_EXPENSIVE_SOLR_JOIN("avoid-expensive-solr-join"),
    /**
     * With this flag enabled, the boolean field publicObject_b:true will be 
     * added to all the indexed Solr documents for publicly-available collections,
     * datasets and files. This flag makes it possible to rely on it in searches,
     * instead of the very expensive join (the feature flag above).
     *
     * @apiNote Raise flag by setting
     * "dataverse.feature.add-publicobject-solr-field"
     * @since Dataverse 6.3
     */
    ADD_PUBLICOBJECT_SOLR_FIELD("add-publicobject-solr-field"),
    /**
     * Dataverse normally deletes all solr documents related to a dataset's files
     * when the dataset is reindexed. With this flag enabled, additional logic is
     * added to the reindex process to delete only the solr documents that are no
     * longer needed. (Required docs will be updated rather than deleted and 
     * replaced.) Enabling this feature flag should make the reindex process 
     * faster without impacting the search results.
     *
     * @apiNote Raise flag by setting
     * "dataverse.feature.reduce-solr-deletes"
     * @since Dataverse 6.3
     */
    REDUCE_SOLR_DELETES("reduce-solr-deletes"),
    /**
     * This flag disables the feature that automatically selects one of the 
     * DataFile thumbnails in the dataset/version as the dedicated thumbnail
     * for the dataset.
     * 
     * @apiNote Raise flag by setting
     * "dataverse.feature.disable-dataset-thumbnail-autoselect"
     * @since Dataverse 6.4
     */
    DISABLE_DATASET_THUMBNAIL_AUTOSELECT("disable-dataset-thumbnail-autoselect"),
    /**
     * Only update a DataCite DOI when needed (for efficiency, lighter load on DataCite).
     */
    ONLY_UPDATE_DATACITE_WHEN_NEEDED("only-update-datacite-when-needed"),


JvmSettings:
    MIN_FILES_TO_USE_PROXY(SCOPE_SOLR, "min-files-to-use-proxy"),

    // INDEX CONCURENCY
    SCOPE_SOLR_CONCURENCY(SCOPE_SOLR, "concurrency"),
    MAX_ASYNC_INDEXES(SCOPE_SOLR_CONCURENCY, "max-async-indexes"),

    //EXPORTS
    SCOPE_EXPORTS(PREFIX, "exports"),
    SCOPE_EXPORTS_SCHEMA_DOT_ORG(SCOPE_EXPORTS, "schema-dot-org"),
    EXPORTS_SCHEMA_DOT_ORG_MAX_FILES_FOR_DOWNLOAD_ENTRIES(SCOPE_EXPORTS_SCHEMA_DOT_ORG, "max-files-for-download-entries"),

Settings:

        SolrFullTextIndexing, //true or false (default)
        SolrMaxFileSizeForFullTextIndexing, //long - size in bytes (default unset/no limit)
        /** Default Key for limiting the number of bytes uploaded via the Data Deposit API, UI (web site and . */
        MaxFileUploadSizeInBytes,
        ZipDownloadLimit,
/* size limit for Tabular data file ingests */
        /* (can be set separately for specific ingestable formats; in which 
        case the actual stored option will be TabularIngestSizeLimit:{FORMAT_NAME}
        where {FORMAT_NAME} is the format identification tag returned by the 
        getFormatName() method in the format-specific plugin; "sav" for the 
        SPSS/sav format, "RData" for R, etc.
        for example: :TabularIngestSizeLimit:RData */
        TabularIngestSizeLimit,
        /* Validate physical files in the dataset when publishing, if the dataset size less than the threshold limit */
        DatasetChecksumValidationSizeLimit,
        /* Validate physical files in the dataset when publishing, if the datafile size less than the threshold limit */
        DataFileChecksumValidationSizeLimit,
        FilePIDsEnabled,
        /**
         * If defined, this is the URL of the zipping service outside 
         * the main Application Service where zip downloads should be directed
         * instead of /api/access/datafiles/
         */
        CustomZipDownloadServiceUrl,

        /**
         * The URL for the DvWebLoader tool (see github.com/gdcc/dvwebloader for details)
         */
        WebloaderUrl, 


        /**
         * A comma-separated list of CategoryName in the desired order for files to be
         * sorted in the file table display. If not set, files will be sorted
         * alphabetically by default. If set, files will be sorted by these categories
         * and alphabetically within each category.
         */
        CategoryOrder,
        /**
         * True(default)/false option deciding whether ordering by folder should be applied to the 
         * dataset listing of datafiles.
         */
        OrderByFolder,
        /**
         * Allows an instance admin to disable Solr search facets on the collection
         * and dataset pages instantly
         */
        DisableSolrFacets,
        DisableSolrFacetsForGuestUsers,
        DisableSolrFacetsWithoutJsession,
        DisableUncheckedTypesFacet,

        /**
         * When ingesting tabular data files, store the generated tab-delimited 
         * files *with* the variable names line up top. 
         */
        StoreIngestedTabularFilesWithVarHeaders,

 There is no well-defined cut-off - the speed of viewing and editing a large dataset will decrease as files are added, and, at some point, Dataverse may need more memory than is available, resulting in drastically slower response / 





The direct upload option now switches between uploading the file in one piece (up to 1 GB by default) and sending it as multiple parts. The default can be changed by setting:
  
``./asadmin create-jvm-options "-Ddataverse.files.<id>.min-part-size=<size in bytes>"``

For AWS, the minimum allowed part size is 5*1024*1024 bytes and the maximum is 5 GB (5*1024**3). Other providers may set different limits.

It is also possible to set file upload size limits per store. See the :MaxFileUploadSizeInBytes setting described in the :doc:`/installation/config` guide.

At present, one potential drawback for direct-upload is that files are only partially 'ingested' - tabular and FITS files are processed, but zip files are not unzipped, and the file contents are not inspected to evaluate their mimetype. This could be appropriate for large files, or it may be useful to completely turn off ingest processing for performance reasons (ingest processing requires a copy of the file to be retrieved by the Dataverse installation from the S3 store). A store using direct upload can be configured to disable all ingest processing for files above a given size limit:

``./asadmin create-jvm-options "-Ddataverse.files.<id>.ingestsizelimit=<size in bytes>"``

.. _s3-direct-upload-features-disabled:

Features that are Disabled if S3 Direct Upload is Enabled
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The following features are disabled when S3 direct upload is enabled.

- Unzipping of zip files. (See :ref:`compressed-files`.)
- Detection of file type based on JHOVE and custom code that reads the first few bytes except for the refinement of Stata file types to include the version. (See :ref:`redetect-file-type`.)
- Extraction of metadata from FITS files. (See :ref:`fits`.)
- Creation of NcML auxiliary files (See :ref:`netcdf-and-hdf5`.)
- Extraction of a geospatial bounding box from NetCDF and HDF5 files (see :ref:`netcdf-and-hdf5`) unless :ref:`dataverse.netcdf.geo-extract-s3-direct-upload` is set to true.

.. _cors-s3-bucket:

Allow CORS for S3 Buckets
^^^^^^^^^^^^^^^^^^^^^^^^

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
^^^^^^^^^^^^^^^^^^^^^^^^

Since the direct upload mechanism creates the final file rather than an intermediate temporary file, user actions, such as neither saving or canceling an upload session before closing the browser page, can leave an abandoned file in the store. The direct upload mechanism attempts to use S3 tags to aid in identifying/removing such files. Upon upload, files are given a "dv-state":"temp" tag which is removed when the dataset changes are saved and new files are added in the Dataverse installation. Note that not all S3 implementations support tags. Minio, for example, does not. With such stores, direct upload may not work and you might need to disable tagging. For details, see :ref:`s3-tagging` in the Installation Guide.

Trusted Remote Storage with the ``remote`` Store Type
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

**Best for:** Very large files that should remain in their original location

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
~~~~~~~~~~~~~~~~~~~

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

Handling High Volume of Files
----------------------------

When dealing with datasets containing thousands or millions of files, different challenges arise beyond just file size.

Performance Considerations
~~~~~~~~~~~~~~~~~~~~~~~~~

**Database Impact**

Each file in Dataverse requires database entries for metadata, permissions, and other attributes. When dealing with very large numbers of files:

* Database query performance may degrade
* Indexing operations take longer
* Dataset versioning becomes more resource-intensive

**Recommended Approaches**

For datasets with extremely high file counts (10,000+), consider these strategies:

1. **Use hierarchical organization** - Group files into logical directories to improve navigation
2. **Batch operations** - Use the API for batch uploads rather than the UI
3. **Consider file bundling** - Where appropriate, bundle related small files into archives
4. **Use Globus for bulk transfers** - Globus is optimized for handling large numbers of files

API-Based Batch Operations
~~~~~~~~~~~~~~~~~~~~~~~~~

For programmatically handling large numbers of files, Dataverse provides API endpoints that support batch operations:

* The `/api/datasets/:id/addFiles` endpoint allows adding multiple files in a single request
* The `/api/datasets/:id/deleteFiles` endpoint allows removing multiple files at once

See the :doc:`/api/native-api` documentation for details on these endpoints.

Monitoring and Maintenance
~~~~~~~~~~~~~~~~~~~~~~~~~

When working with high file volumes:

* Monitor database performance regularly
* Consider implementing scheduled maintenance windows for index optimization
* Use the Dataverse metrics API to track system performance

Storage Strategy Recommendations
-------------------------------

Based on both file size and volume considerations, here are some general recommendations:

1. **For research projects with moderate data (< 2GB files, < 1000 files):**
   * Default Dataverse storage is sufficient

2. **For projects with large files but moderate file counts:**
   * Configure S3 direct upload/download
   * Set appropriate ingest size limits

3. **For projects with very large files or sensitive data that should remain in place:**
   * Use the remote storage option
   * Consider implementing access controls at the remote storage level

4. **For high-performance computing environments or very large datasets:**
   * Implement Globus transfer
   * Use batch operations via API
   * Consider custom workflows for dataset creation and management

5. **For datasets with millions of small files:**
   * Consider file bundling where appropriate
   * Use Globus for transfer
   * Implement hierarchical organization

Performance Tuning
-----------------

Regardless of which storage solution you choose, consider these performance tuning options:

* Increase JVM heap size for Dataverse application
* Optimize database indexes for file metadata tables
* Configure appropriate timeouts for large file transfers
* Consider dedicated storage nodes for high-volume installations

For detailed performance tuning recommendations, see the :doc:`/installation/config` guide.
