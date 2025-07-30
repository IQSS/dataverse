===============
Making Releases
===============

.. contents:: |toctitle|
	:local:

Introduction
------------

This document is about releasing the main Dataverse app (https://github.com/IQSS/dataverse). See :doc:`making-library-releases` for how to release our various libraries. Other projects have their own release documentation.

Below you'll see branches like "develop" and "master" mentioned. For more on our branching strategy, see :doc:`version-control`.

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

Some of the steps in this document are well-served by having their own dedicated GitHub issue. You'll see a label like this on them:

|dedicated|

There are a variety of reasons why a step might deserve its own dedicated issue:

- The step can be done by a team member other than the person doing the release.
- Stakeholders might be interested in the status of a step (e.g. has the release been deployed to the demo site).

Steps don't get their own dedicated issue if it would be confusing to have multiple people involved. Too many cooks in the kitchen, as they say. Also, some steps are so small the overhead of an issue isn't worth it.

Before the release even begins you can coordinate with the project manager about the creation of these issues.

.. |dedicated| raw:: html

      <span class="label label-success pull-left">
        Dedicated Issue
      </span>&nbsp;

Declare a Code Freeze
---------------------

The following steps are made more difficult if code is changing in the "develop" branch. Declare a code freeze until the release is out. Do not allow pull requests to be merged.

For a hotfix, a code freeze is not necessary.

Conduct Performance Testing
---------------------------

|dedicated|

See :doc:`/qa/performance-tests` for details.

Conduct Regression Testing
---------------------------

|dedicated|

See :doc:`/qa/testing-approach` for details.
Refer to the provided regression checklist for the list of items to verify during the testing process: `Regression Checklist <https://docs.google.com/document/d/1OsGJV0sMLDSmfkU9-ee8h_ozbQcUDJ1EOwNPm4dC63Q/edit?usp=sharing>`_.

.. _write-release-notes:

Write Release Notes
-------------------

|dedicated|

Developers express the need for an addition to release notes by creating a "release note snippet" in ``/doc/release-notes`` containing the name of the issue they're working on. The name of the branch could be used for the filename with ".md" appended (release notes are written in Markdown) such as ``5053-apis-custom-homepage.md``. See :ref:`writing-release-note-snippets` for how this is described for contributors.

The task at or near release time is to collect these snippets into a single file.

- Find the issue in GitHub that tracks the work of creating release notes for the upcoming release.
- Create a branch, add a .md file for the release (ex. 5.10.1 Release Notes) in ``/doc/release-notes`` and write the release notes, making sure to pull content from the release note snippets mentioned above. Snippets may not include any issue number or pull request number in the text so be sure to copy the number from the filename of the snippet into the final release note.
- Delete (``git rm``) the release note snippets as the content is added to the main release notes file.
- Include instructions describing the steps required to upgrade the application from the previous version. These must be customized for release numbers and special circumstances such as changes to metadata blocks and infrastructure.
- Make a pull request. Here's an example: https://github.com/IQSS/dataverse/pull/11613
- Note that we won't merge the release notes until after we have confirmed that the upgrade instructions are valid by performing a couple upgrades.

For a hotfix, don't worry about release notes yet.

Deploy Release Candidate to Internal
------------------------------------

|dedicated|

To upgrade internal, go to /doc/release-notes, open the release-notes.md file for the current release and perform all the steps under "Upgrade Instructions".

Note that we haven't bumped the version yet so you won't be able to follow the steps exactly.

For a hotfix, wait until a war file has been built.

Deploy Release Candidate to Demo
--------------------------------

|dedicated|

First, build the release candidate.

ssh into the dataverse-internal server and undeploy the current war file.
Go to /doc/release-notes, open the release-notes.md file for the current release, and perform all the steps under "Upgrade Instructions".

Go to https://jenkins.dataverse.org/job/IQSS_Dataverse_Internal/ and make the following adjustments to the config:

- Repository URL: ``https://github.com/IQSS/dataverse.git``
- Branch Specifier (blank for 'any'): ``*/develop``
- Execute shell: Update version in filenames to ``dataverse-5.10.war`` (for example)

Click "Save" then "Build Now".

This will build the war file, and then automatically deploy it on dataverse-internal. Verify that the application has deployed successfully. 

You can scp the war file to the demo server or download it from https://jenkins.dataverse.org/job/IQSS_Dataverse_Internal/ws/target/

ssh into the demo server and follow the upgrade instructions in the release notes. Again, note that we haven't bumped the version yet.

For a hotfix, wait until a war file has been built.

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

Increment the version number to the milestone (e.g. 5.10.1) in the following two files:

- modules/dataverse-parent/pom.xml -> ``<properties>`` -> ``<revision>`` (e.g. `pom.xml commit <https://github.com/IQSS/dataverse/commit/3943aa0>`_)
- doc/sphinx-guides/source/conf.py

In the following ``versions.rst`` file:

- doc/sphinx-guides/source/versions.rst - Below the ``- |version|`` bullet (``|version|`` comes from the ``conf.py`` file you just edited), add a bullet for what is soon to be the previous release.

Return to the parent pom and make the following change, which is necessary for proper tagging of images:

- modules/dataverse-parent/pom.xml -> ``<profiles>`` -> profile "ct" -> ``<properties>`` -> Set ``<base.image.version>`` to ``${revision}``

When testing the version change in Docker note that you will have to build the base image manually. See

(Before you make this change the value should be ``${parsedVersion.majorVersion}.${parsedVersion.nextMinorVersion}``. Later on, after cutting a release, we'll change it back to that value.)

For a regular release, make the changes above in the release branch you created, but hold off for a moment on making a pull request because Jenkins will fail because it will be testing the previous release.

In the dataverse-ansible repo bump the version in `jenkins.yml <https://github.com/gdcc/dataverse-ansible/blob/develop/tests/group_vars/jenkins.yml>`_ and make a pull request such as https://github.com/gdcc/dataverse-ansible/pull/386. Wait for it to be merged. Note that bumping on the Jenkins side like this will mean that all pull requests will show failures in Jenkins until they are updated to the version we are releasing.

Once dataverse-ansible has been merged, return to the branch you created above ("10852-bump-to-6.4" or whatever) and make a pull request. Ensure that all tests are passing and then put the PR through the normal review and QA process.

If you are making a hotfix release, make the pull request against the "master" branch. Put it through review and QA. Do not delete the branch after merging because we will later merge it into the "develop" branch to pick up the hotfix. More on this later.

Merge "develop" into "master" (non-hotfix only)
-----------------------------------------------

If this is a regular (non-hotfix) release, create a pull request to merge the "develop" branch into the "master" branch using this "compare" link: https://github.com/IQSS/dataverse/compare/master...develop

Once important tests have passed (compile, unit tests, etc.), merge the pull request (skipping code review is ok). Don't worry about style tests failing such as for shell scripts. 

If this is a hotfix release, skip this whole "merge develop to master" step (the "develop" branch is not involved until later).

Add Milestone to Pull Requests and Issues
-----------------------------------------

Often someone is making sure that the proper milestone (e.g. 5.10.1) is being applied to pull requests and issues, but sometimes this falls between the cracks.

Check for merged pull requests that have no milestone by going to https://github.com/IQSS/dataverse/pulls and entering `is:pr is:merged no:milestone <https://github.com/IQSS/dataverse/pulls?q=is%3Apr+is%3Amerged+no%3Amilestone>`_ as a query. If you find any, add the milestone to the pull request and any issues it closes. This includes the "merge develop into master" pull request above.

(Optional) Test Docker Images
-----------------------------

After the "master" branch has been updated and the GitHub Action to build and push Docker images has run (see `PR #9776 <https://github.com/IQSS/dataverse/pull/9776>`_), go to https://hub.docker.com/u/gdcc and make sure the "latest" tag for the following images has been updated:

- https://hub.docker.com/r/gdcc/base
- https://hub.docker.com/r/gdcc/dataverse
- https://hub.docker.com/r/gdcc/configbaker

TODO: Get https://github.com/gdcc/api-test-runner working.

.. _build-guides:

Build the Guides for the Release
--------------------------------

Go to https://jenkins.dataverse.org/job/guides.dataverse.org/ and make the following adjustments to the config:

- Repository URL: ``https://github.com/IQSS/dataverse.git``
- Branch Specifier (blank for 'any'): ``*/master``
- ``VERSION`` (under "Build Steps"): bump to the next release. Don't prepend a "v". Use ``5.10.1`` (for example)

Click "Save" then "Build Now".

Make sure the guides directory appears in the expected location such as https://guides.dataverse.org/en/5.10.1/

As described below, we'll soon point the "latest" symlink to that new directory.

Create a Draft Release on GitHub
--------------------------------

Go to https://github.com/IQSS/dataverse/releases/new to start creating a draft release.

- Under "Choose a tag" you will be creating a new tag. Have it start with a "v" such as ``v5.10.1``. Click "Create new tag on publish".
- Under "Target", choose "master". This commit will appear in ``/api/info/version`` from a running installation.
- Under "Release title" use the same name as the tag such as ``v5.10.1``.
- In the description, copy and paste the content from the release notes .md file created in the "Write Release Notes" steps above.
- Click "Save draft" because we do not want to publish the release yet.

At this point you can send around the draft release for any final feedback. Links to the guides for this release should be working now, since you build them above.

Make corrections to the draft, if necessary. It will be out of sync with the .md file, but that's ok (`#7988 <https://github.com/IQSS/dataverse/issues/7988>`_ is tracking this).

.. _run-build-create-war:

Run a Build to Create the War File
----------------------------------

ssh into the dataverse-internal server and undeploy the current war file.

Go to https://jenkins.dataverse.org/job/IQSS_Dataverse_Internal/ and make the following adjustments to the config:

- Repository URL: ``https://github.com/IQSS/dataverse.git``
- Branch Specifier (blank for 'any'): ``*/master``
- Execute shell: Update version in filenames to ``dataverse-5.10.1.war`` (for example)

Click "Save" then "Build Now".

This will build the war file, and then automatically deploy it on dataverse-internal. Verify that the application has deployed successfully. 

The build number will appear in ``/api/info/version`` (along with the commit mentioned above) from a running installation (e.g. ``{"version":"5.10.1","build":"907-b844672``). 

Note that the build number comes from the following script in an early Jenkins build step...

.. code-block:: bash

  COMMIT_SHA1=`echo $GIT_COMMIT | cut -c-7`
  echo "build.number=${BUILD_NUMBER}-${COMMIT_SHA1}" > $WORKSPACE/src/main/java/BuildNumber.properties

... but we can explore alternative methods of specifying the build number, as described in :ref:`auto-custom-build-number`.

Build Installer (dvinstall.zip)
-------------------------------

ssh into the dataverse-internal server and do the following:

- In a git checkout of the dataverse source switch to the master branch and pull the latest.
- Copy the war file from the previous step to the ``target`` directory in the root of the repo (create it, if necessary):
- ``mkdir target``
- ``cp /tmp/dataverse-5.10.1.war target``
- ``cd scripts/installer``
- ``make clean``
- ``make``

A zip file called ``dvinstall.zip`` should be produced.

Alternatively, you can build the installer on your own dev. instance. But make sure you use the war file produced in the step above, not a war file build from master on your own system! That's because we want the released application war file to contain the build number described above. Download the war file directly from Jenkins, or from dataverse-internal. 

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

ssh into the guides server and update the symlink to point to the latest release, as in the example below.

.. code-block:: bash

  cd /var/www/html/en
  ln -s 5.10.1 latest

This step could be done before publishing the release if you'd like to double check that links in the release notes work.

Close Milestone on GitHub and Create a New One
----------------------------------------------

You can find our milestones at https://github.com/IQSS/dataverse/milestones

Now that we've published the release, close the milestone and create a new one for the **next** release, the release **after** the one we're working on, that is.

Note that for milestones we use just the number without the "v" (e.g. "5.10.1").

On the project board at https://github.com/orgs/IQSS/projects/34 edit the tab (view) that shows the milestone to show the next milestone.

.. _base_image_post_release:

Update the Container Base Image Version Property
------------------------------------------------

|dedicated|

Create a new branch (any name is fine but ``prepare-next-iteration`` is suggested) and update the following files to prepare for the next development cycle:

- modules/dataverse-parent/pom.xml -> ``<profiles>`` -> profile "ct" -> ``<properties>`` -> Set ``<base.image.version>`` to ``${parsedVersion.majorVersion}.${parsedVersion.nextMinorVersion}``

Create a pull request and put it through code review, like usual. Give it a milestone of the next release, the one **after** the one we're working on. Once the pull request has been approved, merge it. It should be the first PR merged of the next release.

For more background, see :ref:`base-image-supported-tags`. For an example, see https://github.com/IQSS/dataverse/pull/10896

For hotfix, we will do this later. See below.

Lift the Code Freeze and Encourage Developers to Update Their Branches
----------------------------------------------------------------------

It's now safe to lift the code freeze. We can start merging pull requests into the "develop" branch for the next release.

Let developers know that they should merge the latest from the "develop" branch into any branches they are working on.

For a hotfix, there is no freeze to lift but soon we'll break the bad news to them if we had to rename SQL scripts. See below.

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

Make a new branch off the hotfix branch. You can call it something like "6.7.1-hotfix-to-develop".

In that branch, do the :ref:`base_image_post_release` step you skipped above. Now is the time.

Create a pull request against develop. Merge conflicts are possible and this pull request should go through review and QA like normal. Afterwards it's fine to delete this branch and the hotfix branch that was merged into master.

For Hotfixes, Rename SQL Scripts
--------------------------------

Because we have merged a version bump from the hotfix into the "develop" branch, any SQL scripts in the "develop" branch should be renamed (from "5.11.0" to "5.11.1" for example). (To read more about our naming conventions for SQL scripts, see :doc:`sql-upgrade-scripts`.) Look at ``src/main/resources/db/migration`` in the "develop" branch and if any SQL scripts have the wrong version, make a pull request to update them (all at once in a single PR is fine). Tell developers and QA to look at open pull requests and to rename SQL scripts that have the wrong version.
