=======================
Making Library Releases
=======================

.. contents:: |toctitle|
	:local:

Introduction
------------

Note: See :doc:`making-releases` for Dataverse itself.

We release Java libraries to Maven Central that are used by Dataverse (and perhaps `other <https://github.com/gdcc/xoai/issues/141>`_ `software <https://github.com/gdcc/xoai/issues/170>`_!):

- https://central.sonatype.com/namespace/org.dataverse
- https://central.sonatype.com/namespace/io.gdcc

We release JavaScript/TypeScript libraries to npm:

- https://www.npmjs.com/package/@iqss/dataverse-design-system

Maven Central (Java)
--------------------

From the perspective of the Maven Central, we are both `producers <https://central.sonatype.org/publish/>`_ because we publish/release libraries there and `consumers <https://central.sonatype.org/consume/>`_ because we pull down those libraries (and many others) when we build Dataverse. 

Releasing Existing Libraries to Maven Central
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If you need to release an existing library, all the setup should be done already. The steps below assume that GitHub Actions are in place to do the heavy lifting for you, such as signing artifacts with GPG.

Releasing a Snapshot Version to Maven Central
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

`Snapshot <https://maven.apache.org/guides/getting-started/index.html#what-is-a-snapshot-version>`_ releases are published automatically through GitHub Actions (e.g. through a `snapshot workflow <https://github.com/gdcc/sword2-server/blob/main/.github/workflows/maven-snapshot.yml>`_ for the SWORD library) every time a pull request is merged (or the default branch, typically ``main``, is otherwise updated).

That is to say, to make a snapshot release, you only need to get one or more commits into the default branch.

Releasing a Release (Non-Snapshot) Version to Maven Central
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

From a pom.xml it may not be apparent that snapshots like ``6.0-SNAPSHOT`` might be changing under your feet. Browsing the snapshot repository (e.g. our `UNF 6.0-SNAPSHOT <https://s01.oss.sonatype.org/content/groups/staging/org/dataverse/unf/6.0-SNAPSHOT/>`_), may reveal versions changing over time. To finalize the code and stop it from changing, we publish/release what Maven calls a "`release version <https://maven.apache.org/guides/getting-started/index.html#what-is-a-snapshot-version>`_". This will remove ``-SNAPSHOT`` from the version (through an ``mvn`` command).

Non-snapshot releases (`release <https://maven.apache.org/guides/getting-started/index.html#what-is-a-snapshot-version>`_ versions) are published automatically through GitHub Actions (e.g. through a `release workflow <https://github.com/gdcc/sword2-server/blob/main/.github/workflows/maven-release.yml>`_), kicked off locally by an ``mvn`` command that invokes the `Maven Release Plugin <https://maven.apache.org/maven-release/maven-release-plugin/>`_.

First, run a clean:

``mvn release:clean``

Then run a prepare:

``mvn release:prepare``

The prepare step is interactive. You will be prompted for the following information:

- the release version (e.g. `2.0.0 <https://repo.maven.apache.org/maven2/io/gdcc/sword2-server/2.0.0/>`_)
- the git tag to create and push (e.g. `sword2-server-2.0.0 <https://github.com/gdcc/sword2-server/releases/tag/sword2-server-2.0.0>`_)
- the next development (snapshot) version (e.g. `2.0.1-SNAPSHOT <https://s01.oss.sonatype.org/#nexus-search;checksum~47575aed5471adeb0a08a02098ce3a23a5778afb>`_)

These examples from the SWORD library. Below is what to expect from the interactive session. In many cases, you can just hit enter to accept the defaults.

.. code-block:: bash

        [INFO] 5/17 prepare:map-release-versions
        What is the release version for "SWORD v2 Common Server Library (forked)"? (sword2-server) 2.0.0: :
        [INFO] 6/17 prepare:input-variables
        What is the SCM release tag or label for "SWORD v2 Common Server Library (forked)"? (sword2-server) sword2-server-2.0.0: :
        [INFO] 7/17 prepare:map-development-versions
        What is the new development version for "SWORD v2 Common Server Library (forked)"? (sword2-server) 2.0.1-SNAPSHOT: :
        [INFO] 8/17 prepare:rewrite-poms-for-release

Note that a commit or two will be made and pushed but if you do a ``git status`` you will see that locally you are behind by that number of commits. To fix this, you can just do a ``git pull``.

It can take some time for the jar to be visible on Maven Central. You can start by looking on the repo1 server, like this: https://repo1.maven.org/maven2/io/gdcc/sword2-server/2.0.0/

Don't bother putting the new version in a pom.xml until you see it on repo1.

Note that the next snapshot release should be available as well, like this: https://s01.oss.sonatype.org/content/groups/staging/io/gdcc/sword2-server/2.0.1-SNAPSHOT/ 

Releasing a New Library to Maven Central
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

At a high level:

- Start with a snapshot release.
- Use an existing pom.xml as a starting point.
- Use existing GitHub Actions workflows as a starting point.
- Create secrets in the new library's GitHub repo used by the workflow.
- If you need an entire new namespace, look at previous issues such as https://issues.sonatype.org/browse/OSSRH-94575 and https://issues.sonatype.org/browse/OSSRH-94577

Updating pom.xml for a Snapshot Release
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Before publishing a final version to Maven Central, you should publish a snapshot release or two. For each snapshot release you publish, the jar name will be unique each time (e.g. ``foobar-0.0.1-20240430.175110-3.jar``), so you can safely publish over and over with the same version number.

We use the `Nexus Staging Maven Plugin <https://github.com/sonatype/nexus-maven-plugins/blob/main/staging/maven-plugin/README.md>`_ to push snapshot releases to https://s01.oss.sonatype.org/content/groups/staging/io/gdcc/ and https://s01.oss.sonatype.org/content/groups/staging/org/dataverse/

Add the following to your pom.xml:

.. code-block:: xml

    <version>0.0.1-SNAPSHOT</version>

    <distributionManagement>
        <snapshotRepository>
            <id>ossrh</id>
            <url>https://s01.oss.sonatype.org/content/repositories/snapshots</url>
        </snapshotRepository>
        <repository>
            <id>ossrh</id>
            <url>https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/</url>
        </repository>
    </distributionManagement>

   <plugin>
       <groupId>org.sonatype.plugins</groupId>
       <artifactId>nexus-staging-maven-plugin</artifactId>
       <version>${nexus-staging.version}</version>
       <extensions>true</extensions>
       <configuration>
           <serverId>ossrh</serverId>
           <nexusUrl>https://s01.oss.sonatype.org</nexusUrl>
           <autoReleaseAfterClose>true</autoReleaseAfterClose>
       </configuration>
   </plugin>

Configuring Secrets
~~~~~~~~~~~~~~~~~~~

In GitHub, you will likely need to configure the following secrets:

- DATAVERSEBOT_GPG_KEY
- DATAVERSEBOT_GPG_PASSWORD
- DATAVERSEBOT_SONATYPE_TOKEN
- DATAVERSEBOT_SONATYPE_USERNAME

Note that some of these secrets might be configured at the org level (e.g. gdcc or IQSS).

Many of the automated tasks are performed by the dataversebot account on GitHub: https://github.com/dataversebot

npm (JavaScript/TypeScript)
---------------------------

Currently, publishing `@iqss/dataverse-design-system <https://www.npmjs.com/package/@iqss/dataverse-design-system>`_ to npm done manually. We plan to automate this as part of https://github.com/IQSS/dataverse-frontend/issues/140

https://www.npmjs.com/package/js-dataverse is the previous 1.0 version of js-dataverse. No 1.x releases are planned. We plan to publish 2.0 (used by the new frontend) as discussed in https://github.com/IQSS/dataverse-frontend/issues/13
