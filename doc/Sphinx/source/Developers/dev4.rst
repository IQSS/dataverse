====================
DVN Developers Guide
====================

Solr
++++

Search in Dataverse 4.0 depends on Solr, which you should run on localhost during development.

Installing and Running Solr
===========================

Download solr-4.6.0.tgz from http://lucene.apache.org/solr/ to any directory you like but in the example below, we have downloaded the tarball to a directory called "solr" in our home directory. For now we are using the "example" template but we are replacing ``schema.xml`` with our own.

- ``cd ~/solr``
- ``tar xvfz solr-4.6.0.tgz``
- ``cd solr-4.6.0/example``
- ``cp ~/NetBeansProjects/dataverse_temp/conf/solr/4.6.0/schema.xml solr/collection1/conf/schema.xml``
- ``java -jar start.jar``

Please note: If you prefer, once the proper ``schema.xml`` file is in place, you can simply double-click "start.jar" rather that running ``java -jar start.jar`` from the command line.

Once Solr is up and running you should be able to see a "Solr Admin" dashboard at http://localhost:8983/solr

Once some dataverses, datasets, and files have been created and indexed, you can experiment with searches directly from Solr at http://localhost:8983/solr/#/collection1/query and look at the JSON output of searches, such as this wildcard search: http://localhost:8983/solr/collection1/select?q=*%3A*&wt=json&indent=true

Setting up your dev environment
+++++++++++++++++++++++++++++++

Once you install glassfish4, you need to configure the environment for the dataverse app - configure the database connection, set some options, etc. Michael has provided a shell script that does it all for you: 

``scripts/setup/asadmin-setup.sh``

before you run it, edit the top portion of the script to reflect the specifics of your environment (most importantly, the database username/password, etc.). 

If you make any changes to the configuration, please remember to modify this script. 

We should probably also provide a list of such configuration additions here, so that those who have already run the setup script above know what to add to their environment. 

So far, the following JVM options have been added: 

``dataverse.files.directory`` - specifies the directory where the app will be storing the data files. 
It's recommended to set it to something like 

``/Users/[YOURNAME]/dataverse/files``

this will create the directory dataverse/files in your home directory.

If you don't do anything, your files will end up being stored under
``/tmp/files``.

JVM options can be configured throught the admin console, with the asadmin command (see Michael's script above), or by directly editing your domain.xml and adding the following line: 

``<jvm-options>-Ddataverse.files.directory=/Users/[YOUR NAME]/dataverse/files</jvm-options>``



Rebuilding your dev environment
+++++++++++++++++++++++++++++++

If you have an old copy of the database and want to start fresh, here are the recommended steps:

- drop your old database
- create a new database
- deploy the app
- ``cd scripts/api``
- ``./setup-users.sh``  
- ``./setup-dvs.sh`` 
- run the reference data script: ``scripts/database/StdyField-TemplateScript.sql`` (NOTE: run the script as user ``postgres``; i.e., do not attempt to run it as your application database user, for example, ``dvnApp``!)
- confirm http://localhost:8080 is up
