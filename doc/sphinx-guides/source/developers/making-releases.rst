===============
Making Releases
===============

.. contents:: |toctitle|
	:local:

Use the number of the milestone with a "v" in front for the release tag. For example: ``v4.6.2``.

Create the release GitHub issue and branch 
------------------------------------------

Use the GitHub issue number and the release tag for the name of the branch. 
For example: 4734-update-v-4.8.6-to-4.9

**Note:** the changes below must be the very last commits merged into the develop branch before it is merged into master and tagged for the release!

Make the following changes in the release branch:

1. Bump Version Numbers
=======================

Increment the version number to the milestone (e.g. 4.6.2) in the following two files:

- pom.xml
- doc/sphinx-guides/source/conf.py (two places)

Add the version being released to the lists in the following two files:

- doc/sphinx-guides/source/versions.rst 
- scripts/database/releases.txt

Here's an example commit where three of the four files above were updated at once: https://github.com/IQSS/dataverse/commit/99e23f96ec362ac2f524cb5cd80ca375fa13f196

2. Check in the Changes Above...
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

Please note that the current process involves copying and pasting a running Google doc into release notes but we are conducting an experiment whereby developers can express the need for an addition to release notes by creating a file in ``/doc/release-notes`` containing the name of the issue they're working on. Perhaps the name of the branch could be used for the filename with ".md" appended (release notes are written in Markdown) such as ``5053-apis-custom-homepage.md``. To avoid accumulating many stale files over time, when a release is cut these files should probably be removed with ``git rm``.

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
