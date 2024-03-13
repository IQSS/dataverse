API Changelog (Breaking Changes)
================================

This API changelog is experimental and we would love feedback on its usefulness. Its primary purpose is to inform API developers of any breaking changes. (We try not ship any backward incompatible changes, but it happens.) To see a list of new APIs and backward-compatible changes to existing API, please see each version's release notes at https://github.com/IQSS/dataverse/releases

.. contents:: |toctitle|
    :local:
    :depth: 1

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