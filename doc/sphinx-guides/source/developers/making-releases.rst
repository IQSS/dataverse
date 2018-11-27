===============
Making Releases
===============

.. contents:: |toctitle|
	:local:

Use the number of the milestone with a "v" in front for the relase tag. For example: ``v4.6.2``.

Create the release GitHub issue and branch 
------------------------------------------

Use the GitHub issue number and the release tag for the name of the branch. 
For example: 4734-update-v-4.8.6-to-4.9

**Note:** the changes below must be the very last commits merged into the develop branch before it is merged into master and tagged for the release!

Make the following changes in the release branch:

1. Bump Version Numbers
=======================

Before tagging, ensure the version number has been incremented to the milestone (i.e. 4.6.2) in the following places:

- pom.xml
- doc/sphinx-guides/source/conf.py
- doc/sphinx-guides/source/versions.rst 
- scripts/database/releases.txt

Here's an example commit where three of the four files above were updated at once: https://github.com/IQSS/dataverse/commit/99e23f96ec362ac2f524cb5cd80ca375fa13f196

2. Save the EJB Database Create Script
======================================

Save the script ``domains/domain1/generated/ejb/dataverse/dataverse_VDCNet-ejbPU_createDDL.jdbc`` created by EJB during the deployment of the release candidate. **Important:** add semicolons to the ends of the SQL commands in the EJB-generated file (see below)! Save the resulting file as ``scripts/database/create/create_v{VERSION_TAG}.sql`` using the version number tag for the release. For example: 

.. code-block:: none

	sed 's/$/;/' dataverse_VDCNet-ejbPU_createDDL.jdbc > scripts/database/create/create_v4.10.sql

(We are saving the script above to support the new experimental process for updating the database across multiple versions; see ``scripts/database/README_upgrade_across_versions.txt`` for more information.)

3. Check in the Changes Above... 
================================

... into the release branch, make a pull request and merge the release branch into develop. 


Merge "develop" into "master"
-----------------------------

The "develop" branch should be merged into "master" before tagging. See also the branching strategy described in the :doc:`version-control` section.

Write Release Notes
-------------------

Create a draft release at https://github.com/IQSS/dataverse/releases/new

- The "tag version" and "title" should be the number of the milestone with a "v" in front (i.e. v4.6.2).
- For the description, follow previous examples at https://github.com/IQSS/dataverse/releases

Please note that the current process involves copying and pasting a running Google doc into release notes but we are conducting an experiment whereby developers can express the need for an addition to release notes by creating a file in ``/doc/release-notes`` containing the name of the issue they're working on. Perhaps the name of the branch could be used for the filename with ".md" appended (release notes are written in Markdown) such as ``5053-apis-custom-homepage.md``. To avoid accumulating many stale files over time, when a release is cut these files should probably be removed with ``git rm``. This experiment may help inform a future experiment having to do with improvements to our process for writing SQL upgrade scripts. See the :doc:`sql-upgrade-scripts` section for more on this topic.

Make Artifacts Available for Download
-------------------------------------

Upload the following artifacts to the draft release you created:

- war file (``mvn package`` from Jenkins)
- installer (``cd scripts/installer && make``)
- database migration script (see also the :doc:`sql-upgrade-scripts` section)
- other files as needed, such as an updated Solr schema

Publish Release
---------------

Click the "Publish release" button.

----

Previous: :doc:`containers` | Next: :doc:`tools`
