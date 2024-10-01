==================
Version Control
==================

The Dataverse Project uses git for version control and GitHub for hosting. On this page we'll explain where to find the code, our branching strategy, advice on how to make a pull request, and other git tips.

.. contents:: |toctitle|
	:local:


Where to Find the Dataverse Software Code
-----------------------------------------

The main Dataverse Software code is available at https://github.com/IQSS/dataverse but as explained in the :doc:`intro` section under "Related Projects", there are many other code bases you can hack on if you wish!

Branching Strategy
------------------

Goals
~~~~~

The goals of the Dataverse Software branching strategy are:

- allow for concurrent development
- only ship stable code

We follow a simplified "git flow" model described at https://nvie.com/posts/a-successful-git-branching-model/ involving a "master" branch, a "develop" branch, and feature branches such as "1234-bug-fix".

Branches
~~~~~~~~

The "master" Branch
*******************

The "`master <https://github.com/IQSS/dataverse/tree/master>`_" branch represents released versions of the Dataverse Software. As mentioned in the :doc:`making-releases` section, at release time we update the master branch to include all the code for that release. Commits are never made directly to master. Rather, master is updated only when we merge code into it from the "develop" branch.

.. _develop-branch:

The "develop" Branch
********************

The "`develop <https://github.com/IQSS/dataverse>`_" branch represents code that was stable enough to merge from a "feature" branch (described below) and that will make it into the next release. Like master, commits are never made to the develop branch. The develop branch is where integration occurs. Your goal is have your code merged into the develop branch after it has been reviewed.

Feature Branches
****************

Feature branches are used for both developing features and fixing bugs. They are named after the GitHub issue they are meant to address, so create a GitHub issue if you need to.

"3728-doc-apipolicy-fix" is an example of a fine name for your feature branch. It tells us that you are addressing https://github.com/IQSS/dataverse/issues/3728 and the "slug" is short, descriptive, and starts with the issue number.

Hotfix Branches
***************

Hotfix branches are described under :doc:`making-releases`.

.. _how-to-make-a-pull-request:

How to Make a Pull Request
--------------------------

Pull requests take all shapes and sizes, from a one-character typo fix to hundreds of files changing at once. Generally speaking, smaller pull requests are better so that they are easier to code review. That said, don't hold back on writing enough code or documentation to address the issue to the best of your ability.

If you are writing code (rather than documentation), please see :doc:`testing` for guidance on writing tests.

The example of creating a pull request below has to do with fixing an important issue with the documentation but applies to fixing code as well.

Find or Create a GitHub Issue
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

An issue represents a bug (unexpected behavior) or a new feature in Dataverse. We'll use the issue number in the branch we create for our pull request.

.. _finding-github-issues-to-work-on:

Finding GitHub Issues to Work On
********************************

Assuming this is your first contribution to Dataverse, you should start with something small. The following issue labels might be helpful in your search:

- `good first issue <https://github.com/IQSS/dataverse/labels/good%20first%20issue>`_ (these appear at https://github.com/IQSS/dataverse/contribute )
- `hacktoberfest <https://github.com/IQSS/dataverse/labels/hacktoberfest>`_
- `Help Wanted: Code <https://github.com/IQSS/dataverse/labels/Help%20Wanted%3A%20Code>`_
- `Help Wanted: Documentation <https://github.com/IQSS/dataverse/labels/Help%20Wanted%3A%20Documentation>`_

For guidance on which issue to work on, please ask! :ref:`getting-help-developers` explains how to get in touch.

Creating GitHub Issues to Work On
*********************************

You are very welcome to create a GitHub issue to work on. However, for significant changes, please reach out (see :ref:`getting-help-developers`) to make sure the team and community agree with the proposed change.

For small changes and especially typo fixes, please don't worry about reaching out first.

Communicate Which Issue You Are Working On
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

In the issue you can simply leave a comment to say you're working on it.

If you tell us your GitHub username we are happy to add you to the "read only" team at https://github.com/orgs/IQSS/teams/dataverse-readonly/members so that we can assign the issue to you while you're working on it. You can also tell us if you'd like to be added to the `Dataverse Community Contributors spreadsheet <https://docs.google.com/spreadsheets/d/1o9DD-MQ0WkrYaEFTD5rF_NtyL8aUISgURsAXSL7Budk/edit?usp=sharing>`_.

.. _create-branch-for-pr:

Create a New Branch Off the develop Branch
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Always create your feature branch from the latest code in develop, pulling the latest code if necessary. As mentioned above, your branch should have a name like "3728-doc-apipolicy-fix" that starts with the issue number you are addressing (e.g. `#3728 <https://github.com/IQSS/dataverse/issues/3728>`_) and ends with a short, descriptive name. Dashes ("-") and underscores ("_") in your branch name are ok, but please try to avoid other special characters such as ampersands ("&") that have special meaning in Unix shells. Please do not call your branch "develop" as it can cause maintainers :ref:`trouble <develop-into-develop>`.

Commit Your Change to Your New Branch
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

For each commit to that branch, try to include the issue number along with a summary in the first line of the commit message, such as ``Fixed BlockedApiPolicy #3728``. You are welcome to write longer descriptions in the body as well!

.. _writing-release-note-snippets:

Writing a Release Note Snippet
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

We highly value your insight as a contributor when in comes to describing your work in our release notes. Not every pull request will be mentioned in release notes but most are.

As described at :ref:`write-release-notes`, at release time we compile together release note "snippets" into the final release notes.

Here's how to add a release note snippet to your pull request:

- Create a Markdown file under ``doc/release-notes``. You can reuse the name of your branch and append ".md" to it, e.g. ``3728-doc-apipolicy-fix.md``
- Edit the snippet to include anything you think should be mentioned in the release notes. Please include the following if they apply:

  - Descriptions of new features or bug fixed, including a link to the HTML preview of the docs you wrote (e.g. https://dataverse-guide--9939.org.readthedocs.build/en/9939/installation/config.html#smtp-email-configuration ) and the phrase "For more information, see #3728" (the issue number). If you know the PR number, you can add that too.
  - New configuration settings
  - Upgrade instructions
  - Etc.

Release note snippets do not need to be long. For a new feature, a single line description might be enough. Please note that your release note will likely be edited (expanded or shortened) when the final release notes are being created.

Push Your Branch to GitHub
~~~~~~~~~~~~~~~~~~~~~~~~~~

Push your feature branch to your fork of the Dataverse Software. Your git command may look something like ``git push origin 3728-doc-apipolicy-fix``.

Make a Pull Request
~~~~~~~~~~~~~~~~~~~

Make a pull request to get approval to merge your changes into the develop branch.

Feedback on the pull request template we use is welcome!

Here's an example of a pull request for issue #9729: https://github.com/IQSS/dataverse/pull/10474

Replace Issue with Pull Request
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

If the pull request closes an issue that has been prioritized, someone from the core team will do the following:

- Move the open issue to the "Done" column of the `project board`_. We do this to track only one card, the pull request, on the project board. Merging the pull request will close the issue because we use the "closes #1234" `keyword <https://docs.github.com/en/issues/tracking-your-work-with-issues/linking-a-pull-request-to-an-issue>`_ .
- Copy all labels from the issue to the pull request with the exception of the "size" label.
- Add a size label to the pull request that reflects the amount of review and QA time needed.
- Move the pull request to the "Ready for Review" column.

.. _project board: https://github.com/orgs/IQSS/projects/34

Make Sure Your Pull Request Has Been Advanced to Code Review
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Now that you've made your pull request, your goal is to make sure it appears in the "Code Review" column on the `project board`_.

Look at :ref:`getting-help-developers` for various ways to reach out to developers who have enough access to the GitHub repo to move your issue and pull request to the "Code Review" column.

Summary of Git commands
~~~~~~~~~~~~~~~~~~~~~~~

This section provides sequences of Git commands for three scenarios:

* preparing the first request, when the IQSS Dataverse Software repository and the forked repository are identical
* creating an additional request after some time, when the IQSS Dataverse Software repository is ahead of the forked repository
* while your pull requests are in review the develop branch has been updated, so you have to keep your code base synchronized with the current state of develop branch

In the examples we use 123-COOL-FEATURE as the name of the feature branch, and https://github.com/YOUR_NAME/dataverse.git as your forked repository's URL. In practice modify both accordingly.

**1st scenario: preparing the first pull request**

.. code-block:: bash

        # clone Dataverse at Github.com ... then

        git clone https://github.com/YOUR_NAME/dataverse.git dataverse_fork
        cd dataverse_fork

        # create a new branch locally for the pull request
        git checkout -b 123-COOL-FEATURE

        # working on the branch ... then commit changes
        git commit -am "#123 explanation of changes"

        # upload the new branch to https://github.com/YOUR_NAME/dataverse
        git push -u origin 123-COOL-FEATURE

        # ... then create pull request at github.com/YOUR_NAME/dataverse


**2nd scenario: preparing another pull request some month later**

.. code-block:: bash

        # register IQSS Dataverse repo
        git remote add upstream https://github.com/IQSS/dataverse.git

        git checkout develop

        # update local develop branch from https://github.com/IQSS/dataverse
        git fetch upstream develop
        git rebase upstream/develop

        # update remote develop branch at https://github.com/YOUR_NAME/dataverse
        git push

        # create a new branch locally for the pull request
        git checkout -b 123-COOL-FEATURE

        # work on the branch and commit changes
        git commit -am "#123 explanation of changes"

        # upload the new branch to https://github.com/YOUR_NAME/dataverse
        git push -u origin 123-COOL-FEATURE

        # ... then create pull request at github.com/YOUR_NAME/dataverse


**3rd scenario: synchronize your branch with develop branch**

.. code-block:: bash

        git checkout develop

        # update local develop branch from https://github.com/IQSS/dataverse
        git fetch upstream develop
        git rebase upstream/develop

        # update remote develop branch at https://github.com/YOUR_NAME/dataverse
        git push

        # change to the already existing feature branch
        git checkout 123-COOL-FEATURE

        # merge changes of develop to the feature branch
        git merge develop

        # check if there are conflicts, if there are follow the next command, otherwise skip to next block
        # 1. fix the relevant files (including testing)
        # 2. commit changes
        git add <fixed files>
        git commit

        # update remote feature branch at https://github.com/YOUR_NAME/dataverse
        git push


How to Resolve Conflicts in Your Pull Request
---------------------------------------------

Unfortunately, pull requests can quickly become "stale" and unmergable as other pull requests are merged into the develop branch ahead of you. This is completely normal, and often occurs because other developers made their pull requests before you did.

The Dataverse Project team may ping you to ask you to merge the latest from the develop branch into your branch and resolve merge conflicts. If this sounds daunting, please just say so and we will assist you.

If you'd like to resolve the merge conflicts yourself, here are some steps to do so that make use of GitHub Desktop and Netbeans.

**In GitHub Desktop:**

1. Sync from develop.
2. Open the specific branch that's having the merge conflict.
3. Click "Update from develop".

**In Netbeans:**

4. Click Window -> Favorites and open your local Dataverse Software project folder in the Favorites panel.
5. In this file browser, you can follow the red cylinder icon to find files with merge conflicts.
6. Double click the red merge conflicted file.
7. Right click on the red tab for that file and select Git -> Resolve Conflicts.
8. Resolve on right or left (if you select "both" you can do finer edits after).
9. Save all changes

**In GitHub Desktop:**

10. Commit the merge (append issue number to end, e.g. #3728) and leave note about what was resolved.

**In GitHub Issues:**

11. Leave a comment for the Dataverse Project team that you have resolved the merge conflicts.

Adding Commits to a Pull Request from a Fork 
--------------------------------------------

By default, when a pull request is made from a fork, "Allow edits from maintainers" is checked as explained at https://help.github.com/articles/allowing-changes-to-a-pull-request-branch-created-from-a-fork/

This is a nice feature of GitHub because it means that the core dev team for the Dataverse Project can make small (or even large) changes to a pull request from a contributor to help the pull request along on its way to QA and being merged.

GitHub documents how to make changes to a fork at https://help.github.com/articles/committing-changes-to-a-pull-request-branch-created-from-a-fork/ but as of this writing the steps involve making a new clone of the repo. This works but you might find it more convenient to add a "remote" to your existing clone. The example below uses the fork at https://github.com/OdumInstitute/dataverse and the branch ``4709-postgresql_96`` but the technique can be applied to any fork and branch:

.. code-block:: bash

        git remote add OdumInstitute git@github.com:OdumInstitute/dataverse.git
        git fetch OdumInstitute
        git checkout 4709-postgresql_96
        vim path/to/file.txt
        git commit
        git push OdumInstitute 4709-postgresql_96

.. _develop-into-develop:

Handing a Pull Request from a "develop" Branch
----------------------------------------------

Note: this is something only maintainers of Dataverse need to worry about, typically.

From time to time a pull request comes in from a fork of Dataverse that uses "develop" as the branch behind the PR. (We've started asking contributors not to do this. See :ref:`create-branch-for-pr`.) This is problematic because the "develop" branch is the main integration branch for the project. (See :ref:`develop-branch`.)

If the PR is perfect and can be merged as-is, no problem. Just merge it. However, if you would like to push commits to the PR, you are likely to run into trouble with multiple "develop" branches locally.

The following is a list of commands oriented toward the simple task of merging the latest from the "develop" branch into the PR but the same technique can be used to push other commits to the PR as well. In this example the PR is coming from username "coder123" on GitHub. At a high level, what we're doing is working in a safe place (/tmp) away from our normal copy of the repo. We clone the main repo from IQSS, check out coder123's version of "develop" (called "dev2" or "false develop"), merge the real "develop" into it, and push to the PR.

If there's a better way to do this, please get in touch!

.. code-block:: bash

        # do all this in /tmp away from your normal code
        cd /tmp
        git clone git@github.com:IQSS/dataverse.git
        cd dataverse
        git remote add coder123 git@github.com:coder123/dataverse.git
        git fetch coder123
        # check out coder123's "develop" to a branch with a different name ("dev2")
        git checkout coder123/develop -b dev2
        # merge IQSS "develop" into coder123's "develop" ("dev2")
        git merge origin/develop
        # delete the IQSS "develop" branch locally (!)
        git branch -d develop
        # checkout "dev2" (false "develop") as "develop" for now
        git checkout -b develop
        # push the false "develop" to coder123's fork (to the PR)
        git push coder123 develop 
        cd ..
        # delete the tmp space (done! \o/)
        rm -rf /tmp/dataverse
