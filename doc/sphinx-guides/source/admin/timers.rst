.. role:: fixedwidthplain

Dataverse Application Timers
============================

Dataverse uses timers to automatically run scheduled jobs for:

* Harvesting metadata
   * See :doc:`/admin/harvestserver` and :doc:`/admin/harvestclients`
   * Created only when scheduling enabled by admin (via "Manage Harvesting Clients" page) and canceled when disabled.
* :doc:`/admin/metadataexport`
   * Enabled by default, non configurable.

All timers are created on application startup and are not configurable when to go off. Since Dataverse 4.10 they are not
persisted to a database, as they had been deleted and re-created on every startup before.

.. contents:: |toctitle|
	:local:

Dataverse server clusters and EJB timers
----------------------------------------

In a multi-node cluster, all timers will be created on a dedicated timer node (see below). This is not necessarily on the
node where configuration of harvesting clients or metadata export has been done by an admin.

Dedicated timer server node
~~~~~~~~~~~~~~~~~~~~~~~~~~~

When running a "cluster" with multiple instances of Dataverse connected to the same database, **only one** of them must
act as the *dedicated timer server*. This is to avoid starting conflicting batch jobs on multiple nodes at the same time.
(Might get addressed for automation in a later Dataverse version using cluster support from the application server.)

This does not affect a single-server installation. So you can safely skip this section unless you are running a multi-server cluster. 

The following system property instructs the application to act as the dedicated timer server:

``dataverse.timerServer=true``

**Note** that when using JVM options to set system properties, please use ``-Ddataverse.timerServer=true``. You should
prefer using ``asadmin`` system properties commands.

**IMPORTANT:** This is automatically set by the Dataverse installer script on every node.

That means that *when configuring a multi-server cluster*, it will be the responsibility of the sysadmin to remove
the option from every node except the one intended to be the timer server. Easiest way to achieve this is by running
``asadmin delete-system-property "dataverse.timerServer"``.
(This option will not be set to ``true`` in future Docker images of Dataverse, it needs to be configured.)

As we don't use persistent timers from Dataverse 4.10 onward, when upgrading, it is up to you to follow the former
recommendation or not. In new installations, this will not be necessary.

  We also recommend that the following entry in the :fixedwidthplain:`domain.xml`:
  ``<ejb-timer-service timer-datasource="jdbc/VDCNetDS">`` is changed back to ``<ejb-timer-service>``
  on all the non-timer server nodes. Similarly, this option is automatically set by the installer script.
  Changing it back to the default setting on a server that doesn't need to run the timer will prevent a potential
  race condition, where multiple servers try to get a lock on the timer database.

Known Issues
------------
 
Former to Dataverse 4.10, we've received several reports of an intermittent issue where the application fails to deploy
with the error message "EJB Timer Service is not available." Please see the :doc:`/admin/troubleshooting` section of
this guide for a workaround.
