.. role:: fixedwidthplain

Troubleshooting
===============

Sometimes a Dataverse installation's users get into trouble. Sometimes a Dataverse installation itself gets into trouble. If something has gone wrong, this section is for you.

.. contents:: Contents:
	:local:

Using Dataverse Installation APIs to Troubleshoot and Fix Problems
------------------------------------------------------------------

See the :doc:`/api/intro` section of the API Guide for a high level overview of Dataverse Software APIs. Below are listed problems that support teams might encounter that can be handled via API (sometimes only via API).

A Dataset Is Locked And Cannot Be Edited or Published
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

There are several types of dataset locks. Locks can be managed using the locks API, or by accessing them directly in the database. Internally locks are maintained in the ``datasetlock`` database table, with the field ``dataset_id`` linking them to specific datasets, and the column ``reason`` specifying the type of lock.

It's normal for the ingest process described in the :doc:`/user/tabulardataingest/ingestprocess` section of the User Guide to take some time but if hours or days have passed and the dataset is still locked, you might want to inspect the locks and consider deleting some or all of them. It is recommended to restart the application server if you are deleting an ingest lock, to make sure the ingest job is no longer running in the background. Ingest locks are idetified by the label ``Ingest`` in the ``reason`` column of the ``DatasetLock`` table in the database.

A dataset is locked with a lock of type ``finalizePublication`` while the persistent identifiers for the datafiles in the dataset are registered or updated, and/or while the physical files are being validated by recalculating the checksums and verifying them against the values stored in the database, before the publication process can be completed (Note that either of the two tasks can be disabled via database options - see :doc:`/installation/config`). If a dataset has been in this state for a long period of time, for hours or longer, it is somewhat safe to assume that it is stuck (for example, the process may have been interrupted by an application server restart, or a system crash), so you may want to remove the lock (to be safe, do restart the application server, to ensure that the job is no longer running in the background) and advise the user to try publishing again. See :doc:`dataverses-datasets` for more information on publishing.

If any files in the dataset fail the validation above the dataset will be left locked with a ``DatasetLock.Reason=FileValidationFailed``. The user will be notified that they need to contact their Dataverse installation's support in order to address the issue before another attempt to publish can be made. The admin will have to address and fix the underlying problems (by either restoring the missing or corrupted files, or by purging the affected files from the dataset) before deleting the lock and advising the user to try to publish again. The goal of the validation framework is to catch these types of conditions while the dataset is still in DRAFT. 

During an attempt to publish a dataset, the validation will stop after encountering the first file that fails it. It is strongly recommended for the admin to review and verify *all* the files in the dataset, so that all the compromised files can be fixed before the lock is removed. We recommend using the ``/api/validate/dataset/files/{id}`` API. It will go through all the files for the dataset specified, and will report which ones have failed validation. see :ref:`Physical Files Validation in a Dataset <dataset-files-validation-api>` in the :doc:`/api/native-api` section of the User Guide.

The following are two real life examples of problems that have resulted in corrupted datafiles during normal operation of a Dataverse installation: 

1. Botched file deletes - while a datafile is in DRAFT, attempting to delete it from the dataset involves deleting both the ``DataFile`` database table entry, and the physical file. (Deleting a datafile from a *published* version merely removes it from the future versions - but keeps the file in the dataset). The problem we've observed in the early versions of the Dataverse Software was a *partially successful* delete, where the database transaction would fail (for whatever reason), but only after the physical file had already been deleted from the filesystem. Thus resulting in a datafile entry remaining in the dataset, but with the corresponding physical file missing. We believe we have addressed the issue that was making this condition possible, so it shouldn't happen again - but there may be a datafile in this state in your database. Assuming the user's intent was in fact to delete the file, the easiest solution is simply to confirm it and purge the datafile entity from the database. Otherwise the file needs to be restored from backups, or obtained from the user and copied back into storage. 
2. Another issue we've observed: a failed tabular data ingest that leaves the datafile un-ingested, BUT with the physical file already replaced by the generated tab-delimited version of the data. This datafile will fail the validation because the checksum in the database matches the file in the original format (Stata, SPSS, etc.) as uploaded by the user. To fix: luckily, this is easily reversible, since the uploaded original should be saved in your storage, with the .orig extension. Simply swapping the .orig copy with the main file associated with the datafile will fix it. Similarly, we believe this condition should not happen again in Dataverse Software 4.20+, but you may have some legacy cases on your server. 

Someone Created Spam Datasets and I Need to Delete Them
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Depending on how open your Dataverse installation is to the general public creating datasets, you may sometimes need to deal with spam datasets.

Look for "destroy" in the :doc:`/api/native-api` section of the API Guide.

A User Needs Their Account to Be Converted From Institutional (Shibboleth), ORCID, Google, or GitHub to Something Else
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

See :ref:`converting-shibboleth-users-to-local` and :ref:`converting-oauth-users-to-local`.

.. _troubleshooting-ingest:

Ingest
------

Long-Running Ingest Jobs Have Exhausted System Resources
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Ingest is both CPU- and memory-intensive, and depending on your system resources and the size and format of tabular data files uploaded, may render your Dataverse installation unresponsive or nearly inoperable. It is possible to cancel these jobs by purging the ingest queue.

``/usr/local/payara5/mq/bin/imqcmd -u admin query dst -t q -n DataverseIngest`` will query the DataverseIngest destination. The password, unless you have changed it, matches the username.

``/usr/local/payara5/mq/bin/imqcmd -u admin purge dst -t q -n DataverseIngest`` will purge the DataverseIngest queue, and prompt for your confirmation.

Finally, list destinations to verify that the purge was successful:

``/usr/local/payara5/mq/bin/imqcmd -u admin list dst``

If you are still running Glassfish, substitute glassfish4 for payara5 above. If you have installed your Dataverse installation in some other location, adjust the above paths accordingly.

.. _troubleshooting-payara:

Payara
------

.. _payara-log:

Finding the Payara Log File
~~~~~~~~~~~~~~~~~~~~~~~~~~~

``/usr/local/payara5/glassfish/domains/domain1/logs/server.log`` is the main place to look when you encounter problems (assuming you installed Payara in the default directory). Hopefully an error message has been logged. If there's a stack trace, it may be of interest to developers, especially they can trace line numbers back to a tagged version or commit. Send more of the stack trace (the entire file if possible) to developers who can help (see "Getting Help", below) and be sure to say which version of the Dataverse Software you have installed.

.. _increase-payara-logging:

Increasing Payara Logging
~~~~~~~~~~~~~~~~~~~~~~~~~

For debugging purposes, you may find it helpful to temporarily increase logging levels. Here's an example of increasing logging for the Java class behind the "datasets" API endpoints:

``./asadmin set-log-levels edu.harvard.iq.dataverse.api.Datasets=FINE``

For more on setting log levels, see the :doc:`/developers/debugging` section of the Developer Guide.

Our guides focus on using the command line to manage Payara but you might be interested in an admin GUI at http://localhost:4848


Deployment fails, "EJB Timer Service not available"
---------------------------------------------------

Sometimes your Dataverse installation fails to deploy, or Payara fails to restart once the application is deployed, with the following error message: :fixedwidthplain:`"remote failure: Error occurred during deployment: Exception while loading the app : EJB Timer Service is not available. Please see server.log for more details."`

We don't know what's causing this issue, but here's a known workaround: 

- Stop Payara;
- Remove the ``generated`` and ``osgi-cache`` directories from the ``domain1`` directory;
- Start Payara

The shell script below performs the steps above. 
Note that it may or may not work on your system, so it is provided as an example only, downloadable :download:`here </_static/util/clear_timer.sh>`. The configuration values might need to be changed to reflect your environment (the Payara directory). See the comments in the script for more information.

.. literalinclude:: ../_static/util/clear_timer.sh

.. _timer-not-working:

Timer Not Working
-----------------

Your Dataverse installation relies on EJB timers to perform scheduled tasks: harvesting from remote servers, updating the local OAI sets and running metadata exports. (See :doc:`timers` for details.) If these scheduled jobs are not running on your server, you might experience the following symptoms:

If you are seeing the following in your server.log...

:fixedwidthplain:`Handling timeout on` ...

followed by an Exception stack trace with these lines in it: 

:fixedwidthplain:`Internal Exception: java.io.StreamCorruptedException: invalid stream header` ...

:fixedwidthplain:`Exception Description: Could not deserialize object from byte array` ...


... you should reach out by opening an issue. In the good ol' days of running Dataverse Software 4.x running on Glassfish 4, this
was a hint for an unsupported JDBC driver. In Dataverse Software 5.x this would be a new regression and its cause would need to be
investigated.


Constraint Violations Issues
----------------------------

In real life production use, it may be possible to end up in a situation where some values associated with the datasets in your database are no longer valid under the constraints enforced by the later versions of the Dataverse Software. This is not very likely to happen, but if it does, the symptoms will be as follows: Some datasets can no longer be edited, long exception stack traces logged in the app server log, caused by::

   javax.validation.ConstraintViolationException: 
   Bean Validation constraint(s) violated while executing Automatic Bean Validation on callback event:'preUpdate'. 
   Please refer to embedded ConstraintViolations for details.

(contrary to what the message suggests, there are no specific "details" anywhere in the stack trace that would explain what values violate which constraints)  

To identify the specific invalid values in the affected datasets, or to check all the datasets in the Dataverse installation for constraint violations, see :ref:`Dataset Validation <dataset-validation-api>` in the :doc:`/api/native-api` section of the User Guide.

Many Files with a File Type of "Unknown", "Application", or "Binary"
--------------------------------------------------------------------

From the home page of a Dataverse installation you can get a count of files by file type by clicking "Files" and then scrolling down to "File Type". If you see a lot of files that are "Unknown", "Application", or "Binary" you can have the Dataverse installation attempt to redetect the file type by using the :ref:`Redetect File Type <redetect-file-type>` API endpoint.

.. _actionlogrecord-trimming:

What's with this Table "ActionLogRecord" in Our Database, It Seems to be Growing Uncontrollably?
------------------------------------------------------------------------------------------------

An entry is created in ActionLogRecord table every time an application command is executed (to be precise, certain non-command actions, such as logins are recorded there as well). This is very useful for investigating problems or usage patterns. However, please note that there is no builtin mechanism in the Application for trimming this table, so it will continue growing as your Dataverse installation is kept in operation. For example, multiple entries in this table are created every time a guest user views the page of a published dataset. Many more are created when an author is actively working on a dataset, making edits, adding new files, etc. On a busy installation this table is likely to grow at a faster rate than the actual data holdings. For example, after five years of production use at Harvard IQSS, the raw size of ActionLogRecord appeared to exceed the combined size of the rest of the database (!). It's worth pointing out that the sheer size of this one table does not by itself result in performance issues in any linear way. But it may still be undesirable to keep that much extra data around; especially since for most installations these records are unlikely to have much value past a certain number of months or years. Some installations may be purchasing their database services from cloud computing providers (RDS, etc.) where extra data may result in higher costs.
Here at Harvard we chose to periodically trim the table manually, deleting all the entries older than 2 years. We recommend that you check on the size of this table in your database, and choose whether, and how often you want to trim it. You will also need to decide whether you want to archive these older records outside the database before deleting them. If you see no reason to keep them around, older records can be erased with a simple query. For example, to delete everything before the year 2021:

``DELETE FROM ACTIONLOGRECORD WHERE starttime < '2021-01-01 00:00:00';``

If you want to preserve these old entries before deleting them, you can save them with, for example, psql:

``psql <CREDENTIALS> -d <DATABASE_NAME> -t -c "SELECT * FROM actionlogrecord WHERE starttime < '2021-01-01 00:00:00' ORDER BY starttime;"``

A full backup of the table can be made with pg_dump, for example:

``pg_dump <CREDENTIALS> --table=actionlogrecord --data-only <DATABASE_NAME> > /tmp/actionlogrecord_backup.sql``

(In the example above, the output will be saved in raw SQL format. It is portable and human-readable, but uses a lot of space. It does, however, compress very well. Add the ``-Fc`` option to save the output in a proprietary, binary format that's already compressed). 


Getting Help
------------

If the troubleshooting advice above didn't help, contact any of the support channels mentioned in the :ref:`support` section of the Installation Guide.
