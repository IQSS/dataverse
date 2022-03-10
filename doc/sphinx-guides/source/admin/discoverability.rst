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

The Dataverse Software supports a protocol called OAI-PMH that facilitates harvesting datasets from one system into another. For details on harvesting, see the :doc:`harvestserver` section.

Machine-Readable Metadata on Dataset Landing Pages
--------------------------------------------------

As recommended in `A Data Citation Roadmap for Scholarly Data Repositories <https://doi.org/10.1101/097196>`_, the Dataverse Software embeds metadata on dataset landing pages in a variety of machine-readable ways. 

Dublin Core HTML Meta Tags
++++++++++++++++++++++++++

The HTML source of a dataset landing page includes "DC" (Dublin Core) ``<meta>`` tags such as the following::

        <meta name="DC.identifier" content="..."
        <meta name="DC.type" content="Dataset"
        <meta name="DC.title" content="..."

https://github.com/IQSS/dataverse/pull/3828

Schema.org JSON-LD Metadata
+++++++++++++++++++++++++++

The HTML source of a dataset landing page includes Schema.org JSON-LD metadata like this::


        <script type="application/ld+json">{"@context":"http://schema.org","@type":"Dataset","@id":"https://doi.org/...


Signposting
-----------

The Dataverse Software supports technology called `Signposting <https://signposting.org>`_ that allows machines to request more information about a dataset through the `Link <https://tools.ietf.org/html/rfc5988>`_ HTTP header.

The following configuration options are available:

- :ref:`:SignpostingMaxItems`

Additional Discoverability Through Integrations
-----------------------------------------------

See :ref:`integrations-discovery` in the Integrations section for additional discovery methods you can enable.
