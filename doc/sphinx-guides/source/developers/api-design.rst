==========
API Design
==========

API design is a large topic. We expect this page to grow over time.

.. contents:: |toctitle|
	:local:

Paths
-----

A reminder `from Wikipedia <https://en.wikipedia.org/wiki/Uniform_Resource_Identifier>`_ of what a path is:

.. code-block:: bash

          userinfo       host      port
          ┌──┴───┐ ┌──────┴──────┐ ┌┴┐
  https://john.doe@www.example.com:123/forum/questions/?tag=networking&order=newest#top
  └─┬─┘   └─────────────┬────────────┘└───────┬───────┘ └────────────┬────────────┘ └┬┘
  scheme          authority                  path                  query           fragment

Exposing Settings
~~~~~~~~~~~~~~~~~

Since Dataverse 4, database settings have been exposed via API at http://localhost:8080/api/admin/settings

(JVM options are probably available via the Payara REST API, but this is out of scope.)

Settings need to be exposed outside to API clients outside of ``/api/admin`` (which is typically restricted to localhost). Here are some guidelines to follow when exposing settings.

- When you are exposing a database setting as-is:

  - Use ``/api/info/settings`` as the root path.

  - Append the name of the setting including the colon (e.g. ``:DatasetPublishPopupCustomText``)

  - Final path example: ``/api/info/settings/:DatasetPublishPopupCustomText``

- If the absence of the database setting is filled in by a default value (e.g. ``:ZipDownloadLimit`` or ``:ApiTermsOfUse``):

  - Use ``/api/info`` as the root path.

  - Append the setting but remove the colon and downcase the first character (e.g. ``zipDownloadLimit``)

  - Final path example: ``/api/info/zipDownloadLimit``

- If the database setting you're exposing make more sense outside of ``/api/info`` because there's more context (e.g. ``:CustomDatasetSummaryFields``):

  - Feel free to use a path outside of ``/api/info`` as the root path.

  - Given additional context, append a shortened name (e.g. ``/api/datasets/summaryFieldNames``).

  - Final path example: ``/api/datasets/summaryFieldNames``

- If you need to expose a JVM option (MicroProfile setting) such as ``dataverse.api.allow-incomplete-metadata``:

  - Use ``/api/info`` as the root path.

  - Append a meaningful name for the setting (e.g. ``incompleteMetadataViaApi``).

  - Final path example: ``/api/info/incompleteMetadataViaApi``

