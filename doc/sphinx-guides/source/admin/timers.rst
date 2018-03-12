.. role:: fixedwidthplain

Dataverse Application Timers
============================

Dataverse uses timers to automatically run scheduled Harvest and Metadata export jobs. 

.. contents:: |toctitle|
	:local:

Dedicated timer server in a Dataverse server cluster
----------------------------------------------------

When running a Dataverse cluster - i.e. multiple Dataverse application
servers talking to the same database - **only one** of them must act
as the *dedicated timer server*. This is to avoid starting conflicting
batch jobs on multiple nodes at the same time.

This does not affect a single-server installation. So you can safely skip this section unless you are running a multi-server cluster. 

The following JVM option instructs the application to act as the dedicated timer server: 

``-Ddataverse.timerServer=true``

**IMPORTANT:** Note that this option is automatically set by the Dataverse installer script. That means that when **configuring a multi-server cluster**, it will be the responsibility of the installer to remove the option from the :fixedwidthplain:`domain.xml` of every node except the one intended to be the timer server. We also recommend that the following entry in the :fixedwidthplain:`domain.xml`: ``<ejb-timer-service timer-datasource="jdbc/VDCNetDS">`` is changed back to ``<ejb-timer-service>`` on all the non-timer server nodes. Similarly, this option is automatically set by the installer script. Changing it back to the default setting on a server that doesn't need to run the timer will prevent a potential race condition, where multiple servers try to get a lock on the timer database. 

**Note** that for the timer to work, the version of the PostgreSQL JDBC driver your instance is using must match the version of your PostgreSQL database. See the 'Timer not working' section of the :doc:`/admin/troubleshooting` guide.

Harvesting Timers 
-----------------

These timers are created when scheduled harvesting is enabled by a local admin user (via the "Manage Harvesting Clients" page). 

In a multi-node cluster, all these timers will be created on the dedicated timer node (and not necessarily on the node where the harvesting clients were created and/or saved). 

A timer will be automatically removed when a harvesting client with an active schedule is deleted, or if the schedule is turned off for an existing client. 

Metadata Export Timer
---------------------

This timer is created automatically whenever the application is deployed or restarted. There is no admin user-accessible configuration for this timer. 

This timer runs a daily job that tries to export all the local, published datasets that haven't been exported yet, in all supported metadata formats, and cache the results on the filesystem. (Note that normally an export will happen automatically whenever a dataset is published. This scheduled job is there to catch any datasets for which that export did not succeed, for one reason or another). Also, since this functionality has been added in version 4.5: if you are upgrading from a previous version, none of your datasets are exported yet. So the first time this job runs, it will attempt to export them all. 

This daily job will also update all the harvestable OAI sets configured on your server, adding new and/or newly published datasets or marking deaccessioned datasets as "deleted" in the corresponding sets as needed. 

This job is automatically scheduled to run at 2AM local time every night. If really necessary, it is possible (for an advanced user) to change that time by directly editing the EJB timer application table in the database.  

Known Issues
------------
 
We've received several reports of an intermittent issue where the application fails to deploy with the error message "EJB Timer Service is not available." Please see the :doc:`/admin/troubleshooting` section of this guide for a workaround. 
