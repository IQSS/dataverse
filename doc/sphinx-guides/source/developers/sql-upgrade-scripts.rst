===================
SQL Upgrade Scripts
===================

The database schema for Dataverse is constantly evolving. As other developers make changes to the database schema you will need to keep up with these changes to have your development environment in working order. Additionally, as you make changes to the database schema, you must write SQL when needed and communicate with your fellow developers about making those SQL scripts.

.. contents:: |toctitle|
	:local:

Location of SQL Upgrade Scripts and Flyway SQL scripts
-----------------------------------------------

``scripts/database/upgrades`` is the directory where we keep SQL upgrade scripts - legacy way of upgrading schema.
``resources/db/migration`` is the directory where we keep migration SQL scripts.

SQL naming conventions
----------------------
In order to make the migrations work you must follow the naming conventions of the flyway - https://flywaydb.org/documentation/migrations#sql-based-migrations

How to Determine if You Need to Create SQL
------------------------------------------

If you are creating a new database table (which maps to an ``@Entity`` in JPA), you do not need to create. The reason for this is that we use ``create-tables`` in ``src/main/resources/META-INF/persistence.xml`` so that new tables are automatically created by Glassfish when you deploy your war file.

If you are doing anything other than creating a new database table such as adding a column to an existing table, you must create SQL.

How to Create SQL
-----------------

We assume you have already read the :doc:`version-control` section and have been keeping your feature branch up to date with the "develop" branch.

First, check https://github.com/IQSS/dataverse/tree/develop/src/main/resources/db/migration to see if a SQL numbers are not colliding with your's.

As with any task related to Dataverse development, if you need any help writing SQL, please reach out using any of the channels mentioned under "Getting Help" in the :doc:`intro` section.

Please note that we are aware of the problem of merge conflicts in the SQL numbers. Please see the :doc:`making-releases` section for how we are running an experiment having to do with release notes that might help inform an improvement of our process for developing SQL scripts.

----

Previous: :doc:`version-control` | Next: :doc:`testing`
