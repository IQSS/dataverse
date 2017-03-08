.. role:: fixedwidthplain

Troubleshooting
===============

.. contents:: :local:

This new (as of v.4.6) section of the Admin guide is for tips on how to diagnose and fix system problems. 

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

