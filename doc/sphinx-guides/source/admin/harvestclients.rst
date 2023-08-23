Managing Harvesting Clients
===========================

.. contents:: |toctitle|
	:local:

Your Dataverse Installation as a Metadata Harvester
---------------------------------------------------

Harvesting is a process of exchanging metadata with other repositories. As a harvesting *client*, your Dataverse installation can gather metadata records from remote sources. These can be other Dataverse installations or other archives that support OAI-PMH, the standard harvesting protocol. Harvested metadata records will be indexed and made searchable by your users. Clicking on a harvested dataset in the search results takes the user to the original repository. Harvested datasets cannot be edited in your Dataverse installation.

Harvested records can be kept in sync with the original repository through scheduled incremental updates, daily or weekly.
Alternatively, harvests can be run on demand, by the Admin.

Managing Harvesting Clients
---------------------------

To start harvesting metadata from a remote OAI repository, you first create and configure a *Harvesting Client*.

Clients are managed on the "Harvesting Clients" page accessible via the :doc:`dashboard`. Click on the *Add Client* button to get started.

The process of creating a new, or editing an existing client, is largely self-explanatory. It is split into logical steps, in a way that allows the user to go back and correct the entries made earlier. The process is interactive and guidance text is provided. For example, the user is required to enter the URL of the remote OAI server. When they click *Next*, the application will try to establish a connection to the server in order to verify that it is working, and to obtain the information about the sets of metadata records and the metadata formats it supports. The choices offered to the user on the next page will be based on this extra information. If the application fails to establish a connection to the remote archive at the address specified, or if an invalid response is received, the user is given an opportunity to check and correct the URL they entered.

Please note that in some rare cases this GUI may fail to create a client because of some unexpected errors during these real time exchanges with an OAI server that is otherwise known to be valid. For example, in the past we have had issues with servers offering very long lists of sets (*really* long, in the thousands). To allow an admin to still be able to create a client in a situation like that, we provide the REST API that will do so without attempting any validation in real time. This obviously makes it the responsibility of the admin to supply the values that are definitely known to be valid - a working OAI url, the name of a set that does exist on the server, and/or a supported metadata format. See the :ref:`managing-harvesting-clients-api` section of the :doc:`/api/native-api` guide for more information.

Note that as of 5.13, a new entry "Custom HTTP Header" has been added to the Step 1. of Create or Edit form. This optional field can be used to configure this client with a specific HTTP header to be added to every OAI request. This is to accommodate a (rare) use case where the remote server may require a special token of some kind in order to offer some content not available to other clients. Most OAI servers offer the same publicly-available content to all clients, so few admins will have a use for this feature. It is however on the very first, Step 1. screen in case the OAI server requires this token even for the "ListSets" and "ListMetadataFormats" requests, which need to be sent in the Step 2. of creating or editing a client. Multiple headers can be supplied separated by `\\n` - actual "backslash" and "n" characters, not a single "new line" character. 


How to Stop a Harvesting Run in Progress
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Some harvesting jobs, especially the initial full harvest of a very large set - such as the default set of public datasets at IQSS - can take many hours. In case it is necessary to terminate such a long-running job, the following mechanism is provided (note that it is only available to a sysadmin with shell access to the application server): Create an empty file in the domain logs directory with the following name: ``stopharvest_<name>.<pid>``, where ``<name>`` is the nickname of the harvesting client and ``<pid>`` is the process id of the Application Server (Payara). This flag file needs to be owned by the same user that's running Payara, so that the application can remove it after stopping the job in progress.

For example:

.. code-block:: bash

  sudo touch /usr/local/payara6/glassfish/domains/domain1/logs/stopharvest_bigarchive.70916
  sudo chown dataverse /usr/local/payara6/glassfish/domains/domain1/logs/stopharvest_bigarchive.70916

Note: If the application server is stopped and restarted, any running harvesting jobs will be killed but may remain marked as in progress in the database. We thus recommend using the mechanism here to stop ongoing harvests prior to a server restart.

		
What if a Run Fails?
~~~~~~~~~~~~~~~~~~~~

Each harvesting client run logs a separate file per run to the app server's default logging directory (``/usr/local/payara6/glassfish/domains/domain1/logs/`` unless you've changed it). Look for filenames in the format  ``harvest_TARGET_YYYY_MM_DD_timestamp.log`` to get a better idea of what's going wrong.

Note that you'll want to run a minimum of Dataverse Software 4.6, optimally 4.18 or beyond, for the best OAI-PMH interoperability.
