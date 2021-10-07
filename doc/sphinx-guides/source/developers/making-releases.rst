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

Make the following changes in the release branch:

..note:: changes must be the very last commits merged into the develop branch before it is merged into master and tagged for the release!

1. Bump Version Numbers
=======================

Increment the version number to the milestone (e.g. 4.6.2) in the following files:

- pom.xml
- dataverse-webapp/pom.xml
- dataverse-test-common/pom.xml
- dataverse-persistence/pom.xml
- doc/sphinx-guides/source/conf.py (two places)

Add the version being released to the lists in the following two files:

- doc/sphinx-guides/source/versions.rst 
- scripts/database/releases.txt

2. Check in the Bumping Version Numbers Changes...
==================================================

... into the release branch, make a pull request and merge the release branch into develop. 


Merge "develop" into "master"
-----------------------------

The "develop" branch should be merged into "master" before tagging. See also the branching strategy described in the :doc:`version-control` section.

Write Release Notes
-------------------

Create a draft release at https://github.com/CeON/dataverse/releases/new

- The "tag version" and "title" should be the number of the milestone with a "v" in front (i.e. v4.6.2).
- For the description, follow previous examples at https://github.com/CeON/dataverse/releases


Make Artifacts Available for Download
-------------------------------------

Upload the following artifacts to the draft release you created:

- war file (``mvn package`` from Jenkins)
- installer (``cd scripts/installer && make``)
- other files as needed, such as an updated Solr schema

Publish Release
---------------

Click the "Publish release" button.

----

Previous: :doc:`containers` | Next: :doc:`tools`
