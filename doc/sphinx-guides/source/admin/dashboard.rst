Dashboard
=========

The Dataverse Software offers a dashboard of administrative tools for superusers only. If you are a logged-in superuser, you can access it by clicking your username in the navbar, and then clicking "Dashboard" from the dropdown. You can verify that you are a superuser by checking the color of your username in the navbar. If it's red, you have the right permissions to use the Dashboard. Superusers can give other users the superuser status via :doc:`user-administration`.

.. contents:: Contents:
	:local:

Harvesting
----------

Harvesting Clients
~~~~~~~~~~~~~~~~~~

This dashboard tool allows you to set up which other repositories your Dataverse installation harvests metadata records from. You can see a list of harvesting clients and add, edit, or remove them. See the :doc:`harvestclients` section for more details.

Harvesting Servers
~~~~~~~~~~~~~~~~~~

This dashboard tool allows you to define sets of local datasets to make available to remote harvesting clients. You can see a list of sets and add, edit, or remove them. See the :doc:`harvestserver` section for more details.

Metadata Export
---------------

This part of the Dashboard is simply a reminder message that metadata export happens through the Dataverse Software API. See the :doc:`metadataexport` section and the :doc:`/api/native-api` section of the API Guide for more details.

Users
-----
 
This dashboard tool allows you to search a list of all users of your Dataverse installation. You can remove roles from user accounts and assign or remove superuser status. See the :doc:`user-administration` section for more details.

Move Data
---------

This tool allows you to move datasets. To move Dataverse collections, see the :doc:`dataverses-datasets` section.
