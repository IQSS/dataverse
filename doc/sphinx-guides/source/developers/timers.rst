==========
EJB Timers
==========

As described in :doc:`../admin/timers`, Dataverse uses EJB timers for scheduled jobs. This section is about the
techniques used for scheduling.

* :doc:`../admin/metadataexport` is done via ``@Schedule`` annotation on ``OAISetServiceBean.exportAllSets()`` and
  ``DatasetServiceBean.exportAll()``. Fixed to 2AM local time every day, non persistent.
* Harvesting is a bit more complicated. The timer is attached to ``HarvesterServiceBean.harvestEnabled()`` via
  ``@Schedule`` annotation every hour at minute 50, non-persistent.
  That method collects all enabled ``HarvestingClient`` and runs them.

.. contents:: |toctitle|
	:local:

----

Previous: :doc:`big-data-support`