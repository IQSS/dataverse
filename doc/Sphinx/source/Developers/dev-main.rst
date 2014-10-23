=========================
Solr and Dev Environment
=========================

Solr
++++

Dataverse 4.0 depends on Solr ( http://lucene.apache.org/solr/ ) , which you should run on localhost during development. The Dataverse-specific ``schema.xml`` configuration file described below is required. Solr must be running with this custom schema in place during setup.

Installing and Running Solr
===========================

Download solr-4.6.0.tgz from http://archive.apache.org/dist/lucene/solr/4.6.0/solr-4.6.0.tgz to any directory you like but in the example below, we have downloaded the tarball to a directory called "solr" in our home directory. For now we are using the "example" template but we are replacing ``schema.xml`` with our own.

- ``cd ~/solr``
- ``tar xvfz solr-4.6.0.tgz``
- ``cd solr-4.6.0/example``
- ``cp ~/NetBeansProjects/dataverse/conf/solr/4.6.0/schema.xml solr/collection1/conf/schema.xml``
- ``java -jar start.jar``

Please note: If you prefer, once the proper ``schema.xml`` file is in place, you can simply double-click "start.jar" rather that running ``java -jar start.jar`` from the command line. Figuring out how to stop Solr after double-clicking it is an exercise for the reader.

Once Solr is up and running you should be able to see a "Solr Admin" dashboard at http://localhost:8983/solr

Once some dataverses, datasets, and files have been created and indexed, you can experiment with searches directly from Solr at http://localhost:8983/solr/#/collection1/query and look at the JSON output of searches, such as this wildcard search: http://localhost:8983/solr/collection1/select?q=*%3A*&wt=json&indent=true . You can also get JSON output of static fields Solr knows about: http://localhost:8983/solr/schema/fields

jq
++

A command-line tool called ``jq`` ( http://stedolan.github.io/jq/ ) is required by the setup scripts.

If you are already using ``brew``, ``apt-get``, or ``yum``, you can install ``jq`` that way. Otherwise, download the binary for your platform from http://stedolan.github.io/jq/ and make sure it is in your ``$PATH`` (``/usr/bin/jq`` is fine) and executable with ``sudo chmod +x /usr/bin/jq``.

Setting up your dev environment
+++++++++++++++++++++++++++++++

Once you install Glassfish 4 and PostgreSQL, you need to configure the environment for the Dataverse app - configure the database connection, set some options, etc. We have a new installer script that should do it all for you:

``cd scripts/install``

``./install``

The script will prompt you  for some configuration values. It is recommended that you choose "localhost" for your hostname if this is a development environment. For everything else it should be safe to accept the defaults. 

This new script is a hybrid of the old installer from v.3.* and Michael's shell script - the latter is used for configuring Glassfish, by means of asadmin commands. A serious advantage of this approach is that you should now be able to safely run the installer on an already configured system. 

All the future changes to the configuration that are Glassfish-specific and can be done through asadmin should now go into ``scripts/install/glassfish-setup.sh``. 

Rebuilding your dev environment
+++++++++++++++++++++++++++++++

If you have an old copy of the database and old Solr data and want to start fresh, here are the recommended steps: 

- drop your old database
- clear out your existing Solr index: ``scripts/search/clear``
- run the installer script above - it will create the db, deploy the app, populate the db with reference data and run all the scripts that create the domain metadata fields. You no longer need to perform these steps separately.
- confirm you are using the latest Dataverse-specific Solr schema.xml per the "Installing and Running Solr" section of this guide
- confirm http://localhost:8080 is up
- If you want to set some dataset-specific facets, go to the root dataverse (or any dataverse; the selections can be inherited) and click "General Information" and make choices under "Select Facets". There is a ticket to automate this: https://github.com/IQSS/dataverse/issues/619
