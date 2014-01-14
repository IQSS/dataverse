====================================
DVN Developers Guide
====================================

Please note: This guide was updated in October 2013 to reflex the switch
from Ant to Maven in DVN 3.6.1.

Build Environment (Configuring NetBeans)
++++++++++++++++++++++++++++++++++++++++

This chapter describes setting up the build environment that you will
need to build the DVN application from source code. 

Install NetBeans and GlassFish
==============================

As of DVN version 3.6.1 and the switch to Maven, a DVN development
environment should not have any dependency on a particular IDE, but use
of NetBeans 7.2.1 is encouraged because it's the version used by most of
the current developers (on Mac OS X).

The NetBeans project is currently offering an installer bundle that
contains both NetBeans 7.2.1 and a supported version of GlassFish
(3.1.2.2). If they choose to discontinue the bundle, you will have to
download and install the two packages separately. Note that you can have
multiple versions of both NetBeans and GlassFish on your system.

Please note: While we intend to investigate NetBeans 7.4 and GlassFish
4, these are not yet known to provide a suitable development
environment.

We strongly recommend that you run both installs **as a regular user**. There's no reason to run your development environment as root.

Install NetBeans bundle
-----------------------

Download NetBeans 7.2.1 Java EE + GlassFish Open Source Edition 3.1.2.2
bundle from https://netbeans.org/downloads/7.2.1

For Mac OS X, you will download a .dmg disk image that will open
automatically and start the installer for you. Choose the typical
installation but be sure to install GlassFish and JUnit when prompted.

Note that you don't have to uninstall your existing NetBeans version.
You can have as many versions installed as you need in parallel.

When you start NetBeans 7.2.1 for the first time, you will be asked if
you want to import the settings from the previous installations. If you
have an existing, pre-DVN 3.\* development environment on your system, 
**answer "no" -- we want to create the new configuration from scratch.**

[If you have to] Install GlassFish 3.1.2.2
------------------------------------------

We **strongly** recommend that you install GlassFish Server 3.1.2.2,
Open Source Edition, **Full Platform**. If you have to install it
separately from NetBeans, it can be obtained from
http://glassfish.java.net/downloads/3.1.2.2-final.html

The page above contains a link to the installation instructions, but the
process is very straightforward - just download and run the installer.

It is strongly recommended that you use Sun/Oracle Java JDK version 1.6.
Please make sure you have the newest (or at least, recent) build number
available for your platform. (On Mac OS X 10.8, since the JDK can be
installed as part of OS distribution, the version currently provided by
Apple should be sufficient). In other words, we do not recommend
building DVN under JDK 1.7 until the ticket regarding the move from Java
6 to 7 has been closed: https://redmine.hmdc.harvard.edu/issues/3306

Note that you don't have to uninstall older versions of GlassFish you
may still have around. It's ok to have multiple versions installed. But
make sure you have the 3.1.2.2 installation selected as the active
server in NetBeans.

**Important:** During the installation, leave the admin password fields
blank. This is not a security risk since out of the box, GlassFish
3.1.2.2 will only be accepting admin connections on the localhost
interface. Choosing a password at this stage, however, will complicate
the installation process unnecessarily. Since this is a development
system, you can probably keep this configuration unchanged (admin on
localhost only). If you need to be able to connect to the admin console
remotely, please see the note in the Appendix section of the main
Installers Guide.

Install JUnit (if you haven't already)
--------------------------------------

Depending on how you installed NetBeans, you might already have JUnit
installed. JUnit can be installed from Tools -> Plugins.

Check out a new copy of the DVN source tree
===========================================

Create a GitHub account [if you don't have one already]
-------------------------------------------------------

Sign up at https://github.com

Please note that primary audience of this guide (for now) is people who
have push access to https://github.com/IQSS/dvn . If you do not have
push access and want to contribute (and we hope you do!) please fork the
repo per https://help.github.com/articles/fork-a-repo and make
adjustments below when cloning the repo.

Set up an ssh keypair (if you haven't already)
-----------------------------------------------------

You *can* use git with passwords over HTTPS but it's much nicer to set
up SSH keys.

https://github.com/settings/ssh is the place to manage the ssh keys
GitHub knows about for you. That page also links to a nice howto:
https://help.github.com/articles/generating-ssh-keys

From the terminal, ``ssh-keygen`` will create new ssh keys for you:

-  private key: ``~/.ssh/id_rsa``

   -  It is **very important to protect your private key**. If someone
      else acquires it, they can access private repositories on GitHub
      and make commits as you! Ideally, you'll store your ssh keys on an
      encrypted volume and protect your private key with a password when
      prompted for one by ``ssh-keygen``. See also "Why do passphrases
      matter" at https://help.github.com/articles/generating-ssh-keys

-  public key: ``~/.ssh/id_rsa.pub``

After you've created your ssh keys, add the public key to your GitHub
account.

Clone the repo
--------------

Please see `branches <#branches>`__ for detail, but in short, the
"develop" branch is where new commits go. Below we will assume you want
to make commits to "develop".

In NetBeans, click Team, then Git, then Clone.

Remote Repository
*****************

-  Repository URL: ``github.com:IQSS/dvn.git``
-  Username: ``git``
-  Private/Public Key

   -  Private Key File: ``/Users/[YOUR_USERNAME]/.ssh/id_rsa``

-  Passphrase: (the passphrase you chose while running ``ssh-keygen``)

Click Next.

If you are prompted about the authenticity of github.com's RSA key fingerprint, answer "Yes" to continue connecting. GitHub's RSA key fingerprint is listed at https://help.github.com/articles/generating-ssh-keys

Remote Branches
***************

Under Select Remote Branches check the "develop" branch.

Please note: You may see other branches listed, such as "master", but
there is no need to check them out at this time.

Click Next.

Destination Directory
*********************

The defaults should be fine:

-  Parent Directory: ``/Users/[YOUR_USERNAME]/NetBeansProjects``
-  Clone Name: ``dvn``
-  Checkout Branch: ``develop*``
-  Remote Name: ``origin``

Click Finish.

You should see a message that 3 projects were cloned. Click "Open
Project".

Open Projects
=============

In the "Open Projects" dialog you should see three projects, DVN-lockss,
DVN-root, and DVN-web (a child of DVN-root).

Highlight DVN-root and check "Open Required" (to include DVN-web) and click "Open".

At this point, you should have two (and only two) projects open in
NetBeans: DVN-root and DVN-web. If you hover over the projects, it's
normal at this point to see warnings such as "Some dependency artifacts
are not in the local repository" or "Cannot find application server:
GlassFish Server 3+". We'll correct these next.

Build for the first time
========================

In NetBeans, right-click DVN-root and click "Build". This will download
many dependencies via Maven and may take several minutes.

When this process has completed, right-click DVN-web and click "Build".
You should expect to see "BUILD SUCCESS". This means you have
successfully built the .war application package, but do not attempt to
deploy the application just yet! We need to configure the server
environment first, which consists of GlassFish and PostgreSQL

Application Environment (Configuring GlassFish and PostgreSQL)
++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

In this chapter, we describe the process of setting up your own local
application environment into which you will deploy the DVN application. 

Install PostgreSQL database server 
==================================

For Mac OS X (our default development OS), you can get the installer
from http://www.postgresql.org/download/macosx

The installation is very straightforward; just make sure you answer
"yes" when asked if Postgres should be accepting network connections.
(The application will be accessing the database at the "localhost"
address). 

Once installed, we recommend that you also allow connections
over local Unix sockets. This way the installer won't have to ask you
for the Postgres password every time it needs to talk to the database.
To do so, modify the "local all all" line in the data/pg\_hba.conf file
to look like this:

| local all all trust

**Note** that this only opens Postgres to the local socket connections,
and should not be considered a security risk. But if you are extra
cautious, you may use instead:

| local all all ident sameuser

Restart Postgres for the changes to take effect!

Please note: if you have any problems with the PostgreSQL setup, please
ensure the right ``psql`` is in your ``$PATH``.

You can check the instructions in the main Installers Guide for more info:
:ref:`PostgreSQL section<postgresql>`;
but the above should be sufficient to get your environment set up.

Run the install-dev script
==========================

The installer is supplied with the DVN source in the tools directory.
You must run it as root (for direct access to Postgres).

| To run the script:
| ``sudo su -``
| ``cd /Users/[YOUR_USERNAME]/NetBeansProjects/dvn/tools/installer/dvninstall``

| then execute
| ``./install-dev``

When prompted for various settings, you will likely be able to accept
all the default values (in a development environment, they are for the
most part the same for everybody).

Testing login
=============

Once the ``install-dev`` script has completed successfully, you will
have a fully functional Dataverse Network server. After making sure
GlassFish has been started per the output of the script, you should be
able to log in DVN with these credentials:

- http://localhost:8080/dvn/
- username: networkAdmin
- password: networkAdmin

Please note that when deploying from NetBeans for the first time, you
will be prompted to select a deployment server. From the drop down,
select "GlassFish Server 3.1.2", click "Remember in Current IDE Session"
and click "OK". 

Developing with Git
++++++++++++++++


.. _commit:

Commit
==================

**Committing Changes**

By following the instructions in the :ref:`build <build>` step, you
should be in the "develop" branch, which is where we want to make
commits as we work toward the next release.

You can verify which branch you are on by clicking Team then "Repository
Browser".

You should see ``dvn [develop]`` at the root of the tree and **develop**
in bold under Branches -> Local

Click Team, then "Show Changes". Select the desired files and
right-click to commit.

To publish your changes on GitHub, you'll need to follow the next step:
:ref:`push <push>`.

.. _push:

Push
===========

**Pushing your commits to GitHub**

After making your :ref:`commit <commit>`, push it to GitHub by clicking
Team -> Remote -> Push, then Next (to use your configured remote
repository), then checking **develop** and Finish.

Your commit should now appear on GitHub in the develop branch:
https://github.com/IQSS/dvn/commits/develop

Your commit should **not** appear in the master branch on GitHub:
https://github.com/IQSS/dvn/commits/master . Not yet anyway. We only
merge commits into master when we are ready to release.  Please see the
`branches <#branches>`__ section for for detail.


Release
============

Merge develop into master
--------------------------------------

Tag the release
***************************

Here is an example of how the 3.4 tag (
`https://github.com/IQSS/dvn/tree/3.4 <https://github.com/IQSS/dvn/tree/3.4>`__) was created and pushed to GitHub:

.. code-block:: guess

    murphy:dvn pdurbin$ git branch
    * develop
      master
    murphy:dvn pdurbin$ git pull
    Already up-to-date.
    murphy:dvn pdurbin$ git checkout master
    Switched to branch 'master'
    murphy:dvn pdurbin$ git merge develop
    Updating fdbfe57..6ceb24f
    (snip)
     create mode 100644 tools/installer/dvninstall/readme.md
    murphy:dvn pdurbin$ git tag
    3.3
    murphy:dvn pdurbin$ git tag -a 3.4 -m 'merged develop, tagging master as 3.4'
    murphy:dvn pdurbin$ git tag
    3.3
    3.4
    murphy:dvn pdurbin$ git push origin 3.4
    Counting objects: 1, done.
    Writing objects: 100% (1/1), 182 bytes, done.
    Total 1 (delta 0), reused 0 (delta 0)
    To git@github.com:IQSS/dvn.git
     * [new tag]         3.4 -> 3.4
    murphy:dvn pdurbin$ 
    murphy:dvn pdurbin$ git push origin master
    Total 0 (delta 0), reused 0 (delta 0)
    To git@github.com:IQSS/dvn.git
       fdbfe57..6ceb24f  master -> master
    murphy:dvn pdurbin$ 

Make release available for download
******************************************************

On dvn-build:

.. code-block:: guess

    cd tools/installer
    make installer

Rename the resulting "dvninstall.zip" to include the release number
(i.e. "dvninstall\_v3\_4.zip") and upload it, the separate war file, a
readme, and a buildupdate script (all these files should include the
release number) to SourceForge (i.e.
`http://sourceforge.net/projects/dvn/files/dvn/3.4/ <http://sourceforge.net/projects/dvn/files/dvn/3.4/>`__).

Increment the version number
*******************************************************

The file to edit is:

| `https://github.com/IQSS/dvn/blob/develop/src/DVN-web/src/VersionNumber.properties <https://github.com/IQSS/dvn/blob/develop/src/DVN-web/sr/VersionNumber.properties>`__

Branches
===========

Current list of branches
-------------------------------------

`https://github.com/IQSS/dvn/branches <https://github.com/IQSS/dvn/branches>`__

New branching model: develop vs. master
-------------------------------------------------

Please note that with the move to git, we are adopting the branching
model described at
`http://nvie.com/posts/a-successful-git-branching-model/ <http://nvie.com/posts/a-successful-git-branching-model/>`__

In this branching model there are two persistent branches:

-  develop: where all new commits go
-  master: where code gets merged and tagged as a release

That is to say, **please make your commits on the develop branch, not
the master branch**.

Feature branches
------------------------

    "The essence of a feature branch is that it exists as long as the
    feature is in development, but will eventually be merged back into
    develop (to definitely add the new feature to the upcoming release)
    or discarded (in case of a disappointing experiment)." --
    `http://nvie.com/posts/a-successful-git-branching-model/ <http://nvie.com/posts/a-successful-git-branching-model/>`__

Example feature branch: 2656-lucene
---------------------------------------------------

First, we create the branch and check it out:

::

    murphy:dvn pdurbin$ git branch
      2656-solr
    * develop
    murphy:dvn pdurbin$ git branch 2656-lucene
    murphy:dvn pdurbin$ 
    murphy:dvn pdurbin$ git branch
      2656-lucene
      2656-solr
    * develop
    murphy:dvn pdurbin$ git checkout 2656-lucene
    Switched to branch '2656-lucene'
    murphy:dvn pdurbin$ 
    murphy:dvn pdurbin$ git status
    # On branch 2656-lucene
    nothing to commit (working directory clean)
    murphy:dvn pdurbin$ 

| Then, we make a change and a commit, and push it to:

| `https://github.com/iqss/dvn/tree/2656-lucene <https://github.com/iqss/dvn/tree/2656-lucene>`__ (creating a new remote branch):


::

    murphy:dvn pdurbin$ vim src/DVN-EJB/src/java/edu/harvard/iq/dvn/core/index/Indexer.java
    murphy:dvn pdurbin$ 
    murphy:dvn pdurbin$ git commit -m 'start lucene faceting branch' src/DVN-EJB/src/java/edu/harvard/iq/dvn/core/index/Indexer.java
    [2656-lucene 3b82f88] start lucene faceting branch
     1 file changed, 73 insertions(+), 2 deletions(-)
    murphy:dvn pdurbin$ 
    murphy:dvn pdurbin$ git push origin 2656-lucene
    Counting objects: 25, done.
    Delta compression using up to 8 threads.
    Compressing objects: 100% (10/10), done.
    Writing objects: 100% (13/13), 2.23 KiB, done.
    Total 13 (delta 6), reused 0 (delta 0)
    To git@github.com:IQSS/dvn.git
     * [new branch]      2656-lucene -> 2656-lucene
    murphy:dvn pdurbin$ 

| 

As we work on the feature branch, we merge the latest changes from
"develop". We want to resolve conflicts in the feature branch itself so
that the feature branch will merge cleanly into "develop" when we're
ready. In the example below, we use ``git mergetool`` and ``opendiff``
to resolve conflicts and save the merge. Then we push the newly-merged
2656-lucene feature branch to GitHub:

| 

::

    murphy:dvn pdurbin$ git branch
    * 2656-lucene
      2656-solr
      develop
    murphy:dvn pdurbin$ git checkout develop
    murphy:dvn pdurbin$ git branch
      2656-lucene
      2656-solr
    * develop
    murphy:dvn pdurbin$ git pull
    remote: Counting objects: 206, done.
    remote: Compressing objects: 100% (43/43), done.
    remote: Total 120 (delta 70), reused 96 (delta 46)
    Receiving objects: 100% (120/120), 17.65 KiB, done.
    Resolving deltas: 100% (70/70), completed with 40 local objects.
    From github.com:IQSS/dvn
       8fd223d..9967413  develop    -> origin/develop
    Updating 8fd223d..9967413
    Fast-forward
     .../admin/EditNetworkPrivilegesServiceBean.java  |    5 +-
    (snip)
     src/DVN-web/web/study/StudyFilesFragment.xhtml   |    2 +-
     12 files changed, 203 insertions(+), 118 deletions(-)
    murphy:dvn pdurbin$ murphy:dvn pdurbin$ git pull
    remote: Counting objects: 206, done.
    remote: Compressing objects: 100% (43/43), done.
    remote: Total 120 (delta 70), reused 96 (delta 46)
    Receiving objects: 100% (120/120), 17.65 KiB, done.
    Resolving deltas: 100% (70/70), completed with 40 local objects.
    From github.com:IQSS/dvn
       8fd223d..9967413  develop    -> origin/develop
    Updating 8fd223d..9967413
    Fast-forward
     .../admin/EditNetworkPrivilegesServiceBean.java  |    5 +-
    (snip)
     .../harvard/iq/dvn/core/web/study/StudyUI.java   |    2 +-
     src/DVN-web/web/HomePage.xhtml                   |    5 +-
    murphy:dvn pdurbin$ 
    murphy:dvn pdurbin$ git checkout 2656-lucene
    Switched to branch '2656-lucene'
    murphy:dvn pdurbin$ 
    murphy:dvn pdurbin$ 
    murphy:dvn pdurbin$ git merge develop
    Auto-merging src/DVN-web/web/BasicSearchFragment.xhtml
    CONFLICT (content): Merge conflict in src/DVN-web/web/BasicSearchFragment.xhtml
    Auto-merging src/DVN-web/src/edu/harvard/iq/dvn/core/web/BasicSearchFragment.java
    Auto-merging src/DVN-EJB/src/java/edu/harvard/iq/dvn/core/index/Indexer.java
    Automatic merge failed; fix conflicts and then commit the result.
    murphy:dvn pdurbin$ 
    murphy:dvn pdurbin$ git status
    # On branch 2656-lucene
    # Changes to be committed:
    #
    #       modified:   src/DVN-EJB/src/java/edu/harvard/iq/dvn/core/admin/EditNetworkPrivilegesServiceBean.java
    (snip)
    #       new file:   src/DVN-web/web/admin/ChooseDataverseForCreateStudy.xhtml
    #       modified:   src/DVN-web/web/study/StudyFilesFragment.xhtml
    #
    # Unmerged paths:
    #   (use "git add/rm <file>..." as appropriate to mark resolution)
    #
    #       both modified:      src/DVN-web/web/BasicSearchFragment.xhtml
    #
    murphy:dvn pdurbin$ git mergetool
    merge tool candidates: opendiff kdiff3 tkdiff xxdiff meld tortoisemerge gvimdiff diffuse ecmerge p4merge araxis bc3 emerge vimdiff
    Merging:
    src/DVN-web/web/BasicSearchFragment.xhtml

    Normal merge conflict for 'src/DVN-web/web/BasicSearchFragment.xhtml':
      {local}: modified file
      {remote}: modified file
    Hit return to start merge resolution tool (opendiff):
    murphy:dvn pdurbin$ 
    murphy:dvn pdurbin$ git add .
    murphy:dvn pdurbin$ 
    murphy:dvn pdurbin$ git commit -m "Merge branch 'develop' into 2656-lucene"
    [2656-lucene 519cd8c] Merge branch 'develop' into 2656-lucene
    murphy:dvn pdurbin$ 
    murphy:dvn pdurbin$ git push origin 2656-lucene
    (snip)
    murphy:dvn pdurbin$ 


| When we are ready to merge the feature branch back into the develop branch, we can do so.

| Here's an example of merging the 2656-lucene branch back into develop:

::

    murphy:dvn pdurbin$ git checkout 2656-lucene
    Switched to branch '2656-lucene'
    murphy:dvn pdurbin$ git pull
    Already up-to-date.
    murphy:dvn pdurbin$ git checkout develop
    Switched to branch 'develop'
    murphy:dvn pdurbin$ git pull
    Already up-to-date.
    murphy:dvn pdurbin$ git merge 2656-lucene
    Removing lib/dvn-lib-EJB/lucene-core-3.0.0.jar
    Merge made by the 'recursive' strategy.
     lib/dvn-lib-EJB/lucene-core-3.0.0.jar                                     |  Bin 1021623 -> 0 bytes
     lib/dvn-lib-EJB/lucene-core-3.5.0.jar                                     |  Bin 0 -> 1466301 bytes
     lib/dvn-lib-EJB/lucene-facet-3.5.0.jar                                    |  Bin 0 -> 293582 bytes
     src/DVN-EJB/src/java/edu/harvard/iq/dvn/core/index/DvnQuery.java          |  160 +++++++++++++++++++++++++++++++++++++++++++++++++++++++++
     src/DVN-EJB/src/java/edu/harvard/iq/dvn/core/index/IndexServiceBean.java  |   56 ++++++++++++++++++++
     src/DVN-EJB/src/java/edu/harvard/iq/dvn/core/index/IndexServiceLocal.java |   16 +++++-
     src/DVN-EJB/src/java/edu/harvard/iq/dvn/core/index/Indexer.java           |  432 +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++--
     src/DVN-EJB/src/java/edu/harvard/iq/dvn/core/index/ResultsWithFacets.java |   71 +++++++++++++++++++++++++
     src/DVN-web/src/SearchFieldBundle.properties                              |    4 +-
     src/DVN-web/src/edu/harvard/iq/dvn/core/web/AdvSearchPage.java            |   86 +++++++++++++++++++++++++++++++
     src/DVN-web/src/edu/harvard/iq/dvn/core/web/BasicSearchFragment.java      |  102 +++++++++++++++++++++++++++++++++++-
     src/DVN-web/src/edu/harvard/iq/dvn/core/web/StudyListing.java             |   11 ++++
     src/DVN-web/src/edu/harvard/iq/dvn/core/web/StudyListingPage.java         |  428 ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++-
     src/DVN-web/src/edu/harvard/iq/dvn/core/web/study/FacetResultUI.java      |   42 +++++++++++++++
     src/DVN-web/src/edu/harvard/iq/dvn/core/web/study/FacetUI.java            |   62 ++++++++++++++++++++++
     src/DVN-web/web/AdvSearchPage.xhtml                                       |    3 +-
     src/DVN-web/web/BasicSearchFragment.xhtml                                 |    9 ++--
     src/DVN-web/web/StudyListingPage.xhtml                                    |   43 +++++++++++-----
     18 files changed, 1500 insertions(+), 25 deletions(-)
     delete mode 100644 lib/dvn-lib-EJB/lucene-core-3.0.0.jar
     create mode 100644 lib/dvn-lib-EJB/lucene-core-3.5.0.jar
     create mode 100644 lib/dvn-lib-EJB/lucene-facet-3.5.0.jar
     create mode 100644 src/DVN-EJB/src/java/edu/harvard/iq/dvn/core/index/DvnQuery.java
     create mode 100644 src/DVN-EJB/src/java/edu/harvard/iq/dvn/core/index/ResultsWithFacets.java
     create mode 100644 src/DVN-web/src/edu/harvard/iq/dvn/core/web/study/FacetResultUI.java
     create mode 100644 src/DVN-web/src/edu/harvard/iq/dvn/core/web/study/FacetUI.java
    murphy:dvn pdurbin$ 
    murphy:dvn pdurbin$ git status
    # On branch develop
    # Your branch is ahead of 'origin/develop' by 68 commits.
    #
    nothing to commit (working directory clean)
    murphy:dvn pdurbin$ 
    murphy:dvn pdurbin$ git push
    Counting objects: 51, done.
    Delta compression using up to 8 threads.
    Compressing objects: 100% (12/12), done.
    Writing objects: 100% (19/19), 1.41 KiB, done.
    Total 19 (delta 7), reused 0 (delta 0)
    To git@github.com:IQSS/dvn.git
       b7fae01..2b88b68  develop -> develop
    murphy:dvn pdurbin$ 

Switching to the master branch to merge commits from the develop branch
-------------------------------------------------------------------------------------------------------

We should really only need to switch from the develop branch to the
master branch as we prepare for a release.

First, we check out the master branch by clicking Team -> Git -> Branch
-> Switch to Branch.

Change Branch to "origin/master" and check the box for "Checkout as New
Branch" and fill in "master" as the "Branch Name" to match the name of
the branch we're switching to. Then click "Switch".

Now, in the Git Repository Browser (from Team -> Repository Browser) the
root of the tree should say ``dvn [master]`` and you should see two
branches under Branches -> Local. **master** should be in bold and
develop should not.

Tips
=========

Previewing changes before a pull
--------------------------------

If the build fails overnight you may want to hold off on doing a pull
until the problem is resolved. To preview what has changed since your
last pull, you can do a ``git fetch`` (the first part of a pull) then
``git log HEAD..origin/develop`` to see the commit messages.
``git log -p`` or ``git diff`` will allow you to see the contents of the
changes:

::

    git checkout develop
    git fetch
    git log HEAD..origin/develop
    git log -p HEAD..origin/develop
    git diff HEAD..origin/develop

After the build is working again, you can simply do a pull as normal.

Errors
===========

Duplicate class
---------------

The error "duplicate class" can result whenever you resolve a merge
conflict in git.

The fix is to close NetBeans and delete (or move aside) the cache like
this:

::

    cd ~/Library/Caches/NetBeans
    mv 7.2.1 7.2.1.moved

According to https://netbeans.org/bugzilla/show_bug.cgi?id=197983 this might be fixed in NetBeans 7.3.
