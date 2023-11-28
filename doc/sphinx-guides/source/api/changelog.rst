API Changelog 
=============

.. contents:: |toctitle|
    :local:
    :depth: 1

v6.1
----

New
~~~
- **/api/dataverses/{id}/datasetSchema**: See :ref:`get-dataset-json-schema`.
- **/api/dataverses/{id}/validateDatasetJson**: See :ref:`validate-dataset-json`.

Changes
~~~~~~~
- **/api/datasets/{id}/versions/{versionId}/citation**: This endpoint now accepts a new boolean optional query parameter "includeDeaccessioned", which, if enabled, causes the endpoint to consider deaccessioned versions when searching for versions to obtain the citation. See :ref:`get-citation`.

v6.0
----

Changes
~~~~~~~
- **/api/access/datafile**: When a null or invalid API token is provided to download a public (non-restricted) file with this API call, it will result on a ``401`` error response. Previously, the download was allowed (``200`` response). Please note that we noticed this change sometime between 5.9 and 6.0. If you can help us pinpoint the exact version (or commit!), please get in touch. See :doc:`dataaccess`.
