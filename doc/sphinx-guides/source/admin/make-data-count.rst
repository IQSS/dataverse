Make Data Count
===============

`Make Data Count`_ is a project to collect and standardize metrics on data use, especially views, downloads, and citations. Dataverse can integrate Make Data Count to collect and display usage metrics including counts of dataset views, file downloads, and dataset citations.

.. contents:: Contents:
	:local:

Origin
------

`Make Data Count`_ is part of a broader Research Data Alliance (RDA) `Data Usage Metrics Working Group`_ which helped to produce a specification called the `COUNTER Code of Practice for Research Data`_ (`PDF`_, `HTML`_) that Dataverse makes every effort to comply with. The Code of Practice (CoP) is built on top of existing standards such as COUNTER and SUSHI that come out of the article publishing world.  The Make Data Count project has emphasized that they would like feedback on the code of practice. You can keep up to date on the Make Data Count project by subscribing to their `newsletter`_.

Architecture
------------

Dataverse installations who would like support for Make Data Count must install `Counter Processor`_, a Python project created by California Digital Library (CDL) which is part of the Make Data Count project and which runs the software in production as part of their `DASH`_ data sharing platform.

.. _Counter Processor: https://github.com/CDLUC3/counter-processor
.. _DASH: https://cdluc3.github.io/dash/

The diagram below shows how Counter Processor interacts with Dataverse and the DataCite hub, once configured. Installations of Dataverse using Handles rather than DOIs should note the limitations in the next section of this page.

|makedatacount_components|

The most important takeaways from the diagram are:

- Once enabled, Dataverse will log activity (views and downloads) to a specialized date-stamped file.
- You should run Counter Processor once a day to create reports in SUSHI (JSON) format that are saved to disk for Dataverse to process and that are sent to the DataCite hub.
- You should set up a cron job to have Dataverse process the daily SUSHI reports, updating the Dataverse database with the latest metrics.
- You should set up a cron job to have Dataverse pull the latest list of citations for each dataset on a periodic basis, perhaps weekly or daily. These citations come from Crossref via the DataCite hub.
- APIs are available in Dataverse to retrieve Make Data Count metrics: views, downloads, and citations.


Limitations for Dataverse Installations Using Handles Rather Than DOIs
----------------------------------------------------------------------

Data repositories using Handles and other identifiers are not supported by Make Data Count but in the notes_ following a July 2018 webinar, you can see the Make Data Count project's response on this topic. In short, the DataCite hub does not want to receive reports for non-DOI datasets. Additionally, citations are only available from the DataCite hub for datasets that have DOIs. The Dataverse usage logging and Counter Processor tool can still be used to track other identifier and store the metrics in Dataverse.

When editing the ``counter-processor-config.yaml`` file mentioned below, make sure that the ``upload_to_hub`` boolean is set to ``False``.

Configuring Dataverse for Make Data Count Views and Downloads
-------------------------------------------------------------

To make Dataverse log dataset usage (views and downloads) for Make Data Count, you must set the ``:MDCLogPath`` database setting. See :ref:`MDCLogPath` for details.

If you haven't already, follow the steps for installing Counter Processor in the :doc:`/installation/prerequisites` section of the Installation Guide.

After you have your first day of logs, you can process them the next day.

* First, become the "counter" Unix user.

  * ``sudo su - counter``

* Download :download:`counter-processor-config.yaml <../_static/admin/counter-processor-config.yaml>`

* Edit the config file and pay particular attention to the FIXME lines.

  * ``vim counter-processor-config.yaml``

* Change to the directory where you installed Counter Processor.

  * ``cd /usr/local/counter-processor-0.0.1``

* If you are starting this installation in the middle of a month, you will need create blank log files for the previous days. e.g.:

  * ``touch sample_logs/counter_2019-02-01.log``
  
  * ``...``
  
  * ``touch sample_logs/counter_2019-02-20.log``
 
* Run Counter Processor.

  * ``CONFIG_FILE=/home/counter/counter-processor-config.yaml python36 main.py``

  * You will need to set up a cron job to run this script periodically, perhaps nightly.

* A JSON file in SUSHI format will be created in the directory you specified under "output_file" in the config file.

* You will want to add a cron job to load the SUSHI file periodically as well, perhaps nightly.

..  * FIXME: Explain how to load the SUSHI file into Dataverse. For now, see the :doc:`/developers/make-data-count` section of the Dev Guide.

Once you have contacted support@datacite.org for your JSON Web Token and changed "upload_to_hub" to "True" in the config file, the following metrics will be sent to the DataCite hub for each published dataset:

- Views ("investigations" in COUNTER)
- Downloads ("requests" in COUNTER)

Configuring Dataverse for Make Data Count Citations
---------------------------------------------------

Please note: as explained in the note above about limitations, this feature is not available to installations of Dataverse that use Handles.

.. FIXME: Document curl command and indicate that it should be called from cron periodically.

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

The Dataverse API endpoints for retrieving Make Data Count metrics are described below under :ref:`Dataset Metrics <dataset-metrics-api>` in the :doc:`/api/native-api` section of the API Guide.

Please note that it is also possible to retrieve metrics from the DataCite hub itself via https://api.datacite.org

.. _notes: https://docs.google.com/document/d/1b1itytDVDsI_Ni2LoxrG887YGt0zDc96tpyJEgBN9Q8/
.. _newsletter: https://makedatacount.org/contact/
.. _COUNTER Code of Practice for Research Data: https://makedatacount.org/counter-code-of-practice-for-research-data/
.. _PDF: https://doi.org/10.7287/peerj.preprints.26505v1
.. _HTML: https://www.projectcounter.org/code-of-practice-rd-sections/foreword/
.. _Make Data Count: https://makedatacount.org
.. _Data Usage Metrics Working Group: https://www.rd-alliance.org/groups/data-usage-metrics-wg

.. |makedatacount_components| image:: ./img/make-data-count.png
   :class: img-responsive
