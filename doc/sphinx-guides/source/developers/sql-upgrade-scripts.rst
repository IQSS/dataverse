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

Create a new file called something like ``V4.11__5513-database-variablemetadata.sql`` in the ``src/main/resources/db/migration`` directory. Use the previously released version (4.11 in the example above) rather than what we guess will be the next released version (which is often uncertain). For the "description" you should the name of your branch, which should include the GitHub issue you are working on, as in the example above. To read more about Flyway file naming conventions, see https://flywaydb.org/documentation/migrations#naming

The SQL migration script you wrote will be part of the war file and executed when the war file is deployed. To see a history of Flyway database migrations that have been applied, look at the ``flyway_schema_history`` table.

As with any task related to Dataverse development, if you need any help writing SQL upgrade scripts, please reach out using any of the channels mentioned under "Getting Help" in the :doc:`intro` section.

Troubleshooting
---------------

Renaming SQL Upgrade Scripts
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Please note that if you need to rename your script (because a new version of Dataverse was released, for example), you will see the error "FlywayException: Validate failed: Detected applied migration not resolved locally" when you attempt to deploy and deployment will fail.

To resolve this problem, delete the old migration from the ``flyway_schema_history`` table and attempt to redeploy.

----

Previous: :doc:`version-control` | Next: :doc:`testing`
