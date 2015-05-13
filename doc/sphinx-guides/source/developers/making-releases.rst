===============
Making Releases
===============

Bump Version Numbers
--------------------

Before tagging, ensure the version number has been incremented in the following places:

- pom.xml
- src/main/java/VersionNumber.properties
- doc/sphinx-guides/source/conf.py

Finalize Documentation
----------------------

The source for user-facing documentation (including this very guide) is under https://github.com/IQSS/dataverse/tree/master/doc

Docs don't write themselves. Please help out! Before a release is tagged documentation related to that release should be checked in to the release branch (i.e. 4.0.1), ultimately to be hosted under a version number at http://guides.dataverse.org

Write Release Notes
-------------------

See http://keepachangelog.com

Merge Release Branch
--------------------

The release branch (i.e. 4.0.1) should be merged into "master" before tagging.

Tag the Release
---------------

The tag will be the number of the milestone and release branch (i.e. 4.0.1).

Make Release Available for Download
-----------------------------------

Upload the following to https://github.com/IQSS/dataverse/releases

- installer (for new installs)
- war file
- database migration script
