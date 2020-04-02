=============
SQL Migration
=============

The database schema for Dataverse is constantly evolving and we have adopted a tool called Flyway to help keep your development environment up to date and in working order. As you make changes to the database schema (changes to ``@Entity`` classes), you must write SQLs when needed and follow Flyway file naming conventions.

.. contents:: |toctitle|
	:local:

Location of SQL Upgrade Scripts
-------------------------------

``dataverse-persistence/src/main/resources/db/migration`` is the directory where we keep SQLs for Flyway to find.

How to Determine if You Need to Create a SQL migration
------------------------------------------------------

If you are doing anything related to ``@Entity`` (Create, Remove, Edit) you must create an migration SQL.

How to Create a SQL
-------------------

We assume you have already read the :doc:`version-control` section and have been keeping your feature branch up to date with the "develop" branch.

Create a new file called something like ``V{NEXT_NUMBER}__renamedTermsOfUse.sql`` in the ``dataverse-persistence/src/main/resources/db/migration`` directory. Use a next number compared to the previous sql, ensuring that the number is unique. To read more about Flyway file naming conventions, see https://flywaydb.org/documentation/migrations#naming

The SQL migration script you wrote will be part of the war file and executed when the war file is deployed. To see a history of Flyway database migrations that have been applied, look at the ``flyway_schema_history`` table.

Troubleshooting
---------------

Renaming SQL
~~~~~~~~~~~~

Please note that if you need to rename your sql (because a new version of Dataverse was released, for example), you will see the error "FlywayException: Validate failed: Detected applied migration not resolved locally" when you attempt to deploy and deployment will fail.

To resolve this problem, delete the old migration from the ``flyway_schema_history`` table and attempt to redeploy.

----

Previous: :doc:`version-control` | Next: :doc:`testing`
