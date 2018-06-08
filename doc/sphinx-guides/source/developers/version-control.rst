==================
Version Control
==================

The Dataverse Project uses git for version control and GitHub for hosting. On this page we'll explain where to find the code, our branching strategey, advice on how to make a pull request, and other git tips.

.. contents:: |toctitle|
	:local:


Where to Find the Dataverse Code
--------------------------------

The main Dataverse code at https://github.com/IQSS/dataverse but as explained in the :doc:`intro` section under "Related Projects", there are many other code bases you can hack on if you wish!

Branching Strategy
------------------

Goals
~~~~~

The goals of the Dataverse branching strategy are:

- allow for concurrent development
- only ship stable code

We follow a simplified "git flow" model described at http://nvie.com/posts/a-successful-git-branching-model/ involving a "master" branch, a "develop" branch, and feature branches such as "1234-bug-fix".

Branches
~~~~~~~~

The "master" Branch
*******************

The "`master <https://github.com/IQSS/dataverse/tree/master>`_" branch represents released versions of Dataverse. As mentioned in the :doc:`making-releases` section, at release time we update the master branch to include all the code for that release. Commits are never made directly to master. Rather, master is updated only when we merge code into it from the "develop" branch.

The "develop" Branch
********************

The "`develop <https://github.com/IQSS/dataverse>`_" branch represents code that was stable enough to merge from a "feature" branch (described below) and that will make it into the next release. Like master, commits are never made to the develop branch. The develop branch is where integration occurs. Your goal is have your code merged into the develop branch after it has been reviewed.

Feature Branches
****************

Feature branches are used for both developing features and fixing bugs. They are named after the GitHub issue they are meant to address, so create a GitHub issue if you need to.

"3728-doc-apipolicy-fix" is an example of a fine name for your feature branch. It tells us that you are addressing https://github.com/IQSS/dataverse/issues/3728 and the "slug" is short, descriptive, and starts with the issue number.

How to Make a Pull Request
--------------------------

Pull requests take all shapes and sizes, from a one-character typo fix to hundreds of files changing at once. Generally speaking, smaller pull requests are better so that they are easier to code review. That said, don't hold back on writing enough code or documentation to address the issue to the best of your ability.

If you are writing code (rather than documentation), please see :doc:`testing` for guidance on writing tests.

The example of creating a pull request below has to do with fixing an important issue with the :doc:`documentation` but applies to fixing code as well.

Find or Create a GitHub Issue
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

For guidance on which issue to work on, please ask! Also, see https://github.com/IQSS/dataverse/blob/develop/CONTRIBUTING.md

Let's say you want to tackle https://github.com/IQSS/dataverse/issues/3728 which points out a typo in a page of Dataverse's documentation.

If you tell us your GitHub username we are happy to add you to the "read only" team at https://github.com/orgs/IQSS/teams/dataverse-readonly/members so that we can assign the issue to you while you're working on it. You can also tell us if you'd like to be added to the `Dataverse Community Contributors spreadsheet <https://docs.google.com/spreadsheets/d/1o9DD-MQ0WkrYaEFTD5rF_NtyL8aUISgURsAXSL7Budk/edit?usp=sharing>`_ and the `Dev Efforts by the Dataverse Community spreadsheet <https://groups.google.com/d/msg/dataverse-community/X2diSWYll0w/ikp1TGcfBgAJ>`_.

Create a New Branch off the develop Branch
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Always create your feature branch from the latest code in develop, pulling the latest code if necessary. As mentioned above, your branch should have a name like "3728-doc-apipolicy-fix" that starts with the issue number you are addressing, and ends with a short, descriptive name. Dashes ("-") and underscores ("_") in your branch name are ok, but please try to avoid other special characters such as ampersands ("&") than have special meaning in Unix shells.

Commit Your Change to Your New Branch
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Making a commit (or several commits) to that branch. Ideally the first line of your commit message includes the number of the issue you are addressing, such as ``Fixed BlockedApiPolicy #3728``.

Push Your Branch to GitHub
~~~~~~~~~~~~~~~~~~~~~~~~~~

Push your feature branch to your fork of Dataverse. Your git command may look something like ``git push origin 3728-doc-apipolicy-fix``.

Make a Pull Request
~~~~~~~~~~~~~~~~~~~

Make a pull request to get approval to merge your changes into the develop branch. Feedback on the pull request template we use is welcome! The "connects to #3728" syntax is important because it's used at https://waffle.io/IQSS/dataverse to associate pull requests with issues.

Here's an example of a pull request for issue #3728: https://github.com/IQSS/dataverse/pull/3827

Make Sure Your Pull Request Has Been Advanced to Code Review
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Now that you've made your pull request, your goal is to make sure it appears in the "Code Review" column at https://waffle.io/IQSS/dataverse 

Look at https://github.com/IQSS/dataverse/blob/master/CONTRIBUTING.md for various ways to reach out to developers who have enough access to the GitHub repo to move your issue and pull request to the "Code Review" column.

How to Resolve Conflicts in Your Pull Request
---------------------------------------------

Unfortunately, pull requests can quickly become "stale" and unmergable as other pull requests are merged into the develop branch ahead of you. This is completely normal, and often occurs because other developers made their pull requests before you did.

The Dataverse team may ping you to ask you to merge the latest from the develop branch into your branch and resolve merge conflicts. If this sounds daunting, please just say so and we will assist you.

If you'd like to resolve the merge conflicts yourself, here are some steps to do so that make use of GitHub Desktop and Netbeans.

**In GitHub Desktop:**

1. Sync from develop.
2. Open the specific branch that's having the merge conflict.
3. Click "Update from develop".

**In Netbeans:**

4. Click Window -> Favorites and open your local Dataverse project folder in the Favorites panel.
5. In this file browser, you can follow the red cylinder icon to find files with merge conflicts.
6. Double click the red merge conflicted file.
7. Right click on the red tab for that file and select Git -> Resolve Conflicts.
8. Resolve on right or left (if you select "both" you can do finer edits after).
9. Save all changes

**In GitHub Desktop:**

10. Commit the merge (append issue number to end, e.g. #3728) and leave note about what was resolved.

**In GitHub Issues:**

11. Leave a comment for the Dataverse team that you have resolved the merge conflicts.

Adding Commits to a Pull Request from a Fork 
--------------------------------------------

By default, when a pull request is made from a fork, "Allow edits from maintainers" is checked as explained at https://help.github.com/articles/allowing-changes-to-a-pull-request-branch-created-from-a-fork/

This is a nice feature of GitHub because it means that the core dev team for Dataverse can make small (or even large) changes to a pull request from a contributor to help the pull request along on its way to QA and being merged.

GitHub documents how to make changes to a fork at https://help.github.com/articles/committing-changes-to-a-pull-request-branch-created-from-a-fork/ but as of this writing the steps involve making a new clone of the repo. This works but you might find it more convenient to add a "remote" to your existing clone. The example below uses the fork at https://github.com/OdumInstitute/dataverse and the branch ``4709-postgresql_96`` but the technique can be applied to any fork and branch:

.. code-block:: bash

        git remote add OdumInstitute git@github.com:OdumInstitute/dataverse.git
        git fetch OdumInstitute
        git checkout 4709-postgresql_96
        vim path/to/file.txt
        git commit
        git push OdumInstitute 4709-postgresql_96

----

Previous: :doc:`troubleshooting` | Next: :doc:`testing`
