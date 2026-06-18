Discoverability
===============

Datasets are made discoverable by a variety of methods.

.. contents:: |toctitle|
  :local:

DataCite Integration
--------------------

If you are using `DataCite <https://datacite.org>`_ as your DOI provider, when datasets are published, metadata is pushed to DataCite, where it can be searched. For more information, see :ref:`:DoiProvider` in the Installation Guide.

OAI-PMH (Harvesting)
--------------------

The Dataverse software supports a protocol called OAI-PMH that facilitates harvesting dataset metadata from one system into another. For details on harvesting, see the :doc:`harvestserver` section.

Machine-Readable Metadata on Dataset Landing Pages
--------------------------------------------------

As recommended in `A Data Citation Roadmap for Scholarly Data Repositories <https://doi.org/10.1101/097196>`_, the Dataverse software embeds metadata on dataset landing pages in a variety of machine-readable ways. 

Dublin Core HTML Meta Tags
++++++++++++++++++++++++++

The HTML source of a dataset landing page includes "DC" (Dublin Core) ``<meta>`` tags such as the following::

        <meta name="DC.identifier" content="..."
        <meta name="DC.type" content="Dataset"
        <meta name="DC.title" content="..."

.. _croissant-head:

Croissant Metadata in the ``<head>`` of Dataset Landing Pages
+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

`Croissant <https://github.com/mlcommons/croissant>`_ is a metadata format for machine learning datasets.

In Dataverse, the ``<head>`` of the HTML source of a dataset landing page includes a "slim" version of Croissant metadata like this::

    <script type="application/ld+json">{"@context":..."cr":"http://mlcommons.org/croissant/"...

This slim version does not have any information about files or variables but is otherwise is identical to the Croissant file you can download from a dataset landing page by clicking "Metadata" then "Export Metadata" (see :ref:`metadata-export-formats`). From the API you can download both ``croissant`` and ``croissantSlim`` formats (see :ref:`export-dataset-metadata-api`).

We include Croissant in the ``<head>`` because it's `recommended <https://github.com/mlcommons/croissant/issues/530#issuecomment-1964227662>`_ by Google for `Google Dataset Search <https://datasetsearch.research.google.com>`_, where they offer a filter to narrow results to only datasets with support for Croissant.

Before Croissant was invented, Google recommended a different format that Dataverse refers to as "Schema.org JSON-LD" in the user interface (and ``schema.org`` in the API). If you prefer to put that older format in the ``<head>``, which was the behavior in older versions of Dataverse, see :ref:`dataverse.legacy.schemaorg-in-html-head`.

See also the :ref:`dataverse.feature.croissant-with-local-reviews` feature flag.

.. _discovery-sign-posting:

Signposting
+++++++++++

The Dataverse software supports `Signposting <https://signposting.org>`_. This allows machines to request more information about a dataset through the `Link <https://tools.ietf.org/html/rfc5988>`_ HTTP header. Links to all enabled metadata export formats are given. See :ref:`metadata-export-formats` for a list.

There are 2 Signposting profile levels, level 1 and level 2. In this implementation, 
 * Level 1 links are shown `as recommended <https://signposting.org/FAIR/>`_ in the "Link"
   HTTP header, which can be fetched by sending an HTTP HEAD request, e.g. ``curl -I https://demo.dataverse.org/dataset.xhtml?persistentId=doi:10.5072/FK2/KPY4ZC``.
   The number of author and file links in the level 1 header can be configured as described below. 
 * The level 2 linkset can be fetched by visiting the dedicated linkset page for 
   that artifact. The link can be seen in level 1 links with key name ``rel="linkset"``.

Note: Authors without author link will not be counted nor shown in any profile/linkset. 
The following configuration options are available:

- :ref:`dataverse.signposting.level1-author-limit`

  Sets the max number of authors to be shown in `level 1` profile.
  If the number of authors (with identifier URLs) exceeds this value, no author links will be shown in `level 1` profile.
  The default is 5.

- :ref:`dataverse.signposting.level1-item-limit`

  Sets the max number of items/files which will be shown in `level 1` profile. Datasets with
  too many files will not show any file links in `level 1` profile. They will be shown in `level 2` linkset only. 
  The default is 5.

See also :ref:`signposting-api` in the API Guide.

Additional Discoverability Through Integrations
-----------------------------------------------

See :ref:`integrations-discovery` in the Integrations section for additional discovery methods you can enable.
