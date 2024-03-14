===================
SQL Upgrade Scripts
===================

The database schema for the Dataverse Software is constantly evolving and we have adopted a tool called Flyway to help keep your development environment up to date and in working order. As you make changes to the database schema (changes to ``@Entity`` classes), you must write SQL upgrade scripts when needed and follow Flyway file naming conventions.

.. contents:: |toctitle|
	:local:

Location of SQL Upgrade Scripts
-------------------------------

``src/main/resources/db/migration`` is the directory where we keep SQL upgrade scripts for Flyway to find.

In the past (before adopting Flyway) we used to keep SQL upgrade scripts in ``scripts/database/upgrades``. These scripts can still be used as reference but no new scripts should be added there.

How to Determine if You Need to Create a SQL Upgrade Script
-----------------------------------------------------------

If you are creating a new database table (which maps to an ``@Entity`` in JPA), you do not need to create or update a SQL upgrade script. The reason for this is that we use ``create-tables`` in ``src/main/resources/META-INF/persistence.xml`` so that new tables are automatically created by the app server when you deploy your war file.

If you are doing anything other than creating a new database table such as adding a column to an existing table, you must create or update a SQL upgrade script.

.. _create-sql-script:

How to Create a SQL Upgrade Script
----------------------------------

We assume you have already read the :doc:`version-control` section and have been keeping your feature branch up to date with the "develop" branch.

Create a new SQL file in the ``src/main/resources/db/migration`` directory. Name the file to something similar to ``V6.1.0.1.sql``. In this example ``6.1`` represents the current version of Dataverse, with the last digit representing number of the script for that version. Should a newer version be merged while you work on your pull request (PR), you may need to update to a higher number. Generally, PR's merged first take priority, and the order may vary depending on QA considerations at the time.

Previously, we used the Flyway file naming conventions. However, this approach occasionally led to inadvertent merging of multiple scripts with the same version, such as ``6.0.0.1__0000-wonderful-pr.sql`` and ``6.0.0.1__0001-wonderful-pr.sql``. After careful consideration and analysis, we agreed to adopt the convention mentioned earlier for naming files. This helps us detect conflicts before merging a PR, preventing the development branch from breaking an installation. For more information on Flyway file naming conventions,  see https://documentation.red-gate.com/fd/migrations-184127470.html 

The SQL migration script you wrote will be part of the war file and executed when the war file is deployed. To see a history of Flyway database migrations that have been applied, look at the ``flyway_schema_history`` table.

As with any task related to the development of the Dataverse Software, if you need any help writing SQL upgrade scripts, please reach out using any of the channels mentioned under "Getting Help" in the :doc:`intro` section.

Troubleshooting
---------------

Renaming SQL Upgrade Scripts
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Please note that if you need to rename your script (because a new version of the Dataverse Software was released, for example), you will see the error "FlywayException: Validate failed: Detected applied migration not resolved locally" when you attempt to deploy and deployment will fail.

To resolve this problem, delete the old migration from the ``flyway_schema_history`` table and attempt to redeploy.
