==========
API Design
==========

API design is a large topic. We expect this page to grow over time.

.. contents:: |toctitle|
	:local:

.. _openapi-dev:

OpenAPI
-------

As you add API endpoints, please be conscious that we are exposing these endpoints as an OpenAPI document at ``/openapi`` (e.g. http://localhost:8080/openapi ). See :ref:`openapi` in the API Guide for the user-facing documentation.

We've played around with validation tools such as https://quobix.com/vacuum/ and https://pb33f.io/doctor/ only to discover that our OpenAPI output is less than ideal, generating various warnings and errors.

You can prevent additional problems in our OpenAPI document by observing the following practices:

- When creating a method name within an API class, make it unique.

If you are looking for a reference about the annotations used to generate the OpenAPI document, you can find it in the `MicroProfile OpenAPI Specification <https://download.eclipse.org/microprofile/microprofile-open-api-3.1/microprofile-openapi-spec-3.1.html#_detailed_usage_of_key_annotations>`_.

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

