.. role:: fixedwidthplain

Troubleshooting
===============

This new (as of v.4.6) section of the Admin guide is for tips on how to diagnose and fix system problems. 

.. contents:: Contents:
	:local:

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

