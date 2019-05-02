Solr Search Index
=================

Dataverse requires Solr to be operational at all times. If you stop Solr, you should see a error about this on the home page, which is powered by the search index Solr provides. You can set up Solr by following the steps in our Installation Guide's :doc:`/installation/prerequisites` and :doc:`/installation/config` sections explaining how to configure it. This section you're reading now is about the care and feeding of the search index. PostgreSQL is the "source of truth" and the Dataverse application will copy data from PostgreSQL into Solr. For this reason, the search index can be rebuilt at any time. Depending on the amount of data you have, this can be a slow process. You are encouraged to experiment with production data to get a sense of how long a full reindexing will take.

.. contents:: Contents:
	:local:

Full Reindex
-------------

There are two ways to perform a full reindex of the Dataverse search index. Starting with a "clear" ensures a completely clean index but involves downtime. Reindexing in place doesn't involve downtime but does not ensure a completely clean index.

Clear and Reindex
+++++++++++++++++

Clearing Data from Solr
~~~~~~~~~~~~~~~~~~~~~~~

Please note that the moment you issue this command, it will appear to end users looking at the home page that all data is gone! This is because the home page is powered by the search index.

``curl http://localhost:8080/api/admin/index/clear``

Start Async Reindex
~~~~~~~~~~~~~~~~~~~

Please note that this operation may take hours depending on the amount of data in your system. This known issue is being tracked at https://github.com/IQSS/dataverse/issues/50

``curl http://localhost:8080/api/admin/index``

Reindex in Place
+++++++++++++++++

An alternative to completely clearing the search index is to reindex in place.

Clear Index Timestamps
~~~~~~~~~~~~~~~~~~~~~~

``curl -X DELETE http://localhost:8080/api/admin/index/timestamps``

Start or Continue Async Reindex
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

If indexing stops, this command should pick up where it left off based on which index timestamps have been set, which is why we start by clearing these timestamps above. These timestamps are stored in the ``dvobject`` database table.

``curl http://localhost:8080/api/admin/index/continue``

Manual Reindexing
-----------------

If you have made manual changes to a dataset in the database or wish to reindex a dataset that solr didn't want to index properly, it is possible to manually reindex dataverses and datasets.

Reindexing Dataverses
++++++++++++++++++++++

Dataverses must be referenced by database object ID. If you have direct database access an SQL query such as

``select id from dataverse where alias='datavarsealias';``

should work, or you may click the Dataverse's "Edit" menu and look for dataverseId= in the URLs produced by the drop-down. Then, to re-index:

``curl http://localhost:8080/api/admin/index/dataverses/135``

which should return: _{"status":"OK","data":{"message":"starting reindex of dataverse 135"}}_

Reindexing Datasets
++++++++++++++++++++

Datasets may be referenced by persistent ID or by database object ID. To re-index by persistent ID:

``curl http://localhost:8080/api/admin/index/dataset?persistentId=doi:10.5072/FK2/AAA000``

To re-index a dataset by its database ID:

``curl http://localhost:8080/api/admin/index/datasets/7504557``

Manually Querying Solr
----------------------

If you suspect something isn't indexed properly in solr, you may bypass the Dataverse web interface and query the command line directly to verify what solr returns:

``curl "http://localhost:8983/solr/collection1/select?q=dsPersistentId:doi:10.15139/S3/HFV0AO"``

to see the JSON you were hopefully expecting to see passed along to Dataverse.