.. role:: fixedwidthplain

Troubleshooting
===============

Sometimes Dataverse users get into trouble. Sometimes Dataverse itself gets into trouble. If something has gone wrong, this section is for you.

.. contents:: Contents:
	:local:

Using Dataverse APIs to Troubleshoot and Fix Problems
-----------------------------------------------------

See the :doc:`/api/intro` section of the API Guide for a high level overview of Dataverse APIs. Below are listed problems that support teams might encounter that can be handled via API (sometimes only via API).

A Dataset Is Locked And Cannot Be Edited or Published
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

There are several types of dataset locks. Locks can be managed using the locks API, or by accessing them directly in the database. Internally locks are maintained in the ``DatasetLock`` database table, with the ``field dataset_id`` linking them to specific datasets, and the column ``reason`` specifying the type of lock.

It's normal for the ingest process described in the :doc:`/user/tabulardataingest/ingestprocess` section of the User Guide to take some time but if hours or days have passed and the dataset is still locked, you might want to inspect the locks and consider deleting some or all of them. It is recommended to restart the application server if you are deleting an ingest lock, to make sure the ingest job is no longer running in the background. Ingest locks are idetified by the label ``Ingest`` in the ``reason`` column of the ``DatasetLock`` table in the database.

A dataset is locked with a lock of type ``finalizePublication`` while the persistent identifiers for the datafiles in the dataset are registered or updated, and/or while the physical files are being validated by recalculating the checksums and verifying them against the values stored in the database, before the publication process can be completed (Note that either of the two tasks can be disabled via database options - see :doc:`config`). If a dataset has been in this state for a long period of time, for hours or longer, it is somewhat safe to assume that it is stuck (for example, the process may have been interrupted by an application server restart, or a system crash), so you may want to remove the lock (to be safe, do restart the application server, to ensure that the job is no longer running in the background) and advise the user to try publishing again. See :doc:`dataverses-datasets` for more information on publishing.

If any files in the dataset fail the validation above the dataset will be left locked with a ``DatasetLock.Reason=FileValidationFailed``. The user will be notified that they need to contact their Dataverse support in order to address the issue before another attempt to publish can be made. The admin will have to address and fix the underlying problems (by either restoring the missing or corrupted files, or by purging the affected files from the dataset) before deleting the lock and advising the user to try to publish again. The goal of the validation framework is to catch these types of conditions while the dataset is still in DRAFT. 

During an attempt to publish a dataset, the validation will stop after encountering the first file that fails it. It is strongly recommended for the admin to review and verify *all* the files in the dataset, so that all the compromised files can be fixed before the lock is removed. We recommend using the ``/api/validate/dataset/files/{id}`` API. It will go through all the files for the dataset specified, and will report which ones have failed validation. see :ref:`Physical Files Validation in a Dataset <dataset-files-validation-api>` in the :doc:`/api/native-api` section of the User Guide.

The following are two real life examples of problems that have resulted in corrupted datafiles during normal operation of Dataverse: 

1. Botched file deletes - while a datafile is in DRAFT, attempting to delete it from the dataset involves deleting both the ``DataFile`` database table entry, and the physical file. (Deleting a datafile from a *published* version merely removes it from the future versions - but keeps the file in the dataset). The problem we've observed in the early versions of Dataverse was a *partially successful* delete, where the database tansaction would fail (for whatever reason), but only after the physical file had already been deleted from the filesystem. Thus resulting in a datafile entry remaining in the dataset, but with the corresponding physical file missing. We believe we have addressed the issue that was making this condition possible, so it shouldn't happen again - but there may be a datafile in this state in your database. Assuming the user's intent was in fact to delete the file, the easiest solution is simply to confirm it and purge the datafile entity from the database. Otherwise the file needs to be restored from backups, or obtained from the user and copied back into storage. 
2. Another issue we've observed: a failed tabular data ingest that leaves the datafile un-ingested, BUT with the physical file already replaced by the generated tab-delimited version of the data. This datafile will fail the validation because the checksum in the database matches the file in the original format (Stata, SPSS, etc.) as uploaded by the user. To fix: luckily, this is easily reversable, since the uploaded original should be saved in your storage, with the .orig extension. Simply swapping the .orig copy with the main file associated with the datafile will fix it. Similarly, we believe this condition should not happen again in Dataverse versions 4.20+, but you may have some legacy cases on your server. 

Someone Created Spam Datasets and I Need to Delete Them
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Depending on how open your installation of Dataverse is to the general public creating datasets, you may sometimes need to deal with spam datasets.

Look for "destroy" in the :doc:`/api/native-api` section of the API Guide.

A User Needs Their Account to Be Converted From Institutional (Shibboleth), ORCID, Google, or GitHub to Something Else
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

See :ref:`converting-shibboleth-users-to-local` and :ref:`converting-oauth-users-to-local`.

.. _troubleshooting-payara:

Payara
------

.. _payara-log:

Finding the Payara Log File
~~~~~~~~~~~~~~~~~~~~~~~~~~~

``/usr/local/payara5/glassfish/domains/domain1/logs/server.log`` is the main place to look when you encounter problems (assuming you installed Payara in the default directory). Hopefully an error message has been logged. If there's a stack trace, it may be of interest to developers, especially they can trace line numbers back to a tagged version or commit. Send more of the stack trace (the entire file if possible) to developers who can help (see "Getting Help", below) and be sure to say which version of Dataverse you are running.

.. _increase-payara-logging:

Increasing Payara Logging
~~~~~~~~~~~~~~~~~~~~~~~~~

For debugging purposes, you may find it helpful to temporarily increase logging levels. Here's an example of increasing logging for the Java class behind the "datasets" API endpoints:

``./asadmin set-log-levels edu.harvard.iq.dataverse.api.Datasets=FINE``

For more on setting log levels, see the :doc:`/developers/debugging` section of the Developer Guide.

Our guides focus on using the command line to manage Payara but you might be interested in an admin GUI at http://localhost:4848


Deployment fails, "EJB Timer Service not available"
---------------------------------------------------

Sometimes the Dataverse application fails to deploy, or Payara fails to restart once the application is deployed, with the following error message: :fixedwidthplain:`"remote failure: Error occurred during deployment: Exception while loading the app : EJB Timer Service is not available. Please see server.log for more details."`

We don't know what's causing this issue, but here's a known workaround: 

- Stop Payara; 

- Remove the ``generated`` and ``osgi-cache`` directories; 

- Delete all the rows from the ``EJB__TIMER__TBL`` table in the database;

- Start Payara

The shell script below performs the steps above. 
Note that it may or may not work on your system, so it is provided as an example only, downloadable :download:`here </_static/util/clear_timer.sh>`. Aside from the configuration values that need to be changed to reflect your environment (the Payara directory, name of the database, etc.) the script relies on the database being configured in a certain way for access. (See the comments in the script for more information)

.. literalinclude:: ../_static/util/clear_timer.sh

.. _timer-not-working:

Timer Not Working
-----------------

Dataverse relies on EJB timers to perform scheduled tasks: harvesting from remote servers, updating the local OAI sets and running metadata exports. (See :doc:`timers` for details.) If these scheduled jobs are not running on your server, this may be the result of the incompatibility between the version of PostgreSQL database you are using, and PostgreSQL JDBC driver in use by your instance of Payara. The symptoms:

If you are seeing the following in your server.log...

:fixedwidthplain:`Handling timeout on` ...

followed by an Exception stack trace with these lines in it: 

:fixedwidthplain:`Internal Exception: java.io.StreamCorruptedException: invalid stream header` ...

:fixedwidthplain:`Exception Description: Could not deserialize object from byte array` ...


... it most likely means that it is the JDBC driver incompatibility that's preventing the timer from working correctly. 
Make sure you install the correct version of the driver. For example, if you are running the version 9.3 of PostgreSQL, make sure you have the driver postgresql-9.3-1104.jdbc4.jar in your :fixedwidthplain:`<PAYARA FOLDER>/glassfish/lib` directory. Go `here <https://jdbc.postgresql.org/download.html>`_
to download the correct version of the driver. If you have an older driver in glassfish/lib, make sure to remove it, replace it with the new version and restart Payara. (You may need to remove the entire contents of :fixedwidthplain:`<PAYARA FOLDER>/glassfish/domains/domain1/generated` before you start Payara). 


Constraint Violations Issues
----------------------------

In real life production use, it may be possible to end up in a situation where some values associated with the datasets in your database are no longer valid under the constraints enforced by the latest version of Dataverse. This is not very likely to happen, but if it does, the symptoms will be as follows: Some datasets can no longer be edited, long exception stack traces logged in the app server log, caused by::

   javax.validation.ConstraintViolationException: 
   Bean Validation constraint(s) violated while executing Automatic Bean Validation on callback event:'preUpdate'. 
   Please refer to embedded ConstraintViolations for details.

(contrary to what the message suggests, there are no specific "details" anywhere in the stack trace that would explain what values violate which constraints)  

To identifiy the specific invalid values in the affected datasets, or to check all the datasets in the Dataverse for constraint violations, see :ref:`Dataset Validation <dataset-validation-api>` in the :doc:`/api/native-api` section of the User Guide.

Many Files with a File Type of "Unknown", "Application", or "Binary"
--------------------------------------------------------------------

From the home page of a Dataverse installation you can get a count of files by file type by clicking "Files" and then scrolling down to "File Type". If you see a lot of files that are "Unknown", "Application", or "Binary" you can have Dataverse attempt to redetect the file type by using the :ref:`Redetect File Type <redetect-file-type>` API endpoint.

Getting Help
------------

If the troubleshooting advice above didn't help, contact any of the support channels mentioned in the :ref:`support` section of the Installation Guide.
