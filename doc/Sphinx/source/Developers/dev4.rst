====================
DVN Developers Guide
====================

Solr
++++

Search in Dataverse 4.0 depends on Solr, which you should run on localhost during development.

Installing and Running Solr
===========================

Download solr-4.6.0.tgz from http://lucene.apache.org/solr/ to any directory you like but in the example below, we have downloaded the tarball to a directory called "solr" in our home directory. For now we are using the "example" template without modification and starting Solr by running "start.jar".

- ``cd ~/solr``
- ``tar xvfz solr-4.6.0.tgz``
- ``cd solr-4.6.0/example``
- ``java -jar start.jar``

Please note: If you prefer, you can simply double-click "start.jar" rather that running ``java -jar start.jar`` from the command line.

Once Solr is up and running you should be able to see a "Solr Admin" dashboard at http://localhost:8983/solr

Once some dataverses, datasets, and files have been created and indexed, you can experiment with searches directly from Solr at http://localhost:8983/solr/#/collection1/query and look at the JSON output of searches, such as this wildcard search: http://localhost:8983/solr/collection1/select?q=*%3A*&wt=json&indent=true

Rebuilding your dev environment
+++++++++++++++++++++++++++++++

If you have an old copy of the database and want to start fresh, here are the recommended steps:

- drop your old database
- create a new database
- deploy the app
- ``cd scripts/api``
- ``./setup-users.sh``  
- ``./setup-dvs.sh`` 
- run the reference data script: scripts/database/StdyField-TemplateScript.sql 
- confirm http://localhost:8080 is up
