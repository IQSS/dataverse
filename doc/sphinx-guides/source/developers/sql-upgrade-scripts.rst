===================
SQL Upgrade Scripts
===================

The database schema for Dataverse is constantly evolving and we have adopted a tool called Flyway to help keep your development environment up to date and in working order. As you make changes to the database schema (changes to ``@Entity`` classes), you must write SQL upgrade scripts when needed and follow Flyway file naming conventions.

.. contents:: |toctitle|
	:local:

Location of SQL Upgrade Scripts
-------------------------------

``src/main/resources/db/migration`` is the directory where we keep SQL upgrade scripts for Flyway to find.

In the past (before adopting Flyway) we used to keep SQL upgrade scripts in ``scripts/database/upgrades``. These scripts can still be used as reference but no new scripts should be added there.

How to Determine if You Need to Create a SQL Upgrade Script
-----------------------------------------------------------

If you are creating a new database table (which maps to an ``@Entity`` in JPA), you do not need to create or update a SQL upgrade script. The reason for this is that we use ``create-tables`` in ``src/main/resources/META-INF/persistence.xml`` so that new tables are automatically created by Glassfish when you deploy your war file.

If you are doing anything other than creating a new database table such as adding a column to an existing table, you must create or update a SQL upgrade script.

How to Create a SQL Upgrade Script
----------------------------------

We assume you have already read the :doc:`version-control` section and have been keeping your feature branch up to date with the "develop" branch.

First, compare https://github.com/IQSS/dataverse/tree/develop/src/main/resources/db/migration to your local ``src/main/resources/db/migration`` directory to make sure you have all of the SQL migration scripts. If you are missing any, merge the "develop" branch into your branch.

Create a new file called something like ``V1.1__5513-database-variablemetadata.sql`` that has a unique file name and follows the Flyway file naming conventions at https://flywaydb.org/documentation/migrations#naming . For the "description" you should include the GitHub issue you are working on, as in the example above.

The SQL migration script you wrote will be part of the war file and executed when the war file is deployed. To see a history of Flyway database migrations that have been applied, look at the ``flyway_schema_history`` table.

As with any task related to Dataverse development, if you need any help writing SQL upgrade scripts, please reach out using any of the channels mentioned under "Getting Help" in the :doc:`intro` section.

----

Previous: :doc:`version-control` | Next: :doc:`testing`
