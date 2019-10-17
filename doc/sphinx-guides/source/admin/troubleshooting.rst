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

It's normal for the ingest process described in the :doc:`/user/tabulardataingest/ingestprocess` section of the User Guide to take some time but if hours or days have passed and the dataset is still locked, you might want to inspect the locks and consider deleting some or all of them.

See :doc:`dataverses-datasets`.

Someone Created Spam Datasets and I Need to Delete Them
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Depending on how open your installation of Dataverse is to the general public creating datasets, you may sometimes need to deal with spam datasets.

Look for "destroy" in the :doc:`/api/native-api` section of the API Guide.

A User Needs Their Account to Be Converted From Institutional (Shibboleth), ORCID, Google, or GitHub to Something Else
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

See :ref:`converting-shibboleth-users-to-local` and :ref:`converting-oauth-users-to-local`.

Glassfish
---------

``server.log`` is the main place to look when you encounter problems. Hopefully an error message has been logged. If there's a stack trace, it may be of interest to developers, especially they can trace line numbers back to a tagged version.

For debugging purposes, you may find it helpful to increase logging levels as mentioned in the :doc:`/developers/debugging` section of the Developer Guide.

Our guides focus on using the command line to manage Glassfish but you might be interested in an admin GUI at http://localhost:4848


Deployment fails, "EJB Timer Service not available"
---------------------------------------------------

Sometimes the Dataverse application fails to deploy, or Glassfish fails to restart once the application is deployed, with the following error message: :fixedwidthplain:`"remote failure: Error occurred during deployment: Exception while loading the app : EJB Timer Service is not available. Please see server.log for more details."`

We don't know what's causing this issue, but here's a known workaround: 

- Stop Glassfish; 

- Remove the ``generated`` and ``osgi-cache`` directories; 

- Delete all the rows from the ``EJB__TIMER__TBL`` table in the database;

- Start Glassfish

The shell script below performs the steps above. 
Note that it may or may not work on your system, so it is provided as an example only, downloadable :download:`here </_static/util/clear_timer.sh>`. Aside from the configuration values that need to be changed to reflect your environment (the Glassfish directory, name of the database, etc.) the script relies on the database being configured in a certain way for access. (See the comments in the script for more information)

.. literalinclude:: ../_static/util/clear_timer.sh

Timer not working 
-----------------

Dataverse relies on EJB timers to perform scheduled tasks: harvesting from remote servers, updating the local OAI sets and running metadata exports. (See :doc:`timers` for details.) If these scheduled jobs are not running on your server, this may be the result of the incompatibility between the version of PostgreSQL database you are using, and PostgreSQL JDBC driver in use by your instance of Glassfish. The symptoms:

If you are seeing the following in your server.log...

:fixedwidthplain:`Handling timeout on` ...

followed by an Exception stack trace with these lines in it: 

:fixedwidthplain:`Internal Exception: java.io.StreamCorruptedException: invalid stream header` ...

:fixedwidthplain:`Exception Description: Could not deserialize object from byte array` ...


... it most likely means that it is the JDBC driver incompatibility that's preventing the timer from working correctly. 
Make sure you install the correct version of the driver. For example, if you are running the version 9.3 of PostgreSQL, make sure you have the driver postgresql-9.3-1104.jdbc4.jar in your :fixedwidthplain:`<GLASSFISH FOLDER>/glassfish/lib` directory. Go `here <https://jdbc.postgresql.org/download.html>`_
to download the correct version of the driver. If you have an older driver in glassfish/lib, make sure to remove it, replace it with the new version and restart Glassfish. (You may need to remove the entire contents of :fixedwidthplain:`<GLASSFISH FOLDER>/glassfish/domains/domain1/generated` before you start Glassfish). 


Constraint Violations Issues
----------------------------

In real life production use, it may be possible to end up in a situation where some values associated with the datasets in your database are no longer valid under the constraints enforced by the latest version of Dataverse. This is not very likely to happen, but if it does, the symptomps will be as follows: Some datasets can no longer be edited, long exception stack traces logged in the Glassfish server log, caused by::

   javax.validation.ConstraintViolationException: 
   Bean Validation constraint(s) violated while executing Automatic Bean Validation on callback event:'preUpdate'. 
   Please refer to embedded ConstraintViolations for details.

(contrary to what the message suggests, there are no specific "details" anywhere in the stack trace that would explain what values violate which constraints)  

To identifiy the specific invalid values in the affected datasets, or to check all the datasets in the Dataverse for constraint violations, see :ref:`Dataset Validation <dataset-validation-api>` in the :doc:`/api/native-api` section of the User Guide.

Many Files with a File Type of "Unknown", "Application", or "Binary"
--------------------------------------------------------------------

From the home page of a Dataverse installation you can get a count of files by file type by clicking "Files" and then scrolling down to "File Type". If you see a lot of files that are "Unknown", "Application", or "Binary" you can have Dataverse attempt to redetect the file type by using the :ref:`Redetect File Type <redetect-file-type>` API endpoint.
