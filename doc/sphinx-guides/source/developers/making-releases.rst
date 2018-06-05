===============
Making Releases
===============

.. contents:: |toctitle|
	:local:

Bump Version Numbers
--------------------

Before tagging, ensure the version number has been incremented to the milestone (i.e. 4.6.2) in the following places:

- pom.xml
- doc/sphinx-guides/source/conf.py
- doc/sphinx-guides/source/versions.rst 

Here's an example commit where all three files were updated at once: https://github.com/IQSS/dataverse/commit/99e23f96ec362ac2f524cb5cd80ca375fa13f196

Merge "develop" into "master"
-----------------------------

The "develop" branch should be merged into "master" before tagging. See also the branching strategy described in the :doc:`version-control` section.

Write Release Notes
-------------------

Create a draft release at https://github.com/IQSS/dataverse/releases/new

- The "tag version" and "title" should be the number of the milestone with a "v" in front (i.e. v4.6.2).
- For the description, follow previous examples at https://github.com/IQSS/dataverse/releases

Make Artifacts Available for Download
-------------------------------------

Upload the following artifacts to the draft release you created:

- war file (``mvn package`` from Jenkins)
- installer (``cd scripts/installer && make``)
- database migration script
- other files as needed, such as an updated Solr schema

Publish Release
---------------

Click the "Publish release" button.

----

Previous: :doc:`containers` | Next: :doc:`tools`
