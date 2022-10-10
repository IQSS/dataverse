===============
Making Releases
===============

.. contents:: |toctitle|
	:local:

Introduction
------------

See :doc:`version-control` for background on our branching strategy.

The steps below describe making both normal releases and hotfix releases.

Write Release Notes
-------------------

Developers express the need for an addition to release notes by creating a file in ``/doc/release-notes`` containing the name of the issue they're working on. The name of the branch could be used for the filename with ".md" appended (release notes are written in Markdown) such as ``5053-apis-custom-homepage.md``. 

The task at or near release time is to collect these notes into a single doc.

- Create an issue in GitHub to track the work of creating release notes for the upcoming release.
- Create a branch, add a .md file for the release (ex. 5.10.1 Release Notes) in ``/doc/release-notes`` and write the release notes, making sure to pull content from the issue-specific release notes mentioned above.
- Delete the previously-created, issue-specific release notes as the content is added to the main release notes file.
- Take the release notes .md through the regular Code Review and QA process.

Create a GitHub Issue and Branch for the Release
------------------------------------------------

Usually we branch from the "develop" branch to create the release branch. If we are creating a hotfix for a particular version (5.11, for example), we branch from the tag (e.g. ``v5.11``).

Use the GitHub issue number and the release tag for the name of the branch. (e.g. ``8583-update-version-to-v5.10.1``

**Note:** the changes below must be the very last commits merged into the develop branch before it is merged into master and tagged for the release!

Make the following changes in the release branch.

Bump Version Numbers
--------------------

Increment the version number to the milestone (e.g. 5.10.1) in the following two files:

- modules/dataverse-parent/pom.xml -> ``<properties>`` -> ``<revision>`` (e.g. `pom.xml commit <https://github.com/IQSS/dataverse/commit/3943aa0>`_)
- doc/sphinx-guides/source/conf.py (two places, e.g. `conf.py commit <https://github.com/IQSS/dataverse/commit/18fd296>`_)  

Add the version being released to the lists in the following two files:

- doc/sphinx-guides/source/versions.rst (e.g. `versions.rst commit <https://github.com/IQSS/dataverse/commit/0511245>`_)

Check in the Changes Above into a Release Branch and Merge It
-------------------------------------------------------------

For any ordinary release, make the changes above in the release branch you created, make a pull request, and merge it into the "develop" branch. Like usual, you can safely delete the branch after the merge is complete.

If you are making a hotfix release, make the pull request against the "master" branch. Do not delete the branch after merging because we will later merge it into the "develop" branch to pick up the hotfix. More on this later.

Either way, as usual, you should ensure that all tests are passing. Please note that you might need to bump the version in `jenkins.yml <https://github.com/GlobalDataverseCommunityConsortium/dataverse-ansible/blob/develop/tests/group_vars/jenkins.yml>`_ in dataverse-ansible to get the tests to run.

Merge "develop" into "master"
-----------------------------

Note: If you are making a hotfix release, the "develop" branch is not involved so you can skip this step.

The "develop" branch should be merged into "master" before tagging.

Create a Draft Release on GitHub
--------------------------------

Create a draft release at https://github.com/IQSS/dataverse/releases/new

The "tag version" and "title" should be the number of the milestone with a "v" in front (i.e. v5.10.1).

Copy in the content from the .md file created in the "Write Release Notes" steps above.

Make Artifacts Available for Download
-------------------------------------

Upload the following artifacts to the draft release you created:

- war file (``mvn package`` from Jenkins)
- installer (``cd scripts/installer && make``)
- other files as needed, such as updated Solr schema and config files

Publish the Release
-------------------

Click the "Publish release" button.

Close Milestone on GitHub and Create a New One
----------------------------------------------

You can find our milestones at https://github.com/IQSS/dataverse/milestones

Now that we've published the release, close the milestone and create a new one.

Note that for milestones we use just the number without the "v" (e.g. "5.10.1").

Add the Release to the Dataverse Roadmap
----------------------------------------

Add an entry to the list of releases at https://www.iq.harvard.edu/roadmap-dataverse-project 

Announce the Release on the Dataverse Blog
------------------------------------------

Make a blog post at https://dataverse.org/blog

Announce the Release on the Mailing List
----------------------------------------

Post a message at https://groups.google.com/g/dataverse-community

For Hotfixes, Merge Hotfix Branch into "develop" and Rename SQL Scripts
-----------------------------------------------------------------------

Note: this only applies to hotfixes!

We've merged the hotfix into the "master" branch but now we need the fixes (and version bump) in the "develop" branch. Make a new branch off the hotfix branch and create a pull request against develop. Merge conflicts are possible and this pull request should go through review and QA like normal. Afterwards it's fine to delete this branch and the hotfix brach that was merged into master.

Because of the hotfix version, any SQL scripts in "develop" should be renamed (from "5.11.0" to "5.11.1" for example). To read more about our naming conventions for SQL scripts, see :doc:`sql-upgrade-scripts`.

Please note that version bumps and SQL script renaming both require all open pull requests to be updated with the latest from the "develop" branch so you might want to add any SQL script renaming to the hotfix branch before you put it through QA to be merged with develop. This way, open pull requests only need to be updated once.

----

Previous: :doc:`containers` | Next: :doc:`tools`
