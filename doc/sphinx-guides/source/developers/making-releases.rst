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
- doc/sphinx-guides/source/index.rst 

Here's an example commit where all three files were updated at once: https://github.com/IQSS/dataverse/commit/813b66a6077e8f94026a8db5320cceffefc10e11

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

- installer
- war file
- database migration script
- other files as needed, such as an updated Solr schema

Publish Release
---------------

Click the "Publish release" button.

----

Previous: :doc:`coding-style` | Next: :doc:`tools`
