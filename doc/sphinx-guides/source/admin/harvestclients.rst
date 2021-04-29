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

What if a Run Fails?
~~~~~~~~~~~~~~~~~~~~

Each harvesting client run logs a separate file per run to the app server's default logging directory (``/usr/local/payara5/glassfish/domains/domain1/logs/`` unless you've changed it). Look for filenames in the format  ``harvest_TARGET_YYYY_MM_DD_timestamp.log`` to get a better idea of what's going wrong.

Note that you'll want to run a minimum of Dataverse Software 4.6, optimally 4.18 or beyond, for the best OAI-PMH interoperability.
