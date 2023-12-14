API Changelog 
=============

.. contents:: |toctitle|
    :local:
    :depth: 1

6.2
---
Changes
~~~~~~~
- **/api/datasets/{id}/versions/{versionId}**: The includeFiles parameter has been renamed to excludeFiles. The default behavior remains the same, which is to include files. However, when excludeFiles is set to true, the files will be excluded. A bug that caused the API to only return a deaccessioned dataset if the user had edit privileges has been fixed.


6.1
---

New
~~~
- **/api/admin/clearThumbnailFailureFlag**: See :ref:`thumbnail_reset`.

Changes
~~~~~~~
- **/api/datasets/{id}/versions/{versionId}/citation**: This endpoint now accepts a new boolean optional query parameter "includeDeaccessioned", which, if enabled, causes the endpoint to consider deaccessioned versions when searching for versions to obtain the citation. See :ref:`get-citation`.

6.0
---

Changes
~~~~~~~
- **/api/access/datafile**: When a null or invalid API token is provided to download a public (non-restricted) file with this API call, it will result on a ``401`` error response. Previously, the download was allowed (``200`` response). Please note that we noticed this change sometime between 5.9 and 6.0. If you can help us pinpoint the exact version (or commit!), please get in touch. See :doc:`dataaccess`.
