Managing Harvesting Clients
===========================

.. contents:: |toctitle|
	:local:
	
Your Dataverse as a Metadata Harvester
--------------------------------------

Harvesting is a process of exchanging metadata with other repositories. As a harvesting *client*, your Dataverse can
gather metadata records from remote sources. These can be other Dataverse instances or other archives that support OAI-PMH, the standard harvesting protocol. Harvested metadata records will be indexed and made searchable by your users. Clicking on a harvested dataset in the search results takes the user to the original repository. Harvested datasets cannot be edited in your Dataverse installation.

Harvested records can be kept in sync with the original repository through scheduled incremental updates, daily or weekly. 
Alternatively, harvests can be run on demand, by the Admin. 

Managing Harvesting Clients
---------------------------

To start harvesting metadata from a remote OAI repository, you first create and configure a *Harvesting Client*. 

Clients are managed on the "Harvesting Clients" page accessible via the :doc:`dashboard`. Click on the *Add Client* button to get started.

The process of creating a new, or editing an existing client, is largely self-explanatory. It is split into logical steps, in a way that allows the user to go back and correct the entries made earlier. The process is interactive and guidance text is provided. For example, the user is required to enter the URL of the remote OAI server. When they click *Next*, the application will try to establish a connection to the server in order to verify that it is working, and to obtain the information about the sets of metadata records and the metadata formats it supports. The choices offered to the user on the next page will be based on this extra information. If the application fails to establish a connection to the remote archive at the address specified, or if an invalid response is received, the user is given an opportunity to check and correct the URL they entered. 

New in Dataverse 4, vs. DVN 3
-----------------------------


- Note that when creating a client you will need to select an existing local dataverse to host the datasets harvested. In DVN 3, a dedicated "harvesting dataverse" would be created specifically for each remote harvesting source. In Dataverse 4, harvested content can be added to *any dataverse*. This means that a dataverse can now contain datasets harvested from multiple sources and/or a mix of local and harvested datasets.


- An extra "Archive Type" pull down menu is added to the Create and Edit dialogs. This setting, selected from the choices such as "Dataverse 4", "DVN, v2-3", "Generic OAI", etc. is used to properly format the harvested metadata as they are shown in the search results. It is **very important** to select the type that best describes this remote server, as failure to do so can result in information missing from the search results, and, a **failure to redirect the user to the archival source** of the data!

  It is, however, **very easy to correct** a mistake like this. For example, let's say you have created a client to harvest from the XYZ Institute and specified the archive type as "Dataverse 4". You have been able to harvest content, the datasets appear in search result, but clicking on them results in a "Page Not Found" error on the remote site. At which point you realize that the XYZ Institute admins have not yet upgraded to Dataverse 4, still running DVN v3.1.2 instead. All you need to do is go back to the Harvesting Clients page, and change the setting to "DVN, v2-3". This will fix the redirects **without having to re-harvest** the datasets. 

- Another extra entry, "Archive Description", is added to the *Edit Harvesting Client* dialog. This description appears at the bottom of each search result card for a harvested dataset or datafile. By default, this text reads "This Dataset is harvested from our partners. Clicking the link will take you directly to the archival source of the data." Here it can be customized to be more descriptive, for example, "This Dataset is harvested from our partners at the XYZ Institute..."


