Dataset Types
+++++++++++++

NOTE: This separate page will be folded into individual pages and removed as the pull request is finalized

.. contents:: |toctitle|
	:local:

Intro
=====

Datasets can have a dataset type such as "dataset", "software", or "workflow".

When browsing or searching, these types appear under a facet called "Dataset Type".

Enabling Dataset Types
======================

Turn on ``dataverse.feature.dataset-types``. See also :ref:`feature-flags`.

Specifying a Dataset Type When Creating a Dataset
=================================================

Native API
----------

An example JSON file is available at :download:`dataset-create-software.json <../_static/api/dataset-create-software.json>`

Semantic API
---------------------------------

An example JSON-LD file is available at :download:`dataset-create-software.jsonld <../_static/api/dataset-create-software.jsonld>`

Import with Native JSON
-----------------------

The same native JSON file as above can be used when importing a dataset: :download:`dataset-create-software.json <../_static/api/dataset-create-software.json>`

Import with DDI
---------------

An example DDI file is available at :download:`dataset-create-software-ddi.xml <../_static/api/dataset-create-software-ddi.xml>`

Note that for DDI import to work ``dataKind`` must be set to one of the valid types. The first valid type wins.
