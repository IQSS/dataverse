Scaling Dataverse with Data Size
================================

This section is intended to help administrators configure Dataverse appropriately to handle larger amounts of data.

Scaling is a complex subject: there are many options available in Dataverse that can improve performance with larger scale data, some of which
work differently than Dataverse's default configuration, potentially requiring user education, and some which can require additional expertise to manage.

In general, there are three dimensions in which Dataverse can scale:
  
1. **Storage size of individual files and aggregate storage size**
2. **Number of files per dataset**
3. **Number of datasets**


.. contents:: |toctitle|
        :local:

.. _choose-store:

Storage: Choosing the Right Store
---------------------------------

The main issues for handling larger files and larger aggregate data size relate to the performance of the storage used and how involved the Dataverse server is in the data transfer. 
With appropriate configuration, Dataverse can support file sizes and aggregate dataset sizes into the terabyte scale and beyond.

The primary choice in Dataverse related to storage is which types of "store" (also called "storage driver") to use:

.. _file-stores:

File Stores
~~~~~~~~~~~

The default storage option in Dataverse uses the local file system. When files are transferred to Dataverse, they are first stored in a
temporary location on the Dataverse server. Any zip files uploaded are unzipped to create multiple individual file entries. Once an upload is completed, 
Dataverse copies the files to permanent storage. Dataverse also takes advantage of the file being local to inspect its bytes to determine its 
MIME type, and, for tabular data, to ":doc:`ingest </user/tabulardataingest/index>`" it - extracting metadata about the variables used in the file and creating a tab-separated values (TSV)
version of the file.

Benefits: 

- This option requires no external services and can potentially handle files into the gigabyte (GB) size range. For smaller institutions,
  and in disciplines where datasets do not have more than a few hundred files and files are not too large, this can be the simplest option.
- Unzipping of zip archives can be simpler for users than having to upload many individual files and was, at one time, the only way to 
  preserve file path names when uploading. In addition, some of the unzipped files might be in a format that can be previewed or otherwise acted upon - see :ref:`file-handling` in the User Guide.

Challenges: In general, file storage is not a good option for larger data sizes - both in terms of file size and number of files. Contributing factors include:
 
- Because temporary storage is used, transfers will temporarily use several times as much space as the final transfer. Unzipping also increases the final storage size of a dataset.
- Because all uploads use the same temporary storage, temporary storage must be large enough to handle multiple users uploading data.
- Each file is uploaded as a single HTTP request, which can cause long transfer times which, in turn, can trigger timeout errors in Dataverse or any proxy or load balancer in front of Dataverse.
- Uploading many files at once can trigger any rate limiter in front of the Dataverse server (i.e. used to throttle AI bots) resulting in failures.
- Because transfers (both uploads and downloads) are handled by the Dataverse server, they add to server processing load which can affect overall performance.
- Cost: local file storage must be provisioned in advance based on anticipated demand. It can involve up-front costs (for a local disk), or, when procured from a 
  cloud provider, is likely to be more expensive than object storage from that provider (see below).

.. _s3-stores:

S3 Stores: Object Storage via S3
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

A more scalable option for storage is to use an object store with transfers managed using the Simple Storage Service (S3) protocol. S3-compatible storage can 
easily be bought (rented) from major cloud providers, but may also be available from institutional clouds. It is also possible to run open source software to provide
S3 storage over a local file system (making it possible to enjoy the advantages discussed below while still leveraging local file storage). 

While S3 Stores can be configured to handle uploads and downloads as with file storage (with zip files being unzipped, but having many of the same challenges in terms of temporary storage and server load as discussed above) 
they can also be configured to use "direct" upload and download. In this configuration, the actual transfer of file bytes is between the user's local machine and
the S3 store. In this configuration, files are never stored on the Dataverse server. Dataverse does not attempt to unzip zip files, and they are stored as a single file in the dataset.

Benefits: S3 offers several advantages over file storage:
 
- Scalability: S3 is designed to handle large amounts of data. It can handle individual files up to several TB in size. 
  Because S3 supports breaking files into pieces, Dataverse can transfer a file in pieces (several in parallel, potentially thousands of pieces per file) making transfers faster
  and more robust (a failure requires only resending the failed piece). It may also be the case that users will have a faster network connection to the S3 store 
  (e.g. in a commercial cloud or High Performance Computing center) than they do to the Dataverse server, reducing transfer time.
- High Availability: S3 provides redundancy beyond what is available with a local file system (valuable for preservation, potentially reducing the need to perform data integrity checks).

Challenges:

- One additional step that is required to enable direct uploads via a Dataverse installation and for direct download to work with previewers and direct upload to work with DVWebloader (:ref:`folder-upload`) is to allow cross site (CORS) requests on your S3 store.
- Cost: S3 offers a pricing model that allows you to pay for the storage and transfer of data based on current usage (versus long term demand) but commercial 
  providers charge more per TB than the equivalent cost of a local disk (though commercial S3 storage is cheaper than commercial file storage).
  There can also be egress and other charges. Overall, S3 storage is generally more expensive than local file storage but cheaper than cloud file storage.
  Running a local S3 storage or leveraging an institutional service can further reduce costs.
- Direct upload via S3 is a :doc:`multi-step process </developers/s3-direct-upload-api>`: Dataverse provides URLs for the uploads, the user's browser or other app uses the URLs to transfer files to the S3 store,
  possibly in many pieces per file, and finally, Dataverse is told that one or more files are in place and should be added to the dataset. If the last step fails, or if
  all parts of a file cannot be transferred, orphaned files or parts of files can be left on S3. These files are not accessible via Dataverse but do use space (for which there is a monetary cost)
  until they are deleted. There is currently no automated clean-up mechanism.

Other Considerations
^^^^^^^^^^^^^^^^^^^^

- S3 Storage without direct upload/download provides minimal benefits with Dataverse as files still pass through the server, files are still uploaded as a single HTTP/HTTPS stream, and temporary storage is still used.
- While not having files unzipped can be confusing to users who are used to it from using Dataverse with file storage, there are ways to minimize the impact. 
  For example, Dataverse can be configured to use a "Zip File Previewer" that allows users to see the contents of a zip file and even download individual files from within it (see :ref:`compressed-files`). 
  For users who still want their data stored as individual files with their relative folder paths, Dataverse can be configured with ":ref:`DVWebloader <folder-upload>`" which allows users to select an entire folder tree of files and
  upload them, with their relative paths intact, to Dataverse. (DVWebloader can only be used with S3/direct upload, but it is much more efficient with many files than using the 
  standard upload interface in Dataverse (which also does not retain path information)).
- Several features that involve Dataverse accessing files' contents, including unzipping zip files, are disabled when S3 direct upload is enabled. See :ref:`s3-direct-upload-features-disabled`.

- Using direct upload stops Dataverse from inspecting the file bytes to determine the MIME type (with one exception - Stata files). Dataverse will still look at the file name and extension to determine the MIME type.
- To perform "ingest" processing (see :doc:`/user/tabulardataingest/index`), Dataverse currently has to copy the file to local storage, negating the benefit of sending data directly to S3. To manage larger files, one can set a per-store
  ingest size limit (which can be 0 bytes) to stop ingest or limit it to smaller files (see :ref:`list-of-s3-storage-options`). 
- Dataverse's mechanism for downloading a whole dataset or multiple selected files involves zipping those files together. Even When using S3 with direct upload/download,
  the file bytes are transferred to the Dataverse server as part of the zipping process. There are ways to reduce the performance impact of this:
  
  - There is a :ref:`Standalone "Zipper" Service Tool <zipdownloader>` that can be run separate from Dataverse to handle the zipping process.
  - Dataverse has a :ref:`:ZipDownloadLimit` that can be used to limit the amount of data that can be zipped. If a dataset is larger than this limit, Dataverse will only add some of the files to the zip and list others in the included manifest file.
  - There are tools such as the Dataverse Dataset Downloader (https://github.com/gdcc/dataverse-recipes/tree/main/shell/download#dataverse-dataset-downloader) that can be used to download all of the files individually. This avoids sending any of the files through the Dataverse server when S3 direct download is enabled.  

- Dataverse leverages S3 features that are not implemented by all servers and has several configuration options geared towards handling variations between servers - see :ref:`s3-compatible`. Site admins should be sure to test with their preferred S3 implementation (and consider adding to the list of working S3 implementations).
- The part-size used when directly transferring files to S3 is configurable (at AWS, from 5 MiB to 5GiB). The default in Dataverse is 1 GiB (1024^3 bytes). If the primary use case is with smaller files than that, decreasing the part size may improve upload speeds.

.. _remote-stores:

Remote Stores
~~~~~~~~~~~~~

Note: Remote Storage is still experimental: feedback is welcome! See :ref:`support`.

For very large, and/or very sensitive data, it may not make sense to transfer or copy files to Dataverse at all.
The ``remote`` store type in the Dataverse software supports these use cases.
It allows Dataverse to store a URL reference to the file rather than transferring the file bytes to a store managed directly by Dataverse.
In the most basic configuration a site administrator configures the base URL for the store, e.g. "https://thirdpartystorage.edu/long-term-storage/" 
and users can then create files referencing any URL starting with that base, e.g. "https://thirdpartystorage.edu/long-term-storage/my_project_dir/my_file.txt".

If the remote site is a public web server, the remote store in Dataverse should be configured to be "public" which will disable the ability to restrict
or embargo files (as they are public on the remote site and Dataverse cannot block access.) Conversely, Dataverse can be configured to sign requests to the 
remote server which the remote server can then, if it is capable of validating them, use to reject requests not approved by Dataverse. In this configuration,
users can restrict and embargo files and Dataverse and the remote server will cooperate to manage access control. Another alternative, with a more advanced
remote store, would be, instead of using URLs that directly enable download of the file, to use URLs that point to a landing page at the remote server that 
may require the user to login and go through some approval process before being able to access the file.

Dataverse considers remote storage to be read-only, or, in cases where the remote service does not provide a way for Dataverse to download the file bytes
(due to access control or because the URL refers to a landing page), inaccessible. Depending on whether Dataverse can access the bytes of the file,
functionality such as ingest and integrity checking may or may not be possible. If the file bytes are not accessible, the remote store in Dataverse should be
configured to disable operations that attempt to access the file (see the files-not-accessible-by-dataverse in :ref:`trusted-remote-storage`).
Regardless of whether the remote files can be read, local storage of other datafiles and auxiliary files in the same dataset is possible. Support for such files is handled by configuring a "base" store with the remote store that is used for these purposes. (This means that while
files added as remote remain on the remote store, other files in the dataset, and potentially thumbnails and the ingested TSV format of remote files would be managed by Dataverse
in the base store. If ingest is not desired, the ingest size limit for the store can be set to 0 bytes).


Benefits: 

- This is a relatively simple way to off-load the management of large and/or sensitive data files to other organizations while still providing Dataverse's overall capabilities for dataset curation and publication to users.   
- If the store has been configured with a remote-store-name or remote-store-url, the dataset file table will include this information for remote files. These provide a visual indicator that the files are not managed directly by Dataverse and are stored/managed by a remote trusted store.

Challenges:

- As Dataverse is relying on the remote service to maintain the integrity and availability of the files, it is likely that the Dataverse site admin will want to have a formal agreement with the remote service 
  operator about their policies.
- Currently, remote files can only be added via the API. (This may be addressed in future versions).
- Remote files can only be added after the dataset is created in the UI (and therefore has an id and PID for use with the API). However, the UI will still allow upload of files to the base store (at dataset creation and when editing), which could be confusing.
- Site admins need to consider carefully how to configure file size limits, ingest size limits, etc. on the remote store and its base store, and whether the remote store is public-only, and whether files there can be read by Dataverse to assure the
  requirements of a specific use case(s) are addressed.
- The current remote store implementation will not prevent you from providing a relative URL that results in a 404 when resolved (i.e. if you make a typo). You should check to make sure the file exists at the location you specify - by trying to download in Dataverse, by checking to see that Dataverse was able to get the file size (which it does with a HEAD call to that location), or just manually trying the URL in your browser.
- For large files, direct-download should always be used with a remote store. (Otherwise the Dataverse will be involved in the download.)
- When multiple files are selected for download, Dataverse will try to include remote files in the zip file being created (up to the max zip size limit) which is inefficient, and will not be able to include remote files that are inaccessible (possibly confusing). 

.. _globus-stores:

Globus Stores: Globus Transfer Between Large File/Tape Archives
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Note: Globus Transfer is still experimental: feedback is welcome! See :ref:`support`.

`Globus <https://www.globus.org>`_ provides file transfer service that is widely used for the world's largest datasets (in terms of both file size and number of files). It provides:

- Robust file transfer capable of handling delays (e.g. due to the time it takes to mount tapes) and restarting after network or endpoint failures
- Rapid parallel file transfers, potentially between clusters of computers on both ends
- Third-party transfer, which enables a user working with their desktop browser to initiate transfers of files between remote endpoints, i.e. sending files on a local high-performance computing cluster to a Dataverse endpoint or vice versa.

Dataverse can be configured to support Globus transfers in multiple ways:

- A Dataverse-managed Globus File Endpoint: Dataverse controls user access to the endpoint, access is only via Globus
- A Dataverse-managed Globus S3 Endpoint: Dataverse controls user access to the endpoint, access is available via S3 and via Globus
- A Globus Endpoint treated as Remote Storage: Dataverse references files on a Globus endpoint managed by a third party

Benefits: 

- Globus scales to higher data volumes than any other option. Users working with large data are often familiar with Globus and are interested in transferring data to/from computational clusters rather than their local machine.
- Globus transfers can be initiated by choosing the Globus option in the dataset upload panel. Analogously, "Globus Transfer" is one of the download options in the "Access Dataset" menu.
- For the non-S3 options, Dataverse supports having a base store (e.g. a local file system or an S3-based store), which can be used internally by Dataverse (e.g. for thumbnails, etc.) and can allow users to upload smaller files (e.g. READMEs, documentation) that might not be suited to a given Globus endpoint (e.g. a tape store).

Challenges: 

- Globus is complex to manage and Dataverse installations will need to develop Globus expertise or partner with another organization (i.e. an institutional high-performance computing center) to manage Globus endpoints.
- For users not familiar with Globus, managing transfers can be confusing. For the non-S3 options, users cannot just download files - they must have access to a destination Globus endpoint and have a Globus account. Globus does provide free accounts and a free "Globus Personal Connect" service which installed on any machine to allow transfers to/from it.
- Globus transfers are not enabled at dataset-creation time. Once the draft version is created, users can initiate Globus transfers to upload files from remote endpoints.
- For Dataverse-managed endpoints, a community-developed `dataverse-globus <https://github.com/gdcc/dataverse-globus>`_ app must be installed and configured in the Dataverse instance. 
  This app manages granting and revoking access for users to upload/download files from Dataverse and handles the translation between Dataverse's internal file naming/organization to that seen by the user.
- Users familiar with Globus sometimes expect to be able to find the Dataverse endpoint in Globus' online service and download files from there. Due to the fact that Dataverse is managing permissions and handling file naming, this doesn't work.
- Due to differences between Dataverse's and Globus's access control models, Dataverse cannot enforce per-file access restrictions - restriction can only be done today at the level of providing access to all files in a dataset.
  Globus stores can be defined as public to disable Dataverse's ability to restrict and embargo files in that store. If the store is configured to support restriction and embargo,
  Dataverse and its Dataverse-Globus app will limit users to downloading only the files they have been granted access to, but a technically knowledgeable user could access other files in the same dataset if they are give access to one.
  (Data depositors would need to be aware of this limitation and could be guided to restrict all files/only grant access to all dataset files in Globus as a work-around).
- Dataverse-managed endpoints must be Globus "guest collections" hosted on either a file-system-based endpoint or an S3-based endpoint (the latter requires use of the Globus
  S3 connector which requires a paid Globus subscription at the host institution). In either case, Dataverse is configured with the Globus credentials of a user account that can manage the endpoint.
  Users will need their own Globus account, which can be obtained via their institution or directly from Globus (at no cost).
- With the file-system endpoint, Dataverse does not currently have access to the file contents. Thus, functionality related to ingest, previews, fixity hash validation, etc. are not available. 
  (Using the S3-based endpoint, Dataverse has access via S3 and all functionality normally associated with direct uploads to S3 is available. In this case admins should be sure to set the maximum size for ingest and avoid requiring hash validation at publication, etc.)
- For the reference use case, Dataverse must be configured with a list of allowed endpoint/base paths from which files may be referenced. In this case, since Dataverse is not accessing the remote endpoint itself, it does not need Globus credentials. 
  Users will also need a Globus account in this case, and the remote endpoint must be configured to allow them access, i.e. be publicly readable, or potentially supporting some out-of-band mechanism for access requests (which could be described, for example, in the dataset's Terms of Use and Access).
- As with remote stores, files can only be added in the Globus reference case via the Dataverse API.
- While Globus itself can handle many (millions of) files of any size, Dataverse cannot handle more than thousands of files per dataset (at best) and some Globus endpoints may have limits on file sizes - both maximums and minimums (e.g. for tape storage where small files are inefficient).
  Users will need to be made aware of these limitations and the possibilities for managing them (e.g. by aggregating multiple files in a single, larger file, or storing smaller files in the base-store via the normal Dataverse upload UI).
- There is currently `a bug <https://github.com/gdcc/dataverse-globus/issues/2>`_ that won't allow users to transfer files from/to endpoints where they do not have permission to list the overall file tree (i.e. an institution manages <endpoint>/institution_name but the user only has access to <endpoint>/institution_name/my_dir.)
  Until that is fixed, a work-around is to first transfer data to an endpoint without this restriction.
- An alternative, experimental implementation of Globus polling of ongoing upload transfers was added in v6.4. This framework does not rely on the instance staying up continuously for the duration of the transfer and saves the state information about Globus upload requests in the database. While it is now the recommended option, it is not enabled by default. See the ``globus-use-experimental-async-framework`` feature flag (see :ref:`feature-flags`) and the JVM option :ref:`dataverse.files.globus-monitoring-server`.

More details of the setup required to enable Globus is described in the `Community Dataverse-Globus Setup and Configuration document <https://docs.google.com/document/d/1mwY3IVv8_wTspQC0d4ddFrD2deqwr-V5iAGHgOy4Ch8/edit?usp=sharing>`_ and the references therein.

An overview of the control and data transfer interactions between components was presented at the 2022 Dataverse Community Meeting and can be viewed in the `Integrations and Tools Session Video <https://youtu.be/3ek7F_Dxcjk?t=5289>`_ around the 1 hr 28 min mark.

See also :ref:`globus-support` and :ref:`Globus settings <:GlobusSettings>`.


Storage Strategy Recommendations
--------------------------------

Based on both file size and volume considerations, here are some general recommendations:

1. **For research projects with moderate data (< 2GB files, < 100s of files/dataset):**

   * The default File Store is sufficient
   * Consider setting file count and size limits (see below)

2. **For projects with larger files (GBs to TBs) and/or more files per dataset (100s to 1000s):**

   * Configure an S3 store with direct upload/download
   * Set appropriate ingest size limits

3. **For projects with very large files or sensitive data that should remain in place:**

   * Use the Remote Store

4. **For high-performance computing environments or very large datasets (TBs+):**

   * Use a Globus Store
   * Work with users to size files appropriate to the underlying storage
   * Consider Globus over S3 when normal upload/download options (via the UI/API) are desired along with Globus transfer

5. **For Petascale datasets, or extreme numbers of files:**

   * Consider a Remote Store and referencing a single URL/Globus endpoint for the entire dataset

6. **For Dataverse installations supporting a range of data scales:**

   * Consider using :ref:`multiple stores <multiple-stores>` and assigning stores to individual collections or datasets
   
Managing More Files per Dataset and More Datasets
-------------------------------------------------

Dataverse can be configured to handle datasets with hundreds or thousands of files and hundreds of thousands of datasets. However, reaching these levels can require significant effort to appropriately configure the server.

Technically, there are two factors that can limit scaling the number of files per dataset: how much the Dataverse server is involved in data transfer, and constraints based on Dataverse's code and database configuration.
The former is dramatically affected by the choice for file storage and options such as the S3 direct upload/download settings and ingest size limits. There are fewer ways to affect the latter beyond increasing the amount of memory and CPU resources available
or rewriting the relevant parts of Dataverse. (There are continuing efforts to improve Dataverse's performance and scaling, so it is also advisable to use the latest version if you are pushing the boundaries on scaling. Progress is being made.)

Scaling to larger numbers of datasets (and to some extent scaling files per dataset) also depends on Dataverse's Solr search engine. There have been very significant improvements in indexing and search performance in recent releases, including some that are not turned on by default (listed below).



Avoiding Many Files
~~~~~~~~~~~~~~~~~~~

Before describing things that can be done to improve scaling, it is important to note that there are configuration options and best practices to suggest to users to help avoid larger datasets and help them avoid hitting performance issues by going beyond the file counts you know work in your instance.

There are a number of settings to limit how many files can be uploaded (see :ref:`database-settings` and :ref:`jvm-options` for more details):

- :ref:`:ZipUploadFilesLimit` - the maximum number of files allowed in an uploaded zip file - only relevant for file stores and S3 when direct upload is not used.
- :ref:`:MultipleUploadFilesLimit` - the number of files the GUI user is allowed to upload in one batch, via drag-and-drop, or through the file select dialog
- :ref:`:MaxFileUploadSizeInBytes` - limit the size of files that can be uploaded
- :ref:`:UseStorageQuotas` - once enabled, super users can set per-collection quotas (in bytes) to limit the aggregate size of all files in the collection
- :ref:`dataverse.files.default-dataset-file-count-limit` - directly limits the number of files per dataset, can be changed per dataset via API (by super users)

Scaling-related Configuration
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

There are a broad range of options (that are not turned on by default) for improving how well Solr indexing and searching scales and for handling more files per dataset. Some of these are useful for all installations while others are related to specific use cases, or are mostly for emergency use (e.g. disabling facets).
(see :ref:`database-settings`, :ref:`jvm-options`, and :ref:`feature-flags` for more details):

- dataverse.feature.add-publicobject-solr-field=true - specifically marks unrestricted content as public in Solr. See :ref:`feature-flags`.
- dataverse.feature.avoid-expensive-solr-join=true - this tells Dataverse to use the feature above to speed up searches. See :ref:`feature-flags`.
- dataverse.feature.reduce-solr-deletes=true - when Solr entries are being updated, this avoids an unnecessary step (deletion of existing entries) for entries that are being replaced. See :ref:`feature-flags`.
- dataverse.feature.disable-dataset-thumbnail-autoselect=true - by default, Dataverse scans through all files in a dataset to find one that can be used as a thumbnail, which is expensive for many files. This disables that behavior to improve performance. See :ref:`feature-flags`.
- dataverse.feature.only-update-datacite-when-needed=true - reduces the load on DataCite and reduces Dataverse failures related to that load, which is important when using file PIDs on Datasets with many files. See :ref:`feature-flags`.
- :ref:`dataverse.solr.min-files-to-use-proxy` =<X> - improve performance/lower memory requirements when indexing datasets with many files, suggested value is in the range 200 to 500
- :ref:`dataverse.solr.concurrency.max-async-indexes` =<X> - limits the number of index operations running in parallel. The default is 4, larger values may improve performance (if the Solr instance is appropriately sized)
- dataverse.exports.schema-dot-org.max-files-for-download-entries - reduces the size of the schema.org metadata export when there are many files per dataset. By default, this is included in the dataset page header, so the smaller version improves page loading and can avoid issues with Google indexing (datasets with thousands of files+)
- :ref:`:SolrFullTextIndexing` - false improves performance at the expense of not indexing file contents
- :ref:`:SolrMaxFileSizeForFullTextIndexing` - size in bytes (default unset/no limit) above which file contents should not be indexed
- :ref:`:ZipDownloadLimit` - the maximum size in bytes for zipped downloads of files from a dataset. If the size of requested files is larger, some files will be omitted and listed in the zip manifest file as not included.
- :ref:`:DatasetChecksumValidationSizeLimit` - by default, Dataverse checks fixity (assuring the file contents match the recorded checksum) as part of publication. This setting specifies a maximum aggregate dataset size, above which this validation will not be done.
- :ref:`:DataFileChecksumValidationSizeLimit` - by default, Dataverse checks fixity (assuring the file contents match the recorded checksum) as part of publication. This setting specifies a maximum file size, above which validation will not be done.
- :ref:`:FilePIDsEnabled` - false is recommended when datasets have many files. Related settings allow file PIDS to be enabled/disabled per collection and per dataset
- :ref:`:CustomZipDownloadServiceUrl` - allows use of a separate process/machine to handle zipping up multi-file downloads. Requires installation of the separate Zip Download app
- :ref:`:WebloaderUrl` - enables use of an installed DVWebloader (by specifying its web location) which is more efficient for uploading many files 
- :ref:`:CategoryOrder` - Pre-sorts the file display by category, e.g. showing all "Documentation" files before "Data" files. Any user selected sorting by name, age, or size is done within these sections  
- :ref:`:OrderByFolder` - pre-sorts files by their directory Label (folder), showing files with no path before others. Any user selected sorting by name, age, or size is done within these sections
- :ref:`:DisableSolrFacets` - disables facets, which are costly to generate, in search results (including the main collection page)
- :ref:`:DisableSolrFacetsForGuestUsers` - only disable facets for guests
- :ref:`:DisableSolrFacetsWithoutJsession` - disables facets for users who have disabled cookies (e.g. for bots)
- :ref:`:DisableUncheckedTypesFacet` - only disables the facet showing the number of collections, datasets, files matching the query (this facet is potentially less useful than others)
- :ref:`:StoreIngestedTabularFilesWithVarHeaders` - by default, Dataverse stores ingested files without headers and dynamically adds them back at download time. Once this setting is enabled, Dataverse will leave the headers in place (for newly ingested files), reducing the cost of downloads


Scaling Infrastructure
----------------------

There is no well-defined cut-off in terms of files per dataset or number of datasets where the Dataverse software will fail. In general the speed of viewing and editing a large dataset will decrease as the volume of datasets and files increases.
For a given installation, at some point, Dataverse will need more memory than is available, or will max out the CPU or other resources and  performance may decline dramatically.

In such cases:

- Consider increasing the memory available to Dataverse (the Java heap size for the Payara instance)
- Consider a larger machine (more CPU resources)
- Verify that performance isn't being limited by Solr or Postgres
- Investigate performance tuning options for Payara, Solr, and Postgres
- Coordinate with others in the community - there is a lot of aggregate knowledge
- Consider contributing to software design changes - Dataverse scaling has improved dramatically over the past several years, but more can be done
- Watch for the new single page application (SPA) front-end for Dataverse. It includes features such as infinite scrolling through files with much faster initial page load times 
