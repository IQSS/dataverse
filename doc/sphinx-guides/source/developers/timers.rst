==========
EJB Timers
==========

As described in :doc:`../admin/timers`, Dataverse uses EJB timers for scheduled jobs. This section is about the
techniques used for scheduling.

* :doc:`../admin/metadataexport` is done via ``@Schedule`` annotation on ``OAISetServiceBean.exportAllSets()`` and
  ``DatasetServiceBean.exportAll()``. Fixed to 2AM local time every day, non persistent.
* Harvesting is a bit more complicated. The timer is attached to ``HarvesterServiceBean.harvestEnabled()`` via
  ``@Schedule`` annotation every hour, non-persistent.
  That method collects all enabled ``HarvestingClient`` and runs them if time from client config matches.

**NOTE:** the timers for Harvesting might cause trouble, when harvesting takes longer than one hour or multiple
harvests configured for the same starting hour stack up. There is a lock in place to prevent "bad things", but that
might result in lost harvest. If this really causes trouble in the future, the code should be refactored to use either
a proper task scheduler, JBatch API or asynchronous execution. A *TODO* message has been left in the code.

.. contents:: |toctitle|
	:local:

----

Previous: :doc:`big-data-support`