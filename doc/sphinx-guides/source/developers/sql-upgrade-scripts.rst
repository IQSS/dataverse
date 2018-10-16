===================
SQL Upgrade Scripts
===================

The database schema for Dataverse is constantly evolving. As other developers make changes to the database schema you will need to keep up with these changes to keep your development environment in working order. Additionally, as you make changes to the database schema, you must help write SQL upgrade scripts as needed and communicate with your fellow developers when the scripts must be applied.

.. contents:: |toctitle|
	:local:

Location of SQL Upgrade Scripts
-------------------------------

``scripts/database/upgrades`` is the directory where we keep or SQL upgrade scripts.

How to Determine if You Need to Create or Update a SQL Upgrade Script
---------------------------------------------------------------------

If you are creating a new database table (which maps to an ``@Entity`` in JPA), you do not need to create or update a SQL upgrade script. The reason for this is that we use ``create-tables`` in ``src/main/resources/META-INF/persistence.xml`` so that new tables are automatically created by Glassfish when you deploy your war file.

If you are doing anything other than creating a new database table such as adding a column to an existing table, you must create or update a SQL upgrade script.

How to Create or Update a SQL Upgrade Script
--------------------------------------------

We assume you have already read the :doc:`version-control` section and have been keeping your feature branch up to date with the "develop" branch.

First, check https://github.com/IQSS/dataverse/tree/develop/scripts/database/upgrades to see if a SQL upgrade script for the next release already exists. For example, if the current release is 4.9.4 and the next release will be 4.10, the script will be named ``upgrade_v4.9.4_to_v4.10.sql``. If the script exists, just add your changes to the bottom of it.

If no SQL upgrade script exists, look at https://github.com/IQSS/dataverse/milestones to figure out the name of the next milestone and create a script using the naming convention above.

As with any task related to Dataverse development, if you need any help writing SQL upgrade scripts, please reach out using any of the channels mentioned under "Getting Help" in the :doc:`intro` section.

Communicating the Need to Run SQL Updates
-----------------------------------------

If you have made a pull request that contains SQL updates and that pull request is merged into the "develop" branch, you are responsible for communicating to other developers that when then pull the latest code from "develop" they must run your SQL updates. Post a message to the "dataverse-dev" mailing list at https://groups.google.com/forum/#!forum/dataverse-dev

----

Previous: :doc:`version-control` | Next: :doc:`testing`
