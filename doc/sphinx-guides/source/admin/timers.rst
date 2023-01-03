.. role:: fixedwidthplain

Dataverse Installation Application Timers
=========================================

Your Dataverse installation uses timers to automatically run scheduled Harvest and Metadata export jobs. 

.. contents:: |toctitle|
	:local:

Dedicated timer server in a Dataverse Installation server cluster
-----------------------------------------------------------------

When running a Dataverse installation cluster - i.e. multiple Dataverse installation application
servers talking to the same database - **only one** of them must act
as the *dedicated timer server*. This is to avoid starting conflicting
batch jobs on multiple nodes at the same time.

This does not affect a single-server installation. So you can safely skip this section unless you are running a multi-server cluster. 

The following JVM option instructs the application to act as the dedicated timer server: 

``-Ddataverse.timerServer=true``

**IMPORTANT:** Note that this option is automatically set by the Dataverse Software installer script. That means that when **configuring a multi-server cluster**, it will be the responsibility of the installer to remove the option from the :fixedwidthplain:`domain.xml` of every node except the one intended to be the timer server.

Harvesting Timers 
-----------------

These timers are created when scheduled harvesting is enabled by a local admin user (via the "Manage Harvesting Clients" page). 

In a multi-node cluster, all these timers will be created on the dedicated timer node (and not necessarily on the node where the harvesting clients were created and/or saved). 

A timer will be automatically removed when a harvesting client with an active schedule is deleted, or if the schedule is turned off for an existing client. 

Metadata Export Timer
---------------------

This timer is created automatically whenever the application is deployed or restarted. There is no admin user-accessible configuration for this timer. 

This timer runs a daily job that tries to export all the local, published datasets that haven't been exported yet, in all supported metadata formats, and cache the results on the filesystem. (Note that normally an export will happen automatically whenever a dataset is published. This scheduled job is there to catch any datasets for which that export did not succeed, for one reason or another). Also, since this functionality has been added in Dataverse Software 4.5: if you are upgrading from a previous version, none of your datasets are exported yet. So the first time this job runs, it will attempt to export them all. 

This daily job will also update all the harvestable OAI sets configured on your server, adding new and/or newly published datasets or marking deaccessioned datasets as "deleted" in the corresponding sets as needed. 

This job is automatically scheduled to run at 2AM local time every night.

.. _saved-search-timer:

Saved Searches Links Timer
--------------------------

This timer is created automatically from an @Schedule annotation on the makeLinksForAllSavedSearchesTimer method of the SavedSearchServiceBean when the bean is deployed. 

This timer runs a weekly job to create links for any saved searches that haven't been linked yet.

This job is automatically scheduled to run once a week at 12:30AM local time on Sunday. If really necessary, it is possible to change that time by deploying the application war file with an ejb-jar.xml file in the WEB-INF directory of the war file. A :download:`sample file <../_static/admin/ejb-jar.xml>` would run the job every Tuesday at 2:30PM. The schedule can be modified to your choice by editing the fields in the session section. If other EJBs require some form of configuration using an ejb-jar file, there should be one ejb-jar file for the entire application, which can have different sections for each EJB. Below are instructions for the simple case of adding the ejb-jar.xml for the first time and making a custom schedule for the saved search timer.

* Create or edit dataverse/src/main/webapp/WEB-INF/ejb-jar.xml, following the :download:`sample file <../_static/admin/ejb-jar.xml>` provided.

* Edit the parameters in the <schedule> section of the ejb-jar file in the WEB-INF directory to suit your preferred schedule

  * The provided parameters in the sample file are <minute>, <hour>, and <dayOfWeek>; additional parameters are available

    * For a complete reference for calendar expressions that can be used to schedule Timer services see: https://docs.oracle.com/javaee/7/tutorial/ejb-basicexamples004.htm

* Build and deploy the application

* Alternatively, you can insert an ejb-jar.xml file into a provided Dataverse Software war file without building the application.

  * Check if there is already an ejb-jar.xml file in the war file 

    * jar tvf $DATAVERSE-WAR-FILENAME | grep ejb-jar.xml

      * if the response includes " WEB-INF/ejb-jar.xml", you will need to extract the ejb-jar.xml file for editing

        * jar xvf $DATAVERSE-WAR-FILENAME WEB-INF/ejb-jar.xml 

          * edit the extracted WEB-INF/ejb-jar.xml, following the :download:`sample file <../_static/admin/ejb-jar.xml>` provided.

        * if the response is empty, create a WEB-INF directory and create en ejb-jar.xml file in it, following the :download:`sample file <../_static/admin/ejb-jar.xml>` provided.

          * edit the parameters in the <schedule> section of the WEB-INF/ejb-jar.xml to suit your preferred schedule

  * Insert the edited WEB-INF/ejb-jar.xml into the dataverse war file

    * jar uvf $DATAVERSE-WAR-FILENAME WEB-INF/ejb-jar.xml

  * Deploy the war file


See also :ref:`saved-search` in the API Guide.

Known Issues
------------
 
We've received several reports of an intermittent issue where the application fails to deploy with the error message "EJB Timer Service is not available." Please see the :doc:`/admin/troubleshooting` section of this guide for a workaround. 
