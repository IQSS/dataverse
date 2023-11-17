API Changelog 
=============

.. contents:: |toctitle|
    :local:
    :depth: 1

6.1
---

New
~~~
- **/api/admin/downloadTmpFile**: See :ref:`download-file-from-tmp`.

6.0
-----

Changes
~~~~~~~
- **/api/access/datafile**: When a null or invalid API token is provided to download a public (non-restricted) file with this API call, it will result on a ``401`` error response. Previously, the download was allowed (``200`` response). Please note that we noticed this change sometime between 5.9 and 6.0. If you can help us pinpoint the exact version (or commit!), please get in touch.
