===============
Making Releases
===============

.. contents:: |toctitle|
	:local:

Introduction
------------

.. note:: This document is about releasing the main Dataverse app (https://github.com/IQSS/dataverse). See :doc:`making-library-releases` for how to release our various libraries. Other projects have their own release documentation.

.. note:: Below you'll see branches like "develop" and "master" mentioned. For more on our branching strategy, see :doc:`version-control`.

Dataverse releases are time-based as opposed to being feature-based. That is, we announce an approximate release date in advance (e.g. for `6.8 <https://groups.google.com/g/dataverse-community/c/Y0G9mw4raLU/m/om8vjjVAAQAJ>`_) and try to hit that deadline. If features we're working on aren't ready yet, the train will leave the station without them. We release quarterly.

We also announce "last call" dates for both community pull requests and those made by core developers. If you are part of the community and have made a pull request, you have until this date to ask the team to add the upcoming milestone to your pull request. The same goes for core developers. This is not a guarantee that these pull requests will be reviewed, tested, QA'ed and merged before :ref:`code freeze <declare-code-freeze>`, but we'll try.

Regular or Hotfix?
------------------

Early on, make sure it's clear what type of release this is. The steps below describe making both regular releases and hotfix releases.

- regular

  - e.g. 6.5 (minor)
  - e.g. 7.0 (major)

- hotfix

  - e.g. 6.4.1 (patch)
  - e.g. 7.0.1 (patch)

Ensure Issues Have Been Created
-------------------------------

We have a "create release issues" script at https://github.com/IQSS/dv-project-metrics that should be run a week or so before code freeze.

For each issue that is created by the script there is likely a corresponding step in this document that has "dedicated" label on it like this:

|dedicated|

There are a variety of reasons why a step might deserve its own dedicated issue:

- The step can be done by a team member other than the person doing the release.
- Stakeholders might be interested in the status of a step (e.g. has the release been deployed to the demo site).

Steps don't get their own dedicated issue if it would be confusing to have multiple people involved. Too many cooks in the kitchen, as they say. Also, some steps are so small the overhead of an issue isn't worth it.

.. |dedicated| raw:: html

      <span class="label label-success pull-left">
        Dedicated Issue
      </span>&nbsp;

.. _declare-code-freeze:

Declare a Code Freeze
---------------------

When we declare a code freeze, we mean:

- No additional features will be merged until the freeze is lifted.
- Bug fixes will only be merged if they relate to the upcoming release in some way, such as fixes for regressions or performance problems in that release.
- Pull requests that directly affect the release, such as bumping the version, will be merged, of course.

The benefits of the code freeze are:

- The team can focus on getting the release out together.
- Regression and performance testing can happen on code that isn't changing.
- The release notes can be written without having to worry about new features (and their release note snippets) being merged in.

In short, the steps described below become easier under a code freeze.

Note: for a hotfix, a code freeze is necessary not because we want code to stop changing in the branch being hotfix released, but because bumping the version used in Jenkins/Ansible means that API tests will fail in pull requests until the version is bumped in those pull requests. Basically, we want to get the hotfix merged quickly so we can propagate the version bump into all open pull requests so that API tests can start passing again in those pull requests.

Push Back Milestones on Pull Requests That Missed the Train
-----------------------------------------------------------

As of this writing, we optimistically add milestones to issues and pull requests, hoping that the work will be complete before code freeze. Inevitably, we're a bit too optimistic.

Hopefully, as the release approached, the team has already decided which pull requests (that aren't related to the release) won't make the cut. If not, go ahead and bump them to the next release.

.. _write-release-notes:

Write Release Notes
-------------------

|dedicated|

Developers express the need for an addition to release notes by creating a "release note snippet" in ``/doc/release-notes`` containing the name of the issue they're working on. The name of the branch could be used for the filename with ".md" appended (release notes are written in Markdown) such as ``5053-apis-custom-homepage.md``. See :ref:`writing-release-note-snippets` for how this is described for contributors.

The task at or near release time is to collect these snippets into a single file.

- Find the issue in GitHub that tracks the work of creating release notes for the upcoming release.
- Create a branch, add a .md file for the release (ex. 6.10.1 Release Notes) in ``/doc/release-notes`` and write the release notes, making sure to pull content from the release note snippets mentioned above. Snippets may not include any issue number or pull request number in the text so be sure to copy the number from the filename of the snippet into the final release note.
- Delete (``git rm``) the release note snippets as the content is added to the main release notes file.
- Include instructions describing the steps required to upgrade the application from the previous version. These must be customized for release numbers and special circumstances such as changes to metadata blocks and infrastructure. These instructions are required for the next steps (deploying to various environments) so try to prioritize them over finding just the right words in release highlights (which you can do later).
- Make a pull request. Here's an example: https://github.com/IQSS/dataverse/pull/11613
- Note that we won't merge the release notes until after we have confirmed that the upgrade instructions are valid by performing a couple upgrades.

For a hotfix, don't worry about release notes yet.

Build Release Candidate
-----------------------

|dedicated|

Go to https://github.com/IQSS/dataverse/actions/workflows/generate_war_file.yml click "run workflow". For a regular release, make sure the branch is "develop". For a hotfix, you will use whatever branch name is used for the hotfix. Leave the custom label blank and click "run workflow". This will create an action that should result in a zip file. Inside that zip is another zip that contains the war file.

Deploy Release Candidate to Internal
------------------------------------

|dedicated|

ssh into the dataverse-internal server and download the release candidate war file you built above.

Go to /doc/release-notes, open the release-notes.md file for the release we're working on, and perform all the steps under "Upgrade Instructions". Note that for regular releases, we haven't bumped the version yet so you won't be able to follow the steps exactly. (For hotfix releases, the version will be bumped already.)

Deploy Release Candidate to QA
------------------------------

|dedicated|

Deploy the same war file to https://qa.dataverse.org using the same upgrade instructions as above.

Solicit Feedback from Curation Team
-----------------------------------

Ask the curation team to test on https://qa.dataverse.org and give them five days to provide feedback.


Conduct Performance Testing
---------------------------

|dedicated|

See :doc:`/qa/performance-tests` for details.

Conduct Regression Testing
---------------------------

|dedicated|

Regression testing should be conducted on production data.
See :doc:`/qa/testing-approach` for details.
Refer to the provided regression checklist for the list of items to verify during the testing process: `Regression Checklist <https://docs.google.com/document/d/1OsGJV0sMLDSmfkU9-ee8h_ozbQcUDJ1EOwNPm4dC63Q/edit?usp=sharing>`_.

Build the Guides for the Release Candidate
------------------------------------------

Go to https://jenkins.dataverse.org/job/guides.dataverse.org/ and make the following adjustments to the config:

- Repository URL: ``https://github.com/IQSS/dataverse.git``
- Branch Specifier (blank for 'any'): ``*/develop``
- ``VERSION`` (under "Build Steps"): use the next release version but add "-rc.1" to the end. Don't prepend a "v". Use ``6.8-rc.1`` (for example)

Click "Save" then "Build Now".

Make sure the guides directory appears in the expected location such as https://guides.dataverse.org/en/6.8-rc.1/

When previewing the HTML version of docs from pull requests, we don't usually use this Jenkins job, relying instead on automated ReadTheDocs builds. The reason for doing this step now while we wait for feedback from the Curation Team is that it's an excellent time to fix the Jenkins job, if necessary, to accommodate any changes needed to continue to build the docs. For example, Sphinx might need to be updated or a dependency might need to be installed. Such changes should be listed in the release notes for documentation writers.

Deploy Release Candidate to Demo
--------------------------------

|dedicated|

Time has passed. The curation team has given feedback. We've finished regression and performance testing. Fixes may have been merged into the "develop" branch. We're ready to actually make the release now, which includes deploying a release candidate to the demo server.

Build a new war file, if necessary, and deploy it to https://demo.dataverse.org using the upgrade instructions in the release notes.

Merge Release Notes (Once Ready)
--------------------------------

If the upgrade instructions are perfect, simply merge the release notes.

If the upgrade instructions aren't quite right, work with the authors of the release notes until they are good enough, and then merge.

For a hotfix, there are no release notes to merge yet.

Prepare Release Branch
----------------------

|dedicated|

The release branch will have the final changes such as bumping the version number.

Usually we branch from the "develop" branch to create the release branch. If we are creating a hotfix for a particular version (5.11, for example), we branch from the tag (e.g. ``v5.11``).

Create a release branch named after the issue that tracks bumping the version with a descriptive name like "10852-bump-to-6.4" from https://github.com/IQSS/dataverse/pull/10871.

**Note:** the changes below must be the very last commits merged into the develop branch before it is merged into master and tagged for the release!

Make the following changes in the release branch.

Increment the version number to the milestone (e.g. 6.10.1) in the following two files:

- modules/dataverse-parent/pom.xml -> ``<properties>`` -> ``<revision>`` (e.g. `pom.xml commit <https://github.com/IQSS/dataverse/commit/3943aa0>`_)
- doc/sphinx-guides/source/conf.py

In the following ``versions.rst`` file:

- doc/sphinx-guides/source/versions.rst - Below the ``- |version|`` bullet (``|version|`` comes from the ``conf.py`` file you just edited), add a bullet for what is soon to be the previous release.

Return to the parent pom and make the following change, which is necessary for proper tagging of images:

- modules/dataverse-parent/pom.xml -> ``<profiles>`` -> profile "ct" -> ``<properties>`` -> Set ``<base.image.version>`` to ``${revision}``

When testing the version change in Docker note that you will have to build the base image manually. See :ref:`base-image-build-instructions`.

(Before you make this change the value should be ``${parsedVersion.majorVersion}.${parsedVersion.nextMinorVersion}``. Later on, after cutting a release, we'll change it back to that value.)

For a regular release, make the changes above in the release branch you created, but hold off for a moment on making a pull request because Jenkins will fail because it will be testing the previous release.

In the dataverse-ansible repo bump the version in `jenkins.yml <https://github.com/gdcc/dataverse-ansible/blob/develop/tests/group_vars/jenkins.yml>`_ and make a pull request such as https://github.com/gdcc/dataverse-ansible/pull/386. Wait for it to be merged. Note that bumping on the Jenkins side like this will mean that all pull requests will show failures in Jenkins until they are updated to the version we are releasing.

Once dataverse-ansible has been merged, return to the branch you created above ("10852-bump-to-6.4" or whatever) and make a pull request. Ensure that all tests are passing and then put the PR through the normal review and QA process.

If you are making a hotfix release, ``<base.image.version>`` should already be set to ``${revision}``. If so, leave it alone. Go ahead and do the normal bumping of version numbers described above. Make the pull request against the "master" branch. Put it through review and QA. Do not delete the branch after merging because we will later merge it into the "develop" branch to pick up the hotfix. More on this later.

Merge "develop" into "master" (non-hotfix only)
-----------------------------------------------

If this is a regular (non-hotfix) release, create a pull request to merge the "develop" branch into the "master" branch using this "compare" link: https://github.com/IQSS/dataverse/compare/master...develop

Allow time for important tests pass:

- Unit tests: Maven Tests
- API tests: Container Integration Tests Workflow

Don't worry about style tests failing such as for shell scripts.

It's ok to skip code review.

When merging the pull request, be sure to choose "create a merge commit" and not "squash and merge" or "rebase and merge". We suspect that choosing squash or rebase may have led to `lots of merge conflicts <https://github.com/IQSS/dataverse/pull/11647#issuecomment-3085289132>`_ when we tried to perform this "merge develop to master" step, forcing us to `re-do <https://docs.google.com/document/d/1oit6LLDUWpNpV_uWveOMvdwDsaUey-74ehvzCZp1f3k/edit?usp=sharing>`_ the previous release before we could proceed with the current release.

If this is a hotfix release, skip this whole "merge develop to master" step (the "develop" branch is not involved until later).

Confirm "master" Mergeability
-----------------------------

Hopefully, the previous step went ok. As a sanity check, use the "compare" link at https://github.com/IQSS/dataverse/compare/master...develop again to look for merge conflicts without making a pull request.

If the GitHub UI tells you there would be merge conflicts, something has gone horribly wrong (again) with the "merge develop to master" step. Stop and ask for help.

Add Milestone to Pull Requests and Issues
-----------------------------------------

Often someone is making sure that the proper milestone (e.g. 6.10.1) is being applied to pull requests and issues, but sometimes this falls between the cracks.

Check for merged pull requests that have no milestone by going to https://github.com/IQSS/dataverse/pulls and entering `is:pr is:merged no:milestone <https://github.com/IQSS/dataverse/pulls?q=is%3Apr+is%3Amerged+no%3Amilestone>`_ as a query. If you find any, first check if those pull requests are against open pull requests. If so, do nothing. Otherwise, add the milestone to the pull request and any issues it closes. This includes the "merge develop into master" pull request above.

.. _build-guides:

Build the Guides for the Release
--------------------------------

Go to https://jenkins.dataverse.org/job/guides.dataverse.org/ and make the following adjustments to the config:

- Repository URL: ``https://github.com/IQSS/dataverse.git``
- Branch Specifier (blank for 'any'): ``*/master``
- ``VERSION`` (under "Build Steps"): bump to the next release. Don't prepend a "v". Use ``6.10.1`` (for example)

Click "Save" then "Build Now".

Make sure the guides directory appears in the expected location such as https://guides.dataverse.org/en/6.10.1/

As described below, we'll soon point the "latest" symlink to that new directory.

.. _run-build-create-war:

Run a Build to Create the War File
----------------------------------

Go to https://github.com/IQSS/dataverse/actions/workflows/generate_war_file.yml click "run workflow". For a regular release, change the branch to "master". For a hotfix release, use whatever branch name is used for the hotfix. Leave the custom label blank and click "run workflow". This will create an action that should result in a zip file. Inside that zip is another zip that contains the war file. Download it.

The build number will appear in ``/api/info/version`` (along with the commit mentioned above) from a running installation (e.g. ``{"version":"6.10.1","build":"master-300d5b5"}``).

Build Installer (dvinstall.zip)
-------------------------------

In a git checkout of the source, switch to the master branch and pull the latest.

Unzip the zip file from the previous step.

Copy the war file to the ``target`` directory in the root of the repo (create the ``target`` directory, if necessary):

.. code-block:: bash

  cp ~/Downloads/built-app.zip .
  unzip built-app.zip
  rm built-app.zip
  mkdir -p target
  mv dataverse-*.war target

Then, create the installer:

.. code-block:: bash

  cd scripts/installer
  make clean
  make

A zip file called ``dvinstall.zip`` should be produced.

Create a Draft Release on GitHub
--------------------------------

Go to https://github.com/IQSS/dataverse/releases/new to start creating a draft release.

- Under "Select tag" you will be creating a new tag. Have it start with a "v" such as ``v6.10.1``. Click "Create new tag". Don't worry, the tag won't be created until you publish.
- Under "Target", choose "master". This commit will appear in ``/api/info/version`` from a running installation.
- Under "Release title" use the same name as the tag such as ``v6.10.1``.
- In the description, copy and paste the content from the release notes .md file created in the "Write Release Notes" steps above.
- Under "attach binaries", upload the war file and installer you created above.
- Click "Save draft" because we do not want to publish the release yet.

At this point you can send around the draft release for any final feedback. Links to the guides for this release should be working now, since you build them above.

Make corrections to the draft, if necessary. It will be out of sync with the .md file, but that's ok (`#7988 <https://github.com/IQSS/dataverse/issues/7988>`_ is tracking this).

Publish the Release
-------------------

Click the "Publish release" button.

Update Guides Link
------------------

"latest" at https://guides.dataverse.org/en/latest/ is a symlink to the directory with the latest release. That directory (e.g. ``6.10.1``) was put into place by the Jenkins "guides" job described above.

ssh into the guides server and update the symlink to point to the latest release, as in the example below.

.. code-block:: bash

  cd /var/www/html/en
  ln -s 6.10.1 latest

This step could be done before publishing the release if you'd like to double check that links in the release notes work.

Test Docker Images
------------------

Publishing the release should have trigged the "Container Images Scheduled Maintenance" GitHub Action. Allow it to finish and then go to https://hub.docker.com/u/gdcc and navigate to "gdcc/dataverse".

Click on "tags" and look at the "latest" tag. Was it just updated? Good! If not, we plan to address this is https://github.com/IQSS/dataverse/issues/12514 but for now, as a workaround, run the action again. Go to https://github.com/IQSS/dataverse/actions/workflows/container_maintenance.yml and click the "run workflow" dropdown. Make sure the branch is set to "develop" and click "run workflow" button.

Wait for the action to finish and then check again that the "latest" tag has been updated.

Locally, delete old images and spin up the "latest" tag.

.. code-block:: bash

  docker rmi gdcc/dataverse:latest
  docker rmi gdcc/configbaker:latest
  cd docker/compose/demo
  rm -rf data
  docker compose up

Wait for the bootstrapping process to complete. Then, look at http://localhost:8080/api/info/version to make sure "version" shows the version that you just released. Note that it's normal for "build" to be null for our Docker images.

Close Milestone on GitHub and Create a New One
----------------------------------------------

You can find our milestones at https://github.com/IQSS/dataverse/milestones

Now that we've published the release, close the milestone and create a new one for the **next** release, the release **after** the one we're working on, that is.

Note that for milestones we use just the number without the "v" (e.g. "6.10.1").

On the project board at https://github.com/orgs/IQSS/projects/34 edit the tab (view) that shows the milestone to show the next milestone.

.. _base_image_post_release:

Update the Container Base Image Version Property
------------------------------------------------

|dedicated|

Create a new branch (any name is fine but ``prepare-next-iteration`` is suggested) and update the following files to prepare for the next development cycle:

- modules/dataverse-parent/pom.xml -> ``<profiles>`` -> profile "ct" -> ``<properties>`` -> Set ``<base.image.version>`` to ``${parsedVersion.majorVersion}.${parsedVersion.nextMinorVersion}``

Create a pull request and put it through code review, like usual. Give it a milestone of the next release, the one **after** the one we're working on. Once the pull request has been approved, merge it. It should be the first PR merged of the next release.

For more background, see :ref:`base-image-supported-tags`. For an example, see https://github.com/IQSS/dataverse/pull/10896

For a hotfix, we will do this later and in a different branch. See below.

Deploy Final Release on Demo
----------------------------

|dedicated|

Above you already did the hard work of deploying a release candidate to https://demo.dataverse.org. It should be relatively straightforward to undeploy the release candidate and deploy the final release.

.. _update-schemaspy:

Update SchemaSpy
----------------

We maintain SchemaSpy at URLs like https://guides.dataverse.org/en/latest/schemaspy/index.html and (for example) https://guides.dataverse.org/en/6.6/schemaspy/index.html

Get the attention of the core team and ask someone to update it for the new release.

Consider updating `the thread <https://groups.google.com/g/dataverse-community/c/f95DQU-wlVM/m/cvUp3E9OBgAJ>`_ on the mailing list once the update is in place.

See also :ref:`schemaspy`.

Alert Translators About the New Release
---------------------------------------

Create an issue at https://github.com/GlobalDataverseCommunityConsortium/dataverse-language-packs/issues to say a new release is out and that we would love for the properties files for English to be added.

For example, for 6.4 we wrote "Update en_US/Bundle.properties etc. for Dataverse 6.4" at https://github.com/GlobalDataverseCommunityConsortium/dataverse-language-packs/issues/125

Add the Release to the Dataverse Roadmap
----------------------------------------

Add an entry to the list of releases at https://www.iq.harvard.edu/roadmap-dataverse-project 

Announce the Release on the Dataverse Blog
------------------------------------------

Make a blog post at https://dataverse.org/blog

Announce the Release on the Mailing List
----------------------------------------

Post a message at https://groups.google.com/g/dataverse-community

Announce the Release on Zulip
-----------------------------

Post a message under #community at https://dataverse.zulipchat.com

For Hotfixes, Merge Hotfix Branch into "develop"
------------------------------------------------

Note: this only applies to hotfixes!

We've merged the hotfix into the "master" branch but now we need the fixes (and version bump) in the "develop" branch.

Make a new branch off the hotfix branch. You can call it something like "6.7.1-merge-hotfix-to-develop".

In that branch, do the :ref:`base_image_post_release` step you skipped above. Now is the time.

Create a pull request against develop. Merge conflicts are possible and this pull request should go through review and QA like normal. Afterwards it's fine to delete this branch and the hotfix branch that was merged into master.

For Hotfixes, Tell Developers to Merge "develop" into Their Branches and Rename SQL Scripts
-------------------------------------------------------------------------------------------

Note: this only applies to hotfixes!

Because we have merged a version bump from the hotfix into the "develop" branch, any SQL scripts in the "develop" branch should be renamed (from "5.11.0" to "5.11.1" for example). (To read more about our naming conventions for SQL scripts, see :doc:`sql-upgrade-scripts`.)

Look at ``src/main/resources/db/migration`` in the "develop" branch and if any SQL scripts have the wrong version, make a pull request (or ask a developer to) to update them (all at once in a single PR is fine).

Tell developers to merge the "develop" into their open pull requests (to pick up the new version and any fixes) and rename SQL scripts (if any) with the new version.

Lift the Code Freeze and Encourage Developers to Update Their Branches
----------------------------------------------------------------------

It's now safe to lift the code freeze. We can start merging pull requests into the "develop" branch for the next release.

Let developers know that they should merge the latest from the "develop" branch into any branches they are working on. (For hotfixes we've already told them this.)
