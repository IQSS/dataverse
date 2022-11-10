===============
Making Releases
===============

.. contents:: |toctitle|
	:local:

Introduction
------------

See :doc:`version-control` for background on our branching strategy.

The steps below describe making both regular releases and hotfix releases.

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

Add the version being released to the lists in the following file:

- doc/sphinx-guides/source/versions.rst (e.g. `versions.rst commit <https://github.com/IQSS/dataverse/commit/0511245>`_)

Check in the Changes Above into a Release Branch and Merge It
-------------------------------------------------------------

For a regular release, make the changes above in the release branch you created, make a pull request, and merge it into the "develop" branch. Like usual, you can safely delete the branch after the merge is complete.

If you are making a hotfix release, make the pull request against the "master" branch. Do not delete the branch after merging because we will later merge it into the "develop" branch to pick up the hotfix. More on this later.

Either way, as usual, you should ensure that all tests are passing. Please note that you will need to bump the version in `jenkins.yml <https://github.com/GlobalDataverseCommunityConsortium/dataverse-ansible/blob/develop/tests/group_vars/jenkins.yml>`_ in dataverse-ansible to get the tests to pass. Consider doing this before making the pull request. Alternatively, you can bump jenkins.yml after making the pull request and re-run the Jenkins job to make sure tests pass.

Merge "develop" into "master"
-----------------------------

If this is a regular (non-hotfix) release, create a pull request to merge the "develop" branch into the "master" branch using this "compare" link: https://github.com/IQSS/dataverse/compare/master...develop

Once important tests have passed (compile, unit tests, etc.), merge the pull request. Don't worry about style tests failing such as for shell scripts. 

If this is a hotfix release, skip this whole "merge develop to master" step (the "develop" branch is not involved until later).

Build the Guides for the Release
--------------------------------

Go to https://jenkins.dataverse.org/job/guides.dataverse.org/ and make the following adjustments to the config:

- Repository URL: ``https://github.com/IQSS/dataverse.git``
- Branch Specifier (blank for 'any'): ``*/master``
- ``VERSION`` (under "Build Steps"): ``5.10.1`` (for example)

Click "Save" then "Build Now".

Make sure the guides directory appears in the expected location such as https://guides.dataverse.org/en/5.10.1/

As described below, we'll soon point the "latest" symlink to that new directory.

Create a Draft Release on GitHub
--------------------------------

Go to https://github.com/IQSS/dataverse/releases/new to start creating a draft release.

- Under "Choose a tag" you will be creating a new tag. Have it start with a "v" such as ``v5.10.1``. Click "Create new tag on publish".
- Under "Target" go to "Recent Commits" and select the merge commit from when you merged ``develop`` into ``master`` above. This commit will appear in ``/api/info/version`` from a running installation.
- Under "Release title" use the same name as the tag such as ``v5.10.1``.
- In the description, copy and paste the content from the release notes .md file created in the "Write Release Notes" steps above.
- Click "Save draft" because we do not want to publish the release yet.

At this point you can send around the draft release for any final feedback. Links to the guides for this release should be working now, since you build them above.

Make corrections to the draft, if necessary. It will be out of sync with the .md file, but that's ok (`#7988 <https://github.com/IQSS/dataverse/issues/7988>`_ is tracking this).

Run a Build to Create the War File
----------------------------------

ssh into the dataverse-internal server and undeploy the current war file.

Go to https://jenkins.dataverse.org/job/IQSS_Dataverse_Internal/ and make the following adjustments to the config:

- Repository URL: ``https://github.com/IQSS/dataverse.git``
- Branch Specifier (blank for 'any'): ``*/master``
- Execute shell: Update version in filenames to ``dataverse-5.10.1.war`` (for example)

Click "Save" then "Build Now".

The build number will appear in ``/api/info/version`` (along with the commit mentioned above) from a running installation (e.g. ``{"version":"5.10.1","build":"907-b844672``).

Build Installer (dvinstall.zip)
-------------------------------

ssh into the dataverse-internal server and do the following:

- In a git checkout of the dataverse source switch to the master branch and pull the latest.
- Copy the war file from the previous step to the ``target`` directory in the root of the repo (create it, if necessary).
- ``cd scripts/installer``
- ``make``

A zip file called ``dvinstall.zip`` should be produced.

Make Artifacts Available for Download
-------------------------------------

Upload the following artifacts to the draft release you created:

- the war file (e.g. ``dataverse-5.10.1.war``, from above)
- the installer (``dvinstall.zip``, from above)
- other files as needed:

  - updated Solr schema
  - metadata block tsv files
  - config files

Publish the Release
-------------------

Click the "Publish release" button.

Update Guides Link
------------------

"latest" at https://guides.dataverse.org/en/latest/ is a symlink to the directory with the latest release. That directory (e.g. ``5.10.1``) was put into place by the Jenkins "guides" job described above.

ssh into the guides server and update the symlink to point to the latest release.

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

We've merged the hotfix into the "master" branch but now we need the fixes (and version bump) in the "develop" branch. Make a new branch off the hotfix branch and create a pull request against develop. Merge conflicts are possible and this pull request should go through review and QA like normal. Afterwards it's fine to delete this branch and the hotfix branch that was merged into master.

Because of the hotfix version, any SQL scripts in "develop" should be renamed (from "5.11.0" to "5.11.1" for example). To read more about our naming conventions for SQL scripts, see :doc:`sql-upgrade-scripts`.

Please note that version bumps and SQL script renaming both require all open pull requests to be updated with the latest from the "develop" branch so you might want to add any SQL script renaming to the hotfix branch before you put it through QA to be merged with develop. This way, open pull requests only need to be updated once.

----

Previous: :doc:`containers` | Next: :doc:`tools`
