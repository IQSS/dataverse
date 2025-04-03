API Changelog (Breaking Changes)
================================

This API changelog is experimental and we would love feedback on its usefulness. Its primary purpose is to inform API developers of any breaking changes. (We try not to ship any backward incompatible changes, but it happens.) To see a list of new APIs and backward-compatible changes to existing API, please see each version's release notes at https://github.com/IQSS/dataverse/releases

.. contents:: |toctitle|
    :local:
    :depth: 1

v6.7
----

- An undocumented :doc:`search` parameter called "show_my_data" has been removed. It was never exercised by tests and is believed to be unused. API users should use the :ref:`api-mydata` API instead.

v6.6
----

- The JSON representation for a datasetVersion sent or received in API calls has changed such that

  - "versionNote" -> "deaccessionNote"
  -  "archiveNote" --> "deaccessionLink"
  - These may be non-null for deaccessioned versions and an optional new "versionNote" field indicating the reason a version was created may be present on any datasetversion. 

- **/api/metadatablocks** is no longer returning duplicated metadata properties and does not omit metadata properties when called.
- **/api/roles**: :ref:`show-role` now properly returns 403 Forbidden instead of 401 Unauthorized when you pass a working API token that doesn't have the right permission.
- The content type for the ``schema.org`` dataset metadata export format has been corrected. It was ``application/json`` and now it is ``application/ld+json``. See also :ref:`export-dataset-metadata-api`.

v6.5
----

- **/api/datasets/{identifier}/links**: The response from :ref:`list-collections-linked-from-dataset` has been improved to provide a more structured (but backward-incompatible) JSON response.

v6.4
----

- **/api/datasets/$dataset-id/modifyRegistration**: Changed from GET to POST
- **/api/datasets/modifyRegistrationPIDMetadataAll**: Changed from GET to POST

v6.3
----

- **/api/admin/superuser/{identifier}**: The POST endpoint that toggles superuser status has been deprecated in favor of a new PUT endpoint that allows you to specify true or false. See :ref:`set-superuser-status`.

v6.2
----

- The fields "northLongitude" and "southLongitude" have been deprecated in favor of "northLatitude" and "southLatitude" in the Geolocation metadata block. After upgrading to 6.2 or later, you will need to use the new fields when creating or updating a dataset.

- **/api/datasets/{id}/versions/{versionId}**: The includeFiles parameter has been renamed to excludeFiles. The default behavior remains the same, which is to include files. However, when excludeFiles is set to true, the files will be excluded. A bug that caused the API to only return a deaccessioned dataset if the user had edit privileges has been fixed.
- **/api/datasets/{id}/versions**: The includeFiles parameter has been renamed to excludeFiles. The default behavior remains the same, which is to include files. However, when excludeFiles is set to true, the files will be excluded.
- **/api/files/$ID/uningest**: Can now be used by users with the ability to publish the dataset to undo a failed ingest. (Removing a successful ingest still requires being superuser)

v6.1
----

- The metadata field "Alternative Title" now supports multiple values so you must pass an array rather than a string when populating that field via API. See https://github.com/IQSS/dataverse/pull/9440

v6.0
----

- **/api/access/datafile**: When a null or invalid API token is provided to download a public (non-restricted) file with this API call, it will result on a ``401`` error response. Previously, the download was allowed (``200`` response). Please note that we noticed this change sometime between 5.9 and 6.0. If you can help us pinpoint the exact version (or commit!), please get in touch. See :doc:`dataaccess`.
- **/openapi**: This endpoint is currently broken. See https://github.com/IQSS/dataverse/issues/9981

v5.6
----

- **/api/dataverses/$PARENT/datasets**: The "create dataset" API endpoint now requires the header ``Content-type:application/json`` to be passed. The error can be confusing, saying something about validation, such as ``'{"status":"ERROR","message":"Validation Failed: Title is required. (Invalid value:edu.harvard.iq.dataverse.DatasetField[ id=null ])...``. See :ref:`create-dataset-command`.
