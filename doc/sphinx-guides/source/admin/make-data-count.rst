Make Data Count
===============

`Make Data Count`_ is a project to collect and standardize metrics on data use, especially views, downloads, and citations.

.. contents:: Contents:
	:local:

Introduction
------------

`Make Data Count`_ is part of a broader Research Data Alliance (RDA) `Data Usage Metrics Working Group`_ that they helped launch and produced a specification called the `COUNTER Code of Practice for Research Data`_ (`PDF`_, `HTML`_) that Dataverse makes every effort to comply with. The Code of Practice (CoP) is built on top of existing standards such as COUNTER and SUSHI that come out of the article publishing world.  The Make Data Count project has emphasized that they would like feedback on the code of practice. You can keep up to date on the Make Data Count project by subscribing to their `newsletter`_.

Architecture
------------

Dataverse installations who would like support for Make Data Count must install `Counter Processor`_, a Python project created by California Digital Library (CDL) which is part of the Make Data Count project and which runs the software in production as part of their `DASH`_ data sharing platform.

.. _Counter Processor: https://github.com/CDLUC3/counter-processor
.. _DASH: https://cdluc3.github.io/dash/

The diagram belows shows how Counter Processor interacts with Dataverse and the DataCite hub, once configured, but installations of Dataverse using Handles rather than DOIs should note the limitations below.

The most important takeaways from the diagram are:

- Once enabled, Dataverse will log activity (views and downloads) to a specialized file date-stamped file.
- You should run Counter Processor once a day to create reports in SUSHI (JSON) format that are saved to disk for Dataverse to process and that are sent to the DataCite hub.
- You should set up a cron job to have Dataverse process the daily SUSHI reports, updating the Dataverse database with the latest metrics.
- You should set up a cron job to have Dataverse pull the latest list of citations for each dataset on a periodic basis, perhaps weekly or daily. These citations come from Crossref via the DataCite hub.
- APIs are available in Dataverse to retrieve Make Data Count metrics: views, downloads, and citations.

|makedatacount_components|

Limitations for Dataverse Installations Using Handles Rather Than DOIs
----------------------------------------------------------------------

Data repositories using Handles and other identifiers are not supported by Make Data Count but in the notes_ following a July 2018 webinar, you can see the Make Data Count project's response on this topic. In short, the DataCite hub does not want to receive reports for non-DOI datasets. Additionally, citations are only available from the DataCite hub for datasets that have DOIs. The remainder of processing is the same.

Configuring Dataverse for Make Data Count Views and Downloads
-------------------------------------------------------------

To make Dataverse log dataset usage (views and downloads) for Make Data Count, you must set the ``:MDCLogPath`` database setting. See the :doc:`/installation/config` section of the Installation Guide for details.

FIXME: Explain more about how to install and configure Counter Processor (for now, see the :doc:`/developers/make-data-count` section in the Dev Guide), setting up cron jobs, etc.

The following metrics will be sent for each published dataset:

- Views ("investigations" in COUNTER)
- Downloads ("requests" in COUNTER)

Configuring Dataverse for Make Data Count Citations
---------------------------------------------------

Please note: as explained in the note above about limitations, this feature is not available to installations of Dataverse that use Handles.

FIXME: Document curl command and indicate that it should be called from cron periodically.

Citations will be retrieved for each published dataset and recorded in the Dataverse database.
  
Please note that while Dataverse has a metadata field for "Related Dataset" this information is not currently sent as a citation to Crossref.

Retrieving Make Data Count Metrics from the DataCite Hub
--------------------------------------------------------

The following metrics can be downloaded directly from the DataCite hub (see https://support.datacite.org/docs/eventdata-guide) for datasets hosted by Dataverse installations that have been configured to send these metrics to the hub:

- Total Views for a Dataset
- Unique Views for a Dataset
- Total Downloads for a Dataset
- Downloads for a Dataset
- Citations for a Dataset (via Crossref)

Retrieving Make Data Count Metrics from Dataverse
-------------------------------------------------

Dataverse users might find it more convenient to retrieve Make Data Count metrics from their installation of Dataverse rather the DataCite hub.

The Dataverse API endpoints for retrieving Make Data Count metrics are described below. Please note that in the curl examples, Bash environment variables are used with the idea that you can set a few environment variables and copy and paste the examples as is. For example, "$DV_BASE_URL" could become "https://demo.dataverse.org" by issuing the following ``export`` command from Bash:

``export DV_BASE_URL=https://demo.dataverse.org``

To confirm that the environment variable was set properly, you can use ``echo`` like this:

``echo $DV_BASE_URL``

FIXME: Explain that you have to pass in a country code (or rewrite the code so you don't have to?)

Retrieving Total Views for a Dataset
+++++++++++++++++++++++++++++++++++++++++++++++++

``curl "$DV_BASE_URL/api/datasets/:persistentId/makeDataCount/viewsTotal?persistentId=$DOI"``

Retrieving Unique Views for a Dataset
+++++++++++++++++++++++++++++++++++++++++++++++++

``curl "$DV_BASE_URL/api/datasets/:persistentId/makeDataCount/viewsUnique?persistentId=$DOI"``

Retrieving Total Downloads for a Dataset
+++++++++++++++++++++++++++++++++++++++++++++++++

``curl "$DV_BASE_URL/api/datasets/:persistentId/makeDataCount/downloadsTotal?persistentId=$DOI"``

Retrieving Unique Downloads for a Dataset
+++++++++++++++++++++++++++++++++++++++++++++++++

``curl "$DV_BASE_URL/api/datasets/:persistentId/makeDataCount/downloadsTotal?persistentId=$DOI"``

Retrieving Citations for a Dataset
+++++++++++++++++++++++++++++++++++++++++++++++++

``curl "$DV_BASE_URL/api/datasets/:persistentId/makeDataCount/citations?persistentId=$DOI"``

.. _notes: https://docs.google.com/document/d/1b1itytDVDsI_Ni2LoxrG887YGt0zDc96tpyJEgBN9Q8/
.. _newsletter: https://makedatacount.org/contact/
.. _COUNTER Code of Practice for Research Data: https://makedatacount.org/counter-code-of-practice-for-research-data/
.. _PDF: https://doi.org/10.7287/peerj.preprints.26505v1
.. _HTML: https://www.projectcounter.org/code-of-practice-rd-sections/foreword/
.. _Make Data Count: https://makedatacount.org
.. _Data Usage Metrics Working Group: https://www.rd-alliance.org/groups/data-usage-metrics-wg

.. |makedatacount_components| image:: ./img/make-data-count.png
   :class: img-responsive
